// src/hooks/useHelp.ts
// NCL-01-CN-006 - Hook lấy nội dung hướng dẫn sử dụng theo màn hình
import { useCallback, useEffect, useState } from 'react';
import { getHelp } from '@/api/helpApi';
import type { HelpContent } from '@/types/help';

/** Cache trong bộ nhớ theo screenKey — tránh gọi lại API khi mở lại drawer. */
const cache = new Map<string, HelpContent | null>();

interface UseHelpResult {
  /** Nội dung hướng dẫn, hoặc null nếu chưa có. */
  data: HelpContent | null;
  /** Đang tải nội dung hay không. */
  isLoading: boolean;
  /** Lỗi khi tải (nếu có). */
  error: string | null;
  /** Tải lại nội dung hướng dẫn. */
  refetch: () => Promise<void>;
}

export const useHelp = (screenKey: string): UseHelpResult => {
  const [data, setData] = useState<HelpContent | null>(() =>
    cache.has(screenKey) ? cache.get(screenKey)! : null,
  );
  const [isLoading, setIsLoading] = useState<boolean>(() => !cache.has(screenKey));
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await getHelp(screenKey);
      cache.set(screenKey, result);
      setData(result);
    } catch (err: any) {
      const message =
        err.response?.data?.message || 'Không thể tải hướng dẫn sử dụng.';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [screenKey]);

  useEffect(() => {
    // Đồng bộ dữ liệu từ cache khi screenKey thay đổi (tránh hiển thị
    // nội dung của màn hình cũ nếu cache đã có sẵn)
    setData(cache.has(screenKey) ? cache.get(screenKey)! : null);
    setIsLoading(!cache.has(screenKey));
    setError(null);
    if (!cache.has(screenKey)) {
      void load();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [screenKey]);

  const refetch = useCallback(async () => {
    cache.delete(screenKey);
    await load();
  }, [load, screenKey]);

  return { data, isLoading, error, refetch };
};