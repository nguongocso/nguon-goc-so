import apiClient from './axiosConfig';
import type { ApiResponse } from '@/types/api';
import type {
  FarmArea,
  CreateFarmAreaRequest,
  UpdateFarmAreaRequest,
  CropType,
  FarmAreaBoundaryResponse,
  UpdateFarmAreaBoundaryRequest,
} from '@/types/farmArea';

// Lấy danh sách vùng trồng
// Lỗi được ném về caller; caller phải dùng try/finally + toApiError để tránh treo UI
export const getFarmAreas = async (activeOnly?: boolean): Promise<FarmArea[]> => {
  const response = await apiClient.get<ApiResponse<FarmArea[]>>('/farm-areas', {
    params: activeOnly !== undefined ? { activeOnly } : undefined,
  });
  return response.data.data;
};

// Lấy chi tiết vùng trồng theo ID
export const getFarmAreaById = async (id: string): Promise<FarmArea> => {
  const response = await apiClient.get<ApiResponse<FarmArea>>(`/farm-areas/${id}`);
  return response.data.data;
};

// Tạo vùng trồng mới
export const createFarmArea = async (data: CreateFarmAreaRequest): Promise<FarmArea> => {
  const response = await apiClient.post<ApiResponse<FarmArea>>('/farm-areas', data);
  return response.data.data;
};

// Cập nhật thông tin vùng trồng (US NCL-02-CN-005)
export const updateFarmArea = async (id: string, data: UpdateFarmAreaRequest): Promise<FarmArea> => {
  const response = await apiClient.put<ApiResponse<FarmArea>>(`/farm-areas/${id}`, data);
  return response.data.data;
};

// Đổi trạng thái kích hoạt / ngừng sử dụng vùng trồng (US NCL-02-CN-005)
export const toggleFarmAreaStatus = async (id: string, isActive: boolean): Promise<FarmArea> => {
  const response = await apiClient.patch<ApiResponse<FarmArea>>(`/farm-areas/${id}/status`, null, {
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
  interface ProductCategoryItem {
    id: string;
    name: string;
  }
  const response = await apiClient.get<ApiResponse<ProductCategoryItem[]>>('/product-categories');
  return response.data.data.map((item: ProductCategoryItem) => ({
    id: item.id,
    name: item.name,
  }));
};

/** Lấy ranh giới vùng trồng (CV-04). */
export const getFarmAreaBoundary = async (id: string): Promise<FarmAreaBoundaryResponse> => {
  const response = await apiClient.get<ApiResponse<FarmAreaBoundaryResponse>>(`/farm-areas/${id}/boundary`);
  return response.data.data;
};

/** Cập nhật ranh giới vùng trồng (CV-04). */
export const updateFarmAreaBoundary = async (
  id: string,
  data: UpdateFarmAreaBoundaryRequest
): Promise<FarmAreaBoundaryResponse> => {
  const response = await apiClient.put<ApiResponse<FarmAreaBoundaryResponse>>(`/farm-areas/${id}/boundary`, data);
  return response.data.data;
};