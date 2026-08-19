import React from 'react';
import { useOfflineSync } from '@/hooks/useOfflineSync';
import { Badge } from '@/components/ui/badge';
import { CloudOff, RefreshCw } from 'lucide-react';

export const OfflineStatusBadge: React.FC = () => {
  const { isOnline, pendingCount, isSyncing, sync } = useOfflineSync();

  if (isOnline && pendingCount === 0) return null;

  return (
    <Badge
      variant={isOnline ? 'default' : 'destructive'}
      className="flex items-center gap-1 cursor-pointer"
      onClick={() => isOnline && pendingCount > 0 && sync()}
    >
      {!isOnline ? (
        <>
          <CloudOff className="h-3 w-3" />
          Ngoại tuyến
        </>
      ) : pendingCount > 0 && (
        <>
          <RefreshCw className={`h-3 w-3 ${isSyncing ? 'animate-spin' : ''}`} />
          {pendingCount} sự kiện chờ đồng bộ
        </>
      )}
    </Badge>
  );
};