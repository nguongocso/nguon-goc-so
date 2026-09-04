import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Globe, Save, Calculator, Loader2, Info } from 'lucide-react';
import { toast } from 'sonner';
import { updateGlobalThreshold } from '@/api/anomalyThresholdApi';
import type { AnomalyThresholdConfig, UpdateGlobalThresholdRequest } from '@/types/anomalyThreshold';

interface GlobalThresholdCardProps {
  initialData: AnomalyThresholdConfig | null;
  onSuccess: (updated: AnomalyThresholdConfig) => void;
  onEstimateImpact: (draft: UpdateGlobalThresholdRequest) => void;
  estimating?: boolean;
}

export const GlobalThresholdCard: React.FC<GlobalThresholdCardProps> = ({
  initialData,
  onSuccess,
  onEstimateImpact,
  estimating = false,
}) => {
  const [maxScansPerHour, setMaxScansPerHour] = useState<number>(5);
  const [maxScansPerDay, setMaxScansPerDay] = useState<number>(10);
  const [maxDistanceKmPer30Min, setMaxDistanceKmPer30Min] = useState<number>(50.0);
  const [minTimeBetweenScansMinutes, setMinTimeBetweenScansMinutes] = useState<number>(30);
  const [activationAgeDays, setActivationAgeDays] = useState<number>(365);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (initialData) {
      setMaxScansPerHour(initialData.maxScansPerHour ?? 5);
      setMaxScansPerDay(initialData.maxScansPerDay ?? 10);
      setMaxDistanceKmPer30Min(initialData.maxDistanceKmPer30Min ?? 50.0);
      setMinTimeBetweenScansMinutes(initialData.minTimeBetweenScansMinutes ?? 30);
      setActivationAgeDays(initialData.activationAgeDays ?? 365);
    }
  }, [initialData]);

  const validate = (): boolean => {
    const errs: Record<string, string> = {};
    if (isNaN(maxScansPerHour) || maxScansPerHour < 1) errs.maxScansPerHour = 'Phải lớn hơn hoặc bằng 1';
    if (isNaN(maxScansPerDay) || maxScansPerDay < 1) errs.maxScansPerDay = 'Phải lớn hơn hoặc bằng 1';
    if (isNaN(maxDistanceKmPer30Min) || maxDistanceKmPer30Min < 0) errs.maxDistanceKmPer30Min = 'Khoảng cách phải không âm';
    if (isNaN(minTimeBetweenScansMinutes) || minTimeBetweenScansMinutes < 0) errs.minTimeBetweenScansMinutes = 'Thời gian phải không âm';
    if (isNaN(activationAgeDays) || activationAgeDays < 0) errs.activationAgeDays = 'Thời hạn phải không âm';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      setSaving(true);
      const payload: UpdateGlobalThresholdRequest = {
        maxScansPerHour,
        maxScansPerDay,
        maxDistanceKmPer30Min,
        minTimeBetweenScansMinutes,
        activationAgeDays,
      };

      const updated = await updateGlobalThreshold(payload);
      toast.success('Cập nhật cấu hình ngưỡng mặc định toàn cục thành công');
      onSuccess(updated);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể lưu cấu hình toàn cục');
    } finally {
      setSaving(false);
    }
  };

  const handleEstimate = () => {
    if (!validate()) return;
    onEstimateImpact({
      maxScansPerHour,
      maxScansPerDay,
      maxDistanceKmPer30Min,
      minTimeBetweenScansMinutes,
      activationAgeDays,
    });
  };

  return (
    <Card className="border shadow-sm">
      <CardHeader className="pb-3 border-b bg-muted/20">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="flex items-center gap-2">
            <Globe className="h-5 w-5 text-emerald-600" />
            <CardTitle className="text-base font-semibold">
              Cấu hình ngưỡng mặc định toàn cục
            </CardTitle>
          </div>
          <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-300">
            Áp dụng cho tất cả loại nông sản
          </Badge>
        </div>
        <CardDescription className="text-xs text-muted-foreground mt-1">
          Các ngưỡng này được áp dụng mặc định cho mọi mã truy xuất, trừ khi loại nông sản đó có cấu hình ghi đè riêng.
        </CardDescription>
      </CardHeader>

      <form onSubmit={handleSave}>
        <CardContent className="pt-4 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <Label htmlFor="global-scans-hour" className="text-sm font-medium">
                Tối đa lượt quét / giờ <span className="text-destructive">*</span>
              </Label>
              <Input
                id="global-scans-hour"
                type="number"
                min={1}
                value={maxScansPerHour}
                onChange={(e) => setMaxScansPerHour(parseInt(e.target.value, 10))}
              />
              {errors.maxScansPerHour && (
                <p className="text-xs text-destructive">{errors.maxScansPerHour}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="global-scans-day" className="text-sm font-medium">
                Tối đa lượt quét / ngày (24h) <span className="text-destructive">*</span>
              </Label>
              <Input
                id="global-scans-day"
                type="number"
                min={1}
                value={maxScansPerDay}
                onChange={(e) => setMaxScansPerDay(parseInt(e.target.value, 10))}
              />
              {errors.maxScansPerDay && (
                <p className="text-xs text-destructive">{errors.maxScansPerDay}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="global-distance" className="text-sm font-medium">
                Khoảng cách tối đa (km) <span className="text-destructive">*</span>
              </Label>
              <Input
                id="global-distance"
                type="number"
                step="0.5"
                min={0}
                value={maxDistanceKmPer30Min}
                onChange={(e) => setMaxDistanceKmPer30Min(parseFloat(e.target.value))}
              />
              {errors.maxDistanceKmPer30Min && (
                <p className="text-xs text-destructive">{errors.maxDistanceKmPer30Min}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="global-min-time" className="text-sm font-medium">
                Khung thời gian xét di chuyển (phút) <span className="text-destructive">*</span>
              </Label>
              <Input
                id="global-min-time"
                type="number"
                min={0}
                value={minTimeBetweenScansMinutes}
                onChange={(e) => setMinTimeBetweenScansMinutes(parseInt(e.target.value, 10))}
              />
              {errors.minTimeBetweenScansMinutes && (
                <p className="text-xs text-destructive">{errors.minTimeBetweenScansMinutes}</p>
              )}
            </div>

            <div className="space-y-1.5 sm:col-span-2 lg:col-span-2">
              <Label htmlFor="global-act-age" className="text-sm font-medium">
                Thời hạn kích hoạt bình thường (ngày) <span className="text-destructive">*</span>
              </Label>
              <Input
                id="global-act-age"
                type="number"
                min={0}
                value={activationAgeDays}
                onChange={(e) => setActivationAgeDays(parseInt(e.target.value, 10))}
              />
              {errors.activationAgeDays && (
                <p className="text-xs text-destructive">{errors.activationAgeDays}</p>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2 p-2.5 rounded bg-muted/40 text-xs text-muted-foreground">
            <Info className="h-4 w-4 text-emerald-600 flex-shrink-0" />
            <span>
              Lưu ý: Thay đổi cấu hình chỉ áp dụng cho các lượt quét phát sinh trong tương lai. Không tính toán lại các lượt quét trong quá khứ.
            </span>
          </div>
        </CardContent>

        <CardFooter className="flex items-center justify-between border-t pt-3 bg-muted/10">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleEstimate}
            disabled={estimating || saving}
            className="border-amber-300 text-amber-800 hover:bg-amber-50"
          >
            {estimating ? (
              <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
            ) : (
              <Calculator className="h-4 w-4 mr-1.5 text-amber-600" />
            )}
            Xem ước lượng tác động (30 ngày)
          </Button>

          <Button type="submit" size="sm" disabled={saving} className="bg-emerald-600 hover:bg-emerald-700 text-white">
            {saving ? (
              <>
                <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
                Đang lưu...
              </>
            ) : (
              <>
                <Save className="h-4 w-4 mr-1.5" />
                Lưu cấu hình toàn cục
              </>
            )}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};
