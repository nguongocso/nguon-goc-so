import type { ApiResult } from '@/types/member';
import type { AdministrativeUnitLevel } from '@/types/administrativeUnit';

export interface UserOption {
  userId: string;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  organizationName: string;
}

export interface AssignedArea {
  assignmentId: string;
  unitId: string;
  unitCode: string;
  unitName: string;
  unitLevel: AdministrativeUnitLevel;
  provinceId: string;
  provinceName: string;
  assignedAt: string;
}

export interface AssignAreasRequest {
  unitIds: string[];
}

export interface AssignAreasResult {
  assignedCount: number;
  assigned: AssignedArea[];
  message?: string;
}

export type AreaAssignmentApiResult<T> = ApiResult<T>;
