import type { PageResponse } from '@/types/common';
import type { ApiResponse } from '@/types/api';
import apiClient from './axiosConfig';
import type {
  FarmLog,
  FarmLogQueryParams,
  CreateFarmLogRequest,
  CorrectFarmLogRequest,
  FarmLogResponse,
} from '@/types/farmLog';

// Lỗi được ném về caller; caller phải dùng try/finally + toApiError để tránh treo UI
export const getFarmLogs = async (
  params: FarmLogQueryParams
): Promise<PageResponse<FarmLog>> => {
  const response = await apiClient.get<ApiResponse<PageResponse<FarmLog>>>('/farm-logs', { params });
  return response.data.data;
};

/**
 * NCL-03-CN-006: lấy chi tiết một nhật ký canh tác theo ID (dùng cho trang
 * đính chính /farm-logs/:id/correct).
 */
export const getFarmLogById = async (id: string): Promise<FarmLog> => {
  const response = await apiClient.get<ApiResponse<FarmLog>>(`/farm-logs/${id}`);
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
  const response = await apiClient.post<ApiResponse<FarmLogResponse>>('/farm-logs', payload);
  return response.data.data;
};

export const getHarvestEligibility = async (
  productionLotId: string
): Promise<import('@/types/farmLog').HarvestEligibilityResponse> => {
  const response = await apiClient.get<ApiResponse<import('@/types/farmLog').HarvestEligibilityResponse>>('/farm-logs/harvest-eligibility', { params: { productionLotId } });
  return response.data.data;
};

/**
 * NCL-03-CN-006: Đính chính một nhật ký canh tác.
 */
export const correctFarmLog = async (
  id: string,
  payload: CorrectFarmLogRequest
): Promise<FarmLogResponse> => {
  const response = await apiClient.post<ApiResponse<FarmLogResponse>>(`/farm-logs/${id}/correct`, payload);
  return response.data.data;
};
