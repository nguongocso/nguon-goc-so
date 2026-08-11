import type { WarehouseReceiptRequest, WarehouseReceiptResponse } from '@/types/warehouseReceipt';
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