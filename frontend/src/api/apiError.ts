import { isAxiosError } from 'axios';

export const DEFAULT_API_ERROR_MESSAGE = 'Không thể kết nối đến máy chủ.';

/**
 * Chuẩn hoá lỗi axios thành Error với message lấy từ
 * `err.response?.data?.message` (backend trả message tiếng Việt trong
 * ApiResult theo docs/NCL-742-api-contract.md), fallback về chuỗi mặc định.
 * Hỗ trợ cả lỗi mock trong test có dạng { response: { data: { message } } }.
 */
export function toApiError(
  err: unknown,
  fallback: string = DEFAULT_API_ERROR_MESSAGE,
): Error {
  if (isAxiosError(err)) {
    const data = err.response?.data as { message?: string; error?: string } | undefined;
    return new Error(data?.message || data?.error || fallback);
  }
  if (typeof err === 'object' && err !== null && 'response' in err) {
    // Tương thích mock test và lỗi không phải axios nhưng có cấu trúc tương tự
    const response = (err as { response?: { data?: { message?: string; error?: string } } }).response;
    const message = response?.data?.message || response?.data?.error;
    if (message) return new Error(message);
  }
  if (err instanceof Error && err.message) return err;
  return new Error(fallback);
}
