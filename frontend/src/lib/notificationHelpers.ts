import type { NotificationResponse } from '@/types/notification';

/**
 * Nhận diện thông báo kết quả đồng bộ nhật ký canh tác ghi khi ngoại tuyến
 */
export const isFarmLogSyncNotification = (
  notification: NotificationResponse,
): boolean => {
  return (
    notification.type === 'FARM_LOG_SYNC_SUCCESS' ||
    notification.type === 'FARM_LOG_SYNC_FAILED'
  );
};

/**
 * Đường dẫn điều hướng khi bấm vào thông báo (null: không điều hướng).
 */
export const resolveNotificationTarget = (
  notification: NotificationResponse,
): string | null => {
  if (isApiKeyWarningNotification(notification)) {
    const action = notification.title.toLowerCase().includes('hạn mức') ? 'quota' : 'renew';
    return notification.entityId
      ? `/integration/api-keys?keyId=${notification.entityId}&action=${action}`
      : '/integration/api-keys';
  }

  if (notification.type === 'ACTIVITY_LOG_EXPORT_READY' && notification.entityId) {
    return `/activity-logs?exportJobId=${notification.entityId}`;
  }

  // Thông báo đồng bộ nhật ký canh tác ngoại tuyến: về danh sách lô sản xuất
  if (isFarmLogSyncNotification(notification)) {
    return '/production-lots';
  }

  if (notification.entityId) {
    return `/shipment-handovers/${notification.entityId}`;
  }

  // NCL-11-CN-004: Điều hướng tới danh sách lô sản xuất khi thông báo liên quan đến kiểm nghiệm
  const text = `${notification.title} ${notification.content}`.toLowerCase();
  if (text.includes('kiểm nghiệm') || text.includes('lô sản xuất')) {
    return '/production-lots';
  }

  return null;
};

/**
 * Nhận diện thông báo cảnh báo khóa truy cập (NCL-12-CN-005).
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
