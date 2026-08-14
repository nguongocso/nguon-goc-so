import React, { useState, useEffect } from 'react';
import { getOfflineEvents, removeOfflineEvent, clearOfflineQueue } from '@/services/offlineQueue';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { toast } from 'sonner';
import { useOfflineSync } from '@/hooks/useOfflineSync';
import { RefreshCw, Trash2, X, Info } from 'lucide-react';
import type { OfflineEvent } from '@/types/offlineEvent';

// Component hiển thị một event
const EventItem: React.FC<{
  event: OfflineEvent;
  onDelete: (id: string) => void;
  onRetry: (id: string) => void;
  isSyncing: boolean;
}> = ({ event, onDelete, onRetry, isSyncing }) => {
  const lotName = 'Không xác định';

  useEffect(() => {
    // ... lấy tên lô (giữ nguyên)
  }, [event.productionLotId]);

  // ✅ Map trạng thái sang Badge
  const statusConfig: Record<string, { label: string; variant: 'secondary' | 'default' | 'destructive' | 'outline' }> = {
    pending: { label: 'Chờ', variant: 'secondary' },
    syncing: { label: 'Đang đồng bộ', variant: 'default' },
    failed: { label: 'Thất bại', variant: 'destructive' },
    success: { label: 'Thành công', variant: 'default' },
    invalid: { label: 'Không hợp lệ', variant: 'outline' },
  };
  const currentStatus = event.status || 'pending';
  const config = statusConfig[currentStatus];

  return (
    <div className="flex items-start justify-between border-b pb-2 pt-2">
      <div className="flex-1">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-medium">{event.eventType}</span>
          <Badge variant="outline" className="text-xs">
            {event.deviceSource || 'MOBILE'}
          </Badge>
          {/* ✅ Badge trạng thái */}
          <Badge variant={config.variant} className="text-xs">
            {config.label}
          </Badge>
        </div>
        <div className="text-sm text-muted-foreground">
          Lô: {lotName} (ID: {event.productionLotId?.slice(0, 8) ?? 'N/A'})
        </div>
        <div className="text-xs text-muted-foreground">
          Ghi lúc: {new Date(event.recordedAt).toLocaleString()}
        </div>
        <div className="text-xs text-muted-foreground mt-1">
          Dữ liệu: {Object.keys(event.eventData).join(', ')}
        </div>
        {/* ✅ Hiển thị lỗi nếu có */}
        {event.errorMessage && (
          <div className="text-xs text-red-500 mt-1">
            ❌ {event.errorMessage}
          </div>
        )}
      </div>
      <div className="flex flex-col items-end gap-1">
        <div className="flex gap-1">
          {/* Chỉ hiển thị nút retry khi thất bại */}
          {event.status === 'failed' && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onRetry(event.offlineEventId)}
              disabled={isSyncing}
              title="Thử lại"
            >
              <RefreshCw className="h-4 w-4" />
            </Button>
          )}
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onDelete(event.offlineEventId)}
            className="text-red-500 hover:text-red-700"
            disabled={isSyncing}
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
};

export const OfflineEventList: React.FC = () => {
  const [events, setEvents] = useState<OfflineEvent[]>(getOfflineEvents());
  const { sync, isSyncing } = useOfflineSync();

  const refreshList = () => {
    setEvents(getOfflineEvents());
  };

  // Tự động refresh mỗi 3 giây (có thể dùng event listener)
  useEffect(() => {
    const interval = setInterval(refreshList, 3000);
    return () => clearInterval(interval);
  }, []);

  const handleDelete = (id: string) => {
    if (window.confirm('Bạn có chắc muốn xóa sự kiện này khỏi hàng chờ?')) {
      removeOfflineEvent(id);
      refreshList();
      toast.info('Đã xóa sự kiện.');
    }
  };

  const handleRetry = async (_id: string) => {
    // Chỉ đồng bộ lại duy nhất một sự kiện? 
    // Cách đơn giản là gọi sync() toàn bộ, nhưng nếu muốn retry riêng thì cần xây dựng logic riêng.
    // Ở đây ta chỉ gọi sync() toàn bộ.
    toast.info('Đang thử đồng bộ lại tất cả sự kiện...');
    await sync();
    refreshList();
  };

  const handleClearAll = () => {
    if (window.confirm('Xóa tất cả sự kiện đang chờ?')) {
      clearOfflineQueue();
      refreshList();
      toast.info('Đã xóa toàn bộ hàng chờ.');
    }
  };

  if (events.length === 0) {
    return (
      <Card>
        <CardContent className="py-8 text-center text-muted-foreground">
          <Info className="h-8 w-8 mx-auto mb-2 text-muted-foreground/50" />
          <p>Không có sự kiện nào đang chờ đồng bộ.</p>
          <p className="text-xs">Khi mất mạng, sự kiện sẽ được lưu tạm và xuất hiện ở đây.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between flex-wrap gap-2">
          <CardTitle className="flex items-center gap-2">
            <span>📋 Sự kiện đang chờ</span>
            <Badge variant="default" className="ml-2">{events.length}</Badge>
          </CardTitle>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={sync} disabled={isSyncing}>
              <RefreshCw className={`h-4 w-4 mr-1 ${isSyncing ? 'animate-spin' : ''}`} />
              {isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ tất cả'}
            </Button>
            <Button variant="destructive" size="sm" onClick={handleClearAll}>
              <X className="h-4 w-4 mr-1" />
              Xóa tất cả
            </Button>
          </div>
        </div>
        <p className="text-sm text-muted-foreground">
          Các sự kiện chưa được ghi lên server do mất mạng hoặc lỗi. Nhấn "Đồng bộ tất cả" để thử lại.
        </p>
      </CardHeader>
      <CardContent>
        <div className="space-y-1 max-h-96 overflow-y-auto pr-2">
          {events.map((evt) => (
            <EventItem
              key={evt.offlineEventId}
              event={evt}
              onDelete={handleDelete}
              onRetry={handleRetry}
              isSyncing={isSyncing}
            />
          ))}
        </div>
      </CardContent>
    </Card>
  );
};