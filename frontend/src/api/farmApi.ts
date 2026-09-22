import type { FarmArea } from "@/types/farmArea";
import type { ApiResponse } from "@/types/api";
import apiClient from "./axiosConfig";

// Lỗi được ném về caller; caller phải dùng try/finally + toApiError để tránh treo UI
export const getFarmAreas = async (): Promise<FarmArea[]> => {
  const response = await apiClient.get<ApiResponse<FarmArea[]>>('/farm-areas');
  return response.data.data;
};