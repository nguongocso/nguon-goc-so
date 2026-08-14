/**
 * Các hàm hỗ trợ ngày/giờ theo múi giờ LOCAL của trình duyệt.
 *
 * Lý do cần file này: `new Date().toISOString().split('T')[0]` trả về ngày
 * theo giờ UTC. Với người dùng ở múi giờ UTC+7 (Việt Nam), vào khoảng
 * 00:00–06:59 giờ địa phương thì ngày UTC vẫn là "hôm qua" => các form bị
 * mặc định sai ngày (hiển thị hôm qua thay vì hôm nay).
 */

/** Trả về ngày hiện tại (hoặc ngày truyền vào) theo giờ local, dạng YYYY-MM-DD — dùng cho <input type="date">. */
export function getLocalDateString(date: Date = new Date()): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Trả về thời điểm hiện tại (hoặc truyền vào) theo giờ local, dạng YYYY-MM-DDTHH:mm — dùng cho <input type="datetime-local">. */
export function getLocalDateTimeString(date: Date = new Date()): string {
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${getLocalDateString(date)}T${hours}:${minutes}`;
}

/** Chuyển một chuỗi ISO (thường là UTC, ví dụ giá trị lưu trong form) sang chuỗi hiển thị cho <input type="datetime-local"> theo giờ local. */
export function isoToLocalDateTimeInputValue(iso: string): string {
  const parsed = new Date(iso);
  if (Number.isNaN(parsed.getTime())) return getLocalDateTimeString();
  return getLocalDateTimeString(parsed);
}
