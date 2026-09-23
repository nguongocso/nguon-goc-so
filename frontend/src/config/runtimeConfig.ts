/**
 * Bộ giải quyết cấu hình môi trường runtime cho ứng dụng frontend.
 * Hỗ trợ nạp động cấu hình từ window.__RUNTIME_CONFIG__, biến môi trường Vite build-time,
 * hoặc fallback về reverse proxy cùng origin (/api/v1).
 */

/**
 * Định nghĩa cấu trúc các biến cấu hình runtime của ứng dụng.
 */
export interface RuntimeConfig {
  API_BASE_URL: string;
  ASSET_BASE_URL: string;
}

declare global {
  interface Window {
    __RUNTIME_CONFIG__?: Partial<RuntimeConfig>;
  }
}

/**
 * Kiểm tra giá trị cấu hình không rỗng và không phải placeholder.
 *
 * @param value Giá trị chuỗi cần kiểm tra.
 * @returns `true` nếu giá trị chuỗi hợp lệ và đã được thiết lập.
 */
function isConfigured(value: string | undefined): value is string {
  return (
    !!value &&
    value.trim().length > 0 &&
    !value.startsWith('__') &&
    !value.endsWith('__')
  );
}

/**
 * Lấy URL gốc cho API client (đảm bảo luôn kết thúc bằng /api/v1).
 *
 * @returns Đường dẫn gốc API chuẩn hoá.
 */
export function getApiBaseUrl(): string {
  const fromWindow = window.__RUNTIME_CONFIG__?.API_BASE_URL;
  if (isConfigured(fromWindow)) {
    return normalizeApiBaseUrl(fromWindow);
  }

  const fromVite =
    import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL;
  if (isConfigured(fromVite)) {
    return normalizeApiBaseUrl(fromVite);
  }

  // Mặc định cùng origin: Nginx reverse proxy chuyển tiếp /api -> backend service
  return normalizeApiBaseUrl('/api/v1');
}

/**
 * Lấy URL gốc cho các tài nguyên tĩnh hoặc file tải xuống (ví dụ: hình ảnh mã QR).
 *
 * @returns Đường dẫn gốc của tài nguyên tĩnh đã loại bỏ dấu gạch chéo cuối.
 */
export function getAssetBaseUrl(): string {
  const fromWindow = window.__RUNTIME_CONFIG__?.ASSET_BASE_URL;
  if (isConfigured(fromWindow)) {
    return fromWindow.replace(/\/$/, '');
  }

  const fromVite = import.meta.env.VITE_ASSET_BASE_URL;
  if (isConfigured(fromVite)) {
    return fromVite.replace(/\/$/, '');
  }

  // Suy xuất từ API URL: loại bỏ hậu tố /api/v1 hoặc /api
  const apiBase = getApiBaseUrl();
  return apiBase.replace(/\/api(?:\/v1)?\/?$/, '').replace(/\/$/, '');
}

/**
 * Tạo URL đầy đủ cho đường dẫn tài nguyên (ví dụ: /uploads/avatar/sample.png).
 *
 * @param url Đường dẫn tương đối hoặc tuyệt đối của tài nguyên.
 * @returns URL hoàn chỉnh để truy cập tài nguyên hoặc undefined nếu url rỗng.
 */
export function getAssetUrl(url?: string | null): string | undefined {
  if (!url) return undefined;
  if (
    url.startsWith('http://') ||
    url.startsWith('https://') ||
    url.startsWith('data:') ||
    url.startsWith('blob:')
  ) {
    return url;
  }
  const assetBase = getAssetBaseUrl();
  if (!assetBase) return url;
  const cleanUrl = url.startsWith('/') ? url : `/${url}`;
  return `${assetBase}${cleanUrl}`;
}

/**
 * Chuẩn hoá đường dẫn gốc API để luôn kết thúc bằng /api/v1.
 *
 * @param raw Chuỗi URL thô ban đầu.
 * @returns Chuỗi URL chuẩn hoá kết thúc bằng /api/v1.
 */
function normalizeApiBaseUrl(raw: string): string {
  const value = raw.trim();
  if (/\/api\/v1\/?$/.test(value)) {
    return value.replace(/\/$/, '');
  }
  if (/\/api\/?$/.test(value)) {
    return value.replace(/\/+$/, '') + '/v1';
  }
  return value.replace(/\/+$/, '') + '/api/v1';
}
