import { AxiosError, AxiosHeaders, type AxiosResponse, type InternalAxiosRequestConfig } from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { FulfilledFn, RejectedFn } from "@/types/testInterceptor";

const handleSessionExpiryMock = vi.fn();
const removeSelectionTokenMock = vi.fn();
const getTokenMock = vi.fn();
const getSelectionTokenMock = vi.fn();

vi.mock("@/config/runtimeConfig", () => ({
  getApiBaseUrl: () => "http://backend.test/api/v1",
}));

vi.mock("@/utils/session", () => ({
  handleSessionExpiry: (...args: unknown[]) => handleSessionExpiryMock(...args),
}));

vi.mock("@/utils/storage", () => ({
  getToken: (...args: unknown[]) => getTokenMock(...args),
  getSelectionToken: (...args: unknown[]) => getSelectionTokenMock(...args),
  removeSelectionToken: (...args: unknown[]) => removeSelectionTokenMock(...args),
}));

// Phải import sau khi mock (vi.mock được hoisted).
import apiClient from "@/api/axiosConfig";

const getResponseRejected = (): RejectedFn => {
  const handlers = (
    apiClient.interceptors.response as unknown as {
      handlers: { rejected?: RejectedFn }[];
    }
  ).handlers;
  const rejected = handlers[0]?.rejected;
  if (!rejected) {
    throw new Error("Response interceptor không được đăng ký.");
  }
  return rejected;
};

const getRequestFulfilled = (): FulfilledFn => {
  const handlers = (
    apiClient.interceptors.request as unknown as {
      handlers: { fulfilled?: FulfilledFn }[];
    }
  ).handlers;
  const fulfilled = handlers[0]?.fulfilled;
  if (!fulfilled) {
    throw new Error("Request interceptor không được đăng ký.");
  }
  return fulfilled;
};

const makeAxiosError = (status: number, url: string, data: unknown): AxiosError => {
  const error = new AxiosError("Request failed", "ERR_BAD_REQUEST");
  error.config = { url, headers: new AxiosHeaders() } as InternalAxiosRequestConfig;
  error.response = {
    status,
    data,
    statusText: "",
    headers: {},
    config: error.config,
  } as AxiosResponse;
  return error;
};

const makeRequestConfig = (url: string): InternalAxiosRequestConfig =>
  ({ url, headers: new AxiosHeaders() }) as InternalAxiosRequestConfig;

describe("api/axiosConfig — response interceptor", () => {
  beforeEach(() => {
    handleSessionExpiryMock.mockReset();
    removeSelectionTokenMock.mockReset();
    getTokenMock.mockReset();
    getSelectionTokenMock.mockReset();
  });

  it("401 từ /auth/login (sai mật khẩu) → KHÔNG logout/clear/redirect", async () => {
    const rejected = getResponseRejected();
    const error = makeAxiosError(401, "/auth/login", {
      success: false,
      status: 401,
      message: "Sai username hoặc mật khẩu",
    });

    await expect(rejected(error)).rejects.toBe(error);
    expect(handleSessionExpiryMock).not.toHaveBeenCalled();
    expect(removeSelectionTokenMock).not.toHaveBeenCalled();
  });

  it("401 từ ORG_SELECTION flow → chỉ xóa selection token, không logout toàn cục", async () => {
    const rejected = getResponseRejected();
    const error = makeAxiosError(401, "/auth/organizations", {
      success: false,
      status: 401,
      message: "Selection token hết hạn",
    });

    await expect(rejected(error)).rejects.toBe(error);
    expect(removeSelectionTokenMock).toHaveBeenCalledTimes(1);
    expect(handleSessionExpiryMock).not.toHaveBeenCalled();
  });

  it("401 từ protected API → logout tập trung", async () => {
    const rejected = getResponseRejected();
    const error = makeAxiosError(401, "/farm-areas", {
      success: false,
      status: 401,
      message: "Unauthorized",
    });

    await expect(rejected(error)).rejects.toBe(error);
    expect(handleSessionExpiryMock).toHaveBeenCalledTimes(1);
  });

  it("403 KHÔNG có body ApiResult (Spring entry point) → logout tập trung", async () => {
    const rejected = getResponseRejected();
    const error = makeAxiosError(403, "/production-lots", {
      timestamp: "2026-01-01T00:00:00",
      status: 403,
      error: "Forbidden",
      path: "/api/v1/production-lots",
    });

    await expect(rejected(error)).rejects.toBe(error);
    expect(handleSessionExpiryMock).toHaveBeenCalledTimes(1);
  });

  it("403 CÓ body ApiResult (phân quyền nghiệp vụ) → KHÔNG logout", async () => {
    const rejected = getResponseRejected();
    const error = makeAxiosError(403, "/admin/monitoring", {
      success: false,
      status: 403,
      message: "Bạn không có quyền thực hiện chức năng này",
      path: "/api/v1/admin/monitoring",
    });

    await expect(rejected(error)).rejects.toBe(error);
    expect(handleSessionExpiryMock).not.toHaveBeenCalled();
  });

  it("403 có body không phải object (rỗng/chuỗi) → không phải ApiResult → logout", async () => {
    const rejected = getResponseRejected();
    const emptyBody = makeAxiosError(403, "/farming-logs", "");

    await expect(rejected(emptyBody)).rejects.toBe(emptyBody);
    expect(handleSessionExpiryMock).toHaveBeenCalledTimes(1);
  });

  it("lỗi không phải 401/403 → KHÔNG logout", async () => {
    const rejected = getResponseRejected();
    const error = makeAxiosError(500, "/farm-areas", {
      success: false,
      status: 500,
      message: "Internal Server Error",
    });

    await expect(rejected(error)).rejects.toBe(error);
    expect(handleSessionExpiryMock).not.toHaveBeenCalled();
  });
});

describe("api/axiosConfig — request interceptor", () => {
  beforeEach(() => {
    handleSessionExpiryMock.mockReset();
    removeSelectionTokenMock.mockReset();
    getTokenMock.mockReset();
    getSelectionTokenMock.mockReset();
  });

  it("request tới /auth/login → không đính Authorization header", () => {
    getTokenMock.mockReturnValue("access-jwt");

    const fulfilled = getRequestFulfilled();
    const config = fulfilled(
      makeRequestConfig("/auth/login")
    ) as InternalAxiosRequestConfig;

    expect(config.headers.has("Authorization")).toBe(false);
  });

  it("request tới /auth/organizations → dùng SELECTION token", () => {
    getSelectionTokenMock.mockReturnValue("selection-jwt");

    const fulfilled = getRequestFulfilled();
    const config = fulfilled(
      makeRequestConfig("/auth/organizations")
    ) as InternalAxiosRequestConfig;

    expect(config.headers.get("Authorization")).toBe("Bearer selection-jwt");
  });

  it("request protected → đính ACCESS token", () => {
    getTokenMock.mockReturnValue("access-jwt");

    const fulfilled = getRequestFulfilled();
    const config = fulfilled(
      makeRequestConfig("/farm-areas")
    ) as InternalAxiosRequestConfig;

    expect(config.headers.get("Authorization")).toBe("Bearer access-jwt");
  });

  it("request protected nhưng không có access token → không đính Authorization", () => {
    getTokenMock.mockReturnValue(null);

    const fulfilled = getRequestFulfilled();
    const config = fulfilled(
      makeRequestConfig("/farm-areas")
    ) as InternalAxiosRequestConfig;

    expect(config.headers.has("Authorization")).toBe(false);
  });
});
