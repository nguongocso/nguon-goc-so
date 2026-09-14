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

  const fetchTemplates = useCallback(async () => {
    if (!organizationId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await getProfileTemplates(organizationId);
      setTemplates(data);
      const def = data.find((t) => t.isDefault) || null;
      setDefaultTemplate(def);
    } catch (err: unknown) {
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
    if (!organizationId) return;
    try {
      const data = await getAvailableFields(organizationId);
      setAvailableFields(data);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Không thể tải danh mục trường dữ liệu';
      toast.error(msg);
    }
  }, [organizationId]);

  useEffect(() => {
    if (organizationId) {
      fetchTemplates();
      fetchAvailableFields();
    }
  }, [organizationId, fetchTemplates, fetchAvailableFields]);

  const handleCreate = async (data: CreateProfileTemplateRequest): Promise<ProfileTemplate> => {
    if (!organizationId) throw new Error('Chưa xác định tổ chức');
    try {
      const created = await createProfileTemplate(organizationId, data);
      toast.success('Tạo mẫu hồ sơ thành công');
      await fetchTemplates();
      return created;
    } catch (err: unknown) {
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
    try {
      const updated = await updateProfileTemplate(organizationId, templateId, data);
      toast.success('Cập nhật mẫu hồ sơ thành công');
      await fetchTemplates();
      return updated;
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Cập nhật mẫu hồ sơ thất bại';
      toast.error(msg);
      throw err;
    }
  };

  const handleDelete = async (templateId: string): Promise<void> => {
    if (!organizationId) throw new Error('Chưa xác định tổ chức');
    try {
      await deleteProfileTemplate(organizationId, templateId);
      toast.success('Xóa mẫu hồ sơ thành công');
      await fetchTemplates();
    } catch (err: unknown) {
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
