export type PartnerApiKeyStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';

export interface CreateApiKeyRequest {
  partnerName: string;
  rateLimitPerHour: number;
  expiresAt: string; // ISO String: YYYY-MM-DDTHH:mm:ss
}

export interface PartnerApiKeyResponse {
  id: string;
  organizationId: string;
  partnerName: string;
  keyPrefix: string;
  rawApiKey?: string;
  rateLimitPerHour: number;
  expiresAt: string;
  status: PartnerApiKeyStatus;
  totalCalls: number;
  failedCalls: number;
  lastCalledAt?: string | null;
  lastCallStatus?: number | null;
  lastCallIp?: string | null;
  createdByUserId: string;
  createdByFullName: string;
  createdAt: string;
  revokedByUserId?: string | null;
  revokedByFullName?: string | null;
  revokedAt?: string | null;
}

export interface ApiKeyPageResponse {
  content: PartnerApiKeyResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
