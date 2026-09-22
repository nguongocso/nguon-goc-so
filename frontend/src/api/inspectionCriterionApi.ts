import apiClient from './axiosConfig';
import type { PageResponse } from '@/types/common';
import type { ProductCategory } from '@/types/productCategory';
import type {
  InspectionCriterion,
  InspectionCriterionRequest,
  InspectionCriterionQueryParams,
  InspectionExpiryThresholdResponse,
  UpdateInspectionExpiryThresholdRequest,
} from '@/types/inspectionCriterion';

export const getInspectionCriteria = async (
  params?: InspectionCriterionQueryParams
): Promise<PageResponse<InspectionCriterion>> => {
  const response = await apiClient.get<{
    data: PageResponse<InspectionCriterion>;
  }>('/inspection-criteria', { params });
  return response.data.data;
};

export const getInspectionCriterion = async (
  id: number
): Promise<InspectionCriterion> => {
  const response = await apiClient.get<{ data: InspectionCriterion }>(
    `/inspection-criteria/${id}`
  );
  return response.data.data;
};

export const createInspectionCriterion = async (
  data: InspectionCriterionRequest
): Promise<InspectionCriterion> => {
  const response = await apiClient.post<{ data: InspectionCriterion }>(
    '/inspection-criteria',
    data
  );
  return response.data.data;
};

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

export const disableInspectionCriterion = async (
  id: number
): Promise<void> => {
  await apiClient.put(`/inspection-criteria/${id}/disable`);
};

export const enableInspectionCriterion = async (
  id: number
): Promise<InspectionCriterion> => {
  const response = await apiClient.put<{ data: InspectionCriterion }>(
    `/inspection-criteria/${id}/enable`
  );
  return response.data.data;
};

export const deleteInspectionCriterion = async (id: number): Promise<void> => {
  await apiClient.delete(`/inspection-criteria/${id}`);
};

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

export const getInspectionExpiryThreshold = async (): Promise<InspectionExpiryThresholdResponse> => {
  const response = await apiClient.get<{ data: InspectionExpiryThresholdResponse }>(
    '/inspection-criteria/expiry-threshold'
  );
  return response.data.data;
};

export const updateInspectionExpiryThreshold = async (
  data: UpdateInspectionExpiryThresholdRequest
): Promise<InspectionExpiryThresholdResponse> => {
  const response = await apiClient.put<{ data: InspectionExpiryThresholdResponse }>(
    '/inspection-criteria/expiry-threshold',
    data
  );
  return response.data.data;
};

