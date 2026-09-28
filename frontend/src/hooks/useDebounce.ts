import { useEffect, useState } from 'react';

/**
 * Hook trì hoãn cập nhật giá trị (Debounce) nhằm hạn chế gọi API liên tục khi người dùng nhập liệu.
 *
 * @param value Giá trị cần debounce
 * @param delay Thời gian chờ (mili-giây), mặc định 400ms
 * @returns Giá trị đã được debounce
 */
export function useDebounce<T>(value: T, delay = 400): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(timer);
    };
  }, [value, delay]);

  return debouncedValue;
}
