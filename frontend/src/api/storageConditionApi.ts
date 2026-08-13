import type { StorageConditionRequest, StorageConditionResponse } from '@/types/storageCondition';
import apiClient from './axiosConfig';

export const recordStorageCondition = async (
  data: StorageConditionRequest
): Promise<StorageConditionResponse> => {
  const response = await apiClient.post<{ data: StorageConditionResponse }>(
    '/chain-events/storage-condition',
    data
  );
  return response.data.data;
};