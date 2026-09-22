import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import {
  getProfileTemplateById,
  getAvailableFields,
} from '@/api/profileTemplateApi';
import { useProfileTemplates } from '@/hooks/useProfileTemplates';
import type {
  FieldGroupDefinition,
  FieldSelectionItem,
} from '@/types/profileTemplate';
import type { ProfileTemplateFormData } from './ProfileTemplateMetaCard';

export interface UseProfileTemplateFormDataParams {
  orgId?: string;
  id?: string;
  isEdit: boolean;
  onSetFormValues: (data: { name: string; partnerName: string; isDefault: boolean }) => void;
}

/**
 * Hook quản lý dữ liệu và logic nghiệp vụ cho form mẫu hồ sơ (khởi tạo, validation QTN-11, lưu mẫu).
 */
export function useProfileTemplateFormData({
  orgId,
  id,
  isEdit,
  onSetFormValues,
}: UseProfileTemplateFormDataParams) {
  const navigate = useNavigate();
  const { createTemplate, updateTemplate } = useProfileTemplates(orgId);

  const [availableGroups, setAvailableGroups] = useState<FieldGroupDefinition[]>([]);
  const [selectedFields, setSelectedFields] = useState<FieldSelectionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [mandatoryValidationErr, setMandatoryValidationErr] = useState<string | null>(null);

  useEffect(() => {
    const initData = async () => {
      if (!orgId) return;

      setLoading(true);
      try {
        const rawGroups = await getAvailableFields(orgId);
        const groups = Array.isArray(rawGroups) ? rawGroups : [];
        setAvailableGroups(groups);

        const defaultMandatoryItems: FieldSelectionItem[] = [];
        groups.forEach((g) => {
          const gKey = g.fieldGroup || g.group || 'OTHER';
          if (Array.isArray(g.fields)) {
            g.fields.forEach((f) => {
              const isMan = Boolean(f.mandatory ?? f.isMandatory);
              const fKey = f.fieldKey || f.key || '';
              if (isMan && fKey) {
                defaultMandatoryItems.push({
                  fieldKey: fKey,
                  fieldGroup: gKey,
                  isMandatory: true,
                  sortOrder: defaultMandatoryItems.length + 1,
                });
              }
            });
          }
        });

        if (isEdit && id) {
          const tpl = await getProfileTemplateById(orgId, id);
          onSetFormValues({
            name: tpl.name || '',
            partnerName: tpl.partnerName || '',
            isDefault: Boolean(tpl.isDefault ?? tpl.default),
          });

          const rawFields = Array.isArray(tpl.fields) ? tpl.fields : [];
          const savedFields: FieldSelectionItem[] = rawFields.map((f, idx) => ({
            fieldKey: f.fieldKey,
            fieldGroup: f.fieldGroup || 'OTHER',
            isMandatory: Boolean(f.isMandatory),
            sortOrder: f.sortOrder || idx + 1,
          }));

          const savedKeySet = new Set(savedFields.map((s) => s.fieldKey));
          defaultMandatoryItems.forEach((m) => {
            if (!savedKeySet.has(m.fieldKey)) {
              savedFields.push(m);
            }
          });

          setSelectedFields(savedFields);
        } else {
          setSelectedFields(defaultMandatoryItems);
        }
      } catch (err: unknown) {
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
          'Không thể khởi tạo thông tin mẫu hồ sơ';
        toast.error(msg);
      } finally {
        setLoading(false);
      }
    };

    void initData();
  }, [orgId, id, isEdit]);

  const validateMandatoryFields = (): boolean => {
    const selectedKeys = new Set(selectedFields.map((f) => f.fieldKey));
    const missingKeys: string[] = [];

    availableGroups.forEach((g) => {
      if (Array.isArray(g.fields)) {
        g.fields.forEach((f) => {
          const isMan = Boolean(f.mandatory ?? f.isMandatory);
          const fKey = f.fieldKey || f.key || '';
          const fLabel = f.displayName || f.label || fKey;
          if (isMan && fKey && !selectedKeys.has(fKey)) {
            missingKeys.push(fLabel);
          }
        });
      }
    });

    if (missingKeys.length > 0) {
      setMandatoryValidationErr(
        `Thiếu các trường bắt buộc theo quy định: ${missingKeys.join(', ')}`
      );
      return false;
    }

    setMandatoryValidationErr(null);
    return true;
  };

  const onSubmit = async (data: ProfileTemplateFormData) => {
    if (!validateMandatoryFields()) {
      toast.error('Mẫu hồ sơ chưa đáp ứng đủ các trường bắt buộc theo quy định');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        name: data.name.trim(),
        partnerName: data.partnerName?.trim() || undefined,
        isDefault: Boolean(data.isDefault),
        selectedFields: selectedFields.map((f, idx) => ({
          fieldKey: f.fieldKey,
          fieldGroup: f.fieldGroup || 'OTHER',
          isMandatory: Boolean(f.isMandatory),
          sortOrder: f.sortOrder || idx + 1,
        })),
      };

      if (isEdit && id) {
        await updateTemplate(id, payload);
      } else {
        await createTemplate(payload);
      }

      navigate('/export/profile-templates');
    } catch (err: unknown) {
      const resp = (err as { response?: { status?: number; data?: { message?: string } } })?.response;
      if (resp?.status === 422) {
        setMandatoryValidationErr(resp.data?.message || 'Thiếu các trường bắt buộc');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return {
    availableGroups,
    selectedFields,
    setSelectedFields,
    loading,
    submitting,
    mandatoryValidationErr,
    setMandatoryValidationErr,
    onSubmit,
  };
}
