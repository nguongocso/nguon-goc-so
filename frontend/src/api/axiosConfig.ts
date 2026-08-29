import axios from "axios";

import { getApiBaseUrl } from "@/config/runtimeConfig";

import {
  getToken,
  getSelectionToken,
  removeSelectionToken,
} from "@/utils/storage";

import { handleSessionExpiry } from "@/utils/session";

const baseURL = getApiBaseUrl();

const apiClient = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json;charset=utf-8",
    "Accept-Charset": "utf-8",
  },
});

/**
 * ============================================================
 * AUTH ENDPOINTS
 * ============================================================
 *
 * Những API này không được gửi ACCESS TOKEN.
 *
 * /auth/login
 *   -> username + password
 *   -> ORG_SELECTION JWT
 *
 * /auth/organizations
 *   -> ORG_SELECTION JWT
 *
 * /auth/select-organization
 *   -> ORG_SELECTION JWT
 */
const NO_ACCESS_TOKEN_ENDPOINTS = [
  "/auth/login",
];

/**
 * ============================================================
 * SELECTION TOKEN ENDPOINTS
 * ============================================================
 *
 * Những API này sử dụng ORG_SELECTION JWT.
 */
const SELECTION_TOKEN_ENDPOINTS = [
  "/auth/organizations",
  "/auth/select-organization",
];

/**
 * Kiểm tra URL có phải endpoint không sử dụng
 * ACCESS TOKEN hay không.
 */
const isNoAccessTokenRequest = (
  url?: string
): boolean => {
  if (!url) {
    return false;
  }

  return NO_ACCESS_TOKEN_ENDPOINTS.some(
    (endpoint) =>
      url === endpoint ||
      url.startsWith(`${endpoint}?`) ||
      url.startsWith(`${endpoint}/`)
  );
};

/**
 * Kiểm tra request có sử dụng ORG_SELECTION JWT hay không.
 */
const isSelectionTokenRequest = (
  url?: string
): boolean => {
  if (!url) {
    return false;
  }

  return SELECTION_TOKEN_ENDPOINTS.some(
    (endpoint) =>
      url === endpoint ||
      url.startsWith(`${endpoint}?`) ||
      url.startsWith(`${endpoint}/`)
  );
};

/**
 * ============================================================
 * REQUEST INTERCEPTOR
 * ============================================================
 */
