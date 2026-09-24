import React, { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
import {
  Download,
  Loader2,
  X,
  FileText,
  Eye,
  Settings,
  CalendarDays,
  FileSpreadsheet,
  FileJson,
  MapPin,
  ShieldCheck,
  CheckCircle2,
} from 'lucide-react';
import { toast } from 'sonner';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
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
import { useExportOpenData } from './open-data/useExportOpenData';
import {
  toDateTimeLocal,
  detectActiveQuickRange,
  type QuickRangeKey,
} from './open-data/dateRangeHelpers';

/**
 * Biểu mẫu cấu hình và xuất dữ liệu mở theo chuẩn quốc gia (Open Data Export Form).
 * Thiết kế giao diện 2 cột đồng bộ với hệ thống: Cột trái cấu hình phạm vi dữ liệu, Cột phải định dạng và tải xuống.
 */
export const ExportOpenDataForm: React.FC = () => {
  const { user } = useAuth();
  const isAdmin = user?.roleCode === 'VT-01';
  // NCL-742 §8: chọn địa bàn cho VT-01/VT-05
  const canFilterByUnit = isAdmin || user?.roleCode === 'VT-05';
  const isManager = user?.roleCode === 'VT-02';
  // Mẫu hồ sơ truy xuất theo đối tác (NCL-07-CN-007) - Chỉ áp dụng cho VT-02 (HTX) và VT-04 (Doanh nghiệp)
  const canUseTemplates = user?.roleCode === 'VT-02' || user?.roleCode === 'VT-04';

  const [unitIds, setUnitIds] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [activeQuickRange, setActiveQuickRange] = useState<QuickRangeKey>(null);
  const [previewTemplateModalOpen, setPreviewTemplateModalOpen] = useState(false);

  const orgIdForTemplate = canUseTemplates ? user?.organizationId : undefined;
  const { templates: profileTemplates } = useProfileTemplates(orgIdForTemplate, canUseTemplates);

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
    defaultValues: {
      format: 'JSON',
      organizationId: undefined,
      productCategoryIds: [],
      shipmentIds: [],
      fromDate: undefined,
      toDate: undefined,
      templateId: undefined,
    },
  });

  const selectedFormat = watch('format') || 'JSON';
  const selectedTemplateId = watch('templateId');
  const selectedOrgId = watch('organizationId');
  const selectedCategoryIds = watch('productCategoryIds') || [];
  const fromDate = watch('fromDate');
  const toDate = watch('toDate');

  // Tự động nhận diện mốc thời gian nhanh đang chọn
  useEffect(() => {
    setActiveQuickRange(detectActiveQuickRange(fromDate, toDate));
  }, [fromDate, toDate]);

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

  // Bộ gán mốc thời gian nhanh
  const setQuickRange = (days: number, key: QuickRangeKey) => {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - days);
    setValue('fromDate', toDateTimeLocal(from, false));
    setValue('toDate', toDateTimeLocal(now, true));
    setActiveQuickRange(key);
  };

  const setThisWeek = () => {
    const now = new Date();
    const dayOfWeek = now.getDay();
    const diff = now.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
    const monday = new Date(now);
    monday.setDate(diff);
    monday.setHours(0, 0, 0, 0);
    setValue('fromDate', toDateTimeLocal(monday, false));
    setValue('toDate', toDateTimeLocal(now, true));
    setActiveQuickRange('week');
  };

  const setThisMonth = () => {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    setValue('fromDate', toDateTimeLocal(firstDay, false));
    setValue('toDate', toDateTimeLocal(now, true));
    setActiveQuickRange('month');
  };

  const setThisYear = () => {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), 0, 1);
    setValue('fromDate', toDateTimeLocal(firstDay, false));
    setValue('toDate', toDateTimeLocal(now, true));
    setActiveQuickRange('year');
  };

  const clearDates = () => {
    setValue('fromDate', undefined);
    setValue('toDate', undefined);
    setActiveQuickRange(null);
  };

  const removeCategory = (id: string) => {
    setValue(
      'productCategoryIds',
      selectedCategoryIds.filter((v) => v !== id),
      { shouldValidate: true }
    );
  };

  const clearAllCategories = () => {
    setValue('productCategoryIds', [], { shouldValidate: true });
  };

  const addCategory = (id: string) => {
    if (!id) return;
    if (selectedCategoryIds.includes(id)) {
      removeCategory(id);
    } else {
      setValue('productCategoryIds', [...selectedCategoryIds, id], {
        shouldValidate: true,
      });
    }
  };

  const getCategoryName = (id: string) => {
    return categories.find((c) => c.id === id)?.name || id;
  };

  if (loading) {
    return (
      <div className="flex justify-center p-8">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Cột trái: Cấu hình phạm vi dữ liệu (8 cols trên màn hình lớn) */}
        <div className="lg:col-span-8 space-y-6">
          {/* Card 1: Địa bàn & Tổ chức */}
          {(isAdmin || canFilterByUnit) && (
            <Card className="rounded-xl border border-slate-200/80 dark:border-slate-800 shadow-xs">
              <CardHeader className="pb-3 border-b border-border/50">
                <div className="flex items-center gap-2">
                  <div className="p-1.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400">
                    <MapPin className="size-4" />
                  </div>
                  <div>
                    <CardTitle className="text-base font-semibold">Phạm vi địa bàn & Tổ chức</CardTitle>
                    <CardDescription className="text-xs">
                      Phạm vi dữ liệu theo thẩm quyền quản lý hoặc tổ chức sản xuất
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="pt-4 space-y-4">
                {/* Tổ chức – chỉ hiển thị cho VT-01 (admin) */}
                {isAdmin && (
                  <div className="space-y-1.5">
                    <Label htmlFor="organizationId" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                      Tổ chức / Hợp tác xã
                    </Label>
                    <Controller
                      name="organizationId"
                      control={control}
                      render={({ field }) => (
                        <Select
                          value={field.value ?? ''}
                          onValueChange={(val) => field.onChange(val || undefined)}
                          disabled={submitting}
                        >
                          <SelectTrigger id="organizationId" className="h-10 rounded-lg">
                            <SelectValue placeholder="Tất cả tổ chức" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="all">Tất cả tổ chức</SelectItem>
                            {organizations.map((org) => (
                              <SelectItem key={org.id} value={org.id}>
                                {org.name} ({org.code})
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                    {errors.organizationId && (
                      <p className="text-xs text-red-500">{errors.organizationId.message}</p>
                    )}
                  </div>
                )}

                {/* Địa bàn – VT-01/VT-05 */}
                {canFilterByUnit && (
                  <div className="space-y-1.5">
                    <Label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                      Địa bàn hành chính quản lý
                    </Label>
                    <ProvinceUnitMultiSelect
                      value={unitIds}
                      onChange={setUnitIds}
                      disabled={submitting}
                    />
                    <p className="text-[11px] text-muted-foreground">
                      Mặc định trích xuất toàn bộ địa bàn được phân công nếu không chọn lọc cụ thể.
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {/* Card 2: Thời gian & Nông sản */}
          <Card className="rounded-xl border border-slate-200/80 dark:border-slate-800 shadow-xs">
            <CardHeader className="pb-3 border-b border-border/50">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400">
                  <CalendarDays className="size-4" />
                </div>
                <div>
                  <CardTitle className="text-base font-semibold">Khoảng thời gian & Danh mục nông sản</CardTitle>
                  <CardDescription className="text-xs">
                    Lọc thời điểm phát sinh sự kiện chuỗi cung ứng và loại cây trồng
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="pt-4 space-y-5">
              {/* Khoảng thời gian */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <Label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                    Khoảng thời gian
                  </Label>
                  {activeQuickRange && (
                    <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">
                      Đang chọn: {
                        activeQuickRange === '7days' ? '7 ngày qua' :
                        activeQuickRange === '30days' ? '30 ngày qua' :
                        activeQuickRange === 'week' ? 'Tuần này' :
                        activeQuickRange === 'month' ? 'Tháng này' : 'Năm nay'
                      }
                    </span>
                  )}
                </div>
                <div className="flex flex-wrap gap-1.5">
                  <Button
                    type="button"
                    size="sm"
                    onClick={() => setQuickRange(7, '7days')}
                    disabled={submitting}
                    className={cn(
                      'h-8 px-3 text-xs font-medium rounded-lg transition-all',
                      activeQuickRange === '7days'
                        ? 'bg-emerald-600 text-white shadow-xs hover:bg-emerald-700'
                        : 'bg-background hover:bg-emerald-50/60 dark:hover:bg-emerald-950/30 text-foreground border border-input'
                    )}
                  >
                    7 ngày qua
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    onClick={() => setQuickRange(30, '30days')}
                    disabled={submitting}
                    className={cn(
                      'h-8 px-3 text-xs font-medium rounded-lg transition-all',
                      activeQuickRange === '30days'
                        ? 'bg-emerald-600 text-white shadow-xs hover:bg-emerald-700'
                        : 'bg-background hover:bg-emerald-50/60 dark:hover:bg-emerald-950/30 text-foreground border border-input'
                    )}
                  >
                    30 ngày qua
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    onClick={setThisWeek}
                    disabled={submitting}
                    className={cn(
                      'h-8 px-3 text-xs font-medium rounded-lg transition-all',
                      activeQuickRange === 'week'
                        ? 'bg-emerald-600 text-white shadow-xs hover:bg-emerald-700'
                        : 'bg-background hover:bg-emerald-50/60 dark:hover:bg-emerald-950/30 text-foreground border border-input'
                    )}
                  >
                    Tuần này
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    onClick={setThisMonth}
                    disabled={submitting}
                    className={cn(
                      'h-8 px-3 text-xs font-medium rounded-lg transition-all',
                      activeQuickRange === 'month'
                        ? 'bg-emerald-600 text-white shadow-xs hover:bg-emerald-700'
                        : 'bg-background hover:bg-emerald-50/60 dark:hover:bg-emerald-950/30 text-foreground border border-input'
                    )}
                  >
                    Tháng này
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    onClick={setThisYear}
                    disabled={submitting}
                    className={cn(
                      'h-8 px-3 text-xs font-medium rounded-lg transition-all',
                      activeQuickRange === 'year'
                        ? 'bg-emerald-600 text-white shadow-xs hover:bg-emerald-700'
                        : 'bg-background hover:bg-emerald-50/60 dark:hover:bg-emerald-950/30 text-foreground border border-input'
                    )}
                  >
                    Năm nay
                  </Button>
                  {(fromDate || toDate) && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={clearDates}
                      disabled={submitting}
                      className="h-8 px-2.5 text-xs text-muted-foreground hover:text-foreground"
                    >
                      Xóa lọc ngày
                    </Button>
                  )}
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
                  <div className="space-y-1.5">
                    <Label htmlFor="fromDate" className="text-xs text-muted-foreground">
                      Từ ngày giờ
                    </Label>
                    <Controller
                      name="fromDate"
                      control={control}
                      render={({ field }) => (
                        <Input
                          id="fromDate"
                          type="datetime-local"
                          value={field.value ?? ''}
                          onChange={field.onChange}
                          disabled={submitting}
                          className="h-9 text-xs rounded-lg"
                        />
                      )}
                    />
                    {errors.fromDate && (
                      <p className="text-xs text-red-500">{errors.fromDate.message}</p>
                    )}
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="toDate" className="text-xs text-muted-foreground">
                      Đến ngày giờ
                    </Label>
                    <Controller
                      name="toDate"
                      control={control}
                      render={({ field }) => (
                        <Input
                          id="toDate"
                          type="datetime-local"
                          value={field.value ?? ''}
                          onChange={field.onChange}
                          disabled={submitting}
                          className="h-9 text-xs rounded-lg"
                        />
                      )}
                    />
                    {errors.toDate && (
                      <p className="text-xs text-red-500">{errors.toDate.message}</p>
                    )}
                  </div>
                </div>
              </div>

              {/* Danh mục sản phẩm */}
              <div className="space-y-2 pt-2 border-t border-border/50">
                <div className="flex items-center justify-between">
                  <Label htmlFor="productCategoryIds" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                    Danh mục nông sản
                  </Label>
                  {selectedCategoryIds.length > 0 && (
                    <span className="text-[11px] text-muted-foreground">
                      Đã chọn {selectedCategoryIds.length} danh mục
                    </span>
                  )}
                </div>
                <Controller
                  name="productCategoryIds"
                  control={control}
                  render={({ field }) => (
                    <Select
                      value=""
                      onValueChange={(val) => {
                        if (val) addCategory(val);
                      }}
                      disabled={submitting}
                    >
                      <SelectTrigger id="productCategoryIds" className="h-10 rounded-lg">
                        <SelectValue placeholder="Chọn danh mục để thêm vào bộ lọc..." />
                      </SelectTrigger>
                      <SelectContent>
                        {categories.map((cat) => (
                          <SelectItem key={cat.id} value={cat.id}>
                            {field.value?.includes(cat.id) ? '✓ ' : ''}
                            {cat.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.productCategoryIds && (
                  <p className="text-xs text-red-500">
                    {errors.productCategoryIds.message}
                  </p>
                )}

                {/* Badges danh sách đã chọn */}
                {selectedCategoryIds.length > 0 && (
                  <div className="pt-2 space-y-2">
                    <div className="flex flex-wrap gap-1.5">
                      {selectedCategoryIds.map((id) => (
                        <Badge
                          key={id}
                          variant="secondary"
                          className="flex items-center gap-1.5 pl-2.5 pr-1.5 py-1 text-xs bg-emerald-50 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-300 border border-emerald-200/60 dark:border-emerald-800"
                        >
                          <span>{getCategoryName(id)}</span>
                          <button
                            type="button"
                            onClick={() => removeCategory(id)}
                            className="rounded-full hover:bg-emerald-200/50 dark:hover:bg-emerald-800 p-0.5"
                            aria-label={`Xóa ${getCategoryName(id)}`}
                          >
                            <X className="size-3" />
                          </button>
                        </Badge>
                      ))}
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="h-6 px-2 text-xs text-muted-foreground hover:text-foreground"
                        onClick={clearAllCategories}
                      >
                        Xóa tất cả
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Card 3: Mẫu hồ sơ truy xuất theo đối tác (NCL-07-CN-007) */}
          {canUseTemplates && (
            <Card className="rounded-xl border border-slate-200/80 dark:border-slate-800 shadow-xs">
              <CardHeader className="pb-3 border-b border-border/50">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="p-1.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400">
                      <FileText className="size-4" />
                    </div>
                    <div>
                      <CardTitle className="text-base font-semibold">Mẫu hồ sơ đối tác</CardTitle>
                      <CardDescription className="text-xs">
                        Tùy biến cấu trúc dữ liệu theo thỏa thuận với bên thu mua
                      </CardDescription>
                    </div>
                  </div>
                  {isManager && (
                    <Link
                      to="/export/profile-templates"
                      className="text-xs text-emerald-600 hover:text-emerald-700 hover:underline flex items-center gap-1 font-medium"
                    >
                      <Settings className="size-3.5" />
                      <span>Cấu hình mẫu</span>
                    </Link>
                  )}
                </div>
              </CardHeader>
              <CardContent className="pt-4 space-y-3">
                <div className="flex items-center gap-2">
                  <div className="flex-1">
                    <Controller
                      name="templateId"
                      control={control}
                      render={({ field }) => (
                        <Select
                          value={field.value || 'default'}
                          onValueChange={(val) =>
                            field.onChange(val === 'default' ? undefined : val)
                          }
                          disabled={submitting}
                        >
                          <SelectTrigger id="templateId" className="h-10 rounded-lg">
                            <SelectValue placeholder="Chọn mẫu hồ sơ áp dụng" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="default" label="Dùng mẫu mặc định của tổ chức">
                              Dùng mẫu mặc định của tổ chức
                            </SelectItem>
                            {profileTemplates.map((tpl) => (
                              <SelectItem
                                key={tpl.id}
                                value={tpl.id}
                                label={tpl.name + (tpl.partnerName ? ` (${tpl.partnerName})` : '') + (tpl.isDefault ? ' — [Mặc định]' : '')}
                              >
                                {tpl.name}
                                {tpl.partnerName ? ` (${tpl.partnerName})` : ''}
                                {tpl.isDefault ? ' — [Mặc định]' : ''}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setPreviewTemplateModalOpen(true)}
                    className="h-10 gap-1.5 shrink-0 px-3 rounded-lg border-input"
                    title="Xem trước cấu trúc hồ sơ theo mẫu"
                  >
                    <Eye className="size-4 text-emerald-600" />
                    <span className="hidden sm:inline text-xs font-medium">Xem trước</span>
                  </Button>
                </div>
                <p className="text-[11px] text-muted-foreground">
                  Chọn mẫu hồ sơ đã định nghĩa sẵn để lọc đúng các trường thông tin đối tác thương mại yêu cầu.
                </p>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Cột phải: Tùy chọn định dạng & Thao tác tải xuống (4 cols) */}
        <div className="lg:col-span-4 space-y-6">
          <Card className="rounded-xl border border-slate-200/80 dark:border-slate-800 shadow-xs sticky top-6">
            <CardHeader className="pb-3 border-b border-border/50">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400">
                  <Download className="size-4" />
                </div>
                <div>
                  <CardTitle className="text-base font-semibold">Định dạng & Tải xuống</CardTitle>
                  <CardDescription className="text-xs">
                    Lựa chọn chuẩn tệp tin và trích xuất dữ liệu
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="pt-4 space-y-5">
              {/* Định dạng tệp */}
              <div className="space-y-2">
                <Label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                  Định dạng xuất dữ liệu *
                </Label>
                <Controller
                  name="format"
                  control={control}
                  render={({ field }) => (
                    <div className="grid grid-cols-2 gap-2.5">
                      <button
                        type="button"
                        disabled={submitting}
                        onClick={() => field.onChange('JSON')}
                        className={cn(
                          'p-3 rounded-xl border text-left transition-all relative flex flex-col justify-between cursor-pointer',
                          field.value === 'JSON'
                            ? 'border-emerald-600 bg-emerald-50/60 dark:bg-emerald-950/30 ring-1 ring-emerald-600 shadow-xs'
                            : 'border-slate-200 hover:border-slate-300 dark:border-slate-800 bg-card hover:bg-slate-50/50'
                        )}
                      >
                        <div className="flex items-center justify-between w-full mb-1.5">
                          <FileJson
                            className={cn(
                              'size-5',
                              field.value === 'JSON' ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-500'
                            )}
                          />
                          {field.value === 'JSON' && (
                            <CheckCircle2 className="size-4 text-emerald-600 dark:text-emerald-400" />
                          )}
                        </div>
                        <div>
                          <div className="font-semibold text-xs text-foreground">Dữ liệu JSON</div>
                          <div className="text-[10px] text-muted-foreground mt-0.5">
                            Tích hợp API
                          </div>
                        </div>
                      </button>

                      <button
                        type="button"
                        disabled={submitting}
                        onClick={() => field.onChange('CSV')}
                        className={cn(
                          'p-3 rounded-xl border text-left transition-all relative flex flex-col justify-between cursor-pointer',
                          field.value === 'CSV'
                            ? 'border-emerald-600 bg-emerald-50/60 dark:bg-emerald-950/30 ring-1 ring-emerald-600 shadow-xs'
                            : 'border-slate-200 hover:border-slate-300 dark:border-slate-800 bg-card hover:bg-slate-50/50'
                        )}
                      >
                        <div className="flex items-center justify-between w-full mb-1.5">
                          <FileSpreadsheet
                            className={cn(
                              'size-5',
                              field.value === 'CSV' ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-500'
                            )}
                          />
                          {field.value === 'CSV' && (
                            <CheckCircle2 className="size-4 text-emerald-600 dark:text-emerald-400" />
                          )}
                        </div>
                        <div>
                          <div className="font-semibold text-xs text-foreground">Bảng tính CSV</div>
                          <div className="text-[10px] text-muted-foreground mt-0.5">
                            Excel, UTF-8 BOM
                          </div>
                        </div>
                      </button>
                    </div>
                  )}
                />
                {errors.format && (
                  <p className="text-xs text-red-500">{errors.format.message}</p>
                )}
              </div>

              {/* Tóm tắt bộ lọc chuẩn bị xuất */}
              <div className="rounded-xl border border-slate-200/80 dark:border-slate-800 bg-slate-50/70 dark:bg-slate-900/40 p-3.5 space-y-2.5">
                <div className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                  Tóm tắt thiết lập xuất
                </div>
                <div className="space-y-1.5 text-xs">
                  <div className="flex justify-between items-center py-1 border-b border-border/40">
                    <span className="text-muted-foreground">Định dạng:</span>
                    <Badge variant="outline" className="font-semibold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800 text-[11px] py-0 px-2">
                      {selectedFormat}
                    </Badge>
                  </div>
                  {canFilterByUnit && (
                    <div className="flex justify-between items-center py-1 border-b border-border/40">
                      <span className="text-muted-foreground">Địa bàn:</span>
                      <span className="font-medium text-foreground text-[11px]">
                        {unitIds.length > 0 ? `${unitIds.length} địa bàn đã chọn` : 'Toàn bộ địa bàn quản lý'}
                      </span>
                    </div>
                  )}
                  <div className="flex justify-between items-center py-1 border-b border-border/40">
                    <span className="text-muted-foreground">Thời gian:</span>
                    <span className="font-medium text-foreground text-[11px] text-right truncate max-w-[150px]" title={fromDate ? `${fromDate} → ${toDate || 'nay'}` : 'Toàn bộ lịch sử'}>
                      {fromDate ? `${fromDate.split('T')[0]} → ${toDate ? toDate.split('T')[0] : 'nay'}` : 'Toàn bộ lịch sử'}
                    </span>
                  </div>
                  <div className="flex justify-between items-center py-1 border-b border-border/40">
                    <span className="text-muted-foreground">Nông sản:</span>
                    <span className="font-medium text-foreground text-[11px]">
                      {selectedCategoryIds.length > 0 ? `${selectedCategoryIds.length} danh mục` : 'Tất cả nông sản'}
                    </span>
                  </div>
                  {isAdmin && selectedOrgId && (
                    <div className="flex justify-between items-center py-1 border-b border-border/40">
                      <span className="text-muted-foreground">Tổ chức:</span>
                      <span className="font-medium text-foreground text-[11px] truncate max-w-[140px]">
                        {organizations.find((o) => o.id === selectedOrgId)?.name || 'Đã chọn'}
                      </span>
                    </div>
                  )}
                  {canUseTemplates && selectedTemplateId && selectedTemplateId !== 'default' && (
                    <div className="flex justify-between items-center py-1">
                      <span className="text-muted-foreground">Mẫu áp dụng:</span>
                      <span className="font-medium text-foreground text-[11px] truncate max-w-[140px]">
                        {profileTemplates.find((t) => t.id === selectedTemplateId)?.name || 'Mẫu tùy chỉnh'}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Nút submit */}
              <Button
                type="submit"
                disabled={submitting}
                className="w-full h-11 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm shadow-md transition-all gap-2"
              >
                {submitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Đang trích xuất dữ liệu...
                  </>
                ) : (
                  <>
                    <Download className="h-4 w-4" />
                    Tải xuống dữ liệu mở ({selectedFormat})
                  </>
                )}
              </Button>

              {/* Box chú thích dữ liệu mở */}
              <div className="rounded-xl border border-emerald-200/70 bg-emerald-50/60 dark:border-emerald-900/50 dark:bg-emerald-950/20 p-3 flex items-start gap-2.5 text-xs text-emerald-900 dark:text-emerald-300">
                <ShieldCheck className="size-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                <div className="space-y-0.5">
                  <div className="font-semibold text-emerald-950 dark:text-emerald-200 text-xs">
                    Tiêu chuẩn dữ liệu mở
                  </div>
                  <p className="text-[11px] text-emerald-800/90 dark:text-emerald-400/90 leading-relaxed">
                    Dữ liệu được trích xuất tuân thủ khung chuẩn hóa quốc gia, minh bạch nguồn gốc và tương thích phân tích lớn.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <Qtn11ErrorModal
        open={qtn11ErrorModalOpen}
        onClose={() => setQtn11ErrorModalOpen(false)}
        errors={qtn11Errors}
      />

      {/* Modal xem trước theo mẫu hồ sơ (NCL-07-CN-007) */}
      <DossierPreviewDialog
        open={previewTemplateModalOpen}
        onClose={() => setPreviewTemplateModalOpen(false)}
        templateId={selectedTemplateId && selectedTemplateId !== 'default' ? selectedTemplateId : undefined}
        templateName={
          profileTemplates.find((t) => t.id === selectedTemplateId)?.name || 'Mẫu mặc định'
        }
      />
    </form>
  );
};
