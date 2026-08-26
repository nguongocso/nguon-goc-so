import type { ProductCategory } from './productCategory';

export const MaterialGroup = {
  PESTICIDE: 'PESTICIDE',
  FERTILIZER: 'FERTILIZER',
  BIOLOGICAL: 'BIOLOGICAL',
  OTHER: 'OTHER',
} as const;

export type MaterialGroup = (typeof MaterialGroup)[keyof typeof MaterialGroup];

export interface InputMaterial {
  id: string;
  name: string;
  materialGroup: MaterialGroup;
  materialGroupDisplayName: string;
  activeIngredient: string | null;
  unit: string;
  quarantineDays: number;
  applyToAllCrops: boolean;
  applicableCropTypes: ProductCategory[];
  referenceSource: string | null;
  imageUrls?: string[];
  isActive: boolean;
  createdBy?: string | null;
  createdAt?: string;
  updatedAt?: string | null;
}

export interface CreateInputMaterialData {
  name: string;
  materialGroup: MaterialGroup;
  activeIngredient?: string;
  unit: string;
  quarantineDays?: number;
  applyToAllCrops?: boolean;
  applicableCropTypeIds?: string[];
  referenceSource?: string;
  imageUrls?: string[];
}

export interface UpdateInputMaterialData {
  name: string;
  materialGroup: MaterialGroup;
  activeIngredient?: string;
  unit: string;
  quarantineDays?: number;
  applyToAllCrops?: boolean;
  applicableCropTypeIds?: string[];
  referenceSource?: string;
  imageUrls?: string[];
  isActive?: boolean;
}

export interface InputMaterialQueryParams {
  keyword?: string;
  group?: MaterialGroup;
  isActive?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface InputMaterialPaginatedResponse {
  content: InputMaterial[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
}
