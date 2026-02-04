#!/bin/bash
#
# Ralph Loop v2 - More Robust Version
#
# Improvements over v1:
# - Timeout per iteration (kills hung processes)
# - Skip spec after N consecutive failures
# - State file for monitoring
# - Better logging and progress tracking
#
# Usage:
#   ./scripts/ralph-loop-v2.sh              # Unlimited iterations
#   ./scripts/ralph-loop-v2.sh 20           # Max 20 iterations
#   ./scripts/ralph-loop-v2.sh --timeout 30 # 30 min timeout per iteration
#

set -e
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
LOG_DIR="$PROJECT_DIR/logs"
STATE_FILE="$PROJECT_DIR/.ralph-state"
BLOCKED_FILE="$PROJECT_DIR/.ralph-blocked"

# Configuration
MAX_ITERATIONS=0  # 0 = unlimited
ITERATION_TIMEOUT=2700  # 45 minutes in seconds (default)
MAX_CONSECUTIVE_FAILURES=3
CLAUDE_CMD="${CLAUDE_CMD:-claude}"
YOLO_FLAG="--dangerously-skip-permissions"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

mkdir -p "$LOG_DIR"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --timeout)
            ITERATION_TIMEOUT=$(($2 * 60))  # Convert minutes to seconds
            shift 2
            ;;
        -h|--help)
            cat <<EOF
Ralph Loop v2 - Robust autonomous development

Usage:
  ./scripts/ralph-loop-v2.sh              # Unlimited iterations
  ./scripts/ralph-loop-v2.sh 20           # Max 20 iterations
  ./scripts/ralph-loop-v2.sh --timeout 30 # 30 min timeout per iteration

Options:
  --timeout <minutes>   Max time per iteration (default: 45)
  -h, --help           Show this help

Files created:
  .ralph-state         Current status (for monitoring)
  .ralph-blocked       List of specs that failed too many times
  logs/                Detailed logs per iteration

Monitoring:
  watch cat .ralph-state          # See current status
  tail -f logs/ralph_*.log        # Follow current log
EOF
            exit 0
            ;;
        [0-9]*)
            MAX_ITERATIONS="$1"
            shift
            ;;
        *)
            echo -e "${RED}Unknown argument: $1${NC}"
            exit 1
            ;;
    esac
done

cd "$PROJECT_DIR"

# Initialize state
update_state() {
    local status="$1"
    local spec="$2"
    local iteration="$3"
    local extra="$4"
    cat > "$STATE_FILE" <<EOF
status=$status
spec=$spec
iteration=$iteration
timestamp=$(date '+%Y-%m-%d %H:%M:%S')
pid=$$
$extra
EOF
}

# Check if spec is blocked
is_blocked() {
    local spec="$1"
    if [ -f "$BLOCKED_FILE" ]; then
        grep -q "^$spec$" "$BLOCKED_FILE" 2>/dev/null && return 0
    fi
    return 1
}

# Mark spec as blocked
block_spec() {
    local spec="$1"
    echo "$spec" >> "$BLOCKED_FILE"
    echo -e "${RED}Blocked spec: $spec (too many failures)${NC}"
}

