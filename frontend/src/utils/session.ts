/**
 * ============================================================
 * SESSION EXPIRY — LOGOUT TẬP TRUNG
 * ============================================================
 *
 * Module duy nhất chịu trách nhiệm xử lý "phiên đăng nhập không
 * còn hợp lệ" (token bị xóa, hết hạn, không hợp lệ, bị thu hồi...).
 *
 * Nguồn kích hoạt:
 *
 *   1. Axios response interceptor (frontend/src/api/axiosConfig.ts)
 *      khi backend trả 401 (không phải từ /auth/login) hoặc 403
 *      chưa-xác-thực (entry point mặc định của Spring Security,
 *      response không có body ApiResult).
 *
 *   2. Watchdog trong AuthContext: phát hiện token/user đã bị xóa
 *      khỏi localStorage trong khi React state vẫn đang đăng nhập
 *      (Case: localStorage.removeItem("access_token")).
 *
 * Bảo đảm:
 *
 *   - Nhiều request đồng thời trả 401/403 → logout CHỈ xử lý MỘT lần
 *     (module-level single-flight guard).
 *   - Đang ở /login thì KHÔNG redirect thêm (tránh vòng lặp).
 *   - User chưa đăng nhập (storage rỗng) thì không bị kéo về /login
 *     khi các API public lỗi 401/403.
 *
 * Luồng xử lý:
 *
 *   401/403 (không xác thực)
 *        ↓
 *   handleSessionExpiry()
 *        ↓
 *   clearAuthStorage()  → xóa access_token + selection_token + user_info
 *        ↓
 *   dispatch SESSION_EXPIRED_EVENT (đồng bộ)
 *        ↓
 *   AuthContext: reset state + toast (1 lần) + navigate("/login")
 *        ↓
 *   (fallback) nếu không có listener xử lý → hard redirect /login
 */

import {
  clearAuthStorage,
  getSelectionToken,
  getToken,
  getUser,
} from "@/utils/storage";

/**
 * Tên event phát ra khi phiên đăng nhập bị kết thúc bắt buộc.
 * AuthContext lắng nghe event này để đồng bộ React state + điều hướng.
 */
export const SESSION_EXPIRED_EVENT = "ngs:session-expired";

/**
 * Thời gian (ms) tính là "vừa mới xử lý xong session expiry".
 * Dùng để các trang auth-flow (login / chọn tổ chức) tránh
 * xử lý trùng (double toast, double navigate) với interceptor.
 */
export const SESSION_EXPIRY_DEBOUNCE_MS = 1500;

/**
 * Single-flight guard: chỉ cho phép xử lý logout MỘT lần
 * cho toàn bộ các 401/403 trả về đồng thời.
 */
let isHandlingSessionExpiry = false;

let lastSessionExpiryHandledAt = 0;

export const isSessionExpiryHandling = (): boolean =>
  isHandlingSessionExpiry;

/**
 * Reset guard sau khi AuthContext đã xử lý xong (reset state +
 * điều hướng về /login), để các lần hết hạn phiên SAU (đăng nhập
 * lại rồi hết hạn tiếp) vẫn được xử lý bình thường.
 */
export const resetSessionExpiryGuard = (): void => {
  isHandlingSessionExpiry = false;
};

/**
 * Đang ở trang /login hay không.
 *
 * Nếu đã ở /login thì tuyệt đối KHÔNG redirect thêm — tránh
 * vòng lặp /login → API → 401 → /login.
 */
export const isOnLoginPage = (): boolean => {
  if (typeof window === "undefined") {
    return true;
  }

  const path = window.location.pathname;

  return path === "/login" || path.startsWith("/login/");
};

/**
 * Trong storage còn tồn tại dữ liệu phiên đăng nhập nào không.
 *
 * Dùng để tránh kéo user CHƯA đăng nhập (đang xem trang public,
 * trang quên mật khẩu...) về /login khi có request lỗi 401/403
 * từ các API public.
 */
export const hasAuthSession = (): boolean =>
  Boolean(getToken() || getUser() || getSelectionToken());

/**
 * Session expiry vừa được xử lý trong khoảng debounce gần đây?
 *
 * Các trang auth-flow (LoginForm, OrganizationSelectionPage) dùng
 * hàm này để KHÔNG xử lý trùng lỗi 401 mà interceptor đã logout rồi.
 */
export const wasSessionExpiryRecentlyHandled = (): boolean =>
  Date.now() - lastSessionExpiryHandledAt < SESSION_EXPIRY_DEBOUNCE_MS;

/**
 * ============================================================
 * HANDLE SESSION EXPIRY
 * ============================================================
 *
 * Hàm logout tập trung khi phiên đăng nhập không còn hợp lệ.
 * Idempotent: gọi bao nhiêu lần cũng chỉ xử lý MỘT lần.
 */
export const handleSessionExpiry = (
  options: { force?: boolean } = {}
): void => {
  /**
   * 1. Đang xử lý rồi → các 401/403 còn lại chỉ cần bỏ qua.
   */
  if (isSessionExpiryHandling()) {
    return;
  }

  /**
   * 2. Đã ở /login → không logout/redirect thêm (tránh loop).
   */
  if (isOnLoginPage()) {
    return;
  }

  /**
   * 3. Không có phiên đăng nhập nào trong storage → không có gì
   *    để logout (user đang ở trang public/chưa đăng nhập).
   *    force = true dùng cho watchdog khi state vẫn còn nhưng
   *    storage đã bị xóa sạch.
   */
  if (!options.force && !hasAuthSession()) {
    return;
  }

  isHandlingSessionExpiry = true;
  lastSessionExpiryHandledAt = Date.now();

  /**
   * 4. Xóa toàn bộ token + user info khỏi storage.
   */
  clearAuthStorage();

  /**
   * 5. Thông báo cho AuthContext (được mount bên trong Router):
   *    reset React state + toast 1 lần + navigate("/login").
   *
   *    dispatchEvent là ĐỒNG BỘ nên đến khi dòng này chạy xong,
   *    AuthContext đã điều hướng xong và reset guard.
   */
  window.dispatchEvent(new CustomEvent(SESSION_EXPIRED_EVENT));

  /**
   * 6. Fallback an toàn: nếu không có listener nào xử lý được
   *    (ví dụ cây React chưa mount xong), hard redirect sau một
   *    khoảng ngắn để đảm bảo user luôn về /login.
   */
  window.setTimeout(() => {
    if (!isOnLoginPage()) {
      window.location.assign("/login");
    }
  }, 200);
};
