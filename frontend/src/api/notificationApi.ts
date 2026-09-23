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

/** Lấy danh sách thông báo theo bộ lọc. */
export const getNotifications = async (
  params: GetNotificationsParams = {},
): Promise<NotificationListResponse> => {
  const response = await apiClient.get<ApiDataResponse<NotificationListResponse>>(
    '/notifications',
    { params },
  );
  return response.data.data;
};

/** Lấy số lượng thông báo chưa đọc. */
export const getUnreadCount = async (): Promise<UnreadCountResponse> => {
  const response = await apiClient.get<ApiDataResponse<UnreadCountResponse>>(
    '/notifications/unread-count',
  );
  return response.data.data;
};

/** Đánh dấu một thông báo là đã đọc. */
export const markNotificationAsRead = async (
  notificationId: string,
): Promise<MarkReadResponse> => {
  const response = await apiClient.patch<ApiDataResponse<MarkReadResponse>>(
    `/notifications/${notificationId}/read`,
  );
  return response.data.data;
};
