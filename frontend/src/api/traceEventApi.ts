import apiClient from './axiosConfig';
import type { HarvestEventPayload, HarvestEventResponse } from '@/types/traceEvent';

/** Ghi nhận sự kiện thu hoạch nông sản POST /api/v1/chain-events/harvest */
export const recordHarvestEvent = async (payload: HarvestEventPayload): Promise<HarvestEventResponse> => {
  const response = await apiClient.post<HarvestEventResponse>('/chain-events/harvest', payload);
  return response.data;
};
