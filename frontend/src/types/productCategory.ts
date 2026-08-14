export interface ProductCategory {
  id: string;
  name: string;
  group: string;
  description: string | null;
  isActive: boolean;
  tempMin?: number;
  tempMax?: number;
  humidityMin?: number;
  humidityMax?: number;
}

export interface ProductCategoryCreateRequest {
  name: string;
  group: string;
  description?: string;
  tempMin?: number;
  tempMax?: number;
  humidityMin?: number;
  humidityMax?: number;
}

export interface ProductCategoryUpdateRequest {
  name: string;
  group: string;
  description?: string;
  isActive: boolean;
  tempMin?: number;
  tempMax?: number;
  humidityMin?: number;
  humidityMax?: number;
}

export interface ProductCategoryQueryParams {
  name?: string;
  categoryGroup?: string;
  isActive?: boolean;
}