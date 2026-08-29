import { act, render } from "@testing-library/react";
import { useLocation, MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { AuthProvider } from "@/contexts/AuthContext";
import { useAuth } from "@/hooks/useAuth";
import {
  handleSessionExpiry,
  resetSessionExpiryGuard,
  SESSION_EXPIRED_EVENT,
} from "@/utils/session";

const toastErrorMock = vi.fn();

vi.mock("sonner", () => ({
  toast: { error: (...args: unknown[]) => toastErrorMock(...args) },
}));

vi.mock("@/api/authApi", () => ({
  getCurrent: vi.fn().mockResolvedValue({
    success: false,
    status: 401,
    message: "",
    data: null,
    timestamp: "",
  }),
}));

/**
 * Probe hiển thị pathname hiện tại + trạng thái auth để kiểm tra
 * AuthProvider sau khi nhận SESSION_EXPIRED_EVENT.
 */
const LocationProbe = () => {
  const location = useLocation();
  const { user, token, selectionToken } = useAuth();

  return (
    <div>
      <span data-testid="pathname">{location.pathname}</span>
      <span data-testid="auth-user">{user ? "user-set" : "user-null"}</span>
      <span data-testid="auth-token">{token ? "token-set" : "token-null"}</span>
      <span data-testid="auth-selection">
        {selectionToken ? "selection-set" : "selection-null"}
      </span>
    </div>
  );
};

const seedSession = (): void => {
  localStorage.setItem("access_token", "access-jwt");
  localStorage.setItem("selection_token", "selection-jwt");
  localStorage.setItem(
    "user_info",
    JSON.stringify({
      userId: "u-1",
      username: "user1",
      fullName: "Người dùng 1",
      roleCode: "VT-02",
      roleName: "Quản lý HTX",
      organizationId: "org-1",
      organizationCode: "HTX1",
      organizationName: "HTX 1",
      organizationType: "COOPERATIVE",
    })
  );
};

describe("contexts/AuthContext — phiên đăng nhập hết hạn", () => {
  beforeEach(() => {
    localStorage.clear();
    toastErrorMock.mockReset();
    resetSessionExpiryGuard();
  });

  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it("SESSION_EXPIRED_EVENT → clear storage + reset state + navigate /login + toast 1 lần", async () => {
    seedSession();

    const { findByTestId } = render(
      <MemoryRouter initialEntries={["/dashboard"]}>
        <AuthProvider>
          <LocationProbe />
        </AuthProvider>
      </MemoryRouter>
    );

    // Đã đăng nhập khi mount
    expect((await findByTestId("auth-token")).textContent).toBe("token-set");

    // Phát event session expired (đồng bộ như axios interceptor gọi)
    act(() => {
      window.dispatchEvent(new CustomEvent(SESSION_EXPIRED_EVENT));
    });

    // Storage bị xóa sạch
    expect(localStorage.getItem("access_token")).toBeNull();
    expect(localStorage.getItem("selection_token")).toBeNull();
    expect(localStorage.getItem("user_info")).toBeNull();

    // React state reset
    expect((await findByTestId("auth-user")).textContent).toBe("user-null");
    expect((await findByTestId("auth-token")).textContent).toBe("token-null");
    expect((await findByTestId("auth-selection")).textContent).toBe("selection-null");

    // Điều hướng về /login
    expect((await findByTestId("pathname")).textContent).toBe("/login");

    // Toast đúng và chỉ MỘT lần
    expect(toastErrorMock).toHaveBeenCalledTimes(1);
    expect(toastErrorMock).toHaveBeenCalledWith(
      "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
    );
  });

  it("2 API cùng trả 401 → handleSessionExpiry 2 lần → chỉ toast 1 lần (single-flight)", async () => {
    seedSession();

    const { findByTestId } = render(
      <MemoryRouter initialEntries={["/dashboard"]}>
        <AuthProvider>
          <LocationProbe />
        </AuthProvider>
      </MemoryRouter>
    );

    expect((await findByTestId("auth-token")).textContent).toBe("token-set");

    // Giống 2 request đồng thời trả 401 cùng lúc
    act(() => {
      handleSessionExpiry();
      handleSessionExpiry();
    });

    expect(toastErrorMock).toHaveBeenCalledTimes(1);
    expect((await findByTestId("auth-token")).textContent).toBe("token-null");
  });
});