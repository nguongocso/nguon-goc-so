import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import {
  getProfileTemplates,
  getAvailableFields,
  createProfileTemplate,
  updateProfileTemplate,
  deleteProfileTemplate,
} from '@/api/profileTemplateApi';
import type {
  ProfileTemplate,
  FieldGroupDefinition,
  CreateProfileTemplateRequest,
  UpdateProfileTemplateRequest,
} from '@/types/profileTemplate';

export const useProfileTemplates = (organizationId?: string) => {
  const [templates, setTemplates] = useState<ProfileTemplate[]>([]);
  const [availableFields, setAvailableFields] = useState<FieldGroupDefinition[]>([]);
  const [defaultTemplate, setDefaultTemplate] = useState<ProfileTemplate | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  console.log('[useProfileTemplates] Hook render: organizationId =', organizationId);

  const fetchTemplates = useCallback(async () => {
    if (!organizationId) {
      console.warn('[useProfileTemplates] fetchTemplates bỏ qua do organizationId rỗng');
      return;
    }
    console.log('[useProfileTemplates] Bắt đầu tải danh sách mẫu hồ sơ: orgId =', organizationId);
    setLoading(true);
    setError(null);
    try {
      const data = await getProfileTemplates(organizationId);
      console.log('[useProfileTemplates] Kết quả nhận được từ getProfileTemplates:', data);
      const safeList = Array.isArray(data) ? data : ((data as unknown as { data?: ProfileTemplate[] })?.data || []);
      setTemplates(safeList);
      const def = safeList.find((t) => t.isDefault) || null;
      setDefaultTemplate(def);
      console.log('[useProfileTemplates] Đã cập nhật templates:', safeList.length, 'mẫu. Mặc định:', def?.name);
    } catch (err: unknown) {
      console.error('[useProfileTemplates] Lỗi khi tải danh sách mẫu hồ sơ:', err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Không thể tải danh sách mẫu hồ sơ';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }, [organizationId]);

  const fetchAvailableFields = useCallback(async () => {
    if (!organizationId) {
      console.warn('[useProfileTemplates] fetchAvailableFields bỏ qua do organizationId rỗng');
      return;
    }
    console.log('[useProfileTemplates] Bắt đầu tải danh mục trường: orgId =', organizationId);
    try {
      const data = await getAvailableFields(organizationId);
      console.log('[useProfileTemplates] Kết quả danh mục trường:', data);
      const safeFields = Array.isArray(data) ? data : ((data as unknown as { data?: FieldGroupDefinition[] })?.data || []);
      setAvailableFields(safeFields);
      console.log('[useProfileTemplates] Đã cập nhật danh mục trường, số nhóm:', safeFields.length);
    } catch (err: unknown) {
      console.error('[useProfileTemplates] Lỗi khi tải danh mục trường:', err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Không thể tải danh mục trường dữ liệu';
      toast.error(msg);
    }
  }, [organizationId]);

  useEffect(() => {
    if (organizationId) {
      console.log('[useProfileTemplates] useEffect kích hoạt tải dữ liệu cho orgId:', organizationId);
      fetchTemplates();
      fetchAvailableFields();
    } else {
      console.warn('[useProfileTemplates] useEffect: Chưa có organizationId');
    }
  }, [organizationId, fetchTemplates, fetchAvailableFields]);

  const handleCreate = async (data: CreateProfileTemplateRequest): Promise<ProfileTemplate> => {
    if (!organizationId) throw new Error('Chưa xác định tổ chức');
    console.log('[useProfileTemplates] Bắt đầu tạo mẫu hồ sơ:', data);
    try {
      const created = await createProfileTemplate(organizationId, data);
      console.log('[useProfileTemplates] Tạo mẫu hồ sơ thành công:', created);
      toast.success('Tạo mẫu hồ sơ thành công');
      await fetchTemplates();
      return created;
    } catch (err: unknown) {
      console.error('[useProfileTemplates] Lỗi khi tạo mẫu hồ sơ:', err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Tạo mẫu hồ sơ thất bại';
      toast.error(msg);
      throw err;
    }
  };

  const handleUpdate = async (
    templateId: string,
    data: UpdateProfileTemplateRequest
  ): Promise<ProfileTemplate> => {
    if (!organizationId) throw new Error('Chưa xác định tổ chức');
    console.log('[useProfileTemplates] Bắt đầu cập nhật mẫu hồ sơ:', templateId, data);
    try {
      const updated = await updateProfileTemplate(organizationId, templateId, data);
      console.log('[useProfileTemplates] Cập nhật mẫu hồ sơ thành công:', updated);
      toast.success('Cập nhật mẫu hồ sơ thành công');
      await fetchTemplates();
      return updated;
    } catch (err: unknown) {
      console.error('[useProfileTemplates] Lỗi khi cập nhật mẫu hồ sơ:', err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Cập nhật mẫu hồ sơ thất bại';
      toast.error(msg);
      throw err;
    }
  };

  const handleDelete = async (templateId: string): Promise<void> => {
    if (!organizationId) throw new Error('Chưa xác định tổ chức');
    console.log('[useProfileTemplates] Bắt đầu xóa mẫu hồ sơ:', templateId);
    try {
      await deleteProfileTemplate(organizationId, templateId);
      console.log('[useProfileTemplates] Xóa mẫu hồ sơ thành công:', templateId);
      toast.success('Xóa mẫu hồ sơ thành công');
      await fetchTemplates();
    } catch (err: unknown) {
      console.error('[useProfileTemplates] Lỗi khi xóa mẫu hồ sơ:', err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Xóa mẫu hồ sơ thất bại';
      toast.error(msg);
      throw err;
    }
  };

  return {
    templates,
    availableFields,
    defaultTemplate,
    loading,
    error,
    refresh: fetchTemplates,
    createTemplate: handleCreate,
    updateTemplate: handleUpdate,
    deleteTemplate: handleDelete,
  };
};
