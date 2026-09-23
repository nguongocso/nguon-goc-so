import { useState, type FormEvent } from 'react';
import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { z } from 'zod';
import { scanLookupTraceCode } from '@/api/chainEventApi';
import { recordStorageCondition } from '@/api/storageConditionApi';
import { useAuth } from '@/hooks/useAuth';
import type { StorageConditionResponse } from '@/types/storageCondition';

const formSchema = z.object({
  codeValue: z.string().min(1, 'Vui lòng nhập mã truy xuất'),
  temperature: z.coerce.number({ required_error: 'Vui lòng nhập nhiệt độ' }),
  humidity: z.coerce
    .number({ required_error: 'Vui lòng nhập độ ẩm' })
    .min(0, 'Độ ẩm phải từ 0 đến 100%')
    .max(100, 'Độ ẩm phải từ 0 đến 100%'),
});

interface StorageLotInfo {
  shipmentName: string;
  productCategoryName: string;
  farmAreaName: string;
  shipmentStatus: string;
  storageEligible?: boolean | null;
}

/** Quản lý dữ liệu và thao tác ghi nhận điều kiện bảo quản. */
export function useStorageCondition() {
  const { user } = useAuth();
  const [codeValue, setCodeValue] = useState('');
  const [temperature, setTemperature] = useState('');
  const [humidity, setHumidity] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [result, setResult] = useState<StorageConditionResponse | null>(null);
  const [lotInfo, setLotInfo] = useState<StorageLotInfo | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [scanError, setScanError] = useState<string | null>(null);

  const isBlocked = lotInfo?.storageEligible === false;
  const blockedMessage = user?.roleCode === 'VT-03'
    ? 'Lô hàng phải có sự kiện vận chuyển trước khi ghi nhận điều kiện bảo quản.'
    : 'Bạn chưa thu mua lô hàng này. Chỉ doanh nghiệp đã thu mua lô hàng mới được ghi mốc bảo quản.';

  const clearLookup = () => {
    setLotInfo(null);
    setScanError(null);
    setFormError(null);
    setResult(null);
  };

  const changeCodeValue = (value: string) => {
    setCodeValue(value);
    clearLookup();
  };

  const handleScan = async (scannedCode?: string) => {
    const code = (scannedCode ?? codeValue).trim();
    if (!code) {
      setScanError('Vui lòng nhập mã truy xuất.');
      return;
    }
    setIsScanning(true);
    setScanError(null);
    setFormError(null);
    setResult(null);
    try {
      const lookupResult = await scanLookupTraceCode(code);
      if (!lookupResult.shipmentId) {
        setScanError('Không tìm thấy lô hàng cho mã này.');
        return;
      }
      setLotInfo({
        shipmentName: lookupResult.shipmentName,
        productCategoryName: lookupResult.productCategoryName,
        farmAreaName: lookupResult.farmAreaName,
        shipmentStatus: lookupResult.shipmentStatus,
        storageEligible: lookupResult.storageEligible,
      });
    } catch (error: unknown) {
      const message =
        (isAxiosError<{ message?: string }>(error) && error.response?.data?.message) ||
        'Không thể tra cứu mã.';
      setScanError(message);
    } finally {
      setIsScanning(false);
    }
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);
    if (isBlocked) {
      setFormError(blockedMessage);
      return;
    }

    const parsed = formSchema.safeParse({ codeValue, temperature, humidity });
    if (!parsed.success) {
      setFormError(parsed.error.issues[0]?.message ?? 'Dữ liệu không hợp lệ');
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await recordStorageCondition({
        codeValue: parsed.data.codeValue.trim(),
        temperature: parsed.data.temperature,
        humidity: parsed.data.humidity,
      });
      setResult(response);
      toast.success('Đã ghi nhận điều kiện bảo quản.');
    } catch (error: unknown) {
      const message =
        (isAxiosError<{ message?: string }>(error) && error.response?.data?.message) ||
        'Không thể ghi nhận.';
      setFormError(message);
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReset = () => {
    setCodeValue('');
    setTemperature('');
    setHumidity('');
    clearLookup();
  };

  return {
    blockedMessage,
    changeCodeValue,
    codeValue,
    formError,
    handleReset,
    handleScan,
    handleSubmit,
    humidity,
    isBlocked,
    isScanning,
    isSubmitting,
    lotInfo,
    recordDisabled: isSubmitting || isScanning || isBlocked,
    result,
    scanError,
    setHumidity,
    setTemperature,
    temperature,
  };
}

export type StorageConditionController = ReturnType<typeof useStorageCondition>;
