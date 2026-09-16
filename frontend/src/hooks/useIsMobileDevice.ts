import { useMemo } from 'react';

/**
 * Dấu hiệu thiết bị di động trong User-Agent (điện thoại, máy tính bảng).
 * Cố ý không dùng độ rộng viewport: cửa sổ desktop thu nhỏ vẫn bị tính là desktop.
 */
const MOBILE_UA_PATTERN =
  /Android|iPhone|iPad|iPod|Windows Phone|Mobile|BlackBerry|Opera Mini/i;

interface DieuHuongNhanDang {
  userAgent?: string;
  maxTouchPoints?: number;
  userAgentData?: { mobile?: boolean };
}

/**
 * Kiểm tra thiết bị di động thật qua User-Agent (NCL-10-CN-012).
 *
 * Thứ tự ưu tiên:
 * 1. `navigator.userAgentData.mobile` (trình duyệt Chromium mới).
 * 2. Regex dấu hiệu mobile trên `navigator.userAgent`.
 *
 * Desktop có màn hình cảm ứng vẫn trả về `false` vì User-Agent của nó
 * không chứa dấu hiệu mobile.
 */
export function laThietBiDiDong(
  dieuHuong: DieuHuongNhanDang = typeof navigator !== 'undefined' ? navigator : {},
): boolean {
  if (typeof dieuHuong.userAgentData?.mobile === 'boolean') {
    return dieuHuong.userAgentData.mobile;
  }
  if (dieuHuong.userAgent && MOBILE_UA_PATTERN.test(dieuHuong.userAgent)) {
    return true;
  }
  return false;
}

/**
 * Hook trả về `true` khi người dùng đang dùng thiết bị di động thật.
 * User-Agent không đổi trong phiên nên chỉ tính một lần.
 */
export function useIsMobileDevice(): boolean {
  return useMemo(() => laThietBiDiDong(), []);
}
