import apiClient from './axiosConfig';
import type {
  AggregateAlertCountResponse,
  AggregateAlertFilterParams,
  AggregateAlertPageResponse,
  UnviewedAlertCountResponse,
} from '@/types/aggregateAlert';

export const getAggregateAlerts = async (
  params: AggregateAlertFilterParams = {},
): Promise<AggregateAlertPageResponse> => {
  const response = await apiClient.get<{
    success: boolean;
    data: AggregateAlertPageResponse;
  }>('/alerts/aggregate', { params });
  return response.data.data;
};

export const getAggregateAlertCounts = async (
  organizationId?: string,
): Promise<AggregateAlertCountResponse> => {
  const response = await apiClient.get<{
    success: boolean;
    data: AggregateAlertCountResponse;
  }>('/alerts/aggregate/counts', {
    params: organizationId ? { organizationId } : {},
  });
  return response.data.data;
};

export const getUnviewedAlertCount = async (): Promise<UnviewedAlertCountResponse> => {
  const response = await apiClient.get<{
    success: boolean;
    data: UnviewedAlertCountResponse;
  }>('/alerts/unviewed-count');
  return response.data.data;
};
