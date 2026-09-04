import apiClient from './axiosConfig';
import type {
  AllThresholdsResponse,
  AnomalyThresholdConfig,
  CategoryThresholdOverrideRequest,
  ImpactEstimationRequest,
  ImpactEstimationResult,
  UpdateGlobalThresholdRequest,
} from '@/types/anomalyThreshold';

const BASE_PREFIX = '/admin/anomaly-thresholds';

/**
 * Lấy toàn bộ cấu hình ngưỡng (Global + Overrides).
 */
export const getAllThresholds = async (): Promise<AllThresholdsResponse> => {
  const response = await apiClient.get<{ data: AllThresholdsResponse }>(BASE_PREFIX);
  return response.data.data;
};

/**
 * Lấy cấu hình ngưỡng mặc định toàn cục.
 */
export const getGlobalThreshold = async (): Promise<AnomalyThresholdConfig> => {
  const response = await apiClient.get<{ data: AnomalyThresholdConfig }>(`${BASE_PREFIX}/global`);
  return response.data.data;
};

/**
 * Cập nhật cấu hình ngưỡng mặc định toàn cục.
 */
export const updateGlobalThreshold = async (
  payload: UpdateGlobalThresholdRequest,
): Promise<AnomalyThresholdConfig> => {
  const response = await apiClient.put<{ data: AnomalyThresholdConfig }>(
    `${BASE_PREFIX}/global`,
    payload,
  );
  return response.data.data;
};

/**
 * Lấy danh sách cấu hình ghi đè theo danh mục nông sản.
 */
export const getCategoryOverrides = async (): Promise<AnomalyThresholdConfig[]> => {
  const response = await apiClient.get<{ data: AnomalyThresholdConfig[] }>(
    `${BASE_PREFIX}/categories`,
  );
  return response.data.data;
};

/**
 * Tạo mới hoặc cập nhật cấu hình ghi đè theo danh mục nông sản.
 */
export const saveCategoryOverride = async (
  payload: CategoryThresholdOverrideRequest,
): Promise<AnomalyThresholdConfig> => {
  const response = await apiClient.post<{ data: AnomalyThresholdConfig }>(
    `${BASE_PREFIX}/categories`,
    payload,
  );
  return response.data.data;
};

/**
 * Xóa cấu hình ghi đè danh mục nông sản (quay về dùng ngưỡng toàn cục).
 */
export const deleteCategoryOverride = async (id: string): Promise<void> => {
  await apiClient.delete(`${BASE_PREFIX}/categories/${id}`);
};

/**
 * Ước lượng tác động của bộ ngưỡng dự thảo trên dữ liệu 30 ngày gần nhất (dry-run).
 */
export const estimateImpact = async (
  payload: ImpactEstimationRequest,
): Promise<ImpactEstimationResult> => {
  const response = await apiClient.post<{ data: ImpactEstimationResult }>(
    `${BASE_PREFIX}/estimate`,
    payload,
  );
  return response.data.data;
};
