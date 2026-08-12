import type { PageResponse } from '@/types/common';

export type LoginAnomalySeverity = 'LOW' | 'MEDIUM' | 'HIGH';

export type LoginAnomalySeverityFilter = LoginAnomalySeverity | 'ALL';

export type LoginAnomalyAccountStatus = 'ACTIVE' | 'LOCKED';

export type LoginAnomalyAccountStatusFilter = LoginAnomalyAccountStatus | 'ALL';

export interface LoginAnomaly {
  id: string;
  username: string;
  fullName: string;
  organizationId: string;
  organizationName: string;
  ipAddress: string;
  location: string;
  reason: string;
  severity: LoginAnomalySeverity;
  loginAt: string;
  accountStatus: LoginAnomalyAccountStatus;
}

export interface LoginAnomalyParams {
  severity?: LoginAnomalySeverityFilter;
  accountStatus?: LoginAnomalyAccountStatusFilter;
  keyword?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface LoginAnomalyListResponse extends PageResponse<LoginAnomaly> {}

export interface LockLoginAnomalyResponse {
  id: string;
  username: string;
  accountStatus: 'LOCKED';
  lockedAt: string;
  lockedBy: string;
}
