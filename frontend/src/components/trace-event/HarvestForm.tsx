import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { isAxiosError } from 'axios';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { toast } from 'sonner';
import { Calendar, Camera, LoaderCircle, Sprout } from 'lucide-react';
import { recordHarvestEvent } from '@/api/traceEventApi';
import { LocationPicker } from '@/pages/packaging-event/components/LocationPicker';
import { useOfflineSync } from '@/hooks/useOfflineSync';
import { addOfflineEvent } from '@/services/offlineQueue';
import { ChainEventType } from '@/enums/chainEventType';
import { getLocalDateString } from '@/utils/dateTime';
import { selectAllOnFocus, preventMouseUpCollapse } from '@/utils/inputUtils';
import { useAutoGeolocation } from '@/hooks/useAutoGeolocation';

const MAX_IMAGES = 5;

const formSchema = z.object({
  harvestDate: z.string().min(1, 'Vui lòng chọn ngày thu hoạch'),
  quantity: z.number({
    required_error: 'Vui lòng nhập sản lượng',
    invalid_type_error: 'Vui lòng nhập sản lượng',
  }).positive('Sản lượng phải lớn hơn 0'),
  latitude: z.number().min(-90).max(90).optional(),
  longitude: z.number().min(-180).max(180).optional(),
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
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);

  const { isOnline } = useOfflineSync();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
    reset,
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      harvestDate: getLocalDateString(),
      quantity: undefined,
      latitude: 0,
      longitude: 0,
    },
  });

  const lat = watch('latitude');
  const lng = watch('longitude');

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
          <Button type="submit" variant="create" disabled={isSubmitting}>
            {isSubmitting && <LoaderCircle className="h-4 w-4 mr-2 animate-spin" />}
            {isSubmitting ? 'Đang ghi nhận...' : 'Ghi nhận thu hoạch'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};