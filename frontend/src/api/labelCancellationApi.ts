import apiClient from './axiosConfig';
import type {
  CancelTraceCodesPayload,
  CancelTraceCodesResult,
  LabelCancellationHistoryItem,
} from '@/types/shipment';

/**
 * Hủy danh sách tem in hỏng và hoàn lại hạn mức dải mã cho hợp tác xã.
 * POST /api/v1/trace/shipments/{shipmentId}/cancel-labels
 */
export const cancelTraceCodes = async (
  shipmentId: string,
  payload: CancelTraceCodesPayload,
): Promise<CancelTraceCodesResult> => {
  const response = await apiClient.post<{
    success: boolean;
    data: CancelTraceCodesResult;
  }>(`/trace/shipments/${shipmentId}/cancel-labels`, payload);

  return response.data.data;
};

/**
 * Lấy lịch sử các đợt hủy tem của lô hàng.
 * GET /api/v1/trace/shipments/{shipmentId}/cancellation-history
 */
export const getLabelCancellationHistory = async (
  shipmentId: string,
): Promise<LabelCancellationHistoryItem[]> => {
  const response = await apiClient.get<{
    success: boolean;
    data: LabelCancellationHistoryItem[];
  }>(`/trace/shipments/${shipmentId}/cancellation-history`);

  return response.data.data;
};
