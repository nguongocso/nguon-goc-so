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
    .replace(/[đĐ]/g, "d")
    .toLowerCase()
    .trim();
};

/**
 * Làm sạch chuỗi phản hồi từ máy chủ đối tác hoặc bên thứ ba.
 * Loại bỏ các thẻ HTML rác (như thẻ <a>, <div>, <p>...) nếu máy chủ trả về dạng trang web,
 * chỉ giữ lại văn bản thuần túy để tránh rò rỉ mã HTML thô ra giao diện người dùng.
 *
 * @param raw Chuỗi phản hồi thô nhận từ máy chủ
 * @returns Chuỗi văn bản thuần sạch đẹp
 */
export const sanitizeResponseBody = (raw?: string | null): string => {
  if (!raw) return "";
  const trimmed = raw.trim();
  // Kiểm tra chuỗi có chứa thẻ HTML hay không
  if (/<[a-z][\s\S]*>/i.test(trimmed)) {
    try {
      if (typeof DOMParser !== "undefined") {
        const doc = new DOMParser().parseFromString(trimmed, "text/html");
        return doc.body.textContent?.trim() || trimmed.replace(/<[^>]+>/g, "").trim();
      }
    } catch {
      // Fallback regex nếu môi trường không có DOMParser
    }
    return trimmed.replace(/<[^>]+>/g, "").trim();
  }
  return trimmed;
};