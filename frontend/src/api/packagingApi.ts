import apiClient from './axiosConfig';
import type { ChainEventResponse, CorrectPackagingRequest, RecordPackagingRequest } from '@/types/packaging';
import type { ProductionLot } from '@/types/productionLot';

/**
 * Ghi nhận sự kiện đóng gói
 * POST /api/v1/chain-events/packaging
 */
export const recordPackagingEvent = async (
  data: RecordPackagingRequest
): Promise<ChainEventResponse> => {
  const response = await apiClient.post<{ data: ChainEventResponse }>('/chain-events/packaging', data);
  return response.data.data;
};

/**
 * Đính chính sự kiện đóng gói đã ghi nhận
 * POST /api/v1/chain-events/packaging/{originalEventId}/correct
 */
export const correctPackagingEvent = async (
  originalEventId: string,
  data: CorrectPackagingRequest
): Promise<ChainEventResponse> => {
  const response = await apiClient.post<{ data: ChainEventResponse }>(
    `/chain-events/packaging/${originalEventId}/correct`,
    data,
  );
  return response.data.data;
};

/**
 * Lấy danh sách lô sản xuất đủ điều kiện đóng gói (đã thu hoạch hoặc đã sơ chế)
 * GET /api/v1/production-lots
 */
export const getHarvestedProductionLots = async (): Promise<ProductionLot[]> => {
  const response = await apiClient.get<{ data: ProductionLot[] }>('/production-lots');
  const lots = response.data.data;
  return lots.filter(
    (lot) => lot.status === 'HARVESTED' || lot.status === 'PREPROCESSED',
  );
};
