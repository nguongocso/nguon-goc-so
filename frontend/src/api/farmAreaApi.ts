import apiClient from './axiosConfig';
import type { FarmArea, CreateFarmAreaRequest, CropType } from '@/types/farmArea';

// Lấy danh sách vùng trồng (dùng cho VT-02)
export const getFarmAreas = async (): Promise<FarmArea[]> => {
  const response = await apiClient.get<{ data: FarmArea[] }>('/farm-areas');
  return response.data.data;
};

// Tạo vùng trồng mới
export const createFarmArea = async (data: CreateFarmAreaRequest): Promise<FarmArea> => {
  const response = await apiClient.post<{ data: FarmArea }>('/farm-areas', data);
  return response.data.data;
};

// Lấy danh sách loại cây trồng (đã có)
export const getCropTypes = async (): Promise<CropType[]> => {
  const response = await apiClient.get('/product-categories');
  return response.data.data.map((item: any) => ({
    id: item.id,
    name: item.name,
  }));
};