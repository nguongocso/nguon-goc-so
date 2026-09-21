import apiClient from './axiosConfig';

import type {
  GetNotificationsParams,
  MarkReadResponse,
  NotificationListResponse,
  UnreadCountResponse,
} from '@/types/notification';

interface ApiDataResponse<T> {
  data: T;
}

interface RequestOptions {
  signal?: AbortSignal;
}

/**
 * Lấy danh sách thông báo của người dùng hiện tại (phân trang, lọc theo trạng thái đọc).
 * GET /api/v1/notifications
 */
export async function getNotifications(
  params: GetNotificationsParams = {},
  { signal }: RequestOptions = {},
): Promise<NotificationListResponse> {
  const response = await apiClient.get<ApiDataResponse<NotificationListResponse>>(
    '/notifications',
    { params, signal },
  );

  return response.data.data;
}

/**
 * Lấy số lượng thông báo chưa đọc của người dùng hiện tại.
 * GET /api/v1/notifications/unread-count
 */
export async function getUnreadCount(
  { signal }: RequestOptions = {},
): Promise<UnreadCountResponse> {
  const response = await apiClient.get<ApiDataResponse<UnreadCountResponse>>(
    '/notifications/unread-count',
    { signal },
  );

  return response.data.data;
}

/**
 * Đánh dấu một thông báo là đã đọc.
 * PATCH /api/v1/notifications/{notificationId}/read
 */
export async function markNotificationAsRead(
  notificationId: string,
): Promise<MarkReadResponse> {
  const response = await apiClient.patch<ApiDataResponse<MarkReadResponse>>(
    `/notifications/${notificationId}/read`,
  );

  return response.data.data;
}
