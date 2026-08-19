export type LoginAnomalyStatus = 'OPEN' | 'DISMISSED';

export interface LoginAnomalyItem {
  id: string;
  userId: string;
  username: string;
  fullName: string;
  roleCode?: string;
  organizationId?: string;
  organizationName?: string;
  reasonCode: 'REPEATED_FAILED_LOGIN' | 'UNUSUAL_COUNTRY';
  attemptCount?: number;
  ipAddress: string;
  countryCode?: string;
  detectedAt: string;
  status: LoginAnomalyStatus;
  accountLocked?: boolean;
  lockUntil?: string;
  permanentLock?: boolean;
  notificationId?: string;
}

export interface SuspiciousCaseItem {
  id: string;
  userId: string;
  username: string;
  fullName: string;
  organizationId?: string;
  organizationName?: string;
  status: LoginAnomalyStatus;
  anomalyCount: number;
  firstDetectedAt: string;
  lastDetectedAt: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface LoginAnomalyFilters {
  status?: LoginAnomalyStatus;
  reasonCode?: string;
  username?: string;
  page?: number;
  size?: number;
}

export interface SuspiciousCaseFilters {
  status?: LoginAnomalyStatus | 'ALL';
  username?: string;
  page?: number;
  size?: number;
}
