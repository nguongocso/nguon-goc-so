import apiClient from './axiosConfig';
import type {
  ProfileTemplate,
  CreateProfileTemplateRequest,
  UpdateProfileTemplateRequest,
  FieldGroupDefinition,
} from '@/types/profileTemplate';

/**
 * Lấy danh sách các trường dữ liệu khả dụng theo nhóm và cờ bắt buộc QTN-11
 * GET /api/v1/organizations/{orgId}/profile-templates/available-fields
 */
export const getAvailableFields = async (
  organizationId: string
): Promise<FieldGroupDefinition[]> => {
  const response = await apiClient.get<FieldGroupDefinition[]>(
    `/organizations/${organizationId}/profile-templates/available-fields`
  );
  return response.data;
};

/**
 * Lấy danh sách các mẫu hồ sơ của tổ chức
 * GET /api/v1/organizations/{orgId}/profile-templates
 */
export const getProfileTemplates = async (
  organizationId: string
): Promise<ProfileTemplate[]> => {
  const response = await apiClient.get<ProfileTemplate[]>(
    `/organizations/${organizationId}/profile-templates`
  );
  return response.data;
};

/**
 * Lấy chi tiết một mẫu hồ sơ theo ID
 * GET /api/v1/organizations/{orgId}/profile-templates/{templateId}
 */
export const getProfileTemplateById = async (
  organizationId: string,
  templateId: string
): Promise<ProfileTemplate> => {
  const response = await apiClient.get<ProfileTemplate>(
    `/organizations/${organizationId}/profile-templates/${templateId}`
  );
  return response.data;
};

/**
 * Lấy mẫu hồ sơ mặc định của tổ chức
 * GET /api/v1/organizations/{orgId}/profile-templates/default
 */
export const getDefaultProfileTemplate = async (
  organizationId: string
): Promise<ProfileTemplate> => {
  const response = await apiClient.get<ProfileTemplate>(
    `/organizations/${organizationId}/profile-templates/default`
  );
  return response.data;
};

/**
 * Tạo mẫu hồ sơ truy xuất mới
 * POST /api/v1/organizations/{orgId}/profile-templates
 */
export const createProfileTemplate = async (
  organizationId: string,
  data: CreateProfileTemplateRequest
): Promise<ProfileTemplate> => {
  const response = await apiClient.post<ProfileTemplate>(
    `/organizations/${organizationId}/profile-templates`,
    data
  );
  return response.data;
};

/**
 * Cập nhật thông tin mẫu hồ sơ truy xuất
 * PUT /api/v1/organizations/{orgId}/profile-templates/{templateId}
 */
export const updateProfileTemplate = async (
  organizationId: string,
  templateId: string,
  data: UpdateProfileTemplateRequest
): Promise<ProfileTemplate> => {
  const response = await apiClient.put<ProfileTemplate>(
    `/organizations/${organizationId}/profile-templates/${templateId}`,
    data
  );
  return response.data;
};

/**
 * Xóa một mẫu hồ sơ truy xuất
 * DELETE /api/v1/organizations/{orgId}/profile-templates/{templateId}
 */
export const deleteProfileTemplate = async (
  organizationId: string,
  templateId: string
): Promise<void> => {
  await apiClient.delete(
    `/organizations/${organizationId}/profile-templates/${templateId}`
  );
};

/**
 * Xem trước hồ sơ truy xuất theo mẫu dạng JSON (không tạo file)
 * GET /api/v1/export/open-data/shipments/{shipmentId}/preview?templateId={templateId}
 */
export const getOpenDataPreview = async (
  shipmentId: string,
  templateId?: string
): Promise<Record<string, unknown>> => {
  const params: Record<string, string> = {};
  if (templateId) {
    params.templateId = templateId;
  }
  const response = await apiClient.get<Record<string, unknown>>(
    `/export/open-data/shipments/${shipmentId}/preview`,
    { params }
  );
  return response.data;
};