apiClient.interceptors.request.use(
  (config) => {
    const url = config.url;

    /**
     * ========================================================
     * 1. LOGIN
     * ========================================================
     *
     * POST /auth/login
     *
     * Tuyệt đối KHÔNG gửi:
     *
     * Authorization: Bearer <ACCESS_TOKEN>
     *
     * Login chỉ gửi username/password.
     */
    if (isNoAccessTokenRequest(url)) {
      if (config.headers) {
        delete config.headers.Authorization;
      }

      return config;
    }

    /**
     * ========================================================
     * 2. ORG SELECTION FLOW
     * ========================================================
     *
     * GET  /auth/organizations
     * POST /auth/select-organization
     *
     * Sử dụng ORG_SELECTION JWT.
     */
    if (isSelectionTokenRequest(url)) {
      const selectionToken = getSelectionToken();

      if (selectionToken) {
        config.headers.Authorization =
          `Bearer ${selectionToken}`;
      } else if (config.headers) {
        delete config.headers.Authorization;
      }

      return config;
    }

    /**
     * ========================================================
     * 3. ACCESS FLOW
     * ========================================================
     *
     * Tất cả API còn lại sử dụng ACCESS JWT.
     *
     * Ví dụ:
     *
     * GET /auth/me
     * GET /organizations
     * GET /shipments
     * POST /farm-logs
     * ...
     */
    const accessToken = getToken();

    if (accessToken) {
      config.headers.Authorization =
        `Bearer ${accessToken}`;
    } else if (config.headers) {
      delete config.headers.Authorization;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * ============================================================
 * RESPONSE INTERCEPTOR
 * ============================================================
 *
 * Phân biệt các nguồn lỗi 401/403 từ backend:
 *
 *   1. 401 từ POST /auth/login
 *      → username/password KHÔNG ĐÚNG.
 *      → Trả lỗi về UI để hiển thị, TUYỆT ĐỐI KHÔNG logout/redirect
 *        (nếu không sẽ tạo vòng lặp: /login → 401 → /login → ...).
 *
 *   2. 401 từ ORG_SELECTION flow (/auth/organizations, /auth/select-organization)
 *      → ORG_SELECTION JWT hết hạn.
 *      → Chỉ xóa selection token; trang auth-flow tự xử lý hiển thị
 *        lỗi và quay về /login.
 *
 *   3. 401 từ protected API (còn lại)
 *      → ACCESS JWT không còn hợp lệ.
 *      → Logout tập trung: clear storage + reset state + /login.
 *
 *   4. 403 KHÔNG có body ApiResult (field "success")
 *      → Spring Security entry point mặc định (Http403ForbiddenEntryPoint):
 *        request CHƯA XÁC THỰC do token thiếu / hết hạn / không hợp lệ.
 *        (JwtAuthenticationFilter nuốt token lỗi rồi đi tiếp, và hệ thống
 *        không cấu hình entry point trả 401 — xem JwtAuthenticationFilter,
 *        SecurityConfig và các test backend ghi chú "403 vì chưa đăng nhập".)
 *      → Logout tập trung như (3).
 *
 *   5. 403 CÓ body ApiResult (field "success")
 *      → Lỗi PHÂN QUYỀN nghiệp vụ (AccessDenied / BusinessException 403).
 *      → KHÔNG logout, chỉ trả lỗi về UI.
 *
 * Logout tập trung do handleSessionExpiry() đảm nhiệm và là idempotent:
 * nhiều request đồng thời trả 401/403 cũng chỉ logout + redirect MỘT lần.
 */

/**
 * Body response có phải là ApiResult của backend hay không.
 *
 * ApiResult luôn chứa field boolean "success"; trong khi đó 403 từ
 * entry point mặc định của Spring Security chỉ trả error JSON chuẩn
 * của Spring Boot (timestamp/status/error/path) hoặc body rỗng.
 */
const isApiResultBody = (data: unknown): boolean =>
  Boolean(
    data &&
      typeof data === "object" &&
      typeof (data as { success?: unknown }).success === "boolean"
  );

apiClient.interceptors.response.use(
  (response) => {
    return response;
  },

  (error) => {
    const status = error.response?.status;
    const url: string | undefined = error.config?.url;

    /**
     * ======================================================
     * 401 UNAUTHORIZED
     * ======================================================
     */
    if (status === 401) {
      /**
       * 401 từ LOGIN = sai username/password.
       * Trả lỗi về UI, không clear auth, không redirect.
       */
      if (isNoAccessTokenRequest(url)) {
        return Promise.reject(error);
      }

      /**
       * 401 từ ORG_SELECTION flow:
       * chỉ gỡ selection token, trang auth-flow tự xử lý
       * (toast + logout + quay về /login).
       */
      if (isSelectionTokenRequest(url)) {
        removeSelectionToken();

        return Promise.reject(error);
      }

      /**
       * 401 từ protected API → token không còn hợp lệ.
       */
      handleSessionExpiry();

      return Promise.reject(error);
    }

    /**
     * ======================================================
     * 403 FORBIDDEN - CHƯA XÁC THỰC
     * ======================================================
     *
     * Backend trả 403 (không có ApiResult body) khi ACCESS JWT
     * thiếu / hết hạn / không hợp lệ trên endpoint protected.
     * Xử lý như phiên đăng nhập đã mất.
     */
    if (status === 403 && !isApiResultBody(error.response?.data)) {
      handleSessionExpiry();

      return Promise.reject(error);
    }

    return Promise.reject(error);
  }
);

export default apiClient;