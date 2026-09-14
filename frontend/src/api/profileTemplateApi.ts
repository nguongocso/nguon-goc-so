import apiClient from './axiosConfig';
import type {
  ProfileTemplate,
  CreateProfileTemplateRequest,
  UpdateProfileTemplateRequest,
  FieldGroupDefinition,
  AvailableFieldItem,
} from '@/types/profileTemplate';

/**
 * Cấu trúc bọc chuẩn ApiResult từ backend Spring Boot
 */
interface ApiResult<T> {
  code?: number;
  status?: string;
  data: T;
  message?: string;
}

/**
 * Helper trích xuất data an toàn từ AxiosResponse của backend
 */
function extractData<T>(resData: ApiResult<T> | T): T {
  if (resData && typeof resData === 'object' && 'data' in (resData as Record<string, unknown>)) {
    return (resData as ApiResult<T>).data;
  }
  return resData as T;
}

/**
 * Lấy danh sách các trường dữ liệu khả dụng theo nhóm và cờ bắt buộc QTN-11
 * GET /api/v1/organizations/{orgId}/profile-templates/catalog
 */
export const getAvailableFields = async (
  organizationId: string
): Promise<FieldGroupDefinition[]> => {
  console.log('[profileTemplateApi] getAvailableFields - Bắt đầu gọi API catalog cho orgId:', organizationId);
  try {
    const response = await apiClient.get<ApiResult<unknown[]> | unknown[]>(
      `/organizations/${organizationId}/profile-templates/catalog`
    );
    const rawData = extractData(response.data);
    const list = Array.isArray(rawData) ? rawData : [];

    // Chuẩn hóa dữ liệu để luôn có đầy đủ key, label, isMandatory, group cho cả code mới và cũ
    const normalized: FieldGroupDefinition[] = list.map((item: unknown) => {
      const g = item as Record<string, unknown>;
      const fieldGroup = String(g.fieldGroup || g.group || '');
      const groupLabel = String(g.groupLabel || fieldGroup);
      const rawFields = Array.isArray(g.fields) ? (g.fields as Record<string, unknown>[]) : [];

      const fields: AvailableFieldItem[] = rawFields.map((f) => {
        const fieldKey = String(f.fieldKey || f.key || '');
        const displayName = String(f.displayName || f.label || fieldKey);
        const mandatory = Boolean(f.mandatory ?? f.isMandatory);
        const description = f.description ? String(f.description) : undefined;

        return {
          fieldKey,
          key: fieldKey,
          displayName,
          label: displayName,
          mandatory,
          isMandatory: mandatory,
          description,
        };
      });

      return {
        fieldGroup,
        group: fieldGroup,
        groupLabel,
        fields,
      };
    });

    console.log('[profileTemplateApi] getAvailableFields - Thành công, số nhóm đã chuẩn hóa:', normalized.length);
    return normalized;
  } catch (err) {
    console.error('[profileTemplateApi] getAvailableFields - Thất bại:', err);
    throw err;
  }
};

/**
 * Lấy danh sách các mẫu hồ sơ của tổ chức
 * GET /api/v1/organizations/{orgId}/profile-templates
 */
export const getProfileTemplates = async (
  organizationId: string
): Promise<ProfileTemplate[]> => {
  console.log('[profileTemplateApi] getProfileTemplates - Bắt đầu gọi API cho orgId:', organizationId);
  try {
    const response = await apiClient.get<ApiResult<ProfileTemplate[]> | ProfileTemplate[]>(
      `/organizations/${organizationId}/profile-templates`
    );
    const data = extractData(response.data);
    console.log('[profileTemplateApi] getProfileTemplates - Thành công, số lượng:', Array.isArray(data) ? data.length : typeof data, data);
    return data;
  } catch (err) {
    console.error('[profileTemplateApi] getProfileTemplates - Thất bại:', err);
    throw err;
  }
};

/**
 * Lấy chi tiết một mẫu hồ sơ theo ID
 * GET /api/v1/organizations/{orgId}/profile-templates/{templateId}
 */
