import apiClient from '@/api/axiosConfig';
import type { ApiResult } from '@/types/auth';
import type {
  CreateApiKeyRequest,
  PartnerApiKeyResponse,
  ApiKeyPageResponse,
  PartnerApiKeyStatus,
} from '@/types/apiKey';

/**
 * Lấy danh sách khóa truy cập API của tổ chức hiện tại
 * GET /api/v1/integration/api-keys
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
    '/integration/api-keys',
    { params },
  );
  return response.data.data;
};

/**
 * Cấp khóa truy cập API mới cho đối tác bên thứ ba
 * POST /api/v1/integration/api-keys
 */
export const createApiKey = async (
  data: CreateApiKeyRequest,
): Promise<PartnerApiKeyResponse> => {
  const response = await apiClient.post<ApiResult<PartnerApiKeyResponse>>(
    '/integration/api-keys',
    data,
  );
  return response.data.data;
};

/**
 * Thu hồi khóa truy cập API đang hoạt động
 * PUT /api/v1/integration/api-keys/{id}/revoke
 */
export const revokeApiKey = async (
  id: string,
): Promise<PartnerApiKeyResponse> => {
  const response = await apiClient.put<ApiResult<PartnerApiKeyResponse>>(
    `/integration/api-keys/${id}/revoke`,
  );
  return response.data.data;
};
