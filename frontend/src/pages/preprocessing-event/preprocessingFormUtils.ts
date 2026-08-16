import { isAxiosError } from "axios";

interface ApiErrorPayload {
  message?: string;
  errors?: Record<string, string>;
}

export const MAX_PREPROCESSING_IMAGES = 5;
export const MAX_PREPROCESSING_IMAGE_SIZE = 5 * 1024 * 1024;

export const fileToBase64 = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(new Error("Không thể đọc tệp ảnh"));
    reader.readAsDataURL(file);
  });

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
    return validationMessages.join(". ");
  }

  if (!error.response) {
    return "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại.";
  }

  if (error.response.status === 403) {
    return "Bạn không có quyền ghi sự kiện cho lô sản xuất này.";
  }

  if (error.response.status === 404) {
    return "Không tìm thấy lô sản xuất hoặc sự kiện cần đính chính.";
  }

  return fallback;
};

export const toOptionalText = (value?: string): string | undefined => {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
};
