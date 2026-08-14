import { useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';
import {
  getNotifications,
  markNotificationAsRead,
} from '@/api/notificationApi';
import type {
  GetNotificationsParams,
  NotificationResponse,
} from '@/types/notification';

interface UseNotificationsOptions {
  size?: number;
  isRead?: boolean;
  // false: không tự tải khi mount (dùng cho dropdown, chỉ tải lúc mở)
  autoLoad?: boolean;
}

export const useNotifications = ({
  size = 20,
  isRead,
  autoLoad = true,
}: UseNotificationsOptions = {}) => {
  const [items, setItems] = useState<NotificationResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(autoLoad);

  const totalPages = size > 0 ? Math.max(1, Math.ceil(totalElements / size)) : 1;

  const load = useCallback(
    async (targetPage = page) => {
      setIsLoading(true);
      try {
        const params: GetNotificationsParams = { page: targetPage, size };
        if (isRead !== undefined) params.isRead = isRead;

        const data = await getNotifications(params);
        setItems(data.items);
        setPage(data.page);
        setTotalElements(data.totalElements);
      } catch (error: any) {
        const message =
          error.response?.data?.message || 'Không thể tải danh sách thông báo.';
        toast.error(message);
      } finally {
        setIsLoading(false);
      }
    },
    [page, size, isRead],
  );

  useEffect(() => {
    if (autoLoad) void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isRead]);

  const markAsRead = useCallback(async (notificationId: string) => {
    // Cập nhật lạc quan trước, đồng bộ lại nếu API thất bại
    setItems((current) =>
      current.map((item) =>
        item.id === notificationId
          ? { ...item, isRead: true, readAt: item.readAt ?? new Date().toISOString() }
          : item,
      ),
    );

    try {
      await markNotificationAsRead(notificationId);
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        'Không thể đánh dấu thông báo đã đọc.';
      toast.error(message);
      // Rollback nếu thất bại
      void load(page);
    }
  }, [load, page]);

  return {
    items,
    page,
    totalPages,
    totalElements,
    isLoading,
    load,
    markAsRead,
  };
};