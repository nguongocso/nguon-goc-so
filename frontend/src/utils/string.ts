/**
 * Chuẩn hóa chuỗi tiếng Việt để tìm kiếm không phân biệt dấu.
 * Chuyển về dạng NFD, loại bỏ dấu tổ hợp (U+0300–U+036F), lowercase và trim.
 *
 * @param str chuỗi gốc cần chuẩn hóa
 * @returns chuỗi đã bỏ dấu, viết thường, cắt khoảng trắng đầu/cuối
 */
export const normalizeVietnamese = (str: string): string => {
  if (!str) return "";
  return str
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
};