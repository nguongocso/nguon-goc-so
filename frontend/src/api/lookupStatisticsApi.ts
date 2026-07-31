import apiClient from './axiosConfig';
import type {
  LookupStatisticsResponse,
  AbnormalScansPage,
  LookupStatisticsParams,
} from '@/types/lookupStatistics';

export const getLookupStatistics = async (
  params: LookupStatisticsParams
): Promise<LookupStatisticsResponse> => {
  const response = await apiClient.get<{ data: LookupStatisticsResponse }>(
    '/reports/lookup-statistics',
    { params }
  );
  return response.data.data;
};

export const getAbnormalScans = async (
  params: LookupStatisticsParams & { page: number; size: number }
): Promise<AbnormalScansPage> => {
  const response = await apiClient.get<{ data: AbnormalScansPage }>(
    '/reports/lookup-statistics/abnormal',
    { params }
  );
  return response.data.data;
};