/**
 * Chuyển đổi đối tượng Date sang chuỗi datetime-local (YYYY-MM-DDTHH:mm).
 *
 * @param date Thời điểm cần định dạng.
 * @param endOfDay Nếu là true, giờ:phút sẽ là 23:59.
 */
export const toDateTimeLocal = (date: Date, endOfDay = false): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  let hours = String(date.getHours()).padStart(2, '0');
  let minutes = String(date.getMinutes()).padStart(2, '0');
  if (endOfDay) {
    hours = '23';
    minutes = '59';
  }
  return `${year}-${month}-${day}T${hours}:${minutes}`;
};

export type QuickRangeKey = '7days' | '30days' | 'week' | 'month' | 'year' | null;

/**
 * Xác định khóa khoảng thời gian nhanh (7 ngày, 30 ngày, tuần này, tháng này, năm nay) dựa vào từ ngày và đến ngày.
 */
export const detectActiveQuickRange = (fromDate?: string, toDate?: string): QuickRangeKey => {
  if (!fromDate || !toDate) return null;

  const normalize = (dateStr: string) => (dateStr ? dateStr.slice(0, 16) : '');
  const now = new Date();
  const todayStr = toDateTimeLocal(now, true);
  const from7days = toDateTimeLocal(new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000));
  const from30days = toDateTimeLocal(new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000));
  const normalizedFrom = normalize(fromDate);
  const normalizedTo = normalize(toDate);

  const dayOfWeek = now.getDay();
  const diff = now.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
  const monday = new Date(now);
  monday.setDate(diff);
  monday.setHours(0, 0, 0, 0);
  const weekFrom = toDateTimeLocal(monday);
  const weekTo = toDateTimeLocal(now, true);

  const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
  const monthFrom = toDateTimeLocal(firstDay);
  const monthTo = toDateTimeLocal(now, true);

  const yearFirst = new Date(now.getFullYear(), 0, 1);
  const yearFrom = toDateTimeLocal(yearFirst);
  const yearTo = toDateTimeLocal(now, true);

  if (normalizedFrom === normalize(from7days) && normalizedTo === normalize(todayStr)) {
    return '7days';
  }
  if (normalizedFrom === normalize(from30days) && normalizedTo === normalize(todayStr)) {
    return '30days';
  }
  if (normalizedFrom === normalize(weekFrom) && normalizedTo === normalize(weekTo)) {
    return 'week';
  }
  if (normalizedFrom === normalize(monthFrom) && normalizedTo === normalize(monthTo)) {
    return 'month';
  }
  if (normalizedFrom === normalize(yearFrom) && normalizedTo === normalize(yearTo)) {
    return 'year';
  }
  return null;
};
