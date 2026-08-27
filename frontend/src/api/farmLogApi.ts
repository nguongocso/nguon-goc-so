import type { PageResponse } from '@/types/common';
import apiClient from './axiosConfig';
import type {
  FarmLog,
  FarmLogQueryParams,
  CreateFarmLogRequest,
  CorrectFarmLogRequest,
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

/**
 * NCL-03-CN-006: Đính chính một nhật ký canh tác.
 */
export const correctFarmLog = async (
  id: string,
  payload: CorrectFarmLogRequest
): Promise<FarmLogResponse> => {
  const response = await apiClient.post<{
    success: boolean;
    data: FarmLogResponse;
  }>(`/farm-logs/${id}/correct`, payload);
  return response.data.data;
};