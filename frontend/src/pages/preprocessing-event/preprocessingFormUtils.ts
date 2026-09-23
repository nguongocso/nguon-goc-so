import { isAxiosError } from 'axios';

interface ApiErrorPayload {
  message?: string;
  errors?: Record<string, string>;
}

/** Giới hạn số lượng ảnh thực địa đính kèm cho mỗi sự kiện sơ chế */
export const MAX_PREPROCESSING_IMAGES = 5;

/** Giới hạn dung lượng tối đa cho mỗi tệp ảnh (5 MB) */
export const MAX_PREPROCESSING_IMAGE_SIZE = 5 * 1024 * 1024;

/** Chuyển đổi File ảnh sang chuỗi base64 DataURL */
export const fileToBase64 = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(new Error('Không thể đọc tệp ảnh'));
    reader.readAsDataURL(file);
  });

/** Trích xuất thông báo lỗi phù hợp từ phản hồi API hoặc lỗi mạng */
export const getPreprocessingErrorMessage = (
  error: unknown,
  fallback: string,
): string => {
  if (!isAxiosError<ApiErrorPayload>(error)) {
    return fallback;
  }

  const payload = error.response?.data;
  if (payload?.message?.trim()) {
    return payload.message;
  }

  const validationMessages = payload?.errors
    ? Object.values(payload.errors).filter(Boolean)
    : [];

  if (validationMessages.length > 0) {
    return validationMessages.join('. ');
  }

  if (!error.response) {
    return 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại.';
  }

  if (error.response.status === 403) {
    return 'Bạn không có quyền ghi sự kiện cho lô sản xuất này.';
  }

  if (error.response.status === 404) {
    return 'Không tìm thấy lô sản xuất hoặc sự kiện cần đính chính.';
  }

  return fallback;
};

/** Chuẩn hóa chuỗi văn bản không bắt buộc (chuyển chuỗi rỗng sau trim thành undefined) */
export const toOptionalText = (value?: string): string | undefined => {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
};
