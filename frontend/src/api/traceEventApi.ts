import type { HarvestEventPayload, HarvestEventResponse } from '@/types/traceEvent';

import apiClient from './axiosConfig';

/** Ghi nhận sự kiện thu hoạch nông sản. */
export const recordHarvestEvent = async (payload: HarvestEventPayload): Promise<HarvestEventResponse> => {
  const response = await apiClient.post<HarvestEventResponse>('/chain-events/harvest', payload);
  return response.data;
};