# Get current spec being worked on (parse from last log or git status)
get_current_spec() {
    # Find first PENDING spec that isn't blocked
    for spec_file in specs/*/spec.md; do
        if [ -f "$spec_file" ]; then
            spec_name=$(dirname "$spec_file" | xargs basename)
            if grep -q "## Status: PENDING" "$spec_file" && ! is_blocked "$spec_name"; then
                echo "$spec_name"
                return 0
            fi
        fi
    done
    echo "none"
}

# Run claude with timeout
run_with_timeout() {
    local prompt_file="$1"
    local log_file="$2"
    local timeout_secs="$3"

    # Start claude in background
    cat "$prompt_file" | timeout "$timeout_secs" "$CLAUDE_CMD" -p $YOLO_FLAG 2>&1 | tee "$log_file" &
    local pid=$!

    # Wait for completion
    wait $pid
    return $?
}

# Build prompt file
PROMPT_FILE="$PROJECT_DIR/PROMPT_build.md"
cat > "$PROMPT_FILE" << 'BUILDEOF'
# Ralph Build Mode v2

Based on Geoffrey Huntley's Ralph Wiggum methodology.

---

## Phase 0: Orient

Read `.specify/memory/constitution.md` to understand project principles and constraints.

---

## Phase 1: Discover Work Items

Search for incomplete work from these sources (in order):

1. **specs/ folder** — Look for `.md` files NOT marked `## Status: COMPLETE`
2. **IMPLEMENTATION_PLAN.md** — If exists, find unchecked `- [ ]` tasks

Pick the **HIGHEST PRIORITY** incomplete item:
- Lower numbers = higher priority (020 before 021)
- Specs marked BLOCKED should be skipped

Before implementing, search the codebase to verify it's not already done.

---

## Phase 2: Implement

Implement the selected spec completely:
- Follow the spec's requirements exactly
- Write clean, maintainable code
- Add tests as needed
- If you get stuck, focus on what IS possible and document blockers

---

## Phase 3: Validate

Run the project's test suite and verify:
- All tests pass (or document which fail and why)
- No lint errors
- The spec's acceptance criteria are met

---

## Phase 4: Commit & Update

1. Mark the spec as complete: change `## Status: PENDING` to `## Status: COMPLETE`
2. `git add -A`
3. `git commit` with a descriptive message
4. `git push`

---

## Completion Signal

**CRITICAL:** Only output the magic phrase when the work is 100% complete.

Check:
- [ ] Implementation matches all requirements
- [ ] All tests pass
- [ ] All acceptance criteria verified
- [ ] Changes committed and pushed
- [ ] Spec marked as complete

**If ALL checks pass, output:** `<promise>DONE</promise>`

**If ANY check fails:** Fix the issue and try again. Do NOT output the magic phrase.

**If you are STUCK and cannot proceed:** Output what you accomplished and what is blocking you.
Do NOT output `<promise>DONE</promise>` if the spec is incomplete.
BUILDEOF

# Check Claude CLI
if ! command -v "$CLAUDE_CMD" &> /dev/null; then
    echo -e "${RED}Error: Claude CLI not found${NC}"
    exit 1
fi

# Get current branch
CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "main")

# Count specs
TOTAL_SPECS=$(find specs -maxdepth 2 -name "spec.md" | wc -l)
PENDING_SPECS=$(grep -l "## Status: PENDING" specs/*/spec.md 2>/dev/null | wc -l || echo 0)
BLOCKED_SPECS=$(wc -l < "$BLOCKED_FILE" 2>/dev/null || echo 0)

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}           RALPH LOOP v2 (Robust Mode) STARTING              ${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}Branch:${NC}        $CURRENT_BRANCH"
echo -e "${BLUE}Timeout:${NC}       $((ITERATION_TIMEOUT / 60)) minutes per iteration"
echo -e "${BLUE}Max failures:${NC}  $MAX_CONSECUTIVE_FAILURES before skipping spec"
echo -e "${BLUE}Specs:${NC}         $PENDING_SPECS pending / $TOTAL_SPECS total"
[ "$BLOCKED_SPECS" -gt 0 ] && echo -e "${YELLOW}Blocked:${NC}       $BLOCKED_SPECS specs"
[ $MAX_ITERATIONS -gt 0 ] && echo -e "${BLUE}Max iters:${NC}     $MAX_ITERATIONS"
echo ""
echo -e "${CYAN}Monitor with: watch cat .ralph-state${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo ""

ITERATION=0
CONSECUTIVE_FAILURES=0
CURRENT_SPEC=""
SPECS_COMPLETED=0

# Trap for cleanup
cleanup() {
    update_state "stopped" "$CURRENT_SPEC" "$ITERATION" "reason=interrupted"
    echo ""
    echo -e "${YELLOW}Ralph stopped. Completed $SPECS_COMPLETED specs in $ITERATION iterations.${NC}"
    exit 0
}
trap cleanup SIGINT SIGTERM

