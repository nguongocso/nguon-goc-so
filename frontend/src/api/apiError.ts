import { isAxiosError } from 'axios';

/** Thông báo lỗi mặc định khi không thể kết nối tới máy chủ. */
export const DEFAULT_API_ERROR_MESSAGE = 'Không thể kết nối đến máy chủ.';

/** Cấu trúc dữ liệu phản hồi lỗi từ API backend (theo hợp đồng ApiResult). */
interface ApiErrorPayload {
  message?: unknown;
  error?: unknown;
  [key: string]: unknown;
}

/** Trích xuất thông báo lỗi hợp lệ từ dữ liệu phản hồi API. */
function getApiErrorMessage(data: unknown): string | null {
  if (typeof data !== 'object' || data === null) return null;

  const payload = data as ApiErrorPayload;
  if (typeof payload.message === 'string' && payload.message.trim()) {
    return payload.message;
  }
  if (typeof payload.error === 'string' && payload.error.trim()) {
    return payload.error;
  }
  return null;
}

/** Chuẩn hóa lỗi API thành đối tượng Error với thông báo từ máy chủ hoặc nội dung dự phòng. */
export function toApiError(
  err: unknown,
  fallback: string = DEFAULT_API_ERROR_MESSAGE,
): Error {
  if (isAxiosError(err)) {
    return new Error(getApiErrorMessage(err.response?.data) || fallback);
  }

  if (typeof err === 'object' && err !== null && 'response' in err) {
    const response = (err as { response?: { data?: unknown } }).response;
    const message = getApiErrorMessage(response?.data);
    if (message) return new Error(message);
  }

  if (err instanceof Error && err.message) {
    return err;
  }

  return new Error(fallback);
}
