import type { FarmActivityType } from '@/types/farmLog';

/**
 * Nhãn tiếng Việt cho loại hoạt động canh tác (dùng chung cho form
 * ngoại tuyến và danh sách chờ NCL-10-CN-012).
 */
export const HOAT_DONG_CANH_TAC_OPTIONS: Array<{
  value: FarmActivityType;
  label: string;
}> = [
  { value: 'PLANTING', label: 'Gieo trồng' },
  { value: 'WATERING', label: 'Tưới nước' },
  { value: 'FERTILIZING', label: 'Bón phân' },
  { value: 'PESTICIDE', label: 'Phun thuốc' },
  { value: 'WEEDING', label: 'Làm cỏ' },
  { value: 'HARVESTING', label: 'Thu hoạch' },
  { value: 'OTHER', label: 'Khác' },
];

/**
 * Lấy nhãn hiển thị của một loại hoạt động, fallback về mã gốc.
 */
export function layNhanHoatDong(value: unknown): string {
  const timThay = HOAT_DONG_CANH_TAC_OPTIONS.find((o) => o.value === value);
  return timThay ? timThay.label : String(value ?? '—');
}
