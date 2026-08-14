import axios from "axios";

import {
  getToken,
  getSelectionToken,
  clearAuthStorage,
  removeSelectionToken,
} from "@/utils/storage";

const rawBaseUrl =
  import.meta.env.VITE_API_BASE_URL ||
  import.meta.env.VITE_API_URL ||
  "http://localhost:8080/api/v1";

const baseURL = rawBaseUrl.endsWith("/api/v1")
  ? rawBaseUrl
  : `${rawBaseUrl.replace(/\/$/, "")}/api/v1`;

const apiClient = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
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
 */
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },

  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url;

      /**
       * ======================================================
       * ORG_SELECTION JWT hết hạn
       * ======================================================
       */
      if (isSelectionTokenRequest(url)) {
        removeSelectionToken();

        window.location.href = "/login";

        return Promise.reject(error);
      }

      /**
       * ======================================================
       * ACCESS JWT hết hạn
       * ======================================================
       */
      clearAuthStorage();

      window.location.href = "/login";
    }

    return Promise.reject(error);
  }
);

export default apiClient;