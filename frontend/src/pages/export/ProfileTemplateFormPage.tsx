import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  FileText,
  Save,
  Eye,
  Loader2,
  Building,
  AlertCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { useAuth } from '@/hooks/useAuth';
import { useProfileTemplates } from '@/hooks/useProfileTemplates';
import {
  getProfileTemplateById,
  getAvailableFields,
} from '@/api/profileTemplateApi';
import { ProfileFieldSelector } from '@/components/export/ProfileFieldSelector';
import { DossierPreviewDialog } from '@/components/export/DossierPreviewDialog';
import type {
  FieldGroupDefinition,
  FieldSelectionItem,
} from '@/types/profileTemplate';
import { toast } from 'sonner';

// Zod schema kiểm tra dữ liệu form
const profileTemplateSchema = z.object({
  name: z
    .string()
    .min(1, 'Tên mẫu hồ sơ không được để trống')
    .max(255, 'Tên mẫu không được vượt quá 255 ký tự'),
  partnerName: z.string().max(255, 'Tên đối tác không được vượt quá 255 ký tự').optional(),
  isDefault: z.boolean(),
});

type ProfileTemplateFormData = {
  name: string;
  partnerName?: string;
  isDefault: boolean;
};

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

  const { createTemplate, updateTemplate } = useProfileTemplates(orgId);

  const [availableGroups, setAvailableGroups] = useState<FieldGroupDefinition[]>([]);
  const [selectedFields, setSelectedFields] = useState<FieldSelectionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [mandatoryValidationErr, setMandatoryValidationErr] = useState<string | null>(null);

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

  // Tải danh mục trường khả dụng và chi tiết template nếu là edit
  useEffect(() => {
    const initData = async () => {
      if (!orgId) {
        console.warn('[ProfileTemplateFormPage] orgId chưa sẵn sàng');
        return;
      }
      console.log('[ProfileTemplateFormPage] Bắt đầu tải dữ liệu form: orgId =', orgId, 'isEdit =', isEdit, 'id =', id);
      setLoading(true);
      try {
        const rawGroups = await getAvailableFields(orgId);
        const groups = Array.isArray(rawGroups) ? rawGroups : [];
        console.log('[ProfileTemplateFormPage] Danh mục trường đã tải:', groups.length, 'nhóm');
        setAvailableGroups(groups);

        // Mặc định nạp tất cả các trường bắt buộc QTN-11
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
          console.log('[ProfileTemplateFormPage] Đang tải chi tiết mẫu hồ sơ:', id);
          const tpl = await getProfileTemplateById(orgId, id);
          console.log('[ProfileTemplateFormPage] Chi tiết mẫu hồ sơ đã tải:', tpl);
          setValue('name', tpl.name || '');
          setValue('partnerName', tpl.partnerName || '');
          setValue('isDefault', Boolean(tpl.isDefault ?? tpl.default));

          // Map trường đã lưu
          const rawFields = Array.isArray(tpl.fields) ? tpl.fields : [];
          const savedFields: FieldSelectionItem[] = rawFields.map((f, idx) => ({
            fieldKey: f.fieldKey,
            fieldGroup: f.fieldGroup || 'OTHER',
            isMandatory: Boolean(f.isMandatory),
            sortOrder: f.sortOrder || idx + 1,
          }));

          // Đảm bảo không bị thiếu trường bắt buộc
          const savedKeySet = new Set(savedFields.map((s) => s.fieldKey));
          defaultMandatoryItems.forEach((m) => {
            if (!savedKeySet.has(m.fieldKey)) {
              savedFields.push(m);
            }
          });

          setSelectedFields(savedFields);
        } else {
          // Khi tạo mới: mặc định chọn tất cả các trường bắt buộc QTN-11
          console.log('[ProfileTemplateFormPage] Tạo mới: nạp các trường bắt buộc:', defaultMandatoryItems.length);
          setSelectedFields(defaultMandatoryItems);
        }
      } catch (err: unknown) {
        console.error('[ProfileTemplateFormPage] Lỗi khi khởi tạo form:', err);
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
          'Không thể khởi tạo thông tin mẫu hồ sơ';
        toast.error(msg);
      } finally {
        setLoading(false);
      }
    };

    initData();
  }, [orgId, id, isEdit, setValue]);

  // Kiểm tra tính hợp lệ của trường bắt buộc theo QTN-11 (TC-02 UX)
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
        `Thiếu các trường bắt buộc theo quy định QTN-11: ${missingKeys.join(', ')}`
      );
      return false;
    }

    setMandatoryValidationErr(null);
    return true;
  };

  // Submit form
  const onSubmit = async (data: ProfileTemplateFormData) => {
    if (!validateMandatoryFields()) {
      toast.error('Mẫu hồ sơ chưa đáp ứng đủ các trường bắt buộc theo quy định QTN-11');
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

      console.log('[ProfileTemplateFormPage] Submit payload:', payload);

      if (isEdit && id) {
        await updateTemplate(id, payload);
      } else {
        await createTemplate(payload);
      }

      navigate('/export/profile-templates');
    } catch (err: unknown) {
      console.error('[ProfileTemplateFormPage] Submit thất bại:', err);
      const resp = (err as { response?: { status?: number; data?: { message?: string } } })?.response;
      if (resp?.status === 422) {
        // Lỗi 422 từ server (TC-02)
        setMandatoryValidationErr(resp.data?.message || 'Thiếu các trường bắt buộc theo QTN-11');
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Tạo mock data xem trước phản ánh đúng cấu trúc thực tế của backend
  const previewMockData = useMemo(() => {
    const selectedKeySet = new Set(selectedFields.map((f) => f.fieldKey));

    const mock: Record<string, unknown> = {
      shipmentId: 'SHIP-MOCK-2026-DEMO',
      appliedTemplate: {
        templateName: formName || 'Mẫu đang tạo',
        totalFields: selectedFields.length,
        isDefault: Boolean(watch('isDefault')),
      },
    };

    // 1. Organization
    const orgData: Record<string, unknown> = {};
    if (selectedKeySet.has('organization.name')) orgData.name = 'Hợp tác xã Nông nghiệp Xanh Lam Đồng';
    if (selectedKeySet.has('organization.code')) orgData.code = 'HTX-LAMDONG-01';
    if (selectedKeySet.has('organization.address')) orgData.address = 'Thôn 3, Xã Đạ Ròn, Huyện Đơn Dương, Tỉnh Lâm Đồng';
    if (selectedKeySet.has('organization.phone')) orgData.phone = '0263.3888.999';
    if (selectedKeySet.has('organization.email')) orgData.email = 'lienhe@htxxanh.vn';
    if (Object.keys(orgData).length > 0) mock.organization = orgData;

    // 2. FarmArea
    const farmAreaData: Record<string, unknown> = {};
    if (selectedKeySet.has('farmArea.name')) farmAreaData.name = 'Vùng chuyên canh Cà Rốt Đơn Dương';
    if (selectedKeySet.has('farmArea.area')) farmAreaData.area = 5.2;
    if (selectedKeySet.has('farmArea.areaUnit')) farmAreaData.areaUnit = 'HECTARE';
    if (Object.keys(farmAreaData).length > 0) mock.farmArea = farmAreaData;

    // 3. ProductionLot
    const lotData: Record<string, unknown> = {};
    if (selectedKeySet.has('productionLot.name')) lotData.name = 'Lô Cà Rốt hữu cơ VietGAP 2026';
    if (selectedKeySet.has('productionLot.productCategory')) lotData.productCategory = 'Rau củ quả tươi';
    if (selectedKeySet.has('productionLot.plantingDate')) lotData.plantingDate = '2026-06-15';
    if (selectedKeySet.has('productionLot.harvestDate')) lotData.harvestDate = '2026-09-10';
    if (selectedKeySet.has('productionLot.expectedQuantity')) lotData.expectedQuantity = 12500;
    if (selectedKeySet.has('productionLot.actualQuantity')) lotData.actualQuantity = 12800;
    if (selectedKeySet.has('productionLot.status')) lotData.status = 'PACKAGED';
    if (Object.keys(lotData).length > 0) mock.productionLot = lotData;

    // 4. Shipment
    const shipmentData: Record<string, unknown> = {};
    if (selectedKeySet.has('shipment.name')) shipmentData.name = 'Chuyến hàng xuất siêu thị Go! - Đà Lạt';
    if (selectedKeySet.has('shipment.totalQuantity')) shipmentData.totalQuantity = 2000;
    if (selectedKeySet.has('shipment.packagingInfo')) shipmentData.packagingInfo = 'Thùng carton 10kg, dán tem QR GS1';
    if (selectedKeySet.has('shipment.status')) shipmentData.status = 'DELIVERING';
    if (Object.keys(shipmentData).length > 0) mock.shipment = shipmentData;

    // 5. FarmLogs
    const hasFarmLogs = selectedFields.some((f) => f.fieldKey.startsWith('farmLog.'));
    if (hasFarmLogs) {
      mock.farmLogs = [
        {
          executedDate: '2026-06-15',
          activityType: 'PLANTING',
          material: 'Giống cà rốt F1 Kuroda',
          quantity: 2.5,
          notes: 'Gieo hạt vụ thu đông, độ ẩm đất 75%',
        },
        {
          executedDate: '2026-07-10',
          activityType: 'FERTILIZING',
          material: 'Phân trùn quế vi sinh',
          quantity: 500,
          notes: 'Bón thúc lần 1 theo quy trình hữu cơ',
        },
      ];
    }

    // 6. Inspections
    const hasInspections = selectedFields.some((f) => f.fieldKey.startsWith('inspection.'));
    if (hasInspections) {
      mock.inspections = [
        {
          sampleSentDate: '2026-09-08',
          inspectionUnit: 'Trung tâm Phân tích Quatest 3',
          status: 'Đạt tiêu chuẩn an toàn VietGAP',
        },
      ];
    }

    // 7. Timeline
    const hasTimeline = selectedFields.some((f) => f.fieldKey.startsWith('chainEvent.'));
    if (hasTimeline) {
      mock.timelineEvents = [
        {
          recordedAt: '2026-09-10 08:30:00',
          eventType: 'HARVEST',
          recordedBy: 'Nguyễn Văn Quản Lý',
        },
        {
          recordedAt: '2026-09-12 14:00:00',
          eventType: 'PACKAGING',
          recordedBy: 'Trần Thị Đóng Gói',
        },
      ];
    }

    return mock;
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
        {/* Card thông tin cơ bản của mẫu */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2 text-primary">
              <FileText className="size-5" />
              <CardTitle className="text-xl">
                {isEdit ? 'Chỉnh sửa mẫu hồ sơ truy xuất' : 'Tạo mẫu hồ sơ truy xuất mới'}
              </CardTitle>
            </div>
            <CardDescription>
              Thiết lập tên mẫu, đối tác mục tiêu và các trường dữ liệu được phép kết xuất.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Tên mẫu hồ sơ */}
              <div className="space-y-2">
                <Label htmlFor="name">
                  Tên mẫu hồ sơ <span className="text-destructive">*</span>
                </Label>
                <Controller
                  name="name"
                  control={control}
                  render={({ field }) => (
                    <Input
                      id="name"
                      placeholder="VD: Mẫu xuất khẩu Châu Âu, Mẫu siêu thị AEON..."
                      disabled={submitting}
                      {...field}
                    />
                  )}
                />
                {errors.name && (
                  <p className="text-xs text-destructive">{errors.name.message}</p>
                )}
              </div>

              {/* Tên đối tác áp dụng */}
              <div className="space-y-2">
                <Label htmlFor="partnerName">
                  Đối tác áp dụng <span className="text-xs text-muted-foreground">(tùy chọn)</span>
                </Label>
                <Controller
                  name="partnerName"
                  control={control}
                  render={({ field }) => (
                    <div className="relative">
                      <Input
                        id="partnerName"
                        placeholder="VD: AEON Mall, Central Retail, WinCommerce..."
                        disabled={submitting}
                        {...field}
                      />
                      <Building className="absolute right-3 top-2.5 size-4 text-muted-foreground pointer-events-none" />
                    </div>
                  )}
                />
                {errors.partnerName && (
                  <p className="text-xs text-destructive">{errors.partnerName.message}</p>
                )}
              </div>
            </div>

            {/* Mặc định switch */}
            <div
              className="flex items-center justify-between p-3.5 rounded-xl border border-border bg-muted/20 hover:bg-muted/30 transition-colors cursor-pointer select-none"
              onClick={() => {
                setValue('isDefault', !watch('isDefault'), { shouldValidate: true, shouldDirty: true });
              }}
            >
              <div className="space-y-0.5 pointer-events-none">
                <Label htmlFor="isDefault" className="text-sm font-medium cursor-pointer">
                  Đặt làm mẫu hồ sơ mặc định của tổ chức
                </Label>
                <p className="text-xs text-muted-foreground">
                  Khi người xuất không chọn mẫu cụ thể, hệ thống sẽ tự động áp dụng mẫu mặc định này.
                </p>
              </div>
              <div onClick={(e) => e.stopPropagation()}>
                <Controller
                  name="isDefault"
                  control={control}
                  render={({ field }) => (
                    <Switch
                      id="isDefault"
                      checked={Boolean(field.value)}
                      onCheckedChange={(checked) => field.onChange(Boolean(checked))}
                      disabled={submitting}
                    />
                  )}
                />
              </div>
            </div>

            {/* Thông báo lỗi validation QTN-11 nếu có */}
            {mandatoryValidationErr && (
              <div className="flex items-start gap-2.5 p-3 rounded-lg border border-destructive/30 bg-destructive/10 text-destructive text-sm">
                <AlertCircle className="size-4 shrink-0 mt-0.5" />
                <div className="flex-1">{mandatoryValidationErr}</div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Card chọn trường dữ liệu theo nhóm */}
        <Card>
          <CardHeader className="pb-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <CardTitle className="text-lg">Danh mục trường dữ liệu</CardTitle>
                <CardDescription className="text-xs">
                  Tích chọn các trường muốn đưa vào hồ sơ kết xuất. Các trường cốt lõi theo quy định QTN-11 luôn được tự động giữ lại.
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

      {/* Modal xem trước */}
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
