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
  lastCalledAt?: string | null;
  lastCallStatus?: number | null;
  lastCallIp?: string | null;
  createdByUserId?: string;
  createdByFullName?: string;
  createdByName?: string;
  createdAt: string;
  revokedByUserId?: string | null;
  revokedByFullName?: string | null;
  revokedByName?: string | null;
  revokedAt?: string | null;
  webhookUrl?: string | null;
  webhookSecret?: string | null;
  isWebhookActive?: boolean;
}

export interface ApiKeyPageResponse {
  content: PartnerApiKeyResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/**
 * Yêu cầu cấu hình địa chỉ nhận thông báo Webhook (NCL-12-CN-006)
 */
export interface PartnerWebhookRegistrationRequest {
  webhookUrl: string;
  isActive?: boolean;
}

/**
 * Phản hồi thông tin cấu hình Webhook (NCL-12-CN-006)
 */
export interface PartnerWebhookResponse {
  id: string;
  partnerName: string;
  keyPrefix: string;
  webhookUrl: string;
  isWebhookActive: boolean;
  webhookSecret: string;
  updatedAt?: string;
}

/**
 * Kết quả kiểm tra bắn thử nghiệm Webhook (Test Ping) (NCL-12-CN-006)
 */
export interface WebhookTestPingResponse {
  targetUrl: string;
  httpStatus?: number | null;
  durationMs: number;
  isSuccess: boolean;
  responseBody?: string | null;
  errorMessage?: string | null;
}

export type WebhookDeliveryStatus = 'PENDING_RETRY' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

/**
 * Chi tiết một lần thử gửi Webhook
 */
export interface PartnerWebhookAttemptItem {
  attemptNumber: number;
  attemptedAt: string;
  httpStatus?: number | null;
  responseBody?: string | null;
  errorMessage?: string | null;
  durationMs: number;
}

/**
 * Thông tin bản ghi gửi thông báo Webhook tới đối tác
 */
export interface PartnerWebhookNotificationResponse {
  id: string;
  partnerApiKeyId: string;
  partnerName: string;
  shipmentId?: string | null;
  lotCode: string;
  newStatus: string;
  targetUrl: string;
  publicReason?: string | null;
  deliveryStatus: WebhookDeliveryStatus;
  attemptCount: number;
  maxAttempts: number;
  nextRetryAt?: string | null;
  lastHttpStatus?: number | null;
  lastErrorMessage?: string | null;
  createdAt: string;
  completedAt?: string | null;
  attempts?: PartnerWebhookAttemptItem[];
}

export interface WebhookNotificationPageResponse {
  content: PartnerWebhookNotificationResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
