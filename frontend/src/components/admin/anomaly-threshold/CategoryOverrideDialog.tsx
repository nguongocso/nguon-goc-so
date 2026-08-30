import React, { useEffect, useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
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
import { Loader2, Calculator, Save, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import { getProductCategories } from '@/api/productCategoryApi';
import { estimateImpact, saveCategoryOverride } from '@/api/anomalyThresholdApi';
import type { ProductCategory } from '@/types/productCategory';
import type {
  AnomalyThresholdConfig,
  CategoryThresholdOverrideRequest,
  ImpactEstimationResult,
} from '@/types/anomalyThreshold';

interface CategoryOverrideDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialData?: AnomalyThresholdConfig | null;
  onSuccess: () => void;
  defaultGlobalValues?: AnomalyThresholdConfig | null;
}

export const CategoryOverrideDialog: React.FC<CategoryOverrideDialogProps> = ({
  open,
  onOpenChange,
  initialData,
  onSuccess,
  defaultGlobalValues,
}) => {
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [loadingCategories, setLoadingCategories] = useState(false);
  const [saving, setSaving] = useState(false);
  const [estimating, setEstimating] = useState(false);
  const [impactResult, setImpactResult] = useState<ImpactEstimationResult | null>(null);

  const isEditing = Boolean(initialData && initialData.productCategoryId);

  const [productCategoryId, setProductCategoryId] = useState<string>('');
  const [maxScansPerHour, setMaxScansPerHour] = useState<number>(5);
  const [maxScansPerDay, setMaxScansPerDay] = useState<number>(10);
  const [maxDistanceKmPer30Min, setMaxDistanceKmPer30Min] = useState<number>(50.0);
  const [minTimeBetweenScansMinutes, setMinTimeBetweenScansMinutes] = useState<number>(30);
  const [activationAgeDays, setActivationAgeDays] = useState<number>(365);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (open) {
      loadCategories();
      setImpactResult(null);
      setErrors({});

      if (initialData) {
        setProductCategoryId(initialData.productCategoryId || '');
        setMaxScansPerHour(initialData.maxScansPerHour ?? 5);
        setMaxScansPerDay(initialData.maxScansPerDay ?? 10);
        setMaxDistanceKmPer30Min(initialData.maxDistanceKmPer30Min ?? 50.0);
        setMinTimeBetweenScansMinutes(initialData.minTimeBetweenScansMinutes ?? 30);
        setActivationAgeDays(initialData.activationAgeDays ?? 365);
      } else {
        setProductCategoryId('');
        setMaxScansPerHour(defaultGlobalValues?.maxScansPerHour ?? 5);
        setMaxScansPerDay(defaultGlobalValues?.maxScansPerDay ?? 10);
        setMaxDistanceKmPer30Min(defaultGlobalValues?.maxDistanceKmPer30Min ?? 50.0);
        setMinTimeBetweenScansMinutes(defaultGlobalValues?.minTimeBetweenScansMinutes ?? 30);
        setActivationAgeDays(defaultGlobalValues?.activationAgeDays ?? 365);
      }
    }
  }, [open, initialData, defaultGlobalValues]);

  const loadCategories = async () => {
    try {
      setLoadingCategories(true);
      const data = await getProductCategories();
      setCategories(data.filter((c) => c.isActive));
    } catch {
      toast.error('Không thể tải danh sách loại nông sản');
    } finally {
      setLoadingCategories(false);
    }
  };

  const validate = (): boolean => {
    const errs: Record<string, string> = {};
    if (!productCategoryId) errs.productCategoryId = 'Vui lòng chọn loại nông sản';
    if (isNaN(maxScansPerHour) || maxScansPerHour < 1) errs.maxScansPerHour = 'Phải ≥ 1';
    if (isNaN(maxScansPerDay) || maxScansPerDay < 1) errs.maxScansPerDay = 'Phải ≥ 1';
    if (isNaN(maxDistanceKmPer30Min) || maxDistanceKmPer30Min < 0) errs.maxDistanceKmPer30Min = 'Phải không âm';
    if (isNaN(minTimeBetweenScansMinutes) || minTimeBetweenScansMinutes < 0) errs.minTimeBetweenScansMinutes = 'Phải không âm';
    if (isNaN(activationAgeDays) || activationAgeDays < 0) errs.activationAgeDays = 'Phải không âm';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleEstimateImpact = async () => {
    if (!validate()) return;
    try {
      setEstimating(true);
      const result = await estimateImpact({
        productCategoryId: productCategoryId || null,
        maxScansPerHour,
        maxScansPerDay,
        maxDistanceKmPer30Min,
        minTimeBetweenScansMinutes,
        activationAgeDays,
      });
      setImpactResult(result);
      toast.info('Đã hoàn thành ước lượng tác động (Dry-run)');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể ước lượng tác động');
    } finally {
      setEstimating(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
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
      toast.success(isEditing ? 'Cập nhật cấu hình ghi đè thành công' : 'Thêm mới cấu hình ghi đè thành công');
      onOpenChange(false);
      onSuccess();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể lưu cấu hình ghi đè');
    } finally {
      setSaving(false);
    }
  };

  const selectedCategoryName =
    categories.find((c) => c.id === productCategoryId)?.name || initialData?.productCategoryName;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {isEditing ? 'Chỉnh sửa cấu hình theo loại nông sản' : 'Thêm cấu hình theo loại nông sản'}
          </DialogTitle>
          <DialogDescription>
            Thiết lập các ngưỡng phát hiện bất thường áp dụng riêng cho loại nông sản này.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSave} className="space-y-4 pt-2">
          <div className="space-y-1.5">
            <Label htmlFor="category-select" className="text-sm font-medium">
              Loại nông sản <span className="text-destructive">*</span>
            </Label>
            {isEditing ? (
              <Input value={selectedCategoryName || ''} disabled className="bg-muted" />
            ) : (
              <Select
                value={productCategoryId}
                onValueChange={(val: string | null) => {
                  setProductCategoryId(val || '');
                  if (errors.productCategoryId) {
                    setErrors((prev) => ({ ...prev, productCategoryId: '' }));
                  }
                }}
                disabled={loadingCategories}
              >
                <SelectTrigger id="category-select">
                  <SelectValue placeholder={loadingCategories ? 'Đang tải danh mục...' : 'Chọn loại nông sản'} />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((cat) => (
                    <SelectItem key={cat.id} value={cat.id}>
                      {cat.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
            {errors.productCategoryId && (
              <p className="text-xs text-destructive">{errors.productCategoryId}</p>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label htmlFor="cat-scans-hour" className="text-sm font-medium">Tối đa lượt quét / giờ</Label>
              <Input
                id="cat-scans-hour"
                type="number"
                min={1}
                value={maxScansPerHour}
                onChange={(e) => setMaxScansPerHour(parseInt(e.target.value, 10))}
              />
              {errors.maxScansPerHour && <p className="text-xs text-destructive">{errors.maxScansPerHour}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="cat-scans-day" className="text-sm font-medium">Tối đa lượt quét / ngày (24h)</Label>
              <Input
                id="cat-scans-day"
                type="number"
                min={1}
                value={maxScansPerDay}
                onChange={(e) => setMaxScansPerDay(parseInt(e.target.value, 10))}
              />
              {errors.maxScansPerDay && <p className="text-xs text-destructive">{errors.maxScansPerDay}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="cat-distance" className="text-sm font-medium">Khoảng cách tối đa (km)</Label>
              <Input
                id="cat-distance"
                type="number"
                step="0.5"
                min={0}
                value={maxDistanceKmPer30Min}
                onChange={(e) => setMaxDistanceKmPer30Min(parseFloat(e.target.value))}
              />
              {errors.maxDistanceKmPer30Min && <p className="text-xs text-destructive">{errors.maxDistanceKmPer30Min}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="cat-min-time" className="text-sm font-medium">Khoảng thời gian xét di chuyển (phút)</Label>
              <Input
                id="cat-min-time"
                type="number"
                min={0}
                value={minTimeBetweenScansMinutes}
                onChange={(e) => setMinTimeBetweenScansMinutes(parseInt(e.target.value, 10))}
              />
              {errors.minTimeBetweenScansMinutes && <p className="text-xs text-destructive">{errors.minTimeBetweenScansMinutes}</p>}
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="cat-act-age" className="text-sm font-medium">Thời hạn kích hoạt bình thường (ngày)</Label>
              <Input
                id="cat-act-age"
                type="number"
                min={0}
                value={activationAgeDays}
                onChange={(e) => setActivationAgeDays(parseInt(e.target.value, 10))}
              />
              {errors.activationAgeDays && <p className="text-xs text-destructive">{errors.activationAgeDays}</p>}
            </div>
          </div>
          {impactResult && (
            <div className="rounded-lg border p-3 bg-amber-50/60 border-amber-200 text-xs space-y-2">
              <div className="flex items-center gap-1.5 font-semibold text-amber-800">
                <AlertCircle className="h-4 w-4 text-amber-600" />
                <span>Ước lượng tác động trên loại nông sản này:</span>
              </div>
              <div className="grid grid-cols-3 gap-2 text-center pt-1">
                <div className="bg-white p-2 rounded border">
                  <div className="text-muted-foreground text-[11px]">Lượt quét (30 ngày)</div>
                  <div className="font-bold text-sm">{impactResult.totalScansAnalyzed}</div>
                </div>
                <div className="bg-white p-2 rounded border">
                  <div className="text-muted-foreground text-[11px]">Mã tem phân tích</div>
                  <div className="font-bold text-sm">{impactResult.totalTraceCodesAnalyzed}</div>
                </div>
                <div className="bg-amber-100 p-2 rounded border border-amber-300">
                  <div className="text-amber-800 text-[11px]">Dự kiến bất thường</div>
                  <div className="font-bold text-sm text-amber-700">{impactResult.estimatedAnomaliesCount}</div>
                </div>
              </div>
            </div>
          )}

          <DialogFooter className="flex-row items-center justify-between pt-3 border-t">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleEstimateImpact}
              disabled={estimating || saving}
              className="border-amber-300 text-amber-800 hover:bg-amber-50"
            >
              {estimating ? (
                <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
              ) : (
                <Calculator className="h-4 w-4 mr-1.5 text-amber-600" />
              )}
              Ước lượng tác động
            </Button>

            <div className="flex items-center gap-2">
              <Button type="button" variant="ghost" size="sm" onClick={() => onOpenChange(false)} disabled={saving}>
                Hủy
              </Button>
              <Button type="submit" size="sm" disabled={saving}>
                {saving ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
                    Đang lưu...
                  </>
                ) : (
                  <>
                    <Save className="h-4 w-4 mr-1.5" />
                    Lưu cấu hình
                  </>
                )}
              </Button>
            </div>
          </DialogFooter>

        </form>
      </DialogContent>
    </Dialog>
  );
};
