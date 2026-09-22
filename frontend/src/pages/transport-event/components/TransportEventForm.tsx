import { useState } from 'react';
import { isAxiosError } from 'axios';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useLocation, useNavigate } from 'react-router-dom';
import { Camera } from 'lucide-react';
import { toast } from 'sonner';

import { recordTransportEvent } from '@/api/transportEventApi';
import { useOfflineSync } from '@/hooks/useOfflineSync';
import { addOfflineEvent } from '@/services/offlineQueue';
import { ChainEventType } from '@/enums/chainEventType';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardFooter,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  transportEventSchema,
  type TransportEventFormValues,
} from '@/utils/validators/transportEventSchema';
import { ScanCodeField } from '@/components/common/ScanCodeField';

const MAX_IMAGES = 5;

function getCurrentDateTimeLocal() {
  const now = new Date();
  const timezoneOffset = now.getTimezoneOffset() * 60_000;
  return new Date(now.getTime() - timezoneOffset).toISOString().slice(0, 16);
}

const fileToBase64 = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = (error) => reject(error);
  });

interface RecordTransportWithImagesPayload {
  codeValue: string;
  fromLocation: string;
  toLocation: string;
  transportTime: string;
  images?: string[];
}

/**
 * Form ghi nhận sự kiện vận chuyển thực tế cho lô hàng
 */
export function TransportEventForm() {
  const navigate = useNavigate();
  const location = useLocation();
  const prefilledCode =
    (location.state as { codeValue?: string } | null)?.codeValue ?? '';

  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<TransportEventFormValues>({
    resolver: zodResolver(transportEventSchema),
    defaultValues: {
      codeValue: prefilledCode,
      fromLocation: '',
      toLocation: '',
      transportTime: getCurrentDateTimeLocal(),
    },
  });

  const codeValue = watch('codeValue');
  const { isOnline } = useOfflineSync();

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

  const saveOffline = (values: TransportEventFormValues) => {
    const validationError = addOfflineEvent({
      eventType: ChainEventType.TRANSPORT,
      recordedAt: values.transportTime,
      latitude: 0,
      longitude: 0,
      images: imagePreviews,
      deviceSource: 'WEB',
      codeValue: values.codeValue,
      eventData: {
        codeValue: values.codeValue,
        fromLocation: values.fromLocation,
        toLocation: values.toLocation,
        transportTime: values.transportTime,
      },
    });

    if (validationError) {
      toast.error(`Không thể lưu tạm: ${validationError}`);
      return false;
    }

    toast.info('Không có kết nối mạng. Sự kiện đã được lưu tạm và sẽ đồng bộ khi có mạng.');
    reset({
      codeValue: '',
      fromLocation: '',
      toLocation: '',
      transportTime: getCurrentDateTimeLocal(),
    });
    setImageFiles([]);
    setImagePreviews([]);
    return true;
  };

  const onSubmit = async (values: TransportEventFormValues) => {
    try {
      if (!isOnline) {
        saveOffline(values);
        return;
      }

      // Chuyển đổi tệp ảnh sang base64
      let base64Images: string[] = [];
      try {
        base64Images = await Promise.all(imageFiles.map(fileToBase64));
      } catch {
        toast.error('Không thể xử lý ảnh. Vui lòng thử lại.');
        return;
      }

      // Gửi request lên backend với hình ảnh thực địa
      const payload: RecordTransportWithImagesPayload = {
        ...values,
        images: base64Images.length > 0 ? base64Images : undefined,
      };

      await recordTransportEvent(payload);

      toast.success('Ghi sự kiện vận chuyển thành công.');

      reset({
        codeValue: '',
        fromLocation: '',
        toLocation: '',
        transportTime: getCurrentDateTimeLocal(),
      });
      setImageFiles([]);
      setImagePreviews([]);
    } catch (error: unknown) {
      let isNetworkError = false;

      if (isAxiosError(error)) {
        isNetworkError = error.code === 'ERR_NETWORK' || !error.response;
      } else if (error instanceof Error) {
        isNetworkError = error.message.includes('Network');
      } else if (typeof error === 'object' && error !== null && 'response' in error) {
        isNetworkError = !(error as { response?: unknown }).response;
      }

      if (isNetworkError) {
        saveOffline(values);
        return;
      }

      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message ??
          (error.response
            ? 'Không thể ghi sự kiện vận chuyển.'
            : 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra backend.')
        : 'Đã xảy ra lỗi khi ghi sự kiện vận chuyển.';

      toast.error(message);
    }
  };

  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-6 pt-6">
          <ScanCodeField
            value={codeValue}
            onChange={(value) =>
              setValue('codeValue', value, {
                shouldValidate: true,
              })
            }
            error={errors.codeValue?.message}
          />

          <div className="grid gap-6 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="fromLocation">Điểm đi *</Label>
              <Input
                id="fromLocation"
                placeholder="Ví dụ: Xã Long Cốc, huyện Tân Sơn, Phú Thọ"
                {...register('fromLocation')}
              />
              {errors.fromLocation && (
                <p className="text-sm text-destructive">
                  {errors.fromLocation.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="toLocation">Điểm đến *</Label>
              <Input
                id="toLocation"
                placeholder="Ví dụ: Kho trung chuyển Việt Trì, Phú Thọ"
                {...register('toLocation')}
              />
              {errors.toLocation && (
                <p className="text-sm text-destructive">
                  {errors.toLocation.message}
                </p>
              )}
            </div>
          </div>

          <div className="max-w-sm space-y-2">
            <Label htmlFor="transportTime">Thời gian vận chuyển *</Label>
            <Input
              id="transportTime"
              type="datetime-local"
              {...register('transportTime')}
            />
            {errors.transportTime && (
              <p className="text-sm text-destructive">
                {errors.transportTime.message}
              </p>
            )}
          </div>

          {/* Tải lên ảnh thực địa */}
          <div className="space-y-2">
            <Label>Hình ảnh thực địa (tối đa {MAX_IMAGES})</Label>
            <div className="flex flex-wrap items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => document.getElementById('transport-image-input')?.click()}
                disabled={isSubmitting || imageFiles.length >= MAX_IMAGES}
              >
                <Camera className="mr-1 h-4 w-4" />
                Chọn ảnh
              </Button>
              <span className="text-sm text-muted-foreground">
                {imageFiles.length}/{MAX_IMAGES}
              </span>
              <input
                id="transport-image-input"
                type="file"
                accept="image/*"
                multiple
                className="hidden"
                onChange={handleImageChange}
                disabled={isSubmitting}
              />
            </div>
            {imagePreviews.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-2">
                {imagePreviews.map((src, idx) => (
                  <div key={idx} className="relative h-16 w-16 overflow-hidden rounded border">
                    <img src={src} alt={`preview-${idx}`} className="h-full w-full object-cover" />
                    <button
                      type="button"
                      className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-xs text-white hover:bg-red-600"
                      onClick={() => removeImage(idx)}
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </CardContent>

        <CardFooter className="flex justify-end gap-3">
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() => navigate(-1)}
            className="border-emerald-200 text-emerald-700 hover:bg-emerald-50"
          >
            Hủy
          </Button>

          <Button type="submit" size="sm" disabled={isSubmitting} variant="create">
            {isSubmitting ? 'Đang ghi...' : 'Ghi sự kiện vận chuyển'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}