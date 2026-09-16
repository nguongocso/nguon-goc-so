export type PartnerApiKeyStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';

export interface CreateApiKeyRequest {
  partnerName: string;
  rateLimitPerHour: number;
  expiresAt: string; // ISO String: YYYY-MM-DDTHH:mm:ss
}

/**
 * Yêu cầu cấp khóa thử nghiệm (Sandbox API Key) cho bên thứ ba (NCL-12-CN-004)
 */
export interface CreateTestApiKeyRequest {
  partnerName: string;
  rateLimitPerHour?: number;
  expiresAt?: string;
  expireDays?: number;
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
  isTest?: boolean;
  is_test?: boolean;
  totalCalls: number;
  failedCalls: number;
  /** Số lượt gọi trong ngày hôm nay (chỉ trả ở danh sách khóa - NCL-12-CN-005) */
  usedCallsToday?: number | null;
  /** Ngưỡng lượt gọi trong ngày chạm mức cảnh báo hạn mức (chỉ trả ở danh sách khóa - NCL-12-CN-005) */
  quotaWarningThreshold?: number | null;
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
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

