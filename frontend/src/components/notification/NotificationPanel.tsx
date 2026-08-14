import { Link } from 'react-router-dom';
import { AlertTriangle, Bell, CheckCircle2, Info } from 'lucide-react';
import type { NotificationResponse, NotificationType } from '@/types/notification';
import { cn } from '@/lib/utils';

interface NotificationPanelProps {
  items: NotificationResponse[];
  isLoading: boolean;
  onItemClick: (notification: NotificationResponse) => void;
}

const TYPE_ICON: Record<NotificationType, typeof Bell> = {
  ALERT: AlertTriangle,
  TASK: CheckCircle2,
  INFO: Info,
};

const TYPE_STYLE: Record<NotificationType, string> = {
  ALERT: 'bg-error-bg text-destructive',
  TASK: 'bg-warning-bg text-status-pending',
  INFO: 'bg-info-bg text-info',
};

const formatRelativeTime = (iso: string) => {
  const date = new Date(iso);
  const diffMs = Date.now() - date.getTime();
  const diffMin = Math.floor(diffMs / 60_000);

  if (diffMin < 1) return 'Vừa xong';
  if (diffMin < 60) return `${diffMin} phút trước`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour} giờ trước`;
  const diffDay = Math.floor(diffHour / 24);
  if (diffDay < 7) return `${diffDay} ngày trước`;

  return date.toLocaleDateString('vi-VN');
};

export const NotificationPanel = ({
  items,
  isLoading,
  onItemClick,
}: NotificationPanelProps) => {
  return (
    <div className="w-80 max-w-[90vw]">
      <div className="flex items-center justify-between border-b px-3 py-2.5">
        <p className="text-sm font-semibold">Thông báo</p>
      </div>

      <div className="max-h-96 overflow-y-auto">
        {isLoading ? (
          <div className="flex justify-center py-8">
            <div className="h-5 w-5 animate-spin rounded-full border-b-2 border-primary" />
          </div>
        ) : items.length === 0 ? (
          <div className="px-4 py-10 text-center text-muted-foreground">
            <Bell className="mx-auto mb-2 h-8 w-8 text-muted-foreground/50" />
            <p className="text-sm">Chưa có thông báo nào</p>
          </div>
        ) : (
          <ul className="divide-y">
            {items.map((item) => {
              const Icon = TYPE_ICON[item.type];
              return (
                <li key={item.id}>
                  <button
                    type="button"
                    onClick={() => onItemClick(item)}
                    className={cn(
                      'flex w-full items-start gap-3 px-3 py-3 text-left transition-colors hover:bg-muted',
                      !item.isRead && 'bg-success-bg/50',
                    )}
                  >
                    <span
                      className={cn(
                        'mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full',
                        TYPE_STYLE[item.type],
                      )}
                    >
                      <Icon className="h-3.5 w-3.5" />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="flex items-center gap-1.5">
                        <span className="truncate text-sm font-medium text-foreground">
                          {item.title}
                        </span>
                        {!item.isRead && (
                          <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                        )}
                      </span>
                      <span className="mt-0.5 block line-clamp-2 text-xs text-muted-foreground">
                        {item.content}
                      </span>
                      <span className="mt-1 block text-xs text-muted-foreground/70">
                        {formatRelativeTime(item.createdAt)}
                      </span>
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      <div className="border-t px-3 py-2">
        <Link
          to="/notifications"
          className="block text-center text-sm font-medium text-primary hover:text-primary-hover"
        >
          Xem tất cả
        </Link>
      </div>
    </div>
  );
};