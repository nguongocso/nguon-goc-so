import apiClient from './axiosConfig';
import type { PageResponse } from '@/types/common';
import type {
  CultivationMilestone,
  CultivationMilestoneRequest,
  CultivationMilestoneQueryParams,
  MilestoneEligibilityResponse,
} from '@/types/cultivationMilestone';

export const getCultivationMilestones = async (
  params?: CultivationMilestoneQueryParams
): Promise<PageResponse<CultivationMilestone>> => {
  const response = await apiClient.get<{
    data: PageResponse<CultivationMilestone>;
  }>('/cultivation-milestones', { params });
  return response.data.data;
};

export const getCultivationMilestone = async (
  id: number
): Promise<CultivationMilestone> => {
  const response = await apiClient.get<{ data: CultivationMilestone }>(
    `/cultivation-milestones/${id}`
  );
  return response.data.data;
};

export const createCultivationMilestone = async (
  data: CultivationMilestoneRequest
): Promise<CultivationMilestone> => {
  const response = await apiClient.post<{ data: CultivationMilestone }>(
    '/cultivation-milestones',
    data
  );
  return response.data.data;
};

export const updateCultivationMilestone = async (
  id: number,
  data: CultivationMilestoneRequest
): Promise<CultivationMilestone> => {
  const response = await apiClient.put<{ data: CultivationMilestone }>(
    `/cultivation-milestones/${id}`,
    data
  );
  return response.data.data;
};

export const getPackagingEligibility = async (
  productionLotId: string
): Promise<MilestoneEligibilityResponse> => {
  const response = await apiClient.get<{ data: MilestoneEligibilityResponse }>(
    '/cultivation-milestones/eligibility',
    { params: { productionLotId } }
  );
  return response.data.data;
};
