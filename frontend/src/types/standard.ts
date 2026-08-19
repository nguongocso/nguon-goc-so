export interface Standard {
  id: string;
  name: string;
  description: string | null;
  issuingBody: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InspectionCriterion {
  criteriaId: number;
  code: string;
  name: string;
  standardId: string;
  standardName: string;
  note?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface InspectionCriterionRequest {
  standardId?: string;
  criterionCode: string;
  criterionName: string;
  note?: string;
}

export interface CreateStandardRequest {
  name: string;
  description?: string;
  issuingBody?: string;
}

export interface UpdateStandardRequest {
  name: string;
  description?: string;
  issuingBody?: string;
  isActive: boolean;
}

export interface StandardListResponse {
  items: Standard[];
  page: number;
  size: number;
  totalElements: number;
}