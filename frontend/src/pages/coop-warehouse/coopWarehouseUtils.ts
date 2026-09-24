import { isAxiosError } from 'axios';

import type { Shipment } from '@/types/shipment';

/** Trạng thái lưu kho của lô hàng đối với kho HTX. */
export interface ShipmentStatusItem {
  shipment: Shipment;
  warehouseStatus: 'IN_WAREHOUSE' | 'NOT_IN_WAREHOUSE';
}

/** Lấy chuỗi thời gian hiện tại theo định dạng local `YYYY-MM-DDTHH:mm`. */
export function getCurrentDatetimeString(): string {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
}

/** Định dạng chuỗi thời gian có thêm giây (`:00`) nếu đang ở định dạng phút `YYYY-MM-DDTHH:mm`. */
export function formatDateTimeWithSeconds(datetime?: string): string {
  if (!datetime) return '';
  return datetime.length === 16 ? `${datetime}:00` : datetime;
}

/** Trích xuất thông báo lỗi an toàn từ phản hồi lỗi API hoặc mạng. */
export function getCoopWarehouseErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError<{ message?: string }>(err) && err.response?.data?.message) {
    return err.response.data.message;
  }
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return fallback;
}
