import apiClient from './axiosConfig';
import type {
  Standard,
  CreateStandardRequest,
  UpdateStandardRequest,
  StandardListResponse,
} from '@/types/standard';

export const createStandard = async (
  data: CreateStandardRequest
): Promise<Standard> => {
  const response = await apiClient.post<{ data: Standard }>(
    '/standards',
    data
  );
  return response.data.data;
};

export const updateStandard = async (
  standardId: string,
  data: UpdateStandardRequest
): Promise<Standard> => {
  const response = await apiClient.put<{ data: Standard }>(
    `/standards/${standardId}`,
    data
  );
  return response.data.data;
};

export const getStandards = async (params?: {
  isActive?: boolean;
  page?: number;
  size?: number;
}): Promise<StandardListResponse> => {
  const response = await apiClient.get<{ data: StandardListResponse }>(
    '/standards',
    { params }
  );
  return response.data.data;
};

export const getActiveStandards = async (): Promise<Standard[]> => {
  const response = await apiClient.get<{ data: { items: Standard[] } }>(
    '/standards',
    { params: { isActive: true, page: 0, size: 100 } }
  );
  return response.data.data.items;
};