import apiClient from './axiosConfig';
import type { FarmArea, CreateFarmAreaRequest, UpdateFarmAreaRequest, CropType } from '@/types/farmArea';

// Lấy danh sách vùng trồng
export const getFarmAreas = async (activeOnly?: boolean): Promise<FarmArea[]> => {
  const response = await apiClient.get<{ data: FarmArea[] }>('/farm-areas', {
    params: activeOnly !== undefined ? { activeOnly } : undefined,
  });
  return response.data.data;
};

// Lấy chi tiết vùng trồng theo ID
export const getFarmAreaById = async (id: string): Promise<FarmArea> => {
  const response = await apiClient.get<{ data: FarmArea }>(`/farm-areas/${id}`);
  return response.data.data;
};

// Tạo vùng trồng mới
export const createFarmArea = async (data: CreateFarmAreaRequest): Promise<FarmArea> => {
  const response = await apiClient.post<{ data: FarmArea }>('/farm-areas', data);
  return response.data.data;
};

// Cập nhật thông tin vùng trồng (US NCL-02-CN-005)
export const updateFarmArea = async (id: string, data: UpdateFarmAreaRequest): Promise<FarmArea> => {
  const response = await apiClient.put<{ data: FarmArea }>(`/farm-areas/${id}`, data);
  return response.data.data;
};

// Đổi trạng thái kích hoạt / ngừng sử dụng vùng trồng (US NCL-02-CN-005)
export const toggleFarmAreaStatus = async (id: string, isActive: boolean): Promise<FarmArea> => {
  const response = await apiClient.patch<{ data: FarmArea }>(`/farm-areas/${id}/status`, null, {
    params: { isActive },
  });
  return response.data.data;
};

// Xóa vùng trồng (US NCL-02-CN-005)
export const deleteFarmArea = async (id: string): Promise<void> => {
  await apiClient.delete(`/farm-areas/${id}`);
};

// Lấy danh sách loại cây trồng (đã có)
export const getCropTypes = async (): Promise<CropType[]> => {
  const response = await apiClient.get('/product-categories');
  return response.data.data.map((item: any) => ({
    id: item.id,
    name: item.name,
  }));
};