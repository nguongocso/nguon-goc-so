export interface CodeRange {
  id: string;
  organizationId: string;
  organizationName: string;
  prefix: string;
  totalLimit: number;
  usedCount: number;
  createdAt: string;
}

export interface CreateCodeRangeRequest {
  organizationId: string;
  prefix: string;
  totalLimit: number;
}

export interface CodeRangeStatusResponse {
  id: string;
  organizationId: string;
  organizationName: string;
  prefix: string;
  totalLimit: number;
  usedCount: number;
  usagePercent: number;
  status: 'OK' | 'NEARLY_EXHAUSTED' | 'EXHAUSTED';
}

export interface CreateCodeRangeFormValues {
  organizationId: string;
  prefix: string;
  totalLimit: number;
}

export interface RemainingCodesResponse {
  remainingCount: number;
  totalLimit: number;
  usedCount: number;
  hasCodeRange: boolean;
}