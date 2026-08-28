import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { isAxiosError } from 'axios';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { toast } from 'sonner';
import { AlertTriangle, Calendar, Camera, CheckCircle2, Info, LoaderCircle, Sprout } from 'lucide-react';
import { recordHarvestEvent } from '@/api/traceEventApi';
import { getHarvestEligibility } from '@/api/farmLogApi';
import { LocationPicker } from '@/pages/packaging-event/components/LocationPicker';
import { useOfflineSync } from '@/hooks/useOfflineSync';
import { useAuth } from '@/hooks/useAuth';
import { addOfflineEvent } from '@/services/offlineQueue';
import { ChainEventType } from '@/enums/chainEventType';
import { getLocalDateString } from '@/utils/dateTime';
import { selectAllOnFocus, preventMouseUpCollapse } from '@/utils/inputUtils';
import { useAutoGeolocation } from '@/hooks/useAutoGeolocation';
import type { HarvestEligibilityResponse } from '@/types/farmLog';

const MAX_IMAGES = 5;

const formSchema = z.object({
  harvestDate: z.string().min(1, 'Vui lòng chọn ngày thu hoạch'),
  quantity: z.number({
    required_error: 'Vui lòng nhập sản lượng',
    invalid_type_error: 'Vui lòng nhập sản lượng',
  }).positive('Sản lượng phải lớn hơn 0'),
  latitude: z.number().min(-90).max(90).optional(),
  longitude: z.number().min(-180).max(180).optional(),
  earlyHarvestReason: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface HarvestFormProps {
  productionLotId: string;
  productionLotName: string;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export const HarvestForm = ({
  productionLotId,
  productionLotName,
  onSuccess,
  onCancel,
}: HarvestFormProps) => {
  const { user } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);
  const [eligibility, setEligibility] = useState<HarvestEligibilityResponse | null>(null);
  const [loadingEligibility, setLoadingEligibility] = useState(false);

  const { isOnline } = useOfflineSync();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
    reset,
    setError: setFormError,
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      harvestDate: getLocalDateString(),
      quantity: undefined,
      latitude: 0,
      longitude: 0,
      earlyHarvestReason: '',
    },
  });

  useEffect(() => {
    if (!productionLotId) return;
    setLoadingEligibility(true);
    getHarvestEligibility(productionLotId)
      .then((res) => setEligibility(res))
      .catch((err) => {
        console.error('Không thể kiểm tra thời gian cách ly', err);
        setEligibility(null);
      })
      .finally(() => setLoadingEligibility(false));
  }, [productionLotId]);

  const selectedHarvestDate = watch('harvestDate');
  const lat = watch('latitude');
  const lng = watch('longitude');

  const isEarlyHarvest = Boolean(
    eligibility?.determined &&
    eligibility.eligibleHarvestDate &&
    selectedHarvestDate &&
    selectedHarvestDate < eligibility.eligibleHarvestDate
  );

  // B-01: Chỉ Quản lý hợp tác xã (VT-02) mới có quyền ghi đè thu hoạch sớm
  const canOverride = Boolean(isEarlyHarvest && user?.roleCode === 'VT-02');
  const isOverrideBlocked = Boolean(isEarlyHarvest && user?.roleCode !== 'VT-02');

  const currentPosition =
    typeof lat === 'number' &&
    Number.isFinite(lat) &&
    typeof lng === 'number' &&
    Number.isFinite(lng) &&
    !(lat === 0 && lng === 0)
      ? {
          lat,
          lng,
        }
      : undefined;

  const handleLocationSelect = (
    selectedLatitude: number,
    selectedLongitude: number,
  ) => {
    setValue('latitude', selectedLatitude, {
      shouldValidate: true,
      shouldDirty: true,
    });
    setValue('longitude', selectedLongitude, {
      shouldValidate: true,
      shouldDirty: true,
    });
  };

  useAutoGeolocation({
    onLocation: (selectedLatitude, selectedLongitude) => {
      handleLocationSelect(selectedLatitude, selectedLongitude);
      toast.success('Đã lấy vị trí hiện tại');
    },
    onError: (message) => {
      toast.error(`Không thể lấy vị trí: ${message}`);
    },
  });

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    const fileArray = Array.from(files);
    if (imageFiles.length + fileArray.length > MAX_IMAGES) {
      toast.error(`Chỉ được chọn tối đa ${MAX_IMAGES} ảnh`);
      return;
    }
    setImageFiles((prev) => [...prev, ...fileArray]);
    const newPreviews = fileArray.map((f) => URL.createObjectURL(f));
    setImagePreviews((prev) => [...prev, ...newPreviews]);
  };

  const removeImage = (index: number) => {
    setImageFiles((prev) => prev.filter((_, i) => i !== index));
    setImagePreviews((prev) => prev.filter((_, i) => i !== index));
  };

  const saveOffline = (data: FormValues) => {
    const eventData = {
      productionLotId,
      harvestDate: data.harvestDate,
      quantity: data.quantity,
      earlyHarvestReason: data.earlyHarvestReason,
    };

    const validationError = addOfflineEvent({
      eventType: ChainEventType.HARVEST,
      productionLotId,
      recordedAt: new Date().toISOString(),
      latitude: data.latitude ?? 0,
      longitude: data.longitude ?? 0,
      images: imagePreviews,
      deviceSource: 'WEB',
      eventData,
    });

    if (validationError) {
      toast.error(`Không thể lưu tạm: ${validationError}`);
      return false;
    }
    toast.info('Không có kết nối mạng. Sự kiện đã được lưu tạm và sẽ đồng bộ khi có mạng.');
    reset();
    setImageFiles([]);
    setImagePreviews([]);
    onSuccess?.();
    return true;
  };

  const onSubmit = async (data: FormValues) => {
    // B-01: Kiểm tra chặn đối với các vai trò không phải Quản lý hợp tác xã (VT-02)
    if (isOverrideBlocked) {
      const msg = `Lô sản xuất chưa hết thời gian cách ly (ngày đủ điều kiện: ${eligibility?.eligibleHarvestDate}). Chỉ Quản lý hợp tác xã (VT-02) mới có quyền ghi đè thu hoạch sớm.`;
      setError(msg);
      toast.error(msg);
      return;
    }

    // Kiểm tra bắt buộc nhập lý do khi Quản lý ghi đè thu hoạch sớm
    if (canOverride && (!data.earlyHarvestReason || !data.earlyHarvestReason.trim())) {
      setFormError('earlyHarvestReason', {
        type: 'manual',
        message: 'Vui lòng nhập lý do bắt buộc khi thu hoạch trước ngày cách ly.',
      });
      toast.error('Vui lòng nhập lý do bắt buộc khi thu hoạch trước ngày cách ly.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    if (!isOnline) {
      saveOffline(data);
      setIsSubmitting(false);
      return;
    }

    try {
      await recordHarvestEvent({
        productionLotId,
        harvestDate: data.harvestDate,
        quantity: data.quantity,
        latitude: data.latitude || undefined,
        longitude: data.longitude || undefined,
        earlyHarvestReason: data.earlyHarvestReason?.trim() || undefined,
      });
      toast.success(`Đã ghi nhận thu hoạch cho lô "${productionLotName}"`);
      reset();
      setImageFiles([]);
      setImagePreviews([]);
      onSuccess?.();
    } catch (err: unknown) {
      const isNetworkError =
        !isAxiosError(err) ||
        (err as { code?: string }).code === 'ERR_NETWORK' ||
        (err as { message?: string })?.message?.includes('Network') ||
        !(err as { response?: unknown }).response;

      if (isNetworkError) {
        saveOffline(data);
        setIsSubmitting(false);
        return;
      }

      const response = (err as any)?.response?.data;
      let message = 'Có lỗi xảy ra khi ghi nhận thu hoạch.';

      if (response) {
        if (response.status === 400 && response.errors) {
          const errorMessages = Object.values(response.errors).join('. ');
          message = errorMessages;
        } else if (response.status === 403) {
          message = response.message || 'Bạn không có quyền thực hiện thao tác này.';
        } else if (response.status === 404) {
          message = response.message || 'Không tìm thấy lô sản xuất.';
        } else if (response.status === 409) {
          message = response.message || 'Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.';
        } else if (response.message) {
          message = response.message;
        }
      }
      setError(message);
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Sprout className="h-5 w-5 text-emerald-600" />
          Ghi nhận thu hoạch
        </CardTitle>
        <CardDescription>
          Ghi nhận sự kiện thu hoạch cho lô sản xuất{' '}
          <span className="font-semibold">{productionLotName}</span>
        </CardDescription>
      </CardHeader>

      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-4">
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* Banner trạng thái kiểm tra thời gian cách ly */}
          {loadingEligibility && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground p-2.5 rounded-lg bg-slate-50 border">
              <LoaderCircle className="h-3.5 w-3.5 animate-spin text-slate-500" />
              <span>Đang kiểm tra thời gian cách ly thuốc bảo vệ thực vật...</span>
            </div>
          )}

          {!loadingEligibility && eligibility?.determined && eligibility.eligibleHarvestDate && (
            <>
              {isEarlyHarvest ? (
                isOverrideBlocked ? (
                  <Alert variant="destructive" className="border-red-300 bg-red-50 text-red-900">
                    <AlertTriangle className="h-4 w-4 text-red-600 shrink-0" />
                    <AlertDescription className="space-y-1 text-sm">
                      <p className="font-semibold">⚠️ Chưa hết thời gian cách ly thuốc BVTV!</p>
                      <p>
                        Lô có thời gian cách ly đến ngày <strong>{eligibility.eligibleHarvestDate}</strong>.
                        Bạn đang chọn ngày thu hoạch <strong>{selectedHarvestDate}</strong> (thu hoạch sớm).
                      </p>
                      <p className="text-xs text-red-700 font-medium pt-1">
                        Chỉ Quản lý hợp tác xã (VT-02) mới có quyền ghi đè thu hoạch sớm kèm lý do bắt buộc. Vui lòng liên hệ Quản lý HTX hoặc chọn ngày thu hoạch sau thời hạn cách ly.
                      </p>
                    </AlertDescription>
                  </Alert>
                ) : (
                  <Alert className="border-amber-300 bg-amber-50 text-amber-900">
                    <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
                    <AlertDescription className="space-y-1 text-sm">
                      <p className="font-semibold">⚠️ Cảnh báo thu hoạch trước thời gian cách ly</p>
                      <p>
                        Lô sản xuất có thời gian cách ly thuốc BVTV đến ngày <strong>{eligibility.eligibleHarvestDate}</strong>.
                        Bạn đang chọn ngày thu hoạch sớm: <strong>{selectedHarvestDate}</strong>.
                      </p>
                      <p className="text-xs text-amber-800 pt-1">
                        Quản lý có thể ghi đè nhưng <strong>bắt buộc phải nhập lý do</strong>. Dữ liệu này sẽ được lưu vết vào lịch sử audit và hồ sơ truy xuất nguồn gốc.
                      </p>
                    </AlertDescription>
                  </Alert>
                )
              ) : (
                <div className="flex items-center gap-2 text-xs text-emerald-800 bg-emerald-50 border border-emerald-200 p-2.5 rounded-lg">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                  <span>
                    Đã đảm bảo thời gian cách ly thuốc BVTV (Đủ điều kiện thu hoạch từ ngày <strong>{eligibility.eligibleHarvestDate}</strong>).
                  </span>
                </div>
              )}
            </>
          )}

          {!loadingEligibility && eligibility && !eligibility.determined && (
            <Alert className="border-blue-200 bg-blue-50 text-blue-900">
              <Info className="h-4 w-4 text-blue-600 shrink-0" />
              <AlertDescription className="space-y-1 text-sm">
                <p className="font-semibold">ℹ️ Thông báo vật tư canh tác</p>
                <p>
                  Lô có vật tư chưa xác định được thời gian cách ly tự động:{' '}
                  <span className="font-medium">{eligibility.unmatchedMaterials?.join(', ') || 'Vật tư ngoài danh mục'}</span>.
                </p>
                <p className="text-xs text-blue-700">
                  Hệ thống cho phép ghi nhận thu hoạch bình thường và sẽ lưu ghi chú vào hồ sơ truy xuất nguồn gốc.
                </p>
              </AlertDescription>
            </Alert>
          )}

          <div className="space-y-2">
            <Label htmlFor="harvestDate">
              Ngày thu hoạch <span className="text-red-500">*</span>
            </Label>
            <div className="relative">
              <Calendar className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="harvestDate"
                type="date"
                className="pl-10"
                {...register('harvestDate')}
              />
            </div>
            {errors.harvestDate && (
              <p className="text-sm text-red-500">{errors.harvestDate.message}</p>
            )}
          </div>

          {/* Ô nhập lý do thu hoạch sớm bắt buộc khi Quản lý ghi đè */}
          {canOverride && (
            <div className="space-y-2 rounded-lg border border-amber-300 bg-amber-50/60 p-3.5">
              <Label htmlFor="earlyHarvestReason" className="text-amber-950 font-semibold flex items-center gap-1">
                Lý do thu hoạch sớm <span className="text-red-500">*</span>
              </Label>
              <Textarea
                id="earlyHarvestReason"
                rows={3}
                placeholder="Nhập lý do bắt buộc giải trình thu hoạch trước thời gian cách ly (ví dụ: bão lũ, thời tiết bất lợi, v.v.)..."
                className="bg-white border-amber-300 focus-visible:ring-amber-400"
                {...register('earlyHarvestReason')}
              />
              {errors.earlyHarvestReason && (
                <p className="text-sm text-red-500">{errors.earlyHarvestReason.message}</p>
              )}
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="quantity">
              Sản lượng thu hoạch (kg) <span className="text-red-500">*</span>
            </Label>
            <Input
              id="quantity"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="Nhập sản lượng thực tế"
              {...register('quantity', { valueAsNumber: true })}
              onFocus={selectAllOnFocus}
              onMouseUp={preventMouseUpCollapse}
            />
            {errors.quantity && (
              <p className="text-sm text-red-500">{errors.quantity.message}</p>
            )}
          </div>

          {/* LocationPicker */}
          <div className="space-y-2">
            <Label>Vị trí thu hoạch (click trên bản đồ)</Label>

            <LocationPicker
              onLocationSelect={handleLocationSelect}
              initialPosition={currentPosition}
              height="300px"
            />
          </div>

          {/* Image Upload */}
          <div className="space-y-2">
            <Label>Hình ảnh thực địa (tối đa {MAX_IMAGES})</Label>
            <div className="flex items-center gap-2 flex-wrap">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => document.getElementById('harvest-image-input')?.click()}
                disabled={isSubmitting || imageFiles.length >= MAX_IMAGES}
              >
                <Camera className="h-4 w-4 mr-1" />
                Chọn ảnh
              </Button>
              <span className="text-sm text-muted-foreground">
                {imageFiles.length}/{MAX_IMAGES}
              </span>
              <input
                id="harvest-image-input"
                type="file"
                accept="image/*"
                multiple
                className="hidden"
                onChange={handleImageChange}
                disabled={isSubmitting}
              />
            </div>
            {imagePreviews.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-2">
                {imagePreviews.map((src, idx) => (
                  <div key={idx} className="relative w-16 h-16 rounded border overflow-hidden">
                    <img src={src} alt={`preview-${idx}`} className="w-full h-full object-cover" />
                    <button
                      type="button"
                      className="absolute -top-1 -right-1 bg-red-500 text-white rounded-full w-5 h-5 flex items-center justify-center text-xs hover:bg-red-600"
                      onClick={() => removeImage(idx)}
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
            <p className="font-medium">⚠️ Lưu ý:</p>
            <ul className="mt-1 list-disc pl-5 space-y-1">
              <li>Lô sản xuất phải ở trạng thái <strong>Đã duyệt (APPROVED)</strong></li>
              <li>Sau khi ghi nhận, trạng thái lô sẽ chuyển sang <strong>Đã thu hoạch (HARVESTED)</strong></li>
              <li>Thao tác này không thể hoàn tác</li>
            </ul>
          </div>
        </CardContent>

        <CardFooter className="flex justify-end gap-3">
          {onCancel && (
            <Button type="button" variant="outline" onClick={onCancel}>
              Hủy
            </Button>
          )}
          <Button
            type="submit"
            variant="create"
            disabled={isSubmitting || isOverrideBlocked}
            title={isOverrideBlocked ? 'Chưa hết thời gian cách ly - Chỉ Quản lý HTX mới có quyền ghi đè' : undefined}
          >
            {isSubmitting && <LoaderCircle className="h-4 w-4 mr-2 animate-spin" />}
            {isSubmitting ? 'Đang ghi nhận...' : 'Ghi nhận thu hoạch'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};