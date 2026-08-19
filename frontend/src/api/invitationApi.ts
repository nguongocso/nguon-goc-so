import apiClient from '@/api/axiosConfig';
import type { ApiResult } from '@/types/auth';
import type {
  CreateInvitationRequest,
  InvitationResponse,
  InvitationPublicResponse,
  AcceptInvitationRequest,
  AcceptInvitationResponse,
} from '@/types/invitation';

/**
 * Tạo thư mời mới (VT-02)
 * POST /api/v1/organization/invitations
 */
export const createInvitation = async (
  data: CreateInvitationRequest,
): Promise<InvitationResponse> => {
  const response = await apiClient.post<ApiResult<InvitationResponse>>(
    '/organization/invitations',
    data,
  );
  return response.data.data;
};

/**
 * Lấy thông tin thư mời từ token (public)
 * GET /api/v1/public/organization/invitations/{token}
 */
export const getInvitationDetails = async (
  token: string,
): Promise<InvitationPublicResponse> => {
  const response = await apiClient.get<ApiResult<InvitationPublicResponse>>(
    `/public/organization/invitations/${token}`,
  );
  return response.data.data;
};

/**
 * Chấp nhận thư mời và đăng ký tài khoản (public)
 * POST /api/v1/public/organization/invitations/{token}/accept
 */
export const acceptInvitation = async (
  token: string,
  data: AcceptInvitationRequest,
): Promise<AcceptInvitationResponse> => {
  const response = await apiClient.post<ApiResult<AcceptInvitationResponse>>(
    `/public/organization/invitations/${token}/accept`,
    data,
  );
  return response.data.data;
};