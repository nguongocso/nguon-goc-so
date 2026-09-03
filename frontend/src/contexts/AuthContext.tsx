import type { AuthUserInfo, LoginUserInfo } from "@/types/auth";

import {
  getSelectionToken,
  getToken,
  getUser,
  removeToken,
  removeSelectionToken,
  setSelectionToken,
  setToken,
  setUser,
} from "@/utils/storage";

import React, {
  createContext,
  useEffect,
  useCallback,
  useState,
  type ReactNode,
} from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

import { getCurrent } from "@/api/authApi";

import {
  handleSessionExpiry,
  resetSessionExpiryGuard,
  SESSION_EXPIRED_EVENT,
} from "@/utils/session";

interface AuthContextType {
  user: AuthUserInfo | null;

  token: string | null;

  selectionToken: string | null;

  isLoading: boolean;

  /**
   * Sau khi username/password đúng.
   */
  loginWithSelection: (
    selectionToken: string,
    user: LoginUserInfo
  ) => void;

  /**
   * Sau khi user chọn organization.
   */
  completeLogin: (
    accessToken: string,
    user: AuthUserInfo
  ) => void;

  updateUser: (user: AuthUserInfo) => void;

  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | undefined>(
  undefined
);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [user, setUserState] = useState<AuthUserInfo | null>(getUser());

  const [token, setTokenState] = useState<string | null>(getToken());

  const [selectionToken, setSelectionTokenState] = useState<string | null>(
    getSelectionToken()
  );

  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const storedToken = getToken();
    const storedSelectionToken = getSelectionToken();
    const storedUser = getUser();

    if (storedToken && storedUser) {
      setTokenState(storedToken);
      setUserState(storedUser);

      // Đồng bộ thông tin mới nhất từ backend (phone, email)
      getCurrent()
        .then((res) => {
          if (res.success && res.data) {
            setUser(res.data);
            setUserState(res.data);
          }
        })
        .catch(() => {});
    }

    if (storedSelectionToken) {
      setSelectionTokenState(storedSelectionToken);
    }

    setIsLoading(false);
  }, []);

  /**
   * BƯỚC 1:
   * Username/password đã xác thực.
   * Chưa có Access JWT.
   */
  const loginWithSelection = useCallback((
    selectionTokenValue: string,
  ) => {
    setSelectionToken(selectionTokenValue);
    setSelectionTokenState(selectionTokenValue);

    // Chưa lưu user vào AuthUserInfo vì chưa có organization.
    // console.log("Authenticated user:", loginUser);
  }, []);

  /**
   * BƯỚC 3:
   * Organization đã được chọn.
   * Backend cấp Access JWT.
   */
  const completeLogin = useCallback((
    accessToken: string,
    userData: AuthUserInfo
  ) => {
    setToken(accessToken);
    setUser(userData);

    setTokenState(accessToken);
    setUserState(userData);

    removeSelectionToken();
    setSelectionTokenState(null);

    // Reset trạng thái xem thông báo email cho phiên đăng nhập mới
    if (userData?.userId) {
      sessionStorage.removeItem(`session_read_email_notice_${userData.userId}`);
    }
  }, []);

  const updateUser = useCallback((updatedUserData: AuthUserInfo) => {
    setUser(updatedUserData);
    setUserState(updatedUserData);
  }, []);

  const logout = useCallback(() => {
    if (user?.userId) {
      sessionStorage.removeItem(`session_read_email_notice_${user.userId}`);
    }

    removeToken();
    removeSelectionToken();

    setTokenState(null);
    setSelectionTokenState(null);
    setUserState(null);
  }, [user?.userId]);

  /**
   * ============================================================
   * LOGOUT TẬP TRUNG KHI PHIÊN ĐĂNG NHẬP HẾT HẠN / MẤT TOKEN
   * ============================================================
   *
   * handleSessionExpiry() (frontend/src/utils/session.ts) được gọi
   * từ axios response interceptor khi protected API trả 401 hoặc
   * 403 chưa-xác-thực (token thiếu / hết hạn / không hợp lệ), hoặc
   * từ watchdog phía dưới khi token bị xóa khỏi localStorage.
   *
   * handleSessionExpiry() đã:
   *   - clear access_token + selection_token + user_info (storage)
   *
   * Listener này chịu trách nhiệm phần React:
   *   - reset user/token/selectionToken state (logout())
   *   - toast thông báo CHỈ MỘT lần (guard đảm bảo idempotent)
   *   - điều hướng về /login bằng SPA navigation (không reload,
   *     không loop — đã ở /login thì handleSessionExpiry bỏ qua)
   */
  const navigate = useNavigate();

  useEffect(() => {
    const handleSessionExpired = () => {
      logout();

      navigate("/login", { replace: true });

      toast.error(
        "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
      );

      /**
       * Guard đã xử lý xong: cho phép xử lý các lần hết hạn
       * phiên TIẾP THEO (sau khi user đăng nhập lại).
       */
      resetSessionExpiryGuard();
    };

    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);

    return () => {
      window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
    };
  }, [logout, navigate]);

  /**
   * ============================================================
   * WATCHDOG — PHÁT HIỆN TOKEN BỊ XÓA KHỎI STORAGE
   * ============================================================
   *
   * React state không tự re-render khi localStorage bị thay đổi
   * (ví dụ: localStorage.removeItem("access_token") từ DevTools).
   *
   * Khi vẫn đang "đăng nhập" trong state nhưng storage đã mất
   * access token / user info → phiên không còn hợp lệ → logout
   * tập trung + về /login mà không cần chờ request API lỗi.
   *
   * (PrivateRoute không phát hiện được trường hợp này vì nó chỉ
   * đọc user từ context.)
   */
  useEffect(() => {
    if (!token) {
      return;
    }

    const intervalId = window.setInterval(() => {
      if (!getToken() || !getUser()) {
        handleSessionExpiry({ force: true });
      }
    }, 2000);

    return () => window.clearInterval(intervalId);
  }, [token]);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        selectionToken,
        isLoading,
        loginWithSelection,
        completeLogin,
        updateUser,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};