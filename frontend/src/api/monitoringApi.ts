import apiClient from './axiosConfig';
import type { SystemStatusResponse, ThresholdItem } from '@/types/monitoring';

const BASE_URL = '/admin/monitoring';

export const getSystemStatus = async (): Promise<SystemStatusResponse> => {
  const res = await apiClient.get<{ data: SystemStatusResponse }>(
    `${BASE_URL}/system-status`,
  );
  return res.data.data;
};

export const getMonitoringThresholds = async (): Promise<ThresholdItem[]> => {
  const res = await apiClient.get<{ data: ThresholdItem[] }>(
    `${BASE_URL}/thresholds`,
  );
  return res.data.data;
};