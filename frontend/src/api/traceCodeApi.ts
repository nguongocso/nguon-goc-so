import apiClient from './axiosConfig';
import type { PageResponse } from '@/types/shipment';
import type {
  ExportTraceCodesPayload,
  GetShipmentTraceCodesParams,
  TraceCodeHistory,
  TraceCodeSummary,
} from '@/types/traceCode';

/**
 * Lấy danh sách mã tem theo lô hàng (hỗ trợ lọc theo trạng thái, tìm kiếm và phân trang)
 * GET /api/v1/shipments/{shipmentId}/trace-codes
 */
export const getShipmentTraceCodes = async (
  shipmentId: string,
  params?: GetShipmentTraceCodesParams,
): Promise<PageResponse<TraceCodeSummary>> => {
  const response = await apiClient.get<{ data: PageResponse<TraceCodeSummary> }>(
    `/shipments/${shipmentId}/trace-codes`,
    { params },
  );
  return response.data.data;
};

/**
 * Tra cứu dòng thời gian lịch sử chi tiết của một mã tem
 * GET /api/v1/trace-codes/{codeValue}/history
 */
export const getTraceCodeHistory = async (
  codeValue: string,
): Promise<TraceCodeHistory> => {
  const response = await apiClient.get<{ data: TraceCodeHistory }>(
    `/trace-codes/${encodeURIComponent(codeValue)}/history`,
  );
  return response.data.data;
};

/**
 * Xuất danh sách mã tem ra file CSV
 * POST /api/v1/shipments/{shipmentId}/trace-codes/export
 */
export const exportTraceCodes = async (
  shipmentId: string,
  payload?: ExportTraceCodesPayload,
): Promise<Blob> => {
  const response = await apiClient.post(
    `/shipments/${shipmentId}/trace-codes/export`,
    payload || {},
    {
      responseType: 'blob',
    },
  );
  return response.data as Blob;
};
