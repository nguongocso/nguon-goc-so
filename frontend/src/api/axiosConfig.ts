import axios from 'axios';
import { getApiBaseUrl } from '@/config/runtimeConfig';
import {
  getToken,
  getSelectionToken,
  removeSelectionToken,
} from '@/utils/storage';
import { handleSessionExpiry } from '@/utils/session';

/** Cấu hình Axios, gắn JWT theo loại endpoint và xử lý phiên hết hạn tập trung. */

const baseURL = getApiBaseUrl();

const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

/** Danh sách các endpoint không được đính kèm Access Token trong Authorization header. */
const NO_ACCESS_TOKEN_ENDPOINTS: readonly string[] = [
  '/auth/login',
  '/public/inspection-result-entry',
];

/** Danh sách các endpoint thuộc quy trình chọn tổ chức sử dụng Selection Token. */
const SELECTION_TOKEN_ENDPOINTS: readonly string[] = [
  '/auth/organizations',
  '/auth/select-organization',
];

/** Cấu trúc dữ liệu đại diện cho ApiResult của backend. */
interface ApiResultLike {
  success: boolean;
  [key: string]: unknown;
}

/** Kiểm tra URL có phải là endpoint công khai không cần xác thực hay không. */
const isPublicEndpoint = (url?: string): boolean => {
  if (!url) return false;
  return (
    url.includes('/public/inspection-result-entry') ||
    url.includes('/public/trace')
  );
};

/** Kiểm tra URL request có thuộc nhóm không được đính kèm Access Token hay không. */
const isNoAccessTokenRequest = (url?: string): boolean => {
  if (!url) return false;
  if (isPublicEndpoint(url)) return true;

  return NO_ACCESS_TOKEN_ENDPOINTS.some(
    (endpoint) =>
      url === endpoint ||
      url.startsWith(`${endpoint}?`) ||
      url.startsWith(`${endpoint}/`),
  );
};

/** Kiểm tra URL request có thuộc quy trình chọn tổ chức sử dụng Selection Token hay không. */
const isSelectionTokenRequest = (url?: string): boolean => {
  if (!url) return false;

  return SELECTION_TOKEN_ENDPOINTS.some(
    (endpoint) =>
      url === endpoint ||
      url.startsWith(`${endpoint}?`) ||
      url.startsWith(`${endpoint}/`),
  );
};

/** Kiểm tra body phản hồi lỗi có phải là đối tượng ApiResult chuẩn của backend hay không. */
const isApiResultBody = (data: unknown): data is ApiResultLike => {
  return Boolean(
    data &&
      typeof data === 'object' &&
      typeof (data as { success?: unknown }).success === 'boolean',
  );
};

/** Request Interceptor: Tự động gán token xác thực tương ứng theo từng loại endpoint. */
apiClient.interceptors.request.use(
  (config) => {
    const url = config.url;

    // 1. Endpoint đăng nhập hoặc công khai: tuyệt đối không gửi token
    if (isNoAccessTokenRequest(url)) {
      if (config.headers) {
        delete config.headers.Authorization;
      }
      return config;
    }

    // 2. Quy trình chọn tổ chức: sử dụng ORG_SELECTION JWT
    if (isSelectionTokenRequest(url)) {
      const selectionToken = getSelectionToken();
      if (selectionToken) {
        config.headers.Authorization = `Bearer ${selectionToken}`;
      } else if (config.headers) {
        delete config.headers.Authorization;
      }
      return config;
    }

    // 3. Các API được bảo vệ: sử dụng ACCESS JWT
    const accessToken = getToken();
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    } else if (config.headers) {
      delete config.headers.Authorization;
    }

    return config;
  },
  (error: unknown) => {
    return Promise.reject(error);
  },
);

/** Phân loại mã lỗi 401/403 để xử lý phiên hết hạn hoặc hiển thị thông báo phù hợp. */
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url: string | undefined = error.config?.url;

    // Xử lý mã lỗi 401 (Unauthorized)
    if (status === 401) {
      // 401 từ login: Sai thông tin đăng nhập, trả lỗi về UI để hiển thị
      if (isNoAccessTokenRequest(url)) {
        return Promise.reject(error);
      }

      // 401 từ selection flow: Hết hạn selection token, gỡ token để form tự chuyển hướng
      if (isSelectionTokenRequest(url)) {
        removeSelectionToken();
        return Promise.reject(error);
      }

      // 401 từ API bảo vệ: Access token hết hạn/không hợp lệ, kích hoạt đăng xuất tập trung
      handleSessionExpiry();
      return Promise.reject(error);
    }

    // Xử lý mã lỗi 403 (Forbidden) do thiếu hoặc hỏng token từ Spring Security entry point
    if (status === 403 && !isApiResultBody(error.response?.data)) {
      if (isNoAccessTokenRequest(url)) {
        return Promise.reject(error);
      }
      handleSessionExpiry();
      return Promise.reject(error);
    }

    return Promise.reject(error);
  },
);

export default apiClient;
