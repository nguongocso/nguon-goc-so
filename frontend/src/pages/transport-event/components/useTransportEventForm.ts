import { useState, type ChangeEvent } from 'react';
import { isAxiosError } from 'axios';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { useLocation } from 'react-router-dom';
import { toast } from 'sonner';
import { recordTransportEvent } from '@/api/transportEventApi';
import { ChainEventType } from '@/enums/chainEventType';
import { useOfflineSync } from '@/hooks/useOfflineSync';
import { addOfflineEvent } from '@/services/offlineQueue';
import {
  transportEventSchema,
  type TransportEventFormValues,
} from '@/utils/validators/transportEventSchema';

export const MAX_TRANSPORT_IMAGES = 5;

interface RecordTransportWithImagesPayload {
  codeValue: string;
  fromLocation: string;
  toLocation: string;
  transportTime: string;
  images?: string[];
}

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

/** Quản lý dữ liệu và thao tác ghi nhận sự kiện vận chuyển. */
export function useTransportEventForm() {
  const location = useLocation();
  const prefilledCode = (location.state as { codeValue?: string } | null)?.codeValue ?? '';
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);
  const { isOnline } = useOfflineSync();
  const form = useForm<TransportEventFormValues>({
    resolver: zodResolver(transportEventSchema),
    defaultValues: {
      codeValue: prefilledCode,
      fromLocation: '',
      toLocation: '',
      transportTime: getCurrentDateTimeLocal(),
    },
  });

  const resetForm = () => {
    form.reset({
      codeValue: '',
      fromLocation: '',
      toLocation: '',
      transportTime: getCurrentDateTimeLocal(),
    });
    setImageFiles([]);
    setImagePreviews([]);
  };

  const handleImageChange = (event: ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files) return;
    const fileArray = Array.from(files);
    if (imageFiles.length + fileArray.length > MAX_TRANSPORT_IMAGES) {
      toast.error(`Chỉ được chọn tối đa ${MAX_TRANSPORT_IMAGES} ảnh`);
      return;
    }
    setImageFiles((current) => [...current, ...fileArray]);
    setImagePreviews((current) => [
      ...current,
      ...fileArray.map((file) => URL.createObjectURL(file)),
    ]);
  };

  const removeImage = (index: number) => {
    setImageFiles((current) => current.filter((_, itemIndex) => itemIndex !== index));
    setImagePreviews((current) => current.filter((_, itemIndex) => itemIndex !== index));
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
      eventData: values,
    });
    if (validationError) {
      toast.error(`Không thể lưu tạm: ${validationError}`);
      return false;
    }
    toast.info('Không có kết nối mạng. Sự kiện đã được lưu tạm và sẽ đồng bộ khi có mạng.');
    resetForm();
    return true;
  };

  const onSubmit = async (values: TransportEventFormValues) => {
    try {
      if (!isOnline) {
        saveOffline(values);
        return;
      }

      let images: string[];
      try {
        images = await Promise.all(imageFiles.map(fileToBase64));
      } catch {
        toast.error('Không thể xử lý ảnh. Vui lòng thử lại.');
        return;
      }
      const payload: RecordTransportWithImagesPayload = {
        ...values,
        images: images.length > 0 ? images : undefined,
      };
      await recordTransportEvent(payload);
      toast.success('Ghi sự kiện vận chuyển thành công.');
      resetForm();
    } catch (error: unknown) {
      if (isNetworkError(error)) {
        saveOffline(values);
        return;
      }
      toast.error(getSubmissionError(error));
    }
  };

  return {
    codeValue: form.watch('codeValue'),
    errors: form.formState.errors,
    handleImageChange,
    imageFiles,
    imagePreviews,
    isSubmitting: form.formState.isSubmitting,
    register: form.register,
    removeImage,
    setCodeValue: (value: string) => form.setValue('codeValue', value, { shouldValidate: true }),
    submitForm: form.handleSubmit(onSubmit),
  };
}

function isNetworkError(error: unknown) {
  if (isAxiosError(error)) return error.code === 'ERR_NETWORK' || !error.response;
  if (error instanceof Error) return error.message.includes('Network');
  if (typeof error === 'object' && error !== null && 'response' in error) {
    return !(error as { response?: unknown }).response;
  }
  return false;
}

function getSubmissionError(error: unknown) {
  if (!isAxiosError<{ message?: string }>(error)) {
    return 'Đã xảy ra lỗi khi ghi sự kiện vận chuyển.';
  }
  return error.response?.data?.message ?? (error.response
    ? 'Không thể ghi sự kiện vận chuyển.'
    : 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra backend.');
}

export type TransportEventController = ReturnType<typeof useTransportEventForm>;
