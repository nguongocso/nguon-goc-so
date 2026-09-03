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
  /** Cờ bắt buộc kiểm nghiệm khi kích hoạt tem (NCL-09-CN-009). */
  requiresInspection?: boolean;
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