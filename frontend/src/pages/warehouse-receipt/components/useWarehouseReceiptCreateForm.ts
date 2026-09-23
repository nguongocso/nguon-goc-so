import { useMemo, useState, type FormEvent } from 'react';
import { isAxiosError } from 'axios';
import { z } from 'zod';
import { scanLookupTraceCode } from '@/api/chainEventApi';
import { useWarehouseReceipt } from '@/hooks/useWarehouseReceipt';
import { getLocalDateString } from '@/utils/dateTime';

const ALLOWED_THRESHOLD = 2;

const formSchema = z.object({
  codeValue: z.string().min(1, 'Vui lòng nhập mã truy xuất'),
  receivedQuantity: z.coerce.number().positive('Số lượng thực nhận phải lớn hơn 0'),
  conditionNote: z.string().max(500, 'Ghi chú tối đa 500 ký tự').optional(),
  receiptDate: z.string().optional(),
  reason: z.string().max(500, 'Lý do tối đa 500 ký tự').optional(),
});

export interface WarehouseReceiptLotInfo {
  shipmentId: string;
  shipmentName: string;
  declaredQuantity: number;
  shipmentStatus: string;
  organizationName: string;
}

interface UseWarehouseReceiptCreateFormOptions {
  onOpenChange: (open: boolean) => void;
  onCreated: () => void;
}

/** Quản lý trạng thái và nghiệp vụ của biểu mẫu nhập kho. */
export function useWarehouseReceiptCreateForm({
  onOpenChange,
  onCreated,
}: UseWarehouseReceiptCreateFormOptions) {
  const today = getLocalDateString();
  const [codeValue, setCodeValue] = useState('');
  const [receivedQuantity, setReceivedQuantity] = useState('');
  const [conditionNote, setConditionNote] = useState('');
  const [receiptDate, setReceiptDate] = useState(today);
  const [reason, setReason] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [lotInfo, setLotInfo] = useState<WarehouseReceiptLotInfo | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [scanError, setScanError] = useState<string | null>(null);
  const { isSubmitting, error, submitReceipt } = useWarehouseReceipt();

  const declaredQuantity = lotInfo?.declaredQuantity ?? 0;
  const actualQty = parseFloat(receivedQuantity) || 0;
  const discrepancyInfo = useMemo(() => {
    if (!lotInfo || !receivedQuantity || Number.isNaN(actualQty)) return null;
    const difference = actualQty - declaredQuantity;
    const percent = declaredQuantity === 0
      ? (actualQty > 0 ? 100 : 0)
      : (difference / declaredQuantity) * 100;
    return {
      difference,
      percent,
      isExceeded: Math.abs(percent) > ALLOWED_THRESHOLD,
    };
  }, [lotInfo, receivedQuantity, actualQty, declaredQuantity]);

  const handleScan = async (scannedCode?: string) => {
    const code = (scannedCode ?? codeValue).trim();
    if (!code) {
      setScanError('Vui lòng nhập mã truy xuất.');
      return;
    }
    setIsScanning(true);
    setScanError(null);
    setLotInfo(null);
    try {
      const result = await scanLookupTraceCode(code);
      if (!result.shipmentId) {
        setScanError('Không tìm thấy lô hàng cho mã truy xuất này.');
        return;
      }
      if (result.shipmentStatus !== 'ACTIVATED') {
        setScanError('Lô hàng chưa được kích hoạt hoặc đã bị thu hồi.');
        return;
      }
      setLotInfo({
        shipmentId: result.shipmentId,
        shipmentName: result.shipmentName,
        declaredQuantity: result.totalQuantity ?? 0,
        shipmentStatus: result.shipmentStatus,
        organizationName: result.organizationName ?? '',
      });
    } catch (err: unknown) {
      const message =
        (isAxiosError<{ message?: string }>(err) && err.response?.data?.message) ||
        'Không thể tra cứu mã truy xuất.';
      setScanError(message);
    } finally {
      setIsScanning(false);
    }
  };

  const resetForm = () => {
    setCodeValue('');
    setReceivedQuantity('');
    setConditionNote('');
    setReceiptDate(today);
    setReason('');
    setFormError(null);
    setLotInfo(null);
    setScanError(null);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!lotInfo) {
      setFormError('Vui lòng tra cứu mã truy xuất trước.');
      return;
    }
    const validation = formSchema.safeParse({
      codeValue,
      receivedQuantity,
      conditionNote: conditionNote || undefined,
      receiptDate: receiptDate || undefined,
      reason: reason || undefined,
    });
    if (!validation.success) {
      setFormError(validation.error.issues[0]?.message ?? 'Dữ liệu không hợp lệ');
      return;
    }
    if (discrepancyInfo?.isExceeded && !reason.trim()) {
      setFormError(
        'Chênh lệch số lượng vượt ngưỡng cho phép (2%). Vui lòng cung cấp lý do chênh lệch.',
      );
      return;
    }
    setFormError(null);
    const success = await submitReceipt({
      codeValue: codeValue.trim(),
      receivedQuantity: parseFloat(receivedQuantity),
      conditionNote: conditionNote || undefined,
      receiptDate: receiptDate || undefined,
      reason: reason || undefined,
    });
    if (success) {
      resetForm();
      onCreated();
    }
  };

  const handleOpenChange = (open: boolean) => {
    if (!open) resetForm();
    onOpenChange(open);
  };

  const handleCodeChange = (value: string) => {
    setCodeValue(value);
    setLotInfo(null);
    setScanError(null);
  };

  return {
    codeValue,
    receivedQuantity,
    conditionNote,
    receiptDate,
    reason,
    formError,
    lotInfo,
    isScanning,
    scanError,
    isSubmitting,
    error,
    actualQty,
    discrepancyInfo,
    setReceivedQuantity,
    setConditionNote,
    setReceiptDate,
    setReason,
    handleCodeChange,
    handleScan,
    handleSubmit,
    handleOpenChange,
  };
}
