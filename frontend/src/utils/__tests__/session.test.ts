import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  handleSessionExpiry,
  hasAuthSession,
  isOnLoginPage,
  isSessionExpiryHandling,
  resetSessionExpiryGuard,
  SESSION_EXPIRED_EVENT,
} from "@/utils/session";

/**
 * jsdom không cho phép spy trực tiếp window.location.assign
 * (property không configurable) nên phải thay thế toàn bộ
 * window.location bằng một object đơn giản.
 */
const defineLocationMock = (
  pathname = "/"
): { pathname: string; assign: ReturnType<typeof vi.fn> } => {
  const locationMock = {
    pathname,
    assign: vi.fn(),
  };

  Object.defineProperty(window, "location", {
    configurable: true,
    value: locationMock,
  });

  return locationMock;
};

/**
 * Seed một phiên đăng nhập hợp lệ trong localStorage
 * (giống hệt các key mà frontend/src/utils/storage.ts quản lý).
 */
const seedSession = (): void => {
  localStorage.setItem("access_token", "access-jwt");
  localStorage.setItem("selection_token", "selection-jwt");
  localStorage.setItem("user_info", JSON.stringify({ userId: "u-1" }));
};

describe("utils/session — handleSessionExpiry", () => {
  let locationMock: { pathname: string; assign: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    resetSessionExpiryGuard();
    locationMock = defineLocationMock("/");
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it("clear toàn bộ token + user info và phát event khi phiên hết hạn", () => {
    seedSession();

    const handler = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, handler);

    handleSessionExpiry();

    expect(localStorage.getItem("access_token")).toBeNull();
    expect(localStorage.getItem("selection_token")).toBeNull();
    expect(localStorage.getItem("user_info")).toBeNull();
    expect(handler).toHaveBeenCalledTimes(1);
    expect(isSessionExpiryHandling()).toBe(true);
  });

  it("nhiều lời gọi đồng thời (nhiều 401 cùng lúc) → chỉ xử lý MỘT lần", () => {
    seedSession();

    const handler = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, handler);

    handleSessionExpiry();
    handleSessionExpiry();
    handleSessionExpiry();

    expect(handler).toHaveBeenCalledTimes(1);
    expect(isSessionExpiryHandling()).toBe(true);
  });

  it("đang ở /login → KHÔNG logout/redirect (tránh vòng lặp redirect)", () => {
    seedSession();
    locationMock.pathname = "/login";

    const handler = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, handler);

    handleSessionExpiry();

    expect(handler).not.toHaveBeenCalled();
    expect(localStorage.getItem("access_token")).toBe("access-jwt");
    expect(isOnLoginPage()).toBe(true);
  });

  it("storage rỗng (chưa đăng nhập) → bỏ qua, không kéo user public về /login", () => {
    const handler = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, handler);

    handleSessionExpiry();

    expect(handler).not.toHaveBeenCalled();
    expect(hasAuthSession()).toBe(false);
  });

  it("force = true vẫn xử lý khi storage đã bị xóa sạch (watchdog)", () => {
    const handler = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, handler);

    handleSessionExpiry({ force: true });

    expect(handler).toHaveBeenCalledTimes(1);
  });

  it("resetSessionExpiryGuard cho phép xử lý lần hết hạn tiếp theo", () => {
    seedSession();

    const handler = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, handler);

    handleSessionExpiry();
    resetSessionExpiryGuard();
    seedSession();
    handleSessionExpiry();

    expect(handler).toHaveBeenCalledTimes(2);
  });

  it("lên lịch hard redirect /login nếu không có listener điều hướng SPA", () => {
    vi.useFakeTimers();
    seedSession();

    handleSessionExpiry();

    // AuthContext không mounted → không ai điều hướng → fallback phải chạy
    vi.advanceTimersByTime(250);

    expect(locationMock.assign).toHaveBeenCalledWith("/login");
    vi.useRealTimers();
  });

  it("fallback KHÔNG redirect khi đã ở /login", () => {
    vi.useFakeTimers();
    seedSession();
    locationMock.pathname = "/login";

    handleSessionExpiry();
    resetSessionExpiryGuard();
    vi.advanceTimersByTime(250);

    expect(locationMock.assign).not.toHaveBeenCalled();
    vi.useRealTimers();
  });
});
