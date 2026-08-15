import { useState } from 'react';
import { AlertTriangle, Bell, CheckCircle2, ChevronLeft, ChevronRight, Info } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { useNotifications } from '@/hooks/useNotifications';
import { useUnreadCount } from '@/hooks/useUnreadCount';
import type { NotificationResponse, NotificationType } from '@/types/notification';

type ReadFilter = 'ALL' | 'UNREAD' | 'READ';

const TYPE_ICON: Record<NotificationType, typeof Bell> = {
  ALERT: AlertTriangle,
  TASK: CheckCircle2,
  INFO: Info,
};

const TYPE_STYLE: Record<NotificationType, string> = {
  ALERT: 'bg-error-bg text-destructive',
  TASK: 'bg-warning-bg text-warning',
  INFO: 'bg-info-bg text-info',
};

const formatDateTime = (iso: string) => {
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return iso;
  }
};

const NotificationsPage = () => {
  const [filter, setFilter] = useState<ReadFilter>('ALL');
  const isRead = filter === 'ALL' ? undefined : filter === 'READ';

  const { items, page, totalPages, isLoading, load, markAsRead } = useNotifications({
    size: 20,
    isRead,
  });
  const { refresh: refreshUnreadCount } = useUnreadCount();

  const handleItemClick = (notification: NotificationResponse) => {
    if (!notification.isRead) {
      void markAsRead(notification.id).then(() => refreshUnreadCount());
    }
  };

  return (
    <div className="container mx-auto max-w-3xl space-y-6 py-8">
      <div>
        <h1 className="text-2xl font-bold">Thông báo</h1>
        <p className="text-sm text-muted-foreground">
          Danh sách việc cần làm và cảnh báo liên quan đến tài khoản của bạn.
        </p>
      </div>

      <div className="flex gap-2">
        {(
          [
            { value: 'ALL', label: 'Tất cả' },
            { value: 'UNREAD', label: 'Chưa đọc' },
            { value: 'READ', label: 'Đã đọc' },
          ] as const
        ).map((option) => (
          <Button
            key={option.value}
            type="button"
            size="sm"
            variant={filter === option.value ? 'default' : 'outline'}
            onClick={() => setFilter(option.value)}
          >
            {option.label}
          </Button>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Danh sách thông báo</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex justify-center py-12">
              <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-primary" />
            </div>
          ) : items.length === 0 ? (
            <div className="px-4 py-16 text-center text-muted-foreground">
              <Bell className="mx-auto mb-3 h-10 w-10 text-muted-foreground/50" />
              <p className="font-medium">Chưa có thông báo nào</p>
            </div>
          ) : (
            <ul className="divide-y">
              {items.map((item) => {
                const Icon = TYPE_ICON[item.type];
                return (
                  <li key={item.id}>
                    <button
                      type="button"
                      onClick={() => handleItemClick(item)}
                      className={cn(
                        'flex w-full items-start gap-3 px-4 py-4 text-left transition-colors hover:bg-muted',
                        !item.isRead && 'bg-success-bg/50',
                      )}
                    >
                      <span
                        className={cn(
                          'mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full',
                          TYPE_STYLE[item.type],
                        )}
                      >
                        <Icon className="h-4 w-4" />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="flex items-center gap-1.5">
                          <span className="font-medium text-foreground">{item.title}</span>
                          {!item.isRead && (
                            <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                          )}
                        </span>
                        <span className="mt-1 block text-sm text-muted-foreground">
                          {item.content}
                        </span>
                        <span className="mt-1.5 block text-xs text-muted-foreground/70">
                          {formatDateTime(item.createdAt)}
                          {item.isRead && item.readAt && ' · Đã đọc'}
                        </span>
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          {!isLoading && totalPages > 1 && (
            <div className="flex items-center justify-between border-t px-4 py-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => void load(page - 1)}
                disabled={page === 0}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm text-muted-foreground">
                Trang {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => void load(page + 1)}
                disabled={page >= totalPages - 1}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default NotificationsPage;