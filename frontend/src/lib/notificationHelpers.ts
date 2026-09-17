import type { NotificationResponse } from '@/types/notification';

/**
 * Nhận diện thông báo cảnh báo khóa truy cập (NCL-12-CN-005).
 * <p>
 * Backend gửi type ALERT kèm entityId là ID khóa; tiêu đề luôn chứa
 * "Khóa truy cập" (3 hằng tiêu đề của ApiKeyWarningService).
 * Các thông báo này bấm vào chỉ ghi đã đọc, không điều hướng sang
 * màn hình khác vì entityId của chúng không phải phiếu bàn giao.
 */
export const isApiKeyWarningNotification = (
  notification: NotificationResponse,
): boolean => {
  return (
    notification.type === 'ALERT' &&
    notification.entityId !== null &&
    notification.title.toLowerCase().includes('khóa truy cập')
  );
};
