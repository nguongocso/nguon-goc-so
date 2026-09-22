import { useState } from 'react';
import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { recordProcurementEvent } from '@/api/procurementEventApi';
import type { RecordProcurementEventRequest, ChainEventResponse } from '@/types/procurementEvent';

/**
 * Kết quả trả về từ hook useProcurementEvent
 */
interface UseProcurementEventResult {
  data: ChainEventResponse | null;
  isLoading: boolean;
  error: string | null;
  submit: (request: RecordProcurementEventRequest) => Promise<void>;
  reset: () => void;
}

/**
 * Hook quản lý trạng thái và gửi yêu cầu ghi nhận sự kiện thu mua
 */
export const useProcurementEvent = (): UseProcurementEventResult => {
  const [data, setData] = useState<ChainEventResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (request: RecordProcurementEventRequest) => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await recordProcurementEvent(request);
      setData(result);
      toast.success('Ghi sự kiện thu mua thành công.');
    } catch (err: unknown) {
      let message = 'Không thể kết nối đến máy chủ.';
      if (isAxiosError(err)) {
        message =
          (err.response?.data as { message?: string } | undefined)?.message ||
          (err.response ? 'Không thể ghi sự kiện thu mua.' : 'Không thể kết nối đến máy chủ.');
      } else if (err instanceof Error) {
        message = err.message;
      }
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