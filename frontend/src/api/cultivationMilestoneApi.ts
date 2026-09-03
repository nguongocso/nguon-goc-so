import apiClient from './axiosConfig';
import type { PageResponse } from '@/types/common';
import type {
  CultivationMilestone,
  CultivationMilestoneRequest,
  CultivationMilestoneQueryParams,
  ProductCategoryMilestone,
  CategoryMilestoneRequest,
} from '@/types/cultivationMilestone';

/**
 * Lấy danh sách mốc canh tác (phân trang, lọc theo keyword/status/activityType)
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

/**
 * Ngừng sử dụng mốc canh tác (chỉ PLATFORM_ADMIN — VT-01)
 * PUT /api/v1/cultivation-milestones/{id}/disable
 */
export const disableCultivationMilestone = async (
  id: number
): Promise<void> => {
  await apiClient.put(`/cultivation-milestones/${id}/disable`);
};

/**
 * Kích hoạt lại mốc canh tác (chỉ PLATFORM_ADMIN — VT-01)
 * PUT /api/v1/cultivation-milestones/{id}/enable
 */
export const enableCultivationMilestone = async (
  id: number
): Promise<CultivationMilestone> => {
  const response = await apiClient.put<{ data: CultivationMilestone }>(
    `/cultivation-milestones/${id}/enable`
  );
  return response.data.data;
};

/**
 * Xóa mốc canh tác chưa tham chiếu (chỉ PLATFORM_ADMIN — VT-01)
 * DELETE /api/v1/cultivation-milestones/{id}
 */
export const deleteCultivationMilestone = async (id: number): Promise<void> => {
  await apiClient.delete(`/cultivation-milestones/${id}`);
};

/**
 * Lấy danh sách mốc canh tác đã gán cho loại nông sản
 * GET /api/v1/product-categories/{categoryId}/milestones
 */
export const getProductCategoryMilestones = async (
  categoryId: string
): Promise<ProductCategoryMilestone[]> => {
  const response = await apiClient.get<{ data: ProductCategoryMilestone[] }>(
    `/product-categories/${categoryId}/milestones`
  );
  return response.data.data;
};

/**
 * Gán (thay thế toàn bộ) mốc canh tác cho loại nông sản
 * (chỉ PLATFORM_ADMIN — VT-01)
 * PUT /api/v1/product-categories/{categoryId}/milestones
 */
export const assignProductCategoryMilestones = async (
  categoryId: string,
  request: CategoryMilestoneRequest
): Promise<ProductCategoryMilestone[]> => {
  const response = await apiClient.put<{ data: ProductCategoryMilestone[] }>(
    `/product-categories/${categoryId}/milestones`,
    request
  );
  return response.data.data;
};
