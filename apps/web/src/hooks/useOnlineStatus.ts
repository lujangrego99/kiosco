'use client';

import { useState, useEffect, useCallback } from 'react';
import { syncService } from '@/lib/sync';
import { getLastSyncTime } from '@/lib/db';

type SyncStatus = 'idle' | 'syncing' | 'error';

interface OnlineStatus {
  isOnline: boolean;
  syncStatus: SyncStatus;
  pendingVentas: number;
  errorCount: number;
  lastSyncAt: number | null;
  forceSync: () => Promise<void>;
}

export function useOnlineStatus(): OnlineStatus {
  const [isOnline, setIsOnline] = useState(true);
  const [syncStatus, setSyncStatus] = useState<SyncStatus>('idle');
  const [pendingVentas, setPendingVentas] = useState(0);
  const [errorCount, setErrorCount] = useState(0);
  const [lastSyncAt, setLastSyncAt] = useState<number | null>(null);

  useEffect(() => {
    // Set initial online status
    setIsOnline(typeof navigator !== 'undefined' ? navigator.onLine : true);

    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  useEffect(() => {
    // Subscribe to sync status changes
    const unsubscribe = syncService.onStatusChange((status, pending) => {
      setSyncStatus(status);
      setPendingVentas(pending);
      // Also update error count when status changes
      syncService.getErrorCount().then(setErrorCount);
    });

    // Initial error count
    syncService.getErrorCount().then(setErrorCount);

    return unsubscribe;
  }, []);

  useEffect(() => {
    // Load last sync time
    getLastSyncTime().then(setLastSyncAt);

    // Update last sync time periodically
    const interval = setInterval(() => {
      getLastSyncTime().then(setLastSyncAt);
    }, 10000);

    return () => clearInterval(interval);
  }, []);

  const forceSync = useCallback(async () => {
    await syncService.fullSync();
    const newLastSync = await getLastSyncTime();
    setLastSyncAt(newLastSync);
  }, []);

  return {
    isOnline,
    syncStatus,
    pendingVentas,
    errorCount,
    lastSyncAt,
    forceSync,
  };
}
