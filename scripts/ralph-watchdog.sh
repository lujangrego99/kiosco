#!/bin/bash
#
# Ralph Watchdog - Monitors and restarts Ralph if it hangs
#
# Usage:
#   ./scripts/ralph-watchdog.sh              # Run with default settings
#   ./scripts/ralph-watchdog.sh --max-idle 60  # Restart if no progress in 60 min
#
# This script:
# 1. Starts ralph-loop-v2.sh
# 2. Monitors .ralph-state for activity
# 3. Restarts if no progress for MAX_IDLE_MINUTES
# 4. Stops after MAX_RESTARTS or when all specs complete
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
STATE_FILE="$PROJECT_DIR/.ralph-state"
WATCHDOG_LOG="$PROJECT_DIR/logs/watchdog.log"

# Configuration
MAX_IDLE_MINUTES=60      # Restart if no state update for this long
MAX_RESTARTS=5           # Give up after this many restarts
CHECK_INTERVAL=300       # Check every 5 minutes

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --max-idle)
            MAX_IDLE_MINUTES="$2"
            shift 2
            ;;
        --max-restarts)
            MAX_RESTARTS="$2"
            shift 2
            ;;
        -h|--help)
            cat <<EOF
Ralph Watchdog - Monitors and auto-restarts Ralph

Usage:
  ./scripts/ralph-watchdog.sh                    # Default settings
  ./scripts/ralph-watchdog.sh --max-idle 60      # Restart if idle for 60 min
  ./scripts/ralph-watchdog.sh --max-restarts 10  # Max 10 restarts

Options:
  --max-idle <minutes>     Minutes without progress before restart (default: 60)
  --max-restarts <count>   Max restart attempts (default: 5)

The watchdog monitors .ralph-state and restarts ralph-loop-v2.sh if:
- The process dies unexpectedly
- No state update for MAX_IDLE_MINUTES (process hung)
EOF
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown argument: $1${NC}"
            exit 1
            ;;
    esac
done

cd "$PROJECT_DIR"
mkdir -p "$(dirname "$WATCHDOG_LOG")"

log() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] $1"
    echo -e "$msg"
    echo "$msg" >> "$WATCHDOG_LOG"
}

RALPH_PID=""
RESTART_COUNT=0
LAST_STATE_MTIME=0

start_ralph() {
    log "${GREEN}Starting Ralph...${NC}"
    ./scripts/ralph-loop-v2.sh &
    RALPH_PID=$!
    log "${BLUE}Ralph started with PID: $RALPH_PID${NC}"
    sleep 10  # Give it time to initialize
}

stop_ralph() {
    if [ -n "$RALPH_PID" ] && kill -0 "$RALPH_PID" 2>/dev/null; then
        log "${YELLOW}Stopping Ralph (PID: $RALPH_PID)...${NC}"
        kill "$RALPH_PID" 2>/dev/null || true
        sleep 5
        kill -9 "$RALPH_PID" 2>/dev/null || true
    fi
    # Also kill any orphaned claude processes
    pkill -f "claude.*-p.*--dangerously-skip-permissions" 2>/dev/null || true
}

check_health() {
    # Check if Ralph process is alive
    if [ -z "$RALPH_PID" ] || ! kill -0 "$RALPH_PID" 2>/dev/null; then
        log "${RED}Ralph process died!${NC}"
        return 1
    fi

    # Check if state file is being updated
    if [ -f "$STATE_FILE" ]; then
        local current_mtime
        current_mtime=$(stat -c %Y "$STATE_FILE" 2>/dev/null || stat -f %m "$STATE_FILE" 2>/dev/null || echo 0)

        if [ "$LAST_STATE_MTIME" -eq 0 ]; then
            LAST_STATE_MTIME=$current_mtime
            return 0
        fi

        if [ "$current_mtime" -eq "$LAST_STATE_MTIME" ]; then
            local idle_seconds=$(($(date +%s) - current_mtime))
            local idle_minutes=$((idle_seconds / 60))

            if [ $idle_minutes -ge $MAX_IDLE_MINUTES ]; then
                log "${RED}No state update for $idle_minutes minutes - Ralph may be hung${NC}"
                return 1
            fi
        else
            LAST_STATE_MTIME=$current_mtime
        fi

        # Check if completed
        if grep -q "status=complete\|status=finished" "$STATE_FILE" 2>/dev/null; then
            log "${GREEN}Ralph completed all work!${NC}"
            return 2  # Special code for "done"
        fi
    fi

    return 0
}

cleanup() {
    log "${YELLOW}Watchdog stopping...${NC}"
    stop_ralph
    exit 0
}
trap cleanup SIGINT SIGTERM

# Main watchdog loop
log "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
log "${GREEN}              RALPH WATCHDOG STARTING                         ${NC}"
log "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
log "${BLUE}Max idle time: ${MAX_IDLE_MINUTES} minutes${NC}"
log "${BLUE}Max restarts: ${MAX_RESTARTS}${NC}"
log "${BLUE}Check interval: $((CHECK_INTERVAL / 60)) minutes${NC}"
log ""

start_ralph

while true; do
    sleep $CHECK_INTERVAL

    check_health
    health_status=$?

    if [ $health_status -eq 2 ]; then
        # Completed
        log "${GREEN}All specs completed! Watchdog exiting.${NC}"
        break
    elif [ $health_status -eq 1 ]; then
        # Unhealthy - restart
        RESTART_COUNT=$((RESTART_COUNT + 1))

        if [ $RESTART_COUNT -gt $MAX_RESTARTS ]; then
            log "${RED}Max restarts ($MAX_RESTARTS) exceeded. Giving up.${NC}"
            log "${RED}Check logs in $PROJECT_DIR/logs/${NC}"
            stop_ralph
            exit 1
        fi

        log "${YELLOW}Restarting Ralph (attempt $RESTART_COUNT/$MAX_RESTARTS)...${NC}"
        stop_ralph
        sleep 10
        LAST_STATE_MTIME=0
        start_ralph
    else
        # Healthy
        if [ -f "$STATE_FILE" ]; then
            local current_spec
            current_spec=$(grep "^spec=" "$STATE_FILE" 2>/dev/null | cut -d= -f2 || echo "unknown")
            local current_status
            current_status=$(grep "^status=" "$STATE_FILE" 2>/dev/null | cut -d= -f2 || echo "unknown")
            log "${BLUE}Health check OK - Status: $current_status, Spec: $current_spec${NC}"
        fi
    fi
done

log "${GREEN}Watchdog finished. Restarts: $RESTART_COUNT${NC}"
