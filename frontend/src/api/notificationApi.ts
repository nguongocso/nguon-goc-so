import apiClient from './axiosConfig';
import type { ApiResponse } from '@/types/api';
import type {
  GetNotificationsParams,
  MarkAllReadResponse,
  MarkReadResponse,
  NotificationListResponse,
  UnreadCountResponse,
} from '@/types/notification';

export const getNotifications = async (params: GetNotificationsParams = {}): Promise<NotificationListResponse> => {
  const response = await apiClient.get<ApiResponse<NotificationListResponse>>('/notifications', { params });
  return response.data.data;
};

export const getUnreadCount = async (): Promise<UnreadCountResponse> => {
  const response = await apiClient.get<ApiResponse<UnreadCountResponse>>('/notifications/unread-count');
  return response.data.data;
};

export const markNotificationAsRead = async (notificationId: string): Promise<MarkReadResponse> => {
  const response = await apiClient.patch<ApiResponse<MarkReadResponse>>(`/notifications/${notificationId}/read`);
  return response.data.data;
};

export const markAllNotificationsAsRead = async (): Promise<MarkAllReadResponse> => {
  const response = await apiClient.patch<ApiResponse<MarkAllReadResponse>>('/notifications/read-all');
  return response.data.data;
};
