import type { FarmArea } from "@/types/farmArea";
import apiClient from "./axiosConfig";

export const getFarmAreas = async (): Promise<FarmArea[]> => {
  const response = await apiClient.get<{ data: FarmArea[] }>('/farm-areas');
  return response.data.data;
};