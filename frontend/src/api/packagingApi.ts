import type { ChainEventResponse, CorrectPackagingRequest, RecordPackagingRequest } from '@/types/packaging';
import type { ProductionLot } from '@/types/productionLot';

import apiClient from './axiosConfig';

/** Ghi nhận sự kiện đóng gói. */
export const recordPackagingEvent = async (
  data: RecordPackagingRequest
): Promise<ChainEventResponse> => {
  const response = await apiClient.post<{ data: ChainEventResponse }>('/chain-events/packaging', data);
  return response.data.data;
};

/** Đính chính sự kiện đóng gói đã ghi nhận. */
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

/** Lấy danh sách lô sản xuất đủ điều kiện đóng gói (đã thu hoạch hoặc đã sơ chế). */
export const getHarvestedProductionLots = async (): Promise<ProductionLot[]> => {
  const response = await apiClient.get<{ data: ProductionLot[] }>('/production-lots');
  const lots = response.data.data;
  return lots.filter(
    (lot) => lot.status === 'HARVESTED' || lot.status === 'PREPROCESSED',
  );
};
