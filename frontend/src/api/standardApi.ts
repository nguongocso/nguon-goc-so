import apiClient from './axiosConfig';
import type {
  Standard,
  CreateStandardRequest,
  UpdateStandardRequest,
  StandardListResponse,
  InspectionCriterion,
  InspectionCriterionRequest,
} from '@/types/standard';

/**
 * Tạo mới tiêu chuẩn
 * POST /api/v1/standards
 */
export const createStandard = async (
  data: CreateStandardRequest
): Promise<Standard> => {
  const response = await apiClient.post<{ data: Standard }>(
    '/standards',
    data
  );
  return response.data.data;
};

/**
 * Cập nhật tiêu chuẩn
 * PUT /api/v1/standards/{standardId}
 */
export const updateStandard = async (
  standardId: string,
  data: UpdateStandardRequest
): Promise<Standard> => {
  const response = await apiClient.put<{ data: Standard }>(
    `/standards/${standardId}`,
    data
  );
  return response.data.data;
};

/**
 * Lấy danh sách tiêu chuẩn (có phân trang, lọc)
 * GET /api/v1/standards
 */
export const getStandards = async (params?: {
  isActive?: boolean;
  page?: number;
  size?: number;
}): Promise<StandardListResponse> => {
  const response = await apiClient.get<{ data: StandardListResponse }>(
    '/standards',
    { params }
  );
  return response.data.data;
};

export const getActiveStandards = async (): Promise<Standard[]> => {
  const response = await apiClient.get<{ data: { items: Standard[] } }>(
    '/standards',
    { params: { isActive: true, page: 0, size: 100 } }
  );
  return response.data.data.items;
};

export const getStandardCriteria = async (
  standardId: string
): Promise<InspectionCriterion[]> => {
  const response = await apiClient.get<{ data: InspectionCriterion[] }>(
    `/standards/${standardId}/criteria`
  );
  return response.data.data;
};

export const createStandardCriterion = async (
  standardId: string,
  data: InspectionCriterionRequest
): Promise<InspectionCriterion> => {
  const response = await apiClient.post<{ data: InspectionCriterion }>(
    `/standards/${standardId}/criteria`,
    {
      ...data,
      standardId,
    }
  );
  return response.data.data;
};

export const updateStandardCriterion = async (
  standardId: string,
  criteriaId: number,
  data: InspectionCriterionRequest
): Promise<InspectionCriterion> => {
  const response = await apiClient.put<{ data: InspectionCriterion }>(
    `/standards/${standardId}/criteria/${criteriaId}`,
    {
      ...data,
      standardId,
    }
  );
  return response.data.data;
};

export const deleteStandardCriterion = async (
  standardId: string,
  criteriaId: number
): Promise<void> => {
  await apiClient.delete(`/standards/${standardId}/criteria/${criteriaId}`);
};