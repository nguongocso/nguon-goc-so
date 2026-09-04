import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
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
import { Loader2, Calculator, Save, AlertCircle, ArrowLeft, Layers } from 'lucide-react';
import { toast } from 'sonner';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { getProductCategories } from '@/api/productCategoryApi';
import {
  estimateImpact,
  getCategoryOverrides,
  getGlobalThreshold,
  saveCategoryOverride,
} from '@/api/anomalyThresholdApi';
import type { ProductCategory } from '@/types/productCategory';
import type {
  CategoryThresholdOverrideRequest,
  ImpactEstimationResult,
} from '@/types/anomalyThreshold';

export const CategoryOverridePage: React.FC = () => {
  const { id } = useParams<{ id?: string }>();
  const navigate = useNavigate();
  const isEditing = Boolean(id);

  useSetBreadcrumb([
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Cấu hình ngưỡng quét bất thường', href: '/admin/anomaly-thresholds' },
    { label: isEditing ? 'Chỉnh sửa cấu hình theo loại nông sản' : 'Thêm mới cấu hình theo loại nông sản' },
  ]);

  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [saving, setSaving] = useState<boolean>(false);
  const [estimating, setEstimating] = useState<boolean>(false);
  const [impactResult, setImpactResult] = useState<ImpactEstimationResult | null>(null);

  const [productCategoryId, setProductCategoryId] = useState<string>('');
  const [maxScansPerHour, setMaxScansPerHour] = useState<number>(5);
  const [maxScansPerDay, setMaxScansPerDay] = useState<number>(10);
  const [maxDistanceKmPer30Min, setMaxDistanceKmPer30Min] = useState<number>(50.0);
  const [minTimeBetweenScansMinutes, setMinTimeBetweenScansMinutes] = useState<number>(30);
  const [activationAgeDays, setActivationAgeDays] = useState<number>(365);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    let mounted = true;

    const initData = async () => {
      try {
        setLoading(true);
        // Load categories first to ensure options are available for Select display
        const catList = await getProductCategories();
        if (!mounted) return;
        setCategories(catList.filter((c) => c.isActive !== false));

        if (isEditing && id) {
          const overrides = await getCategoryOverrides();
          if (!mounted) return;
          const found = overrides.find((item) => item.id === id || item.productCategoryId === id);
          if (found) {
            setProductCategoryId(found.productCategoryId || '');
            setMaxScansPerHour(found.maxScansPerHour ?? 5);
            setMaxScansPerDay(found.maxScansPerDay ?? 10);
            setMaxDistanceKmPer30Min(found.maxDistanceKmPer30Min ?? 50.0);
            setMinTimeBetweenScansMinutes(found.minTimeBetweenScansMinutes ?? 30);
            setActivationAgeDays(found.activationAgeDays ?? 365);
          } else {
            toast.error('Không tìm thấy cấu hình ghi đè');
            navigate('/admin/anomaly-thresholds');
          }
        } else {
          try {
            const global = await getGlobalThreshold();
            if (!mounted) return;
            if (global) {
              setMaxScansPerHour(global.maxScansPerHour ?? 5);
              setMaxScansPerDay(global.maxScansPerDay ?? 10);
              setMaxDistanceKmPer30Min(global.maxDistanceKmPer30Min ?? 50.0);
              setMinTimeBetweenScansMinutes(global.minTimeBetweenScansMinutes ?? 30);
              setActivationAgeDays(global.activationAgeDays ?? 365);
            }
          } catch {
            // fallback
          }
        }
      } catch (error: any) {
        if (!mounted) return;
        toast.error(error.response?.data?.message || 'Không thể tải dữ liệu');
      } finally {
        if (mounted) setLoading(false);
      }
    };

    initData();

    return () => {
      mounted = false;
    };
  }, [id, isEditing, navigate]);

  const validate = (): boolean => {
    const errs: Record<string, string> = {};

    if (!productCategoryId) {
      errs.productCategoryId = 'Vui lòng chọn loại nông sản';
    }
    if (!maxScansPerHour || maxScansPerHour < 1) {
      errs.maxScansPerHour = 'Số lần quét/giờ phải ≥ 1';
    }
    if (!maxScansPerDay || maxScansPerDay < 1) {
      errs.maxScansPerDay = 'Số lần quét/ngày phải ≥ 1';
    }
    if (maxScansPerHour && maxScansPerDay && maxScansPerHour > maxScansPerDay) {
      errs.maxScansPerHour = 'Số lượt quét/giờ không được vượt quá số lượt quét/ngày';
    }
    if (!maxDistanceKmPer30Min || maxDistanceKmPer30Min < 0.1) {
      errs.maxDistanceKmPer30Min = 'Khoảng cách tối đa phải ≥ 0.1 km';
    }
    if (!minTimeBetweenScansMinutes || minTimeBetweenScansMinutes < 1) {
      errs.minTimeBetweenScansMinutes = 'Thời gian di chuyển tối thiểu phải ≥ 1 phút';
    }
    if (activationAgeDays === undefined || activationAgeDays === null || activationAgeDays < 0) {
      errs.activationAgeDays = 'Thời hạn kích hoạt phải ≥ 0 ngày';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleEstimateImpact = async () => {
    if (!validate()) {
      toast.error('Vui lòng kiểm tra lại các giá trị cấu hình trước khi ước lượng');
      return;
    }

    try {
      setEstimating(true);
      const res = await estimateImpact({
        productCategoryId,
        maxScansPerHour,
        maxScansPerDay,
        maxDistanceKmPer30Min,
        minTimeBetweenScansMinutes,
        activationAgeDays,
      });
      setImpactResult(res);
      toast.info('Đã hoàn thành ước lượng tác động (30 ngày)');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tính toán ước lượng tác động');
    } finally {
      setEstimating(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      setSaving(true);
      const payload: CategoryThresholdOverrideRequest = {
        productCategoryId,
        maxScansPerHour,
        maxScansPerDay,
        maxDistanceKmPer30Min,
        minTimeBetweenScansMinutes,
        activationAgeDays,
      };

      await saveCategoryOverride(payload);
      toast.success(
        isEditing
          ? 'Đã cập nhật cấu hình ghi đè danh mục thành công'
          : 'Đã thêm cấu hình ghi đè theo loại nông sản thành công',
      );
      navigate('/admin/anomaly-thresholds');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Lỗi khi lưu cấu hình ghi đè');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-emerald-600" />
      </div>
    );
  }

  return (
    <div className="space-y-6 p-4 md:p-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate('/admin/anomaly-thresholds')}
              className="h-8 w-8 text-muted-foreground hover:text-slate-900"
            >
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
              <Layers className="size-6 text-emerald-600" />
              {isEditing ? 'Chỉnh sửa cấu hình theo loại nông sản' : 'Thêm mới cấu hình theo loại nông sản'}
            </h1>
          </div>
          <p className="text-sm text-muted-foreground mt-1 ml-10">
            {isEditing
              ? 'Điều chỉnh các ngưỡng quét bất thường riêng biệt cho loại nông sản đã chọn.'
              : 'Thiết lập các ngưỡng quét bất thường đặc thù cho từng loại nông sản có tính chất riêng.'}
          </p>
        </div>
      </div>
      <form onSubmit={handleSubmit} className="space-y-6">
        <Card className="border-slate-200 shadow-sm">
          <CardHeader>
            <CardTitle className="text-base font-semibold text-slate-900">
              Thông tin loại nông sản & Ngưỡng quét
            </CardTitle>
            <CardDescription>
              Các tham số này sẽ ghi đè cấu hình toàn cục khi quét mã thuộc loại nông sản này.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-6">
            {/* Category Select */}
            <div className="space-y-2">
              <Label htmlFor="category-select" className="text-sm font-medium text-slate-700">
                Loại nông sản <span className="text-destructive">*</span>
              </Label>
              <Select
                value={productCategoryId}
                onValueChange={(val) => {
                  setProductCategoryId(val || '');
                  setImpactResult(null);
                  if (errors.productCategoryId) {
                    setErrors((prev) => ({ ...prev, productCategoryId: '' }));
                  }
                }}
                disabled={isEditing}
              >
                <SelectTrigger id="category-select" className="bg-white">
                  <SelectValue placeholder="-- Chọn loại nông sản --">
                    {productCategoryId
                      ? categories.find((cat) => cat.id === productCategoryId)?.name
                      : undefined}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {categories.map((cat) => (
                    <SelectItem key={cat.id} value={cat.id}>
                      {cat.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.productCategoryId && (
                <p className="text-xs text-destructive">{errors.productCategoryId}</p>
              )}
            </div>

            {/* Threshold Fields Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="cat-scans-per-hour" className="text-sm font-medium text-slate-700">
                  Số lượt quét tối đa / giờ (1 mã) <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="cat-scans-per-hour"
                  type="number"
                  min={1}
                  value={maxScansPerHour}
                  onChange={(e) => setMaxScansPerHour(parseInt(e.target.value, 10) || 0)}
                  placeholder="Ví dụ: 5"
                />
                {errors.maxScansPerHour && (
                  <p className="text-xs text-destructive">{errors.maxScansPerHour}</p>
                )}
                <p className="text-xs text-muted-foreground">
                  Số lần quét tối đa cho phép trên 1 tem trong 60 phút.
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="cat-scans-per-day" className="text-sm font-medium text-slate-700">
                  Số lượt quét tối đa / ngày (24h) <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="cat-scans-per-day"
                  type="number"
                  min={1}
                  value={maxScansPerDay}
                  onChange={(e) => setMaxScansPerDay(parseInt(e.target.value, 10) || 0)}
                  placeholder="Ví dụ: 10"
                />
                {errors.maxScansPerDay && (
                  <p className="text-xs text-destructive">{errors.maxScansPerDay}</p>
                )}
                <p className="text-xs text-muted-foreground">
                  Số lần quét tối đa cho phép trên 1 tem trong 24 giờ.
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="cat-max-dist" className="text-sm font-medium text-slate-700">
                  Khoảng cách di chuyển tối đa (km) <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="cat-max-dist"
                  type="number"
                  step="0.1"
                  min={0.1}
                  value={maxDistanceKmPer30Min}
                  onChange={(e) => setMaxDistanceKmPer30Min(parseFloat(e.target.value) || 0)}
                  placeholder="Ví dụ: 50.0"
                />
                {errors.maxDistanceKmPer30Min && (
                  <p className="text-xs text-destructive">{errors.maxDistanceKmPer30Min}</p>
                )}
                <p className="text-xs text-muted-foreground">
                  Khoảng cách tối đa có thể di chuyển giữa 2 lần quét.
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="cat-min-time" className="text-sm font-medium text-slate-700">
                  Thời gian di chuyển tối thiểu (phút) <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="cat-min-time"
                  type="number"
                  min={1}
                  value={minTimeBetweenScansMinutes}
                  onChange={(e) => setMinTimeBetweenScansMinutes(parseInt(e.target.value, 10) || 0)}
                  placeholder="Ví dụ: 30"
                />
                {errors.minTimeBetweenScansMinutes && (
                  <p className="text-xs text-destructive">{errors.minTimeBetweenScansMinutes}</p>
                )}
                <p className="text-xs text-muted-foreground">
                  Cửa sổ thời gian tối thiểu được áp dụng để kiểm tra khoảng cách.
                </p>
              </div>

              <div className="space-y-2 md:col-span-2">
                <Label htmlFor="cat-act-age" className="text-sm font-medium text-slate-700">
                  Thời hạn kích hoạt bình thường (ngày) <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="cat-act-age"
                  type="number"
                  min={0}
                  value={activationAgeDays}
                  onChange={(e) => setActivationAgeDays(parseInt(e.target.value, 10) || 0)}
                  placeholder="Ví dụ: 365"
                />
                {errors.activationAgeDays && (
                  <p className="text-xs text-destructive">{errors.activationAgeDays}</p>
                )}
                <p className="text-xs text-muted-foreground">
                  Số ngày tối đa từ khi kích hoạt tem đến khi quét mã. Vượt quá sẽ cảnh báo tuổi thọ tem.
                </p>
              </div>
            </div>
            {/* Impact Result Box */}
            {impactResult && (
              <div className="rounded-xl border p-4 bg-amber-50/60 border-amber-200 text-sm space-y-3">
                <div className="flex items-center gap-2 font-semibold text-amber-800">
                  <AlertCircle className="h-5 w-5 text-amber-600 shrink-0" />
                  <span>Ước lượng tác động trên loại nông sản này (dựa trên dữ liệu 30 ngày qua):</span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
                  <div className="bg-white p-3 rounded-lg border text-center shadow-xs">
                    <div className="text-muted-foreground text-xs">Tổng lượt quét phân tích</div>
                    <div className="font-bold text-lg text-slate-900 mt-0.5">
                      {impactResult.totalScansAnalyzed}
                    </div>
                  </div>
                  <div className="bg-white p-3 rounded-lg border text-center shadow-xs">
                    <div className="text-muted-foreground text-xs">Tổng mã tem phân tích</div>
                    <div className="font-bold text-lg text-slate-900 mt-0.5">
                      {impactResult.totalTraceCodesAnalyzed}
                    </div>
                  </div>
                  <div className="bg-amber-100/70 p-3 rounded-lg border border-amber-300 text-center shadow-xs">
                    <div className="text-amber-800 text-xs font-medium">Dự kiến phát hiện bất thường</div>
                    <div className="font-bold text-lg text-amber-700 mt-0.5">
                      {impactResult.estimatedAnomaliesCount}
                    </div>
                  </div>
                </div>
                {impactResult.message && (
                  <p className="text-xs text-amber-700 italic">{impactResult.message}</p>
                )}
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-slate-100">
              <Button
                type="button"
                variant="outline"
                onClick={handleEstimateImpact}
                disabled={estimating || saving}
                className="w-full sm:w-auto border-amber-300 text-amber-800 hover:bg-amber-50"
              >
                {estimating ? (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                ) : (
                  <Calculator className="h-4 w-4 mr-2 text-amber-600" />
                )}
                Ước lượng tác động (30 ngày)
              </Button>

              <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => navigate('/admin/anomaly-thresholds')}
                  disabled={saving}
                >
                  Hủy
                </Button>
                <Button
                  type="submit"
                  disabled={saving}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white"
                >
                  {saving ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      Đang lưu...
                    </>
                  ) : (
                    <>
                      <Save className="h-4 w-4 mr-2" />
                      Lưu cấu hình
                    </>
                  )}
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </form>
    </div>
  );
};

export default CategoryOverridePage;


