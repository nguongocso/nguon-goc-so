import React, { useState, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Save, Eye, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle,
} from '@/components/ui/card';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { useAuth } from '@/hooks/useAuth';
import { ProfileFieldSelector } from '@/components/export/ProfileFieldSelector';
import { DossierPreviewDialog } from '@/components/export/DossierPreviewDialog';
import { ProfileTemplateMetaCard, type ProfileTemplateFormData } from './ProfileTemplateMetaCard';
import { buildProfileTemplateMockData } from './profileTemplateMockData';
import { useProfileTemplateFormData } from './useProfileTemplateFormData';

// Zod schema kiểm tra dữ liệu form
const profileTemplateSchema = z.object({
  name: z
    .string()
    .min(1, 'Tên mẫu hồ sơ không được để trống')
    .max(255, 'Tên mẫu không được vượt quá 255 ký tự'),
  partnerName: z.string().max(255, 'Tên đối tác không được vượt quá 255 ký tự').optional(),
  isDefault: z.boolean(),
});

/** Tạo mới hoặc chỉnh sửa mẫu hồ sơ truy xuất theo đối tác. */
export const ProfileTemplateFormPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);

  const { user } = useAuth();
  const orgId = user?.organizationId;

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Mẫu hồ sơ truy xuất', href: '/export/profile-templates' },
    { label: isEdit ? 'Chỉnh sửa mẫu hồ sơ' : 'Tạo mẫu hồ sơ mới' },
  ]);

  const [previewOpen, setPreviewOpen] = useState(false);

  const {
    control,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<ProfileTemplateFormData>({
    resolver: zodResolver(profileTemplateSchema),
    defaultValues: {
      name: '',
      partnerName: '',
      isDefault: false,
    },
  });

  const formName = watch('name');

  const {
    availableGroups,
    selectedFields,
    setSelectedFields,
    loading,
    submitting,
    mandatoryValidationErr,
    setMandatoryValidationErr,
    onSubmit,
  } = useProfileTemplateFormData({
    orgId,
    id,
    isEdit,
    onSetFormValues: (data) => {
      setValue('name', data.name);
      setValue('partnerName', data.partnerName);
      setValue('isDefault', data.isDefault);
    },
  });

  const previewMockData = useMemo(() => {
    return buildProfileTemplateMockData(formName, Boolean(watch('isDefault')), selectedFields);
  }, [formName, selectedFields, watch]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center p-12 gap-3">
        <Loader2 className="size-8 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">Đang tải cấu hình mẫu hồ sơ...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <ProfileTemplateMetaCard
          control={control}
          setValue={setValue}
          watch={watch}
          submitting={submitting}
          isEdit={isEdit}
          errors={errors}
          mandatoryValidationErr={mandatoryValidationErr}
        />

        <Card>
          <CardHeader className="pb-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <CardTitle className="text-lg">Danh mục trường dữ liệu</CardTitle>
                <CardDescription className="text-xs">
                  Tích chọn các trường muốn đưa vào hồ sơ kết xuất. Các trường
                  cốt lõi theo quy định luôn được tự động giữ lại.
                </CardDescription>
              </div>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setPreviewOpen(true)}
                className="gap-1.5"
              >
                <Eye className="size-4 text-primary" />
                <span>Xem trước hồ sơ</span>
              </Button>
            </div>
          </CardHeader>

          <CardContent>
            <ProfileFieldSelector
              availableGroups={availableGroups}
              selectedFields={selectedFields}
              onChange={(next) => {
                setSelectedFields(next);
                setMandatoryValidationErr(null);
              }}
              disabled={submitting}
            />
          </CardContent>

          <CardFooter className="flex justify-between items-center border-t pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate('/export/profile-templates')}
              disabled={submitting}
            >
              Hủy bỏ
            </Button>

            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => setPreviewOpen(true)}
                disabled={submitting}
                className="gap-1.5"
              >
                <Eye className="size-4" />
                <span>Xem trước</span>
              </Button>

              <Button type="submit" variant="create" disabled={submitting} className="gap-2">
                {submitting ? (
                  <>
                    <Loader2 className="size-4 animate-spin" />
                    <span>Đang lưu...</span>
                  </>
                ) : (
                  <>
                    <Save className="size-4" />
                    <span>{isEdit ? 'Cập nhật mẫu' : 'Lưu mẫu hồ sơ'}</span>
                  </>
                )}
              </Button>
            </div>
          </CardFooter>
        </Card>
      </form>

      <DossierPreviewDialog
        open={previewOpen}
        onClose={() => setPreviewOpen(false)}
        templateName={formName || 'Bản xem trước'}
        initialData={previewMockData}
      />
    </div>
  );
};

export default ProfileTemplateFormPage;