while true; do
    # Check max iterations
    if [ $MAX_ITERATIONS -gt 0 ] && [ $ITERATION -ge $MAX_ITERATIONS ]; then
        echo -e "${GREEN}Reached max iterations: $MAX_ITERATIONS${NC}"
        break
    fi

    # Get current spec to work on
    CURRENT_SPEC=$(get_current_spec)
    if [ "$CURRENT_SPEC" = "none" ]; then
        echo -e "${GREEN}No more pending specs! All done.${NC}"
        update_state "complete" "all" "$ITERATION" "specs_completed=$SPECS_COMPLETED"
        break
    fi

    ITERATION=$((ITERATION + 1))
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

    echo ""
    echo -e "${PURPLE}════════════════════ ITERATION $ITERATION ════════════════════${NC}"
    echo -e "${BLUE}[$TIMESTAMP]${NC} Working on: ${CYAN}$CURRENT_SPEC${NC}"
    echo ""

    # Update state
    update_state "running" "$CURRENT_SPEC" "$ITERATION" "started=$(date +%s)"

    # Log file for this iteration
    LOG_FILE="$LOG_DIR/ralph_iter_${ITERATION}_${CURRENT_SPEC}_$(date '+%H%M%S').log"

    # Run Claude with timeout
    CLAUDE_EXIT_CODE=0
    CLAUDE_OUTPUT=""

    echo -e "${BLUE}Running Claude (timeout: $((ITERATION_TIMEOUT / 60)) min)...${NC}"

    if CLAUDE_OUTPUT=$(timeout "$ITERATION_TIMEOUT" bash -c "cat '$PROMPT_FILE' | '$CLAUDE_CMD' -p $YOLO_FLAG 2>&1" | tee "$LOG_FILE"); then
        CLAUDE_EXIT_CODE=0
    else
        CLAUDE_EXIT_CODE=$?
    fi

    # Check result
    if [ $CLAUDE_EXIT_CODE -eq 124 ]; then
        # Timeout
        echo -e "${RED}✗ Iteration timed out after $((ITERATION_TIMEOUT / 60)) minutes${NC}"
        CONSECUTIVE_FAILURES=$((CONSECUTIVE_FAILURES + 1))
        update_state "timeout" "$CURRENT_SPEC" "$ITERATION" "failures=$CONSECUTIVE_FAILURES"

    elif [ $CLAUDE_EXIT_CODE -ne 0 ]; then
        # Error
        echo -e "${RED}✗ Claude execution failed (exit code: $CLAUDE_EXIT_CODE)${NC}"
        CONSECUTIVE_FAILURES=$((CONSECUTIVE_FAILURES + 1))
        update_state "error" "$CURRENT_SPEC" "$ITERATION" "failures=$CONSECUTIVE_FAILURES"

    elif echo "$CLAUDE_OUTPUT" | grep -qE "<promise>(ALL_)?DONE</promise>"; then
        # Success!
        echo -e "${GREEN}✓ Spec completed: $CURRENT_SPEC${NC}"
        CONSECUTIVE_FAILURES=0
        SPECS_COMPLETED=$((SPECS_COMPLETED + 1))
        update_state "completed_spec" "$CURRENT_SPEC" "$ITERATION" "specs_completed=$SPECS_COMPLETED"

        # Push changes
        git push origin "$CURRENT_BRANCH" 2>/dev/null || true

    else
        # No completion signal
        echo -e "${YELLOW}⚠ No completion signal - spec may be incomplete${NC}"
        CONSECUTIVE_FAILURES=$((CONSECUTIVE_FAILURES + 1))
        update_state "incomplete" "$CURRENT_SPEC" "$ITERATION" "failures=$CONSECUTIVE_FAILURES"
    fi

    # Check if we should skip this spec
    if [ $CONSECUTIVE_FAILURES -ge $MAX_CONSECUTIVE_FAILURES ]; then
        echo ""
        echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${RED}  $CURRENT_SPEC failed $MAX_CONSECUTIVE_FAILURES times - SKIPPING${NC}"
        echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo ""

        block_spec "$CURRENT_SPEC"
        CONSECUTIVE_FAILURES=0

        # Check remaining specs
        REMAINING=$(get_current_spec)
        if [ "$REMAINING" = "none" ]; then
            echo -e "${YELLOW}No more specs to try. Some are blocked.${NC}"
            echo -e "${YELLOW}Blocked specs: $(cat "$BLOCKED_FILE" 2>/dev/null | tr '\n' ' ')${NC}"
            break
        fi
    fi

    # Brief pause
    echo ""
    echo -e "${BLUE}Next iteration in 5s...${NC}"
    sleep 5
done

# Final summary
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}                    RALPH LOOP FINISHED                       ${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}Iterations:${NC}      $ITERATION"
echo -e "${GREEN}Specs completed:${NC} $SPECS_COMPLETED"
if [ -f "$BLOCKED_FILE" ] && [ -s "$BLOCKED_FILE" ]; then
    echo -e "${RED}Specs blocked:${NC}   $(wc -l < "$BLOCKED_FILE")"
    echo -e "${RED}  $(cat "$BLOCKED_FILE" | tr '\n' ' ')${NC}"
fi
echo ""

update_state "finished" "all" "$ITERATION" "specs_completed=$SPECS_COMPLETED"
