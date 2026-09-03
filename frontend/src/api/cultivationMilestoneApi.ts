import apiClient from './axiosConfig';
import type { PageResponse } from '@/types/common';
import type {
  CultivationMilestone,
  CultivationMilestoneRequest,
  CultivationMilestoneQueryParams,
} from '@/types/cultivationMilestone';

/**
 * Lấy danh sách mốc canh tác (phân trang, lọc theo keyword/activityType/category/standard)
 * GET /api/v1/cultivation-milestones
 */
export const getCultivationMilestones = async (
  params?: CultivationMilestoneQueryParams
): Promise<PageResponse<CultivationMilestone>> => {
  const response = await apiClient.get<{
    data: PageResponse<CultivationMilestone>;
  }>('/cultivation-milestones', { params });
  return response.data.data;
};

/**
 * Chi tiết mốc canh tác
 * GET /api/v1/cultivation-milestones/{id}
 */
export const getCultivationMilestone = async (
  id: number
): Promise<CultivationMilestone> => {
  const response = await apiClient.get<{ data: CultivationMilestone }>(
    `/cultivation-milestones/${id}`
  );
  return response.data.data;
};

/**
 * Tạo mốc canh tác (chỉ PLATFORM_ADMIN — VT-01)
 * POST /api/v1/cultivation-milestones
 */
export const createCultivationMilestone = async (
  data: CultivationMilestoneRequest
): Promise<CultivationMilestone> => {
  const response = await apiClient.post<{ data: CultivationMilestone }>(
    '/cultivation-milestones',
    data
  );
  return response.data.data;
};

/**
 * Cập nhật mốc canh tác (chỉ PLATFORM_ADMIN — VT-01)
 * PUT /api/v1/cultivation-milestones/{id}
 */
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
