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

export const getAllThresholds = async (): Promise<AllThresholdsResponse> => {
  const response = await apiClient.get<{ data: AllThresholdsResponse }>(BASE_PREFIX);
  return response.data.data;
};

export const getGlobalThreshold = async (): Promise<AnomalyThresholdConfig> => {
  const response = await apiClient.get<{ data: AnomalyThresholdConfig }>(`${BASE_PREFIX}/global`);
  return response.data.data;
};

export const updateGlobalThreshold = async (
  payload: UpdateGlobalThresholdRequest,
): Promise<AnomalyThresholdConfig> => {
  const response = await apiClient.put<{ data: AnomalyThresholdConfig }>(
    `${BASE_PREFIX}/global`,
    payload,
  );
  return response.data.data;
};

export const getCategoryOverrides = async (): Promise<AnomalyThresholdConfig[]> => {
  const response = await apiClient.get<{ data: AnomalyThresholdConfig[] }>(
    `${BASE_PREFIX}/categories`,
  );
  return response.data.data;
};

export const saveCategoryOverride = async (
  payload: CategoryThresholdOverrideRequest,
): Promise<AnomalyThresholdConfig> => {
  const response = await apiClient.post<{ data: AnomalyThresholdConfig }>(
    `${BASE_PREFIX}/categories`,
    payload,
  );
  return response.data.data;
};

export const deleteCategoryOverride = async (id: string): Promise<void> => {
  await apiClient.delete(`${BASE_PREFIX}/categories/${id}`);
};

export const estimateImpact = async (
  payload: ImpactEstimationRequest,
): Promise<ImpactEstimationResult> => {
  const response = await apiClient.post<{ data: ImpactEstimationResult }>(
    `${BASE_PREFIX}/estimate`,
    payload,
  );
  return response.data.data;
};
