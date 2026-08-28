import apiClient from './axiosConfig';
import type { PageResponse } from '@/types/common';
import type { ProductCategory } from '@/types/productCategory';
import type {
  InspectionCriterion,
  InspectionCriterionRequest,
  InspectionCriterionQueryParams,
} from '@/types/inspectionCriterion';

/**
 * Lấy danh sách chỉ tiêu kiểm nghiệm (phân trang, lọc theo keyword/status)
 * GET /api/v1/inspection-criteria
 */
export const getInspectionCriteria = async (
  params?: InspectionCriterionQueryParams
): Promise<PageResponse<InspectionCriterion>> => {
  const response = await apiClient.get<{
    data: PageResponse<InspectionCriterion>;
  }>('/inspection-criteria', { params });
  return response.data.data;
};

/**
 * Chi tiết chỉ tiêu kiểm nghiệm
 * GET /api/v1/inspection-criteria/{id}
 */
export const getInspectionCriterion = async (
  id: number
): Promise<InspectionCriterion> => {
  const response = await apiClient.get<{ data: InspectionCriterion }>(
    `/inspection-criteria/${id}`
  );
  return response.data.data;
};

/**
 * Tạo chỉ tiêu kiểm nghiệm (chỉ PLATFORM_ADMIN — VT-01)
 * POST /api/v1/inspection-criteria
 */
export const createInspectionCriterion = async (
  data: InspectionCriterionRequest
): Promise<InspectionCriterion> => {
  const response = await apiClient.post<{ data: InspectionCriterion }>(
    '/inspection-criteria',
    data
  );
  return response.data.data;
};

/**
 * Cập nhật chỉ tiêu kiểm nghiệm (chỉ PLATFORM_ADMIN — VT-01)
 * PUT /api/v1/inspection-criteria/{id}
 */
export const updateInspectionCriterion = async (
  id: number,
  data: InspectionCriterionRequest
): Promise<InspectionCriterion> => {
  const response = await apiClient.put<{ data: InspectionCriterion }>(
    `/inspection-criteria/${id}`,
    data
  );
  return response.data.data;
};

/**
 * Ngừng sử dụng chỉ tiêu kiểm nghiệm (chỉ PLATFORM_ADMIN — VT-01)
 * PUT /api/v1/inspection-criteria/{id}/disable
 */
export const disableInspectionCriterion = async (
  id: number
): Promise<void> => {
  await apiClient.put(`/inspection-criteria/${id}/disable`);
};

/**
 * Kích hoạt lại chỉ tiêu kiểm nghiệm (chỉ PLATFORM_ADMIN — VT-01)
 * PUT /api/v1/inspection-criteria/{id}/enable
 */
export const enableInspectionCriterion = async (
  id: number
): Promise<InspectionCriterion> => {
  const response = await apiClient.put<{ data: InspectionCriterion }>(
    `/inspection-criteria/${id}/enable`
  );
  return response.data.data;
};

/**
 * Xóa chỉ tiêu kiểm nghiệm chưa tham chiếu (chỉ PLATFORM_ADMIN — VT-01).
 * Backend trả 409 nếu chỉ tiêu đang được yêu cầu kiểm nghiệm tham chiếu.
 * DELETE /api/v1/inspection-criteria/{id}
 */
export const deleteInspectionCriterion = async (id: number): Promise<void> => {
  await apiClient.delete(`/inspection-criteria/${id}`);
};

/**
 * Lấy bộ chỉ tiêu đã gán cho loại nông sản
 * GET /api/v1/product-categories/{categoryId}/criteria
 */
export const getProductCategoryCriteria = async (
  categoryId: string,
  activeOnly = true
): Promise<InspectionCriterion[]> => {
  const response = await apiClient.get<{ data: InspectionCriterion[] }>(
    `/product-categories/${categoryId}/criteria`,
    { params: { activeOnly } }
  );
  return response.data.data;
};

/**
 * Gán (thay thế toàn bộ) bộ chỉ tiêu mặc định cho loại nông sản
 * (chỉ PLATFORM_ADMIN — VT-01)
 * PUT /api/v1/product-categories/{categoryId}/criteria
 */
export const assignProductCategoryCriteria = async (
  categoryId: string,
  criterionIds: number[]
): Promise<InspectionCriterion[]> => {
  const response = await apiClient.put<{ data: InspectionCriterion[] }>(
    `/product-categories/${categoryId}/criteria`,
    { criterionIds }
  );
  return response.data.data;
};

/**
 * Bật/tắt cờ bắt buộc kiểm nghiệm của loại nông sản
 * (chỉ PLATFORM_ADMIN — VT-01). Backend trả 400 nếu bật khi chưa có chỉ tiêu.
 * PUT /api/v1/product-categories/{categoryId}/mandatory-inspection
 */
export const setMandatoryInspection = async (
  categoryId: string,
  required: boolean
): Promise<ProductCategory> => {
  const response = await apiClient.put<{ data: ProductCategory }>(
    `/product-categories/${categoryId}/mandatory-inspection`,
    { required }
  );
  return response.data.data;
};
