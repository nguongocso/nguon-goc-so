import apiClient from './axiosConfig';
import type { RecordMobileEventRequest } from '@/types/chainEvent';
import type { ChainEventResponse } from '@/types/packaging';
import type { OfflineSyncRequest, OfflineSyncResponse } from '@/types/offlineEvent';
import type { ScanLookupResponse } from '@/types/scan';

/** Lấy danh sách dòng thời gian sự kiện của lô hàng GET /api/v1/shipments/{shipmentId}/chain-events */
export const getShipmentTimeline = async (shipmentId: string): Promise<ChainEventResponse[]> => {
  const response = await apiClient.get<{ data: ChainEventResponse[] }>(
    `/shipments/${shipmentId}/chain-events`
  );
  return response.data.data;
};

/** Ghi nhận sự kiện chuỗi cung ứng từ thiết bị di động POST /api/v1/chain-events/mobile */
export const recordMobileEvent = async (
  data: RecordMobileEventRequest
): Promise<ChainEventResponse> => {
  const response = await apiClient.post<{ data: ChainEventResponse }>(
    '/chain-events/mobile',
    data
  );
  return response.data.data;
};

/** Đồng bộ danh sách sự kiện ngoại tuyến POST /api/v1/chain-events/sync */
export const syncOfflineEvents = async (
  payload: OfflineSyncRequest
): Promise<OfflineSyncResponse> => {
  const response = await apiClient.post<{ data: OfflineSyncResponse }>(
    '/chain-events/sync',
    payload
  );
  return response.data.data;
};

/** Tra cứu thông tin từ mã truy xuất khi quét mã GET /api/v1/chain-events/scan-lookup?codeValue={code} */
export const scanLookupTraceCode = async (
  code: string,
): Promise<ScanLookupResponse> => {
  const response = await apiClient.get<{ data: ScanLookupResponse }>(
    '/chain-events/scan-lookup',
    { params: { codeValue: code } },
  );

  return response.data.data;
};
