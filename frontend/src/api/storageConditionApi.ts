import type { StorageConditionRequest, StorageConditionResponse } from '@/types/storageCondition';

import apiClient from './axiosConfig';

/** Ghi nhận điều kiện bảo quản / nhiệt độ, độ ẩm của lô hàng. */
export const recordStorageCondition = async (
  data: StorageConditionRequest
): Promise<StorageConditionResponse> => {
  const response = await apiClient.post<{ data: StorageConditionResponse }>(
    '/chain-events/storage-condition',
    data
  );
  return response.data.data;
};
