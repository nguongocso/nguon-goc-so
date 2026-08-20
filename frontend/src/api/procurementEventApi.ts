import apiClient from './axiosConfig';
import type { RecordProcurementEventRequest, ChainEventResponse } from '@/types/procurementEvent';

/**
 * Ghi nhận sự kiện thu mua
 * POST /api/v1/chain-events/procurement
 */
export const recordProcurementEvent = async (
  request: RecordProcurementEventRequest
): Promise<ChainEventResponse> => {
  const response = await apiClient.post<{ success: boolean; data: ChainEventResponse }>(
    '/chain-events/procurement',
    request
  );
  return response.data.data;
};