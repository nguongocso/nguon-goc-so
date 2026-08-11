import { useState } from 'react';
import { toast } from 'sonner';
import { recordWarehouseReceipt } from '@/api/warehouseReceiptApi';
import type { WarehouseReceiptRequest, WarehouseReceiptResponse } from '@/types/warehouseReceipt';

interface UseWarehouseReceiptResult {
  data: WarehouseReceiptResponse | null;
  isLoading: boolean;
  error: string | null;
  submit: (request: WarehouseReceiptRequest) => Promise<void>;
  reset: () => void;
}

export const useWarehouseReceipt = (): UseWarehouseReceiptResult => {
  const [data, setData] = useState<WarehouseReceiptResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (request: WarehouseReceiptRequest) => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await recordWarehouseReceipt(request);
      setData(result);
      toast.success('Ghi nhận nhập kho thành công.');
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        (err.response ? 'Không thể ghi nhận nhập kho.' : 'Không thể kết nối đến máy chủ.');
      setError(message);
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const reset = () => {
    setData(null);
    setError(null);
  };

  return { data, isLoading, error, submit, reset };
};