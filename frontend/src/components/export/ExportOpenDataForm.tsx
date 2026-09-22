import React, { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Download, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { getProductCategories } from '@/api/productCategoryApi';
import { getOrganizations } from '@/api/organizationApi';
import { ProvinceUnitMultiSelect } from '@/components/common/ProvinceUnitMultiSelect';
import { useAuth } from '@/hooks/useAuth';
import { useProfileTemplates } from '@/hooks/useProfileTemplates';
import type { Organization } from '@/types/organization';
import type { ProductCategory } from '@/types/productCategory';
import {
  exportOpenDataSchema,
  type ExportOpenDataFormValues,
} from '@/utils/validators';
import { Qtn11ErrorModal } from './Qtn11ErrorModal';
import { DossierPreviewDialog } from './DossierPreviewDialog';
import { ExportOrganizationFilter } from './open-data/ExportOrganizationFilter';
import { ExportDateRangeFilter } from './open-data/ExportDateRangeFilter';
import { ExportCategoryFilter } from './open-data/ExportCategoryFilter';
import { ExportTemplateSection } from './open-data/ExportTemplateSection';
import { ExportFormatSection } from './open-data/ExportFormatSection';
import { useExportOpenData } from './open-data/useExportOpenData';

/**
 * Biểu mẫu cấu hình và thực hiện xuất dữ liệu mở theo lược đồ chuẩn quốc gia.
 * Hỗ trợ lọc theo tổ chức, địa bàn, thời gian, danh mục và mẫu hồ sơ theo đối tác thu mua.
 */
export const ExportOpenDataForm: React.FC = () => {
  const { user } = useAuth();
  const isAdmin = user?.roleCode === 'VT-01';
  const canFilterByUnit = isAdmin || user?.roleCode === 'VT-05';
  const isManager = user?.roleCode === 'VT-02';

  const [unitIds, setUnitIds] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [previewTemplateModalOpen, setPreviewTemplateModalOpen] = useState(false);

  const { templates: profileTemplates } = useProfileTemplates(user?.organizationId);
  const {
    submitting,
    qtn11ErrorModalOpen,
    setQtn11ErrorModalOpen,
    qtn11Errors,
    onSubmit,
  } = useExportOpenData({ canFilterByUnit, unitIds });

  const {
    control,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<ExportOpenDataFormValues>({
    resolver: zodResolver(exportOpenDataSchema),
    defaultValues: { format: 'JSON', productCategoryIds: [], shipmentIds: [] },
  });

  const selectedFormat = watch('format');
  const selectedTemplateId = watch('templateId');

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [orgs, cats] = await Promise.all([
          isAdmin ? getOrganizations() : Promise.resolve([] as Organization[]),
          getProductCategories(),
        ]);
        setOrganizations(orgs);
        setCategories(cats);
      } catch {
        toast.error('Không thể tải dữ liệu danh mục');
      } finally {
        setLoading(false);
      }
    };
    void fetchData();
  }, [isAdmin]);

  if (loading) {
    return (
      <div className="flex justify-center p-8">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <Card className="mx-auto max-w-2xl">
      <CardHeader>
        <CardTitle>Xuất dữ liệu mở</CardTitle>
        <CardDescription>
          Chọn phạm vi và định dạng để xuất dữ liệu truy xuất theo lược đồ chuẩn.
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-4">
          {isAdmin && (
            <ExportOrganizationFilter
              control={control}
              organizations={organizations}
              submitting={submitting}
              errorMessage={errors.organizationId?.message}
            />
          )}

          {canFilterByUnit && (
            <div className="space-y-2">
              <Label>Địa bàn</Label>
              <ProvinceUnitMultiSelect
                value={unitIds}
                onChange={setUnitIds}
                disabled={submitting}
              />
            </div>
          )}

          <ExportDateRangeFilter
            control={control}
            setValue={setValue}
            watch={watch}
            submitting={submitting}
            errors={errors}
          />

          <ExportCategoryFilter
            categories={categories}
            control={control}
            setValue={setValue}
            watch={watch}
            submitting={submitting}
            errorMessage={errors.productCategoryIds?.message}
          />

          <ExportTemplateSection
            control={control}
            profileTemplates={profileTemplates}
            isManager={isManager}
            submitting={submitting}
            onOpenPreview={() => setPreviewTemplateModalOpen(true)}
          />

          <ExportFormatSection
            control={control}
            selectedFormat={selectedFormat}
            submitting={submitting}
            errorMessage={errors.format?.message}
          />
        </CardContent>

        <CardFooter className="flex justify-end gap-2">
          <Button type="submit" variant="view" disabled={submitting}>
            {submitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Đang xuất...
              </>
            ) : (
              <>
                <Download className="mr-2 h-4 w-4" />
                Xuất dữ liệu
              </>
            )}
          </Button>
        </CardFooter>
      </form>

      <Qtn11ErrorModal
        open={qtn11ErrorModalOpen}
        onClose={() => setQtn11ErrorModalOpen(false)}
        errors={qtn11Errors}
      />

      <DossierPreviewDialog
        open={previewTemplateModalOpen}
        onClose={() => setPreviewTemplateModalOpen(false)}
        templateId={selectedTemplateId && selectedTemplateId !== 'default' ? selectedTemplateId : undefined}
        templateName={
          profileTemplates.find((t) => t.id === selectedTemplateId)?.name || 'Mẫu mặc định'
        }
      />
    </Card>
  );
};