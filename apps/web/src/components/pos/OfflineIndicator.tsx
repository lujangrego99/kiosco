'use client';

import { useOnlineStatus } from '@/hooks/useOnlineStatus';
import { Wifi, WifiOff, RefreshCw, CloudOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

export function OfflineIndicator() {
  const { isOnline, syncStatus, pendingVentas, lastSyncAt, forceSync } = useOnlineStatus();

  const formatLastSync = (timestamp: number | null): string => {
    if (!timestamp) return 'Nunca';
    const diff = Date.now() - timestamp;
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'Ahora';
    if (minutes < 60) return `Hace ${minutes}m`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `Hace ${hours}h`;
    return `Hace ${Math.floor(hours / 24)}d`;
  };

  const handleSync = async () => {
    if (syncStatus === 'syncing') return;
    await forceSync();
  };

  if (!isOnline) {
    return (
      <div className="flex items-center gap-2 px-3 py-1.5 bg-orange-100 dark:bg-orange-950 text-orange-700 dark:text-orange-300 rounded-lg text-sm">
        <WifiOff className="h-4 w-4" />
        <span>Offline</span>
        {pendingVentas > 0 && (
          <span className="px-1.5 py-0.5 bg-orange-200 dark:bg-orange-900 rounded text-xs font-medium">
            {pendingVentas} pendiente{pendingVentas !== 1 ? 's' : ''}
          </span>
        )}
      </div>
    );
  }

  if (syncStatus === 'syncing') {
    return (
      <div className="flex items-center gap-2 px-3 py-1.5 bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 rounded-lg text-sm">
        <RefreshCw className="h-4 w-4 animate-spin" />
        <span>Sincronizando...</span>
      </div>
    );
  }

  if (syncStatus === 'error') {
    return (
      <Button
        variant="ghost"
        size="sm"
        onClick={handleSync}
        className="flex items-center gap-2 px-3 py-1.5 bg-red-100 dark:bg-red-950 text-red-700 dark:text-red-300 hover:bg-red-200 dark:hover:bg-red-900 rounded-lg text-sm h-auto"
      >
        <CloudOff className="h-4 w-4" />
        <span>Error - Reintentar</span>
      </Button>
    );
  }

  // Online and idle
  return (
    <Button
      variant="ghost"
      size="sm"
      onClick={handleSync}
      className={cn(
        'flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm h-auto',
        pendingVentas > 0
          ? 'bg-yellow-100 dark:bg-yellow-950 text-yellow-700 dark:text-yellow-300 hover:bg-yellow-200 dark:hover:bg-yellow-900'
          : 'bg-green-100 dark:bg-green-950 text-green-700 dark:text-green-300 hover:bg-green-200 dark:hover:bg-green-900'
      )}
    >
      <Wifi className="h-4 w-4" />
      <span>Online</span>
      {pendingVentas > 0 && (
        <span className="px-1.5 py-0.5 bg-yellow-200 dark:bg-yellow-900 rounded text-xs font-medium">
          {pendingVentas}
        </span>
      )}
      <span className="text-xs opacity-70">{formatLastSync(lastSyncAt)}</span>
    </Button>
  );
}