export const getProfileTemplateById = async (
  organizationId: string,
  templateId: string
): Promise<ProfileTemplate> => {
  console.log('[profileTemplateApi] getProfileTemplateById:', { organizationId, templateId });
  try {
    const response = await apiClient.get<ApiResult<ProfileTemplate> | ProfileTemplate>(
      `/organizations/${organizationId}/profile-templates/${templateId}`
    );
    const data = extractData(response.data);
    console.log('[profileTemplateApi] getProfileTemplateById - Thành công:', data);
    return data;
  } catch (err) {
    console.error('[profileTemplateApi] getProfileTemplateById - Thất bại:', err);
    throw err;
  }
};

/**
 * Lấy mẫu hồ sơ mặc định của tổ chức
 * GET /api/v1/organizations/{orgId}/profile-templates/default
 */
export const getDefaultProfileTemplate = async (
  organizationId: string
): Promise<ProfileTemplate> => {
  console.log('[profileTemplateApi] getDefaultProfileTemplate - orgId:', organizationId);
  try {
    const response = await apiClient.get<ApiResult<ProfileTemplate> | ProfileTemplate>(
      `/organizations/${organizationId}/profile-templates/default`
    );
    const data = extractData(response.data);
    console.log('[profileTemplateApi] getDefaultProfileTemplate - Thành công:', data);
    return data;
  } catch (err) {
    console.error('[profileTemplateApi] getDefaultProfileTemplate - Thất bại:', err);
    throw err;
  }
};

/**
 * Tạo mẫu hồ sơ truy xuất mới
 * POST /api/v1/organizations/{orgId}/profile-templates
 */
export const createProfileTemplate = async (
  organizationId: string,
  data: CreateProfileTemplateRequest
): Promise<ProfileTemplate> => {
  console.log('[profileTemplateApi] createProfileTemplate:', { organizationId, data });
  try {
    const response = await apiClient.post<ApiResult<ProfileTemplate> | ProfileTemplate>(
      `/organizations/${organizationId}/profile-templates`,
      data
    );
    const created = extractData(response.data);
    console.log('[profileTemplateApi] createProfileTemplate - Thành công:', created);
    return created;
  } catch (err) {
    console.error('[profileTemplateApi] createProfileTemplate - Thất bại:', err);
    throw err;
  }
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
  console.log('[profileTemplateApi] updateProfileTemplate:', { organizationId, templateId, data });
  try {
    const response = await apiClient.put<ApiResult<ProfileTemplate> | ProfileTemplate>(
      `/organizations/${organizationId}/profile-templates/${templateId}`,
      data
    );
    const updated = extractData(response.data);
    console.log('[profileTemplateApi] updateProfileTemplate - Thành công:', updated);
    return updated;
  } catch (err) {
    console.error('[profileTemplateApi] updateProfileTemplate - Thất bại:', err);
    throw err;
  }
};

/**
 * Xóa một mẫu hồ sơ truy xuất
 * DELETE /api/v1/organizations/{orgId}/profile-templates/{templateId}
 */
export const deleteProfileTemplate = async (
  organizationId: string,
  templateId: string
): Promise<void> => {
  console.log('[profileTemplateApi] deleteProfileTemplate:', { organizationId, templateId });
  try {
    await apiClient.delete(
      `/organizations/${organizationId}/profile-templates/${templateId}`
    );
    console.log('[profileTemplateApi] deleteProfileTemplate - Thành công');
  } catch (err) {
    console.error('[profileTemplateApi] deleteProfileTemplate - Thất bại:', err);
    throw err;
  }
};

/**
 * Xem trước hồ sơ truy xuất theo mẫu dạng JSON
 * GET /api/v1/export/shipments/{shipmentId}/preview?templateId={templateId}
 */
export const getOpenDataPreview = async (
  shipmentId: string,
  templateId?: string
): Promise<Record<string, unknown>> => {
  console.log('[profileTemplateApi] getOpenDataPreview:', { shipmentId, templateId });
  const params: Record<string, string> = {};
  if (templateId) {
    params.templateId = templateId;
  }
  try {
    const response = await apiClient.get<ApiResult<Record<string, unknown>> | Record<string, unknown>>(
      `/export/shipments/${shipmentId}/preview`,
      { params }
    );
    const data = extractData(response.data);
    console.log('[profileTemplateApi] getOpenDataPreview - Thành công:', data);
    return data;
  } catch (err) {
    console.error('[profileTemplateApi] getOpenDataPreview - Thất bại:', err);
    throw err;
  }
};
