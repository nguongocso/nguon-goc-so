import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { zodResolver } from '@hookform/resolvers/zod';

import { useAutoGeolocation } from '@/hooks/useAutoGeolocation';
import { useLotValidation } from '@/hooks/useLotValidation';

import { recordPreprocessingEvent } from '@/api/preprocessingApi';
import { getProductionLots } from '@/api/productionLotApi';
import type { ProductionLot } from '@/types/productionLot';
import { getLocalDateString } from '@/utils/dateTime';
import {
  recordPreprocessingSchema,
  type RecordPreprocessingFormValues,
} from '@/utils/validators/preprocessingEventSchema';

import {
  fileToBase64,
  getPreprocessingErrorMessage,
  MAX_PREPROCESSING_IMAGES,
  MAX_PREPROCESSING_IMAGE_SIZE,
  toOptionalText,
} from '../preprocessingFormUtils';

/** Quản lý dữ liệu và hành vi của biểu mẫu tạo sự kiện sơ chế. */
export function useCreatePreprocessingForm() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const preselectedLotId = searchParams.get('productionLotId') ?? '';
  const [productionLots, setProductionLots] = useState<ProductionLot[]>([]);
  const [loadingLots, setLoadingLots] = useState(true);
  const [lotsError, setLotsError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);
  const previewUrlsRef = useRef<string[]>([]);
  const form = useForm<RecordPreprocessingFormValues>({
    resolver: zodResolver(recordPreprocessingSchema),
    defaultValues: {
      productionLotId: '',
      inputQuantity: undefined,
      outputQuantity: undefined,
      grade: '',
      processingMethod: '',
      preprocessingDate: getLocalDateString(),
      latitude: undefined,
      longitude: undefined,
    },
  });
  const selectedLotId = form.watch('productionLotId');
  const inputQuantity = form.watch('inputQuantity');
  const outputQuantity = form.watch('outputQuantity');
  const latitude = form.watch('latitude');
  const longitude = form.watch('longitude');
  const selectedLot = productionLots.find((lot) => lot.id === selectedLotId);
  const { validation, loading: validationLoading, error: validationError } =
    useLotValidation(selectedLotId, 'PREPROCESSING');
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

  const loadLots = useCallback(async () => {
    setLoadingLots(true);
    setLotsError(null);
    try {
      const lots = await getProductionLots('HARVESTED');
      setProductionLots(lots.filter((lot) => lot.status === 'HARVESTED'));
    } catch (error) {
      setProductionLots([]);
      setLotsError(getPreprocessingErrorMessage(
        error,
        'Không thể tải danh sách lô đã thu hoạch.',
      ));
    } finally {
      setLoadingLots(false);
    }
  }, []);

  useEffect(() => void loadLots(), [loadLots]);
  useEffect(() => {
    if (loadingLots || !preselectedLotId || selectedLotId) return;
    const lot = productionLots.find((item) => item.id === preselectedLotId);
    if (!lot) {
      setLotsError(
        'Lô được chọn không còn ở trạng thái đã thu hoạch. Hãy chọn một lô hợp lệ.',
      );
      return;
    }
    form.setValue('productionLotId', lot.id, { shouldValidate: true });
    if (lot.actualQuantity && lot.actualQuantity > 0) {
      form.setValue('inputQuantity', lot.actualQuantity, {
        shouldValidate: true,
      });
    }
  }, [form, loadingLots, preselectedLotId, productionLots, selectedLotId]);
  useEffect(
    () => () => previewUrlsRef.current.forEach(URL.revokeObjectURL),
    [],
  );

  const handleLotChange = (lotId: string | null) => {
    const nextLotId = lotId ?? '';
    const lot = productionLots.find((item) => item.id === nextLotId);
    setServerError(null);
    setLotsError(null);
    form.setValue('productionLotId', nextLotId, {
      shouldDirty: true,
      shouldValidate: true,
    });
    if (lot?.actualQuantity && lot.actualQuantity > 0) {
      form.setValue('inputQuantity', lot.actualQuantity, {
        shouldDirty: true,
        shouldValidate: true,
      });
    }
  };
  const handleLocationSelect = useCallback((lat: number, lng: number) => {
    form.setValue('latitude', lat, { shouldDirty: true, shouldValidate: true });
    form.setValue('longitude', lng, { shouldDirty: true, shouldValidate: true });
  }, [form]);
  useAutoGeolocation({
    onLocation: (lat, lng) => {
      handleLocationSelect(lat, lng);
      toast.success('Đã cập nhật vị trí hiện tại cho sự kiện sơ chế.');
    },
    onError: (message) => {
      toast.error(`Không thể lấy vị trí hiện tại: ${message}`);
    },
  });

  const handleImageChange = async (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const files = event.target.files;
    if (!files?.length) return;
    const fileList = Array.from(files);
    if (imageFiles.length + fileList.length > MAX_PREPROCESSING_IMAGES) {
      toast.error(`Chỉ được chọn tối đa ${MAX_PREPROCESSING_IMAGES} ảnh thực địa.`);
      event.target.value = '';
      return;
    }
    for (const file of fileList) {
      if (!file.type.startsWith('image/')) {
        toast.error(`File "${file.name}" không phải định dạng ảnh.`);
        event.target.value = '';
        return;
      }
      if (file.size > MAX_PREPROCESSING_IMAGE_SIZE) {
        toast.error(`Ảnh "${file.name}" vượt quá kích thước 5 MB.`);
        event.target.value = '';
        return;
      }
    }
    const newUrls = fileList.map(URL.createObjectURL);
    previewUrlsRef.current = [...previewUrlsRef.current, ...newUrls];
    setImageFiles((current) => [...current, ...fileList]);
    setImagePreviews((current) => [...current, ...newUrls]);
    event.target.value = '';
  };
  const removeImage = (index: number) => {
    const targetUrl = imagePreviews[index];
    if (targetUrl) {
      URL.revokeObjectURL(targetUrl);
      previewUrlsRef.current = previewUrlsRef.current.filter(
        (url) => url !== targetUrl,
      );
    }
    setImageFiles((current) => current.filter((_, i) => i !== index));
    setImagePreviews((current) => current.filter((_, i) => i !== index));
  };
  const onSubmit = async (values: RecordPreprocessingFormValues) => {
    setServerError(null);
    if (validation && !validation.valid) {
      const message = validation.message || 'Lô sản xuất không hợp lệ.';
      setServerError(message);
      toast.error(message);
      return;
    }
    try {
      const images = await Promise.all(imageFiles.map(fileToBase64));
      await recordPreprocessingEvent({
        productionLotId: values.productionLotId,
        inputQuantity: values.inputQuantity,
        outputQuantity: values.outputQuantity,
        grade: toOptionalText(values.grade),
        processingMethod: toOptionalText(values.processingMethod),
        preprocessingDate: values.preprocessingDate,
        latitude: values.latitude,
        longitude: values.longitude,
        images: images.length ? images : undefined,
      });
      toast.success('Ghi nhận sự kiện sơ chế thành công!');
      navigate(`/production-lots/${values.productionLotId}`);
    } catch (error) {
      const message = getPreprocessingErrorMessage(
        error,
        'Không thể lưu sự kiện sơ chế. Vui lòng kiểm tra lại thông tin.',
      );
      setServerError(message);
      if (message.includes('Sản lượng sau sơ chế')) {
        form.setError('outputQuantity', { type: 'server', message });
      } else if (message.includes('Sản lượng ban đầu')) {
        form.setError('inputQuantity', { type: 'server', message });
      }
      toast.error(message);
    }
  };

  return {
    ...form,
    currentPosition,
    handleImageChange,
    handleLocationSelect,
    handleLotChange,
    imageFiles,
    imagePreviews,
    loadingLots,
    loadLots,
    lossRate,
    lotsError,
    navigate,
    onSubmit,
    productionLots,
    removeImage,
    selectedLot,
    selectedLotId,
    serverError,
    validation,
    validationError,
    validationLoading,
  };
}

export type CreatePreprocessingController = ReturnType<
  typeof useCreatePreprocessingForm
>;
