import { useState } from 'react';
import { toast } from 'sonner';
import { recordProcurementEvent } from '@/api/procurementEventApi';
import type { RecordProcurementEventRequest, ChainEventResponse } from '@/types/procurementEvent';

interface UseProcurementEventResult {
  data: ChainEventResponse | null;
  isLoading: boolean;
  error: string | null;
  submit: (request: RecordProcurementEventRequest) => Promise<void>;
  reset: () => void;
}

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
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        (err.response ? 'Không thể ghi sự kiện thu mua.' : 'Không thể kết nối đến máy chủ.');
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