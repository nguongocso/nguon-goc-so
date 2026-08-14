import type { PageResponse } from '@/types/common';
import apiClient from './axiosConfig';
import type {
  FarmLog,
  FarmLogQueryParams,
  CreateFarmLogRequest,
  FarmLogResponse,
} from '@/types/farmLog';

export const getFarmLogs = async (
  params: FarmLogQueryParams
): Promise<PageResponse<FarmLog>> => {
  const response = await apiClient.get<{
    success: boolean;
    data: PageResponse<FarmLog>;
  }>('/farm-logs', { params });
  return response.data.data;
};

export const getAllFarmLogsByProductionLot = async (
  productionLotId: string,
): Promise<FarmLog[]> => {
  const logs: FarmLog[] = [];
  let page = 0;
  let totalPages = 1;

  while (page < totalPages) {
    const response = await getFarmLogs({
      productionLotId,
      page,
      size: 100,
    });

    logs.push(...response.items);
    totalPages = response.totalPages;
    page += 1;
  }

  return logs;
};

export const createFarmLog = async (
  payload: CreateFarmLogRequest
): Promise<FarmLogResponse> => {
  const response = await apiClient.post<{
    success: boolean;
    data: FarmLogResponse;
  }>('/farm-logs', payload);
  return response.data.data;
};