export interface ProductCategory {
  id: string;
  name: string;
  group: string;
  description: string | null;
  isActive: boolean;
}

export interface ProductCategoryCreateRequest {
  name: string;
  group: string;
  description?: string;
}

export interface ProductCategoryUpdateRequest {
  name: string;
  group: string;
  description?: string;
  isActive: boolean;
}

export interface ProductCategoryQueryParams {
  name?: string;
  categoryGroup?: string;
  isActive?: boolean;
}