import type { AuthUserInfo } from "@/types/auth";

const ACCESS_TOKEN_KEY = "access_token";
const SELECTION_TOKEN_KEY = "selection_token";
const USER_KEY = "user_info";

// =========================
// ACCESS TOKEN
// =========================

export const setToken = (token: string): void => {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
};

export const getToken = (): string | null => {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
};

export const removeToken = (): void => {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
};

// =========================
// SELECTION TOKEN
// =========================

export const setSelectionToken = (token: string): void => {
  localStorage.setItem(SELECTION_TOKEN_KEY, token);
};

export const getSelectionToken = (): string | null => {
  return localStorage.getItem(SELECTION_TOKEN_KEY);
};

export const removeSelectionToken = (): void => {
  localStorage.removeItem(SELECTION_TOKEN_KEY);
};

// =========================
// USER
// =========================

export const setUser = (user: AuthUserInfo): void => {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
};

export const getUser = (): AuthUserInfo | null => {
  const data = localStorage.getItem(USER_KEY);

  if (!data || data === "undefined" || data === "null") {
    return null;
  }

  try {
    return JSON.parse(data) as AuthUserInfo;
  } catch (error) {
    // console.error(
    //   "Invalid user data in localStorage:",
    //   error
    // );

    localStorage.removeItem(USER_KEY);

    return null;
  }
};

// =========================
// CLEAR AUTH DATA
// =========================

export const clearAuthStorage = (): void => {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(SELECTION_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
};