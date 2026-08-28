export interface Standard {
  id: string;
  name: string;
  description: string | null;
  issuingBody: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
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