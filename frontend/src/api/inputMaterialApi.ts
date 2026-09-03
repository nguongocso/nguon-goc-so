import apiClient from './axiosConfig';
import type {
  InputMaterial,
  CreateInputMaterialData,
  UpdateInputMaterialData,
  InputMaterialQueryParams,
  InputMaterialPaginatedResponse,
} from '@/types/inputMaterial';

export const getInputMaterials = async (
  params?: InputMaterialQueryParams
): Promise<InputMaterialPaginatedResponse> => {
  const response = await apiClient.get<{ data: InputMaterialPaginatedResponse }>('/input-materials', {
    params,
  });
  return response.data.data;
};

export const getInputMaterialById = async (id: string): Promise<InputMaterial> => {
  const response = await apiClient.get<{ data: InputMaterial }>(`/input-materials/${id}`);
  return response.data.data;
};

export const createInputMaterial = async (data: CreateInputMaterialData): Promise<InputMaterial> => {
  const response = await apiClient.post<{ data: InputMaterial }>('/input-materials', data);
  return response.data.data;
};

export const updateInputMaterial = async (
  id: string,
  data: UpdateInputMaterialData
): Promise<InputMaterial> => {
  const response = await apiClient.put<{ data: InputMaterial }>(`/input-materials/${id}`, data);
  return response.data.data;
};

export const toggleInputMaterialStatus = async (
  id: string,
  isActive: boolean
): Promise<InputMaterial> => {
  const response = await apiClient.patch<{ data: InputMaterial }>(
    `/input-materials/${id}/status`,
    null,
    { params: { isActive } }
  );
  return response.data.data;
};

export const deleteInputMaterial = async (id: string): Promise<void> => {
  await apiClient.delete(`/input-materials/${id}`);
};
