import apiClient from "./axiosConfig";
import type { PageResponse } from "@/types/common";
import type { LoginHistoryItem, LoginHistoryParams } from "@/types/loginHistory";

export const getLoginHistory = async (
  params: LoginHistoryParams
): Promise<PageResponse<LoginHistoryItem>> => {
  const response = await apiClient.get<{ data: PageResponse<LoginHistoryItem> }>(
    "/auth/security/login-history",
    { params }
  );

  return response.data.data;
};
