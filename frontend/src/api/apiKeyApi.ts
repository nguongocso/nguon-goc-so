import apiClient from '@/api/axiosConfig';
import type { ApiResult } from '@/types/auth';
import type {
  CreateApiKeyRequest,
  CreateTestApiKeyRequest,
  PartnerApiKeyResponse,
  ApiKeyPageResponse,
  PartnerApiKeyStatus,
} from '@/types/apiKey';

/**
 * Lấy danh sách khóa truy cập API của tổ chức hiện tại
 * GET /api/v1/organization/api-keys
 */
export const getApiKeys = async (
  status?: PartnerApiKeyStatus,
  page = 0,
  size = 10,
): Promise<ApiKeyPageResponse> => {
  const params: Record<string, any> = { page, size };
  if (status) {
    params.status = status;
  }
  const response = await apiClient.get<ApiResult<ApiKeyPageResponse>>(
    '/organization/api-keys',
    { params },
  );
  return response.data.data;
};

/**
 * Cấp khóa truy cập API mới cho đối tác bên thứ ba
 * POST /api/v1/organization/api-keys
 */
export const createApiKey = async (
  data: CreateApiKeyRequest,
): Promise<PartnerApiKeyResponse> => {
  const response = await apiClient.post<ApiResult<PartnerApiKeyResponse>>(
    '/organization/api-keys',
    data,
  );
  return response.data.data;
};

/**
 * Cấp khóa truy cập API thử nghiệm (Sandbox) cho bên thứ ba (NCL-12-CN-004)
 * POST /api/v1/organization/api-keys/test
 */
export const createTestApiKey = async (
  data: CreateTestApiKeyRequest,
): Promise<PartnerApiKeyResponse> => {
  const response = await apiClient.post<ApiResult<PartnerApiKeyResponse>>(
    '/organization/api-keys/test',
    data,
  );
  return response.data.data;
};

/**
 * Thu hồi khóa truy cập API đang hoạt động
 * POST /api/v1/organization/api-keys/{id}/revoke
 */
export const revokeApiKey = async (
  id: string,
): Promise<PartnerApiKeyResponse> => {
  const response = await apiClient.post<ApiResult<PartnerApiKeyResponse>>(
    `/organization/api-keys/${id}/revoke`,
  );
  return response.data.data;
};

/**
 * Lấy thông tin cấu hình Webhook (bao gồm webhookSecret) của một khóa API
 * GET /api/v1/organization/api-keys/{id}/webhook
 */
export const getPartnerWebhook = async (
  apiKeyId: string,
): Promise<import('@/types/apiKey').PartnerWebhookResponse> => {
  const response = await apiClient.get<ApiResult<import('@/types/apiKey').PartnerWebhookResponse>>(
    `/organization/api-keys/${apiKeyId}/webhook`,
  );
  return response.data.data;
};

/**
 * Đăng ký hoặc cập nhật địa chỉ Webhook nhận thông báo thu hồi
 * PUT /api/v1/organization/api-keys/{id}/webhook
 */
export const updatePartnerWebhook = async (
  apiKeyId: string,
  data: import('@/types/apiKey').PartnerWebhookRegistrationRequest,
): Promise<import('@/types/apiKey').PartnerWebhookResponse> => {
  const response = await apiClient.put<ApiResult<import('@/types/apiKey').PartnerWebhookResponse>>(
    `/organization/api-keys/${apiKeyId}/webhook`,
    data,
  );
  return response.data.data;
};

/**
 * Bắn thử nghiệm webhook kiểm tra kết nối (Test Ping)
 * POST /api/v1/organization/api-keys/{id}/webhook/test-ping
 */
export const testPingPartnerWebhook = async (
  apiKeyId: string,
): Promise<import('@/types/apiKey').WebhookTestPingResponse> => {
  const response = await apiClient.post<ApiResult<import('@/types/apiKey').WebhookTestPingResponse>>(
    `/organization/api-keys/${apiKeyId}/webhook/test-ping`,
  );
  return response.data.data;
};

/**
 * Xem lịch sử gửi thông báo thu hồi tới webhook của đối tác
 * GET /api/v1/organization/api-keys/{id}/notifications
 */
export const getPartnerWebhookNotifications = async (
  apiKeyId: string,
  status?: import('@/types/apiKey').WebhookDeliveryStatus,
  page = 0,
  size = 10,
): Promise<import('@/types/apiKey').WebhookNotificationPageResponse> => {
  const params: Record<string, any> = { page, size };
  if (status) {
    params.deliveryStatus = status;
  }
  const response = await apiClient.get<ApiResult<import('@/types/apiKey').WebhookNotificationPageResponse>>(
    `/organization/api-keys/${apiKeyId}/notifications`,
    { params },
  );
  return response.data.data;
};

