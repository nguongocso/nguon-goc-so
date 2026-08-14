import type { WarehouseReceiptRequest, WarehouseReceiptResponse } from '@/types/warehouseReceipt';
import type { PageResponse } from '@/types/common';
import apiClient from './axiosConfig';

export const recordWarehouseReceipt = async (
  data: WarehouseReceiptRequest
): Promise<WarehouseReceiptResponse> => {
  const response = await apiClient.post<{ data: WarehouseReceiptResponse }>(
    '/chain-events/warehouse-receipt',
    data
  );
  return response.data.data;
};

export const getWarehouseReceipts = async (
  page: number = 0,
  size: number = 10
): Promise<PageResponse<WarehouseReceiptResponse>> => {
  const response = await apiClient.get<{ data: PageResponse<WarehouseReceiptResponse> }>(
    '/chain-events/warehouse-receipts',
    { params: { page, size, sort: 'recordedAt,desc' } }
  );
  return response.data.data;
};

export const getWarehouseReceiptDetail = async (
  eventId: string
): Promise<WarehouseReceiptResponse> => {
  const response = await apiClient.get<{ data: WarehouseReceiptResponse }>(
    `/chain-events/warehouse-receipts/${eventId}`
  );
  return response.data.data;
};
