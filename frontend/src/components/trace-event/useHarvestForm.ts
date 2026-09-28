import { useEffect, useState } from 'react';
import { isAxiosError } from 'axios';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

import { useAuth } from '@/hooks/useAuth';
import { useAutoGeolocation } from '@/hooks/useAutoGeolocation';
import { useOfflineSync } from '@/hooks/useOfflineSync';
import type { HarvestFormProps } from './HarvestForm';

import { getHarvestEligibility } from '@/api/farmLogApi';
import { recordHarvestEvent } from '@/api/traceEventApi';
import { ChainEventType } from '@/enums/chainEventType';
import { addOfflineEvent } from '@/services/offlineQueue';
import type { HarvestEligibilityResponse } from '@/types/farmLog';
import { getLocalDateString } from '@/utils/dateTime';

const MAX_IMAGES = 5;

interface BackendErrorData {
  status?: number;
  message?: string;
  errors?: Record<string, string>;
}

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

/** Quản lý dữ liệu và hành vi của biểu mẫu thu hoạch. */
export function useHarvestForm({
  productionLotId,
  productionLotName,
  onSuccess,
}: HarvestFormProps) {
  const { user } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);
  const [eligibility, setEligibility] =
    useState<HarvestEligibilityResponse | null>(null);
  const [loadingEligibility, setLoadingEligibility] = useState(false);
  const { isOnline } = useOfflineSync();
  const form = useForm<FormValues>({
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
      .then(setEligibility)
      .catch(() => setEligibility(null))
      .finally(() => setLoadingEligibility(false));
  }, [productionLotId]);
  const selectedHarvestDate = form.watch('harvestDate');
  const latitude = form.watch('latitude');
  const longitude = form.watch('longitude');
  const isEarlyHarvest = Boolean(
    eligibility?.determined &&
    eligibility.eligibleHarvestDate &&
    selectedHarvestDate &&
    selectedHarvestDate < eligibility.eligibleHarvestDate,
  );
  const canOverride = Boolean(isEarlyHarvest && user?.roleCode === 'VT-02');
  const isOverrideBlocked = Boolean(
    isEarlyHarvest && user?.roleCode !== 'VT-02',
  );
  const currentPosition =
    typeof latitude === 'number' &&
    Number.isFinite(latitude) &&
    typeof longitude === 'number' &&
    Number.isFinite(longitude) &&
    !(latitude === 0 && longitude === 0)
      ? { lat: latitude, lng: longitude }
      : undefined;
  const handleLocationSelect = (latitude: number, longitude: number) => {
    form.setValue('latitude', latitude, {
      shouldValidate: true,
      shouldDirty: true,
    });
    form.setValue('longitude', longitude, {
      shouldValidate: true,
      shouldDirty: true,
    });
  };
  useAutoGeolocation({
    onLocation: (latitude, longitude) => {
      handleLocationSelect(latitude, longitude);
      toast.success('Đã lấy vị trí hiện tại');
    },
    onError: (message) => toast.error(`Không thể lấy vị trí: ${message}`),
  });
  const handleImageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files) return;
    const fileArray = Array.from(files);
    if (imageFiles.length + fileArray.length > MAX_IMAGES) {
      toast.error(`Chỉ được chọn tối đa ${MAX_IMAGES} ảnh`);
      return;
    }
    setImageFiles((current) => [...current, ...fileArray]);
    setImagePreviews((current) => [
      ...current,
      ...fileArray.map(URL.createObjectURL),
    ]);
  };
  const removeImage = (index: number) => {
    setImageFiles((current) => current.filter((_, i) => i !== index));
    setImagePreviews((current) => current.filter((_, i) => i !== index));
  };
  const saveOffline = (data: FormValues) => {
    const validationError = addOfflineEvent({
      eventType: ChainEventType.HARVEST,
      productionLotId,
      recordedAt: new Date().toISOString(),
      latitude: data.latitude ?? 0,
      longitude: data.longitude ?? 0,
      images: imagePreviews,
      deviceSource: 'WEB',
      eventData: {
        productionLotId,
        harvestDate: data.harvestDate,
        quantity: data.quantity,
        earlyHarvestReason: data.earlyHarvestReason,
      },
    });
    if (validationError) {
      toast.error(`Không thể lưu tạm: ${validationError}`);
      return false;
    }
    toast.info(
      'Không có kết nối mạng. Sự kiện đã được lưu tạm và sẽ đồng bộ khi có mạng.',
    );
    form.reset();
    setImageFiles([]);
    setImagePreviews([]);
    onSuccess?.();
    return true;
  };
  const onSubmit = async (data: FormValues) => {
    if (isOverrideBlocked) {
      const eligibleDate = eligibility?.eligibleHarvestDate;
      const message =
        `Lô sản xuất chưa hết thời gian cách ly (ngày đủ điều kiện: ${eligibleDate}). ` +
        'Chỉ Quản lý hợp tác xã (VT-02) mới có quyền ghi đè thu hoạch sớm.';
      setError(message);
      toast.error(message);
      return;
    }
    if (canOverride && !data.earlyHarvestReason?.trim()) {
      const message = 'Vui lòng nhập lý do bắt buộc khi thu hoạch trước ngày cách ly.';
      form.setError('earlyHarvestReason', { type: 'manual', message });
      toast.error(message);
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
      form.reset();
      setImageFiles([]);
      setImagePreviews([]);
      onSuccess?.();
    } catch (requestError: unknown) {
      const isNetworkError = !isAxiosError(requestError) ||
        requestError.code === 'ERR_NETWORK' ||
        requestError.message?.includes('Network') ||
        !requestError.response;
      if (isNetworkError) {
        saveOffline(data);
        return;
      }
      const response = isAxiosError<BackendErrorData>(requestError)
        ? requestError.response?.data
        : undefined;
      let message = 'Có lỗi xảy ra khi ghi nhận thu hoạch.';
      if (response?.status === 400 && response.errors) {
        message = Object.values(response.errors).join('. ');
      } else if (response?.status === 403) {
        message = response.message || 'Bạn không có quyền thực hiện thao tác này.';
      } else if (response?.status === 404) {
        message = response.message || 'Không tìm thấy lô sản xuất.';
      } else if (response?.status === 409) {
        message = response.message ||
          'Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.';
      } else if (response?.message) {
        message = response.message;
      }
      setError(message);
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };
  return {
    ...form,
    canOverride,
    currentPosition,
    eligibility,
    error,
    handleImageChange,
    handleLocationSelect,
    imageFiles,
    imagePreviews,
    isOverrideBlocked,
    isSubmitting,
    loadingEligibility,
    maxImages: MAX_IMAGES,
    onSubmit,
    removeImage,
    selectedHarvestDate,
    isEarlyHarvest,
  };
}

export type HarvestFormController = ReturnType<typeof useHarvestForm>;
