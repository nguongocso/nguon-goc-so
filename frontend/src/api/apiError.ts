import { isAxiosError } from 'axios';

export const DEFAULT_API_ERROR_MESSAGE = 'Không thể kết nối đến máy chủ.';

/**
 * Chuẩn hoá lỗi axios thành Error với message lấy từ
 * `err.response?.data?.message` (backend trả message tiếng Việt trong
 * ApiResult theo docs/NCL-742-api-contract.md), fallback về chuỗi mặc định.
 */
export function toApiError(
  err: unknown,
  fallback: string = DEFAULT_API_ERROR_MESSAGE,
): Error {
  if (isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    return new Error(data?.message || fallback);
  }
  if (err instanceof Error && err.message) return err;
  return new Error(fallback);
}
