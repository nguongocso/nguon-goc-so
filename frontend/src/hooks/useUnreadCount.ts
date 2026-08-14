import { useCallback, useEffect, useState } from 'react';
import { getUnreadCount } from '@/api/notificationApi';
import { useAuth } from '@/hooks/useAuth';

const POLL_INTERVAL_MS = 30_000;

export const useUnreadCount = () => {
  const { user } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);

  const refresh = useCallback(async () => {
    if (!user) return;
    try {
      const data = await getUnreadCount();
      setUnreadCount(data.unreadCount);
    } catch {
      // best-effort — không chặn UI nếu đếm chưa đọc thất bại
    }
  }, [user]);

  useEffect(() => {
    if (!user) {
      setUnreadCount(0);
      return;
    }

    void refresh();
    const intervalId = window.setInterval(() => {
      void refresh();
    }, POLL_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, [user, refresh]);

  return { unreadCount, refresh };
};