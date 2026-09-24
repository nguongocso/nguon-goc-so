import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';

import {
  getProfileTemplates,
  getAvailableFields,
  createProfileTemplate,
  updateProfileTemplate,
  deleteProfileTemplate,
} from '@/api/profileTemplateApi';
import { toApiError } from '@/api/apiError';
import type {
  ProfileTemplate,
  FieldGroupDefinition,
  CreateProfileTemplateRequest,
  UpdateProfileTemplateRequest,
} from '@/types/profileTemplate';

/** Quản lý danh sách, danh mục trường và thao tác CRUD cho mẫu hồ sơ truy xuất. */
export const useProfileTemplates = (organizationId?: string, enabled = true) => {
  const [templates, setTemplates] = useState<ProfileTemplate[]>([]);
  const [availableFields, setAvailableFields] = useState<FieldGroupDefinition[]>([]);
  const [defaultTemplate, setDefaultTemplate] = useState<ProfileTemplate | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchTemplates = useCallback(async () => {
    if (!organizationId || !enabled) return;

    setLoading(true);
    setError(null);
    try {
      const data = await getProfileTemplates(organizationId);
      const safeList = Array.isArray(data)
        ? data
        : ((data as unknown as { data?: ProfileTemplate[] })?.data || []);
      setTemplates(safeList);
      const def = safeList.find((t) => t.isDefault) || null;
      setDefaultTemplate(def);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      const msg = toApiError(err, 'Không thể tải danh sách mẫu hồ sơ').message;
      setError(msg);
      if (status !== 403) {
        toast.error(msg);
      }
    } finally {
      setLoading(false);
    }
  }, [organizationId, enabled]);

  const fetchAvailableFields = useCallback(async () => {
    if (!organizationId || !enabled) return;

    try {
      const data = await getAvailableFields(organizationId);
      const safeFields = Array.isArray(data)
        ? data
        : ((data as unknown as { data?: FieldGroupDefinition[] })?.data || []);
      setAvailableFields(safeFields);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      const msg = toApiError(err, 'Không thể tải danh mục trường dữ liệu').message;
      if (status !== 403) {
        toast.error(msg);
      }
    }
  }, [organizationId, enabled]);

  useEffect(() => {
    if (organizationId && enabled) {
      void fetchTemplates();
      void fetchAvailableFields();
    }
  }, [organizationId, enabled, fetchTemplates, fetchAvailableFields]);

  const handleCreate = async (
    data: CreateProfileTemplateRequest,
  ): Promise<ProfileTemplate> => {
    if (!organizationId) throw new Error('Chưa xác định tổ chức');
    try {
      const created = await createProfileTemplate(organizationId, data);
      toast.success('Tạo mẫu hồ sơ thành công');
      await fetchTemplates();
      return created;
    } catch (err: unknown) {
      const msg = toApiError(err, 'Tạo mẫu hồ sơ thất bại').message;
      toast.error(msg);
      throw err;
    }
  };

  const handleUpdate = async (
    templateId: string,
    data: UpdateProfileTemplateRequest,
  ): Promise<ProfileTemplate> => {
    if (!organizationId) throw new Error('Chưa xác định tổ chức');
    try {
      const updated = await updateProfileTemplate(organizationId, templateId, data);
      toast.success('Cập nhật mẫu hồ sơ thành công');
      await fetchTemplates();
      return updated;
    } catch (err: unknown) {
      const msg = toApiError(err, 'Cập nhật mẫu hồ sơ thất bại').message;
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
      const msg = toApiError(err, 'Xóa mẫu hồ sơ thất bại').message;
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
