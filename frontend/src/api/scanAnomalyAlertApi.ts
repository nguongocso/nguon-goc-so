import apiClient from './axiosConfig';
import type {
  ResolveScanAnomalyAlertRequest,
  ResolveScanAnomalyAlertResponse,
  ScanAnomalyAlertListResponse,
  ScanAnomalyAlertParams,
} from '@/types/scanAnomalyAlert';

export const getScanAnomalyAlerts = async (
  params: ScanAnomalyAlertParams,
): Promise<ScanAnomalyAlertListResponse> => {
  const response = await apiClient.get<{
  data: ScanAnomalyAlertListResponse;
}>('/alerts', {
  params: {
    ...params,
    type: 'SCAN_ANOMALY',
  },
});
  return response.data.data;
};

export const resolveScanAnomalyAlert = async (
  alertId: string,
  body: ResolveScanAnomalyAlertRequest,
): Promise<ResolveScanAnomalyAlertResponse> => {
  const response = await apiClient.patch<{
    data: ResolveScanAnomalyAlertResponse;
  }>(`/alerts/${alertId}/resolve`, body);
  return response.data.data;
};
