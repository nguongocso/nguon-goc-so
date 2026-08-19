import type { ChainEventResponse, CorrectPackagingRequest, RecordPackagingRequest } from '@/types/packaging';
import type { ProductionLot } from '@/types/productionLot';
import apiClient from './axiosConfig';

export const recordPackagingEvent = async (
  data: RecordPackagingRequest
): Promise<ChainEventResponse> => {
  const response = await apiClient.post('/chain-events/packaging', data);
  return response.data.data; // ApiResult wrapper, lấy data
};

export const correctPackagingEvent = async (
  originalEventId: string,
  data: CorrectPackagingRequest
): Promise<ChainEventResponse> => {
  const response = await apiClient.post(`/chain-events/packaging/${originalEventId}/correct`, data);
  return response.data.data;
};

// Lô đã thu hoạch có thể đóng gói trực tiếp; lô đã sơ chế cũng tiếp tục
// được phép đóng gói theo chu trình HARVESTED -> PREPROCESSED -> PACKAGED.
export const getHarvestedProductionLots = async (): Promise<ProductionLot[]> => {
  const response = await apiClient.get('/production-lots');
  const lots = response.data.data as ProductionLot[];
  return lots.filter(
    (lot) => lot.status === 'HARVESTED' || lot.status === 'PREPROCESSED',
  );
};
