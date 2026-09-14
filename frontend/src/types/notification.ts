export type NotificationType =
  | 'ALERT'
  | 'TASK'
  | 'INFO'
  | 'LOGIN_ANOMALY_DETECTED'
  | 'ACCOUNT_LOCKED'
  | 'ANOMALY_OPEN'
  | 'ANOMALY_DISMISSED'
  | 'ACCOUNT_UNLOCKED';

export interface NotificationResponse {
  id: string;
  type: NotificationType;
  title: string;
  content: string;
  /**
   * ID thực thể nghiệp vụ liên kết (VD: phiếu bàn giao lô hàng).
   * Null khi thông báo không có thực thể đính kèm.
   */
  entityId: string | null;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationListResponse {
  items: NotificationResponse[];
  page: number;
  size: number;
  totalElements: number;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export interface MarkReadResponse {
  id: string;
  isRead: boolean;
  readAt: string | null;
}

export interface GetNotificationsParams {
  page?: number;
  size?: number;
  isRead?: boolean;
}