import { useNavigate } from 'react-router-dom';
import { KeyRound, Clock, Gauge } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { DetailSection } from '@/components/common/detail/DetailSection';
import { DetailField } from '@/components/common/detail/DetailField';
import type { NotificationResponse } from '@/types/notification';

interface Props {
  notification: NotificationResponse | null;
  onClose: () => void;
  onMarkAsRead: (id: string) => void;
}

/**
 * Nhận diện thông báo cảnh báo khóa truy cập (NCL-12-CN-005).
 * BE gửi type ALERT kèm entityId là ID khóa; tiêu đề luôn chứa "Khóa truy cập".
 */
export const isApiKeyWarningNotification = (notification: NotificationResponse): boolean => {
  return (
    notification.type === 'ALERT' &&
    notification.entityId !== null &&
    notification.title.toLowerCase().includes('khóa truy cập')
  );
};

const formatDateTime = (value: string | null) => {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return value;
  }
};

/**
 * Popup chi tiết cảnh báo khóa truy cập: chỉ hiển thị thông báo, không tạo trang mới.
 * Nút duy nhất điều hướng là "Xem khóa" (deep-link tới trang quản trị khóa sẵn có).
 */
export function NotificationDetailDialog({ notification, onClose, onMarkAsRead }: Props) {
  const navigate = useNavigate();
  const isQuota = (notification?.title || '').toLowerCase().includes('hạn mức');
  const Icon = isQuota ? Gauge : Clock;

  const handleViewKey = () => {
    if (notification && !notification.isRead) {
      onMarkAsRead(notification.id);
    }
    onClose();
    navigate('/integration/api-keys');
  };

  return (
    <Dialog open={Boolean(notification)} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-lg">
        {notification && (
          <>
            <DialogHeader>
              <div className="flex items-start gap-3 pr-8">
                <div className="rounded-full bg-orange-100 p-2 text-orange-700">
                  <Icon className="h-5 w-5" />
                </div>
                <div className="space-y-1">
                  <DialogTitle>{notification.title}</DialogTitle>
                  <DialogDescription>Thông báo lúc {formatDateTime(notification.createdAt)}</DialogDescription>
                </div>
              </div>
            </DialogHeader>

            <DetailSection title="Nội dung cảnh báo">
              <p className="text-sm leading-relaxed text-foreground">{notification.content}</p>
            </DetailSection>

            <DetailSection title="Thông tin" contentClassName="grid gap-3 sm:grid-cols-2">
              <DetailField
                label="Trạng thái"
                value={notification.isRead ? `Đã đọc${notification.readAt ? ` · ${formatDateTime(notification.readAt)}` : ''}` : 'Chưa đọc'}
              />
              <DetailField label="Kênh xử lý" value="Trang quản trị khóa API đối tác" />
            </DetailSection>

            <DialogFooter className="gap-2 sm:gap-2">
              {!notification.isRead && (
                <Button type="button" variant="outline" onClick={() => onMarkAsRead(notification.id)}>
                  Đánh dấu đã đọc
                </Button>
              )}
              <Button type="button" onClick={handleViewKey}>
                <KeyRound className="mr-1.5 h-4 w-4" />
                Xem khóa
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
