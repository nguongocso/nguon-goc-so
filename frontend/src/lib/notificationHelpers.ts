import type { NotificationResponse } from '@/types/notification';

/** Nhận diện thông báo cảnh báo khóa truy cập. */
export const isApiKeyWarningNotification = (
  notification: NotificationResponse,
): boolean => {
  return (
    notification.type === 'ALERT' &&
    notification.entityId !== null &&
    notification.title.toLowerCase().includes('khóa truy cập')
  );
};
