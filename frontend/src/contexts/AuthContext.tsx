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
  }, []);

  const updateUser = useCallback((updatedUserData: AuthUserInfo) => {
    setUser(updatedUserData);
    setUserState(updatedUserData);
  }, []);

  const logout = useCallback(() => {
    removeToken();
    removeSelectionToken();

    setTokenState(null);
    setSelectionTokenState(null);
    setUserState(null);
  }, []);

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