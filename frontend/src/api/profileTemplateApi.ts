import type {
  ProfileTemplate,
  CreateProfileTemplateRequest,
  UpdateProfileTemplateRequest,
  FieldGroupDefinition,
  AvailableFieldItem,
} from '@/types/profileTemplate';

import apiClient from './axiosConfig';

export type { ProfileTemplate };

/** Cấu trúc bao bọc chuẩn ApiResult từ backend Spring Boot. */
interface ApiResult<T> {
  code?: number;
  status?: string;
  data: T;
  message?: string;
}

/** Trích xuất an toàn thuộc tính data từ đối tượng ApiResult hoặc chính payload thô. */
function extractData<T>(resData: ApiResult<T> | T): T {
  if (resData && typeof resData === 'object' && 'data' in (resData as Record<string, unknown>)) {
    return (resData as ApiResult<T>).data;
  }
  return resData as T;
}

/** Chuẩn hóa đối tượng ProfileTemplate đảm bảo trường isDefault luôn là boolean chính xác. */
export const normalizeProfileTemplate = (template: unknown): ProfileTemplate => {
  if (!template || typeof template !== 'object') {
    return template as ProfileTemplate;
  }
  const record = template as Record<string, unknown>;
  const isDefault = Boolean(record.isDefault ?? record.default ?? record.is_default ?? false);
  return {
    ...(record as unknown as ProfileTemplate),
    isDefault,
  };
};

/** Lấy danh mục các trường dữ liệu khả dụng theo nhóm và cờ bắt buộc QTN-11. */
export const getAvailableFields = async (
  organizationId: string,
): Promise<FieldGroupDefinition[]> => {
  const response = await apiClient.get<ApiResult<unknown[]> | unknown[]>(
    `/organizations/${organizationId}/profile-templates/catalog`,
  );
  const rawData = extractData(response.data);
  const list = Array.isArray(rawData) ? rawData : [];

  return list.map((item: unknown) => {
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
};

/** Lấy danh sách các mẫu hồ sơ truy xuất của tổ chức. */
export const getProfileTemplates = async (
  organizationId: string,
): Promise<ProfileTemplate[]> => {
  const response = await apiClient.get<ApiResult<ProfileTemplate[]> | ProfileTemplate[]>(
    `/organizations/${organizationId}/profile-templates`,
  );
  const data = extractData(response.data);
  return Array.isArray(data) ? data.map(normalizeProfileTemplate) : [];
};

/** Lấy thông tin chi tiết một mẫu hồ sơ theo ID. */
export const getProfileTemplateById = async (
  organizationId: string,
  templateId: string,
): Promise<ProfileTemplate> => {
  const response = await apiClient.get<ApiResult<ProfileTemplate> | ProfileTemplate>(
    `/organizations/${organizationId}/profile-templates/${templateId}`,
  );
  return normalizeProfileTemplate(extractData(response.data));
};

/** Lấy mẫu hồ sơ mặc định của tổ chức. */
export const getDefaultProfileTemplate = async (
  organizationId: string,
): Promise<ProfileTemplate> => {
  const response = await apiClient.get<ApiResult<ProfileTemplate> | ProfileTemplate>(
    `/organizations/${organizationId}/profile-templates/default`,
  );
  return normalizeProfileTemplate(extractData(response.data));
};

/** Tạo mới một mẫu hồ sơ truy xuất cho tổ chức. */
export const createProfileTemplate = async (
  organizationId: string,
  data: CreateProfileTemplateRequest,
): Promise<ProfileTemplate> => {
  const response = await apiClient.post<ApiResult<ProfileTemplate> | ProfileTemplate>(
    `/organizations/${organizationId}/profile-templates`,
    data,
  );
  return normalizeProfileTemplate(extractData(response.data));
};

/** Cập nhật thông tin một mẫu hồ sơ truy xuất hiện có. */
export const updateProfileTemplate = async (
  organizationId: string,
  templateId: string,
  data: UpdateProfileTemplateRequest,
): Promise<ProfileTemplate> => {
  const response = await apiClient.put<ApiResult<ProfileTemplate> | ProfileTemplate>(
    `/organizations/${organizationId}/profile-templates/${templateId}`,
    data,
  );
  return normalizeProfileTemplate(extractData(response.data));
};

/** Xóa một mẫu hồ sơ truy xuất của tổ chức. */
export const deleteProfileTemplate = async (
  organizationId: string,
  templateId: string,
): Promise<void> => {
  await apiClient.delete(
    `/organizations/${organizationId}/profile-templates/${templateId}`,
  );
};

/** Xem trước dữ liệu hồ sơ truy xuất theo mẫu dưới định dạng JSON. */
export const getOpenDataPreview = async (
  shipmentId: string,
  templateId?: string,
): Promise<Record<string, unknown>> => {
  const params: Record<string, string> = {};
  if (templateId) {
    params.templateId = templateId;
  }
  const response = await apiClient.get<ApiResult<Record<string, unknown>> | Record<string, unknown>>(
    `/export/shipments/${shipmentId}/preview`,
    { params },
  );
  return extractData(response.data);
};

/** Lấy danh sách mẫu hồ sơ tổng hợp từ nhiều tổ chức (dành cho vai trò VT-04 xuất hàng loạt). */
export const getBatchProfileTemplates = async (
  organizationIds: string[],
): Promise<ProfileTemplate[]> => {
  const params = new URLSearchParams();
  organizationIds.forEach((id) => {
    if (id) params.append('organizationIds', id);
  });
  const response = await apiClient.get<ApiResult<ProfileTemplate[]> | ProfileTemplate[]>(
    `/organizations/batch/templates?${params.toString()}`,
  );
  const data = extractData(response.data);
  return Array.isArray(data) ? data.map(normalizeProfileTemplate) : [];
};
