import apiClient from './axiosConfig';
import type { StorageConditionRequest, StorageConditionResponse } from '@/types/storageCondition';

/**
 * Ghi nhận điều kiện bảo quản / nhiệt độ, độ ẩm của lô hàng
 * POST /api/v1/chain-events/storage-condition
 */
export const recordStorageCondition = async (
  data: StorageConditionRequest
): Promise<StorageConditionResponse> => {
  const response = await apiClient.post<{ data: StorageConditionResponse }>(
    '/chain-events/storage-condition',
    data
  );
  return response.data.data;
};
