import apiClient from './axiosConfig';
import type {
  ProductCategory,
  ProductCategoryCreateRequest,
  ProductCategoryUpdateRequest,
  ProductCategoryQueryParams,
} from '@/types/productCategory';

export const getProductCategories = async (params?: ProductCategoryQueryParams): Promise<ProductCategory[]> => {
  const response = await apiClient.get<{ data: ProductCategory[] }>('/product-categories', { params });
  return response.data.data;
};

export const createProductCategory = async (data: ProductCategoryCreateRequest): Promise<ProductCategory> => {
  const response = await apiClient.post<{ data: ProductCategory }>('/product-categories', data);
  return response.data.data;
};

export const updateProductCategory = async (id: string, data: ProductCategoryUpdateRequest): Promise<ProductCategory> => {
  const response = await apiClient.put<{ data: ProductCategory }>(`/product-categories/${id}`, data);
  return response.data.data;
};