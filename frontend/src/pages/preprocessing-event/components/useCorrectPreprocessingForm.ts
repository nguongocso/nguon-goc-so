import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { zodResolver } from '@hookform/resolvers/zod';

import { useAutoGeolocation } from '@/hooks/useAutoGeolocation';

import { correctPreprocessingEvent } from '@/api/preprocessingApi';
import type { PreprocessingEventResponse } from '@/types/preprocessing';
import { getLocalDateString } from '@/utils/dateTime';
import {
  correctPreprocessingSchema,
  type CorrectPreprocessingFormValues,
} from '@/utils/validators/preprocessingEventSchema';

import {
  getPreprocessingErrorMessage,
  toOptionalText,
} from '../preprocessingFormUtils';

interface CorrectionLocationState {
  preprocessingEvent?: PreprocessingEventResponse;
}

/** Quản lý dữ liệu và hành vi của biểu mẫu đính chính sơ chế. */
export function useCorrectPreprocessingForm() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const sourceEvent = (location.state as CorrectionLocationState | null)
    ?.preprocessingEvent;
  const sourceData = sourceEvent?.eventData;
  const [serverError, setServerError] = useState<string | null>(null);
  const form = useForm<CorrectPreprocessingFormValues>({
    resolver: zodResolver(correctPreprocessingSchema),
    defaultValues: {
      inputQuantity: sourceData?.inputQuantity ?? 0,
      outputQuantity: sourceData?.outputQuantity ?? 0,
      grade: sourceData?.grade ?? '',
      processingMethod: sourceData?.processingMethod ?? '',
      preprocessingDate:
        sourceData?.preprocessingDate ?? getLocalDateString(),
      correctionReason: '',
      latitude: sourceEvent?.latitude ?? undefined,
      longitude: sourceEvent?.longitude ?? undefined,
    },
  });
  const inputQuantity = form.watch('inputQuantity');
  const outputQuantity = form.watch('outputQuantity');
  const latitude = form.watch('latitude');
  const longitude = form.watch('longitude');
  const currentPosition =
    typeof latitude === 'number' &&
    Number.isFinite(latitude) &&
    typeof longitude === 'number' &&
    Number.isFinite(longitude)
      ? { lat: latitude, lng: longitude }
      : undefined;
  const lossRate = useMemo(() => {
    if (
      !Number.isFinite(inputQuantity) ||
      inputQuantity <= 0 ||
      !Number.isFinite(outputQuantity) ||
      outputQuantity < 0 ||
      outputQuantity > inputQuantity
    ) return null;
    return Math.round(
      ((inputQuantity - outputQuantity) / inputQuantity) * 10_000,
    ) / 100;
  }, [inputQuantity, outputQuantity]);
  const handleLocationSelect = (latitude: number, longitude: number) => {
    form.setValue('latitude', latitude, {
      shouldDirty: true,
      shouldValidate: true,
    });
    form.setValue('longitude', longitude, {
      shouldDirty: true,
      shouldValidate: true,
    });
  };
  useAutoGeolocation({ onLocation: handleLocationSelect });
  const onSubmit = async (values: CorrectPreprocessingFormValues) => {
    if (!id) {
      setServerError('Thiếu ID sự kiện sơ chế gốc.');
      return;
    }
    setServerError(null);
    try {
      await correctPreprocessingEvent(id, {
        inputQuantity: values.inputQuantity,
        outputQuantity: values.outputQuantity,
        grade: toOptionalText(values.grade),
        processingMethod: toOptionalText(values.processingMethod),
        preprocessingDate: values.preprocessingDate,
        correctionReason: values.correctionReason.trim(),
        latitude: values.latitude,
        longitude: values.longitude,
      });
      toast.success('Đính chính sự kiện sơ chế thành công.');
      navigate(-1);
    } catch (error) {
      const message = getPreprocessingErrorMessage(
        error,
        'Không thể đính chính sự kiện sơ chế. Vui lòng thử lại.',
      );
      setServerError(message);
      toast.error(message);
    }
  };
  return {
    ...form,
    currentPosition,
    handleLocationSelect,
    id,
    lossRate,
    navigate,
    onSubmit,
    serverError,
    sourceData,
    sourceEvent,
  };
}

export type CorrectPreprocessingController = ReturnType<
  typeof useCorrectPreprocessingForm
>;
