import { useState, useCallback } from 'react';
import { toast } from 'sonner';
import { recordWarehouseReceipt, getWarehouseReceipts, getWarehouseReceiptDetail } from '@/api/warehouseReceiptApi';
import type { WarehouseReceiptRequest, WarehouseReceiptResponse } from '@/types/warehouseReceipt';
import type { PageResponse } from '@/types/common';

interface UseWarehouseReceiptResult {
  // List
  list: WarehouseReceiptResponse[];
  pageData: PageResponse<WarehouseReceiptResponse> | null;
  isLoadingList: boolean;
  error: string | null;

  // Create
  createResult: WarehouseReceiptResponse | null;
  isSubmitting: boolean;

  // Detail
  detail: WarehouseReceiptResponse | null;
  isLoadingDetail: boolean;

  // Actions
  fetchList: (page?: number, size?: number) => Promise<void>;
  submitReceipt: (request: WarehouseReceiptRequest) => Promise<boolean>;
  fetchDetail: (eventId: string) => Promise<void>;
  resetCreateResult: () => void;
}

export const useWarehouseReceipt = (): UseWarehouseReceiptResult => {
  // List state
  const [list, setList] = useState<WarehouseReceiptResponse[]>([]);
  const [pageData, setPageData] = useState<PageResponse<WarehouseReceiptResponse> | null>(null);
  const [isLoadingList, setIsLoadingList] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Create state
  const [createResult, setCreateResult] = useState<WarehouseReceiptResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Detail state
  const [detail, setDetail] = useState<WarehouseReceiptResponse | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  const fetchList = useCallback(async (page: number = 0, size: number = 10) => {
    setIsLoadingList(true);
    setError(null);
    try {
      const result = await getWarehouseReceipts(page, size);
      setList(result.items);
      setPageData(result);
    } catch (err: any) {
      const message =
        err.response?.data?.message || 'Không thể tải danh sách nhập kho.';
      setError(message);
      toast.error(message);
    } finally {
      setIsLoadingList(false);
    }
  }, []);

  const submitReceipt = useCallback(async (request: WarehouseReceiptRequest): Promise<boolean> => {
    setIsSubmitting(true);
    setError(null);
    try {
      const result = await recordWarehouseReceipt(request);
      setCreateResult(result);
      toast.success('Ghi nhận nhập kho thành công.');
      return true;
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        (err.response ? 'Không thể ghi nhận nhập kho.' : 'Không thể kết nối đến máy chủ.');
      setError(message);
      toast.error(message);
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  const fetchDetail = useCallback(async (eventId: string) => {
    setIsLoadingDetail(true);
    setError(null);
    try {
      const result = await getWarehouseReceiptDetail(eventId);
      setDetail(result);
    } catch (err: any) {
      const message =
        err.response?.data?.message || 'Không thể tải chi tiết nhập kho.';
      setError(message);
      toast.error(message);
    } finally {
      setIsLoadingDetail(false);
    }
  }, []);

  const resetCreateResult = useCallback(() => {
    setCreateResult(null);
  }, []);

  return {
    list,
    pageData,
    isLoadingList,
    error,
    createResult,
    isSubmitting,
    detail,
    isLoadingDetail,
    fetchList,
    submitReceipt,
    fetchDetail,
    resetCreateResult,
  };
};