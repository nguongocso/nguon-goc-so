import apiClient from './axiosConfig';
import type {
  AggregateAlertCountResponse,
  AggregateAlertFilterParams,
  AggregateAlertPageResponse,
  UnviewedAlertCountResponse,
} from '@/types/aggregateAlert';

/**
 * Lấy danh sách cảnh báo tổng hợp gom từ cả 7 nguồn (NCL-08-CN-016).
 */
export const getAggregateAlerts = async (
  params: AggregateAlertFilterParams = {},
): Promise<AggregateAlertPageResponse> => {
  const response = await apiClient.get<{
    success: boolean;
    data: AggregateAlertPageResponse;
  }>('/alerts/aggregate', { params });
  return response.data.data;
};

/**
 * Lấy thống kê số lượng cảnh báo theo mức độ khẩn cấp và theo loại.
 */
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

/**
 * Lấy số lượng cảnh báo chưa xử lý phục vụ huy hiệu đếm trên thanh điều hướng.
 */
export const getUnviewedAlertCount = async (): Promise<UnviewedAlertCountResponse> => {
  const response = await apiClient.get<{
    success: boolean;
    data: UnviewedAlertCountResponse;
  }>('/alerts/unviewed-count');
  return response.data.data;
};
