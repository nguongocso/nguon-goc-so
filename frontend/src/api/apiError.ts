import { isAxiosError } from 'axios';

/** Thông báo lỗi mặc định khi không thể kết nối tới máy chủ backend. */
export const DEFAULT_API_ERROR_MESSAGE = 'Không thể kết nối đến máy chủ.';

/** Cấu trúc dữ liệu phản hồi lỗi từ API backend (theo ApiResult contract). */
interface ApiErrorPayload {
  message?: string;
  [key: string]: unknown;
}

/** Kiểm tra xem dữ liệu phản hồi có chứa trường message hợp lệ hay không. */
function hasErrorMessage(data: unknown): data is ApiErrorPayload & { message: string } {
  return (
    typeof data === 'object' &&
    data !== null &&
    'message' in data &&
    typeof (data as ApiErrorPayload).message === 'string' &&
    (data as ApiErrorPayload).message!.trim().length > 0
  );
}

/** Chuẩn hóa lỗi API thành Error với thông báo backend hoặc nội dung dự phòng. */
export function toApiError(
  err: unknown,
  fallback: string = DEFAULT_API_ERROR_MESSAGE,
): Error {
  if (isAxiosError(err)) {
    const data = err.response?.data;
    if (hasErrorMessage(data)) {
      return new Error(data.message);
    }
    return new Error(fallback);
  }

  if (err instanceof Error && err.message) {
    return err;
  }

  return new Error(fallback);
}
