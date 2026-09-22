import apiClient from './axiosConfig';
import type { ApiResponse } from '@/types/api';
import type {
  GetNotificationsParams,
  MarkReadResponse,
  NotificationListResponse,
  UnreadCountResponse,
} from '@/types/notification';

// Lỗi được ném về caller (useNotifications); caller dùng try/finally + toApiError để tránh treo UI
// GET /api/v1/notifications
export const getNotifications = async (
  params: GetNotificationsParams = {},
): Promise<NotificationListResponse> => {
  const response = await apiClient.get<ApiResponse<NotificationListResponse>>(
    '/notifications',
    { params },
  );
  return response.data.data;
};

// GET /api/v1/notifications/unread-count
export const getUnreadCount = async (): Promise<UnreadCountResponse> => {
  const response = await apiClient.get<ApiResponse<UnreadCountResponse>>(
    '/notifications/unread-count',
  );
  return response.data.data;
};

// PATCH /api/v1/notifications/{notificationId}/read
export const markNotificationAsRead = async (
  notificationId: string,
): Promise<MarkReadResponse> => {
  const response = await apiClient.patch<ApiResponse<MarkReadResponse>>(
    `/notifications/${notificationId}/read`,
  );
  return response.data.data;
};