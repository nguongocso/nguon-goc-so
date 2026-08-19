import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Download, Loader2, X } from 'lucide-react';
import { toast } from 'sonner';
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
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { exportOpenData } from '@/api/exportApi';
import { getProductCategories } from '@/api/productCategoryApi';
import { getOrganizations } from '@/api/organizationApi';
import { useAuth } from '@/hooks/useAuth';
import type { Organization } from '@/types/organization';
import type { ProductCategory } from '@/types/productCategory';
import {
  exportOpenDataSchema,
  type ExportOpenDataFormValues,
} from '@/utils/validators';
import {
  Qtn11ErrorModal,
  type Qtn11ErrorDetail,
} from './Qtn11ErrorModal';

// Helper: format date to datetime-local string (YYYY-MM-DDTHH:mm)
const toDateTimeLocal = (date: Date, endOfDay = false): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  let hours = String(date.getHours()).padStart(2, '0');
  let minutes = String(date.getMinutes()).padStart(2, '0');
  if (endOfDay) {
    hours = '23';
    minutes = '59';
  }
  return `${year}-${month}-${day}T${hours}:${minutes}`;
};

type QuickRangeKey = '7days' | '30days' | 'week' | 'month' | 'year' | null;

export const ExportOpenDataForm = () => {
  const { user } = useAuth();
  const isAdmin = user?.roleCode === 'VT-01';
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [activeQuickRange, setActiveQuickRange] = useState<QuickRangeKey>(null);
  const [qtn11ErrorModalOpen, setQtn11ErrorModalOpen] = useState(false);
  const [qtn11Errors, setQtn11Errors] = useState<Qtn11ErrorDetail[]>([]);

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
    },
  });

  const selectedFormat = watch('format');
  const selectedCategoryIds = watch('productCategoryIds') || [];
  const fromDate = watch('fromDate');
  const toDate = watch('toDate');

  // Detect if current date range matches any preset
  useEffect(() => {
    if (!fromDate || !toDate) {
      setActiveQuickRange(null);
      return;
    }

    // Helper to compare dates ignoring seconds/milliseconds
    const normalize = (dateStr: string) => {
      if (!dateStr) return '';
      // trim seconds and timezone
      return dateStr.slice(0, 16);
    };

    const now = new Date();
    const todayStr = toDateTimeLocal(now, true);
    const from7days = toDateTimeLocal(new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000));
    const from30days = toDateTimeLocal(new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000));
    const normalizedFrom = normalize(fromDate);
    const normalizedTo = normalize(toDate);

    // Week
    const dayOfWeek = now.getDay();
    const diff = now.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
    const monday = new Date(now);
    monday.setDate(diff);
    monday.setHours(0, 0, 0, 0);
    const weekFrom = toDateTimeLocal(monday);
    const weekTo = toDateTimeLocal(now, true);

    // Month
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const monthFrom = toDateTimeLocal(firstDay);
    const monthTo = toDateTimeLocal(now, true);

    // Year
    const yearFirst = new Date(now.getFullYear(), 0, 1);
    const yearFrom = toDateTimeLocal(yearFirst);
    const yearTo = toDateTimeLocal(now, true);

    if (normalizedFrom === normalize(from7days) && normalizedTo === normalize(todayStr)) {
      setActiveQuickRange('7days');
    } else if (normalizedFrom === normalize(from30days) && normalizedTo === normalize(todayStr)) {
      setActiveQuickRange('30days');
    } else if (normalizedFrom === normalize(weekFrom) && normalizedTo === normalize(weekTo)) {
      setActiveQuickRange('week');
    } else if (normalizedFrom === normalize(monthFrom) && normalizedTo === normalize(monthTo)) {
      setActiveQuickRange('month');
    } else if (normalizedFrom === normalize(yearFrom) && normalizedTo === normalize(yearTo)) {
      setActiveQuickRange('year');
    } else {
      setActiveQuickRange(null);
    }
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
    fetchData();
  }, [isAdmin]);

  const onSubmit = async (data: ExportOpenDataFormValues) => {
    setSubmitting(true);
    try {
      const payload: Record<string, unknown> = { format: data.format };
      if (data.organizationId) payload.organizationId = data.organizationId;
      if (data.fromDate) payload.fromDate = data.fromDate;
      if (data.toDate) payload.toDate = data.toDate;
      if (data.productCategoryIds?.length)
        payload.productCategoryIds = data.productCategoryIds;
      if (data.shipmentIds?.length) payload.shipmentIds = data.shipmentIds;

      const blob = await exportOpenData(payload as Parameters<typeof exportOpenData>[0]);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      const fileName = `export_${new Date().toISOString().slice(0, 10)}.${data.format.toLowerCase()}`;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      toast.success('Xuất dữ liệu thành công!');
    } catch (error: unknown) {
      const axiosError = error as { response?: { data?: Blob; status?: number } };
      if (axiosError.response?.data instanceof Blob) {
        const text = await axiosError.response.data.text();
        try {
          const json: { message?: string; errors?: Qtn11ErrorDetail[] } = JSON.parse(text);
          if (json.errors && Array.isArray(json.errors) && json.errors.length > 0) {
            setQtn11Errors(json.errors);
            setQtn11ErrorModalOpen(true);
            toast.error(json.message || 'Không có lô hàng nào đáp ứng đủ quy tắc QTN-11');
          } else {
            toast.error(json.message || 'Xuất dữ liệu thất bại');
          }
        } catch {
          toast.error('Xuất dữ liệu thất bại');
        }
      } else {
        toast.error((error as { message?: string }).message || 'Xuất dữ liệu thất bại');
      }
    } finally {
      setSubmitting(false);
    }
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

  // Quick date range setters
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
          {/* Tổ chức – chỉ hiển thị cho VT-01 (admin), VT-05 không có quyền /admin/organizations */}
          {isAdmin && (
            <div className="space-y-2">
              <Label htmlFor="organizationId">Tổ chức</Label>
              <Controller
                name="organizationId"
                control={control}
                render={({ field }) => (
                  <Select
                    value={field.value ?? ''}
                    onValueChange={(val) => field.onChange(val || undefined)}
                    disabled={submitting}
                  >
                    <SelectTrigger id="organizationId">
                      <SelectValue placeholder="Tất cả tổ chức" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">Tất cả</SelectItem>
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
                <p className="text-sm text-red-500">{errors.organizationId.message}</p>
              )}
            </div>
          )}

          {/* Khoảng thời gian */}
          <div className="space-y-2">
            <Label>Khoảng thời gian</Label>
            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                variant={activeQuickRange === '7days' ? 'default' : 'outline'}
                size="sm"
                onClick={() => setQuickRange(7, '7days')}
                disabled={submitting}
              >
                7 ngày qua
              </Button>
              <Button
                type="button"
                variant={activeQuickRange === '30days' ? 'default' : 'outline'}
                size="sm"
                onClick={() => setQuickRange(30, '30days')}
                disabled={submitting}
              >
                30 ngày qua
              </Button>
              <Button
                type="button"
                variant={activeQuickRange === 'week' ? 'default' : 'outline'}
                size="sm"
                onClick={setThisWeek}
                disabled={submitting}
              >
                Tuần này
              </Button>
              <Button
                type="button"
                variant={activeQuickRange === 'month' ? 'default' : 'outline'}
                size="sm"
                onClick={setThisMonth}
                disabled={submitting}
              >
                Tháng này
              </Button>
              <Button
                type="button"
                variant={activeQuickRange === 'year' ? 'default' : 'outline'}
                size="sm"
                onClick={setThisYear}
                disabled={submitting}
              >
                Năm nay
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={clearDates}
                disabled={submitting}
                className="text-muted-foreground"
              >
                Xóa
              </Button>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="fromDate">Từ ngày</Label>
              <Controller
                name="fromDate"
                control={control}
                render={({ field }) => (
                  <Input
                    id="fromDate"
                    type="datetime-local"
                    value={field.value ?? ''}
                    onChange={(e) => {
                      field.onChange(e);
                      // if user manually changes, clear active quick range (will be detected by useEffect)
                    }}
                    disabled={submitting}
                  />
                )}
              />
              {errors.fromDate && (
                <p className="text-sm text-red-500">{errors.fromDate.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="toDate">Đến ngày</Label>
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
                  />
                )}
              />
              {errors.toDate && (
                <p className="text-sm text-red-500">{errors.toDate.message}</p>
              )}
            </div>
          </div>

          {/* Danh mục sản phẩm */}
          <div className="space-y-2">
            <Label htmlFor="productCategoryIds">Danh mục sản phẩm</Label>
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
                  <SelectTrigger id="productCategoryIds">
                    <SelectValue placeholder="Chọn danh mục (có thể chọn nhiều)" />
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
              <p className="text-sm text-red-500">
                {errors.productCategoryIds.message}
              </p>
            )}

            {/* Hiển thị danh sách đã chọn */}
            {selectedCategoryIds.length > 0 && (
              <div className="mt-2 space-y-2">
                <div className="flex flex-wrap gap-2">
                  {selectedCategoryIds.map((id) => (
                    <Badge
                      key={id}
                      variant="secondary"
                      className="flex items-center gap-1 pl-3 pr-1 py-1"
                    >
                      {getCategoryName(id)}
                      <button
                        type="button"
                        onClick={() => removeCategory(id)}
                        className="ml-1 rounded-full hover:bg-muted-foreground/20 p-0.5"
                        aria-label={`Xóa ${getCategoryName(id)}`}
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </Badge>
                  ))}
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-6 px-2 text-xs text-muted-foreground"
                    onClick={clearAllCategories}
                  >
                    Xóa tất cả
                  </Button>
                </div>
                <p className="text-xs text-muted-foreground">
                  Đã chọn {selectedCategoryIds.length} danh mục
                </p>
              </div>
            )}
          </div>

          {/* Định dạng */}
          <div className="space-y-2">
            <Label>Định dạng *</Label>
            <Controller
              name="format"
              control={control}
              render={({ field }) => (
                <div className="flex gap-4">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      value="JSON"
                      checked={field.value === 'JSON'}
                      onChange={() => field.onChange('JSON')}
                      disabled={submitting}
                    />
                    JSON
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      value="CSV"
                      checked={field.value === 'CSV'}
                      onChange={() => field.onChange('CSV')}
                      disabled={submitting}
                    />
                    CSV
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      value="XML"
                      checked={field.value === 'XML'}
                      onChange={() => field.onChange('XML')}
                      disabled={submitting}
                    />
                    XML (chưa hỗ trợ)
                  </label>
                </div>
              )}
            />
            {errors.format && (
              <p className="text-sm text-red-500">{errors.format.message}</p>
            )}
            {selectedFormat === 'XML' && (
              <p className="text-sm text-yellow-600">
                ⚠️ Định dạng XML chưa được hỗ trợ. Vui lòng chọn JSON hoặc CSV.
              </p>
            )}
          </div>
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
    </Card>
  );
};