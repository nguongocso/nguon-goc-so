import apiClient from './axiosConfig';
import type { CreateHandoverPayload, ShipmentHandover, HandoverDetailResponse } from '@/types/shipmentHandover';
import type { HandoverSummary, HandoverListParams } from '@/types/handover';
import type { PageResponse } from '@/types/common';

/**
 * Lấy danh sách phiếu bàn giao nhận cho tổ chức thu mua (VT-04).
 * GET /api/v1/handovers
 */
export const getHandovers = async (
  params?: HandoverListParams,
): Promise<PageResponse<HandoverSummary>> => {
  const response = await apiClient.get<{ data: PageResponse<HandoverSummary> }>('/handovers', {
    params,
  });
  return response.data.data;
};

/**
 * Alias cho acceptHandover
 */
export const confirmHandover = acceptHandover;

/**
 * Tạo phiếu bàn giao mới.
 * POST /api/v1/shipment-handovers
 */
export const createHandover = async (payload: CreateHandoverPayload): Promise<ShipmentHandover> => {
  const response = await apiClient.post<{ data: ShipmentHandover }>('/shipment-handovers', payload);
  return response.data.data;
};

/**
 * Hủy phiếu bàn giao.
 * POST /api/v1/shipment-handovers/{id}/cancel
 */
export const cancelHandover = async (id: string, reason: string): Promise<ShipmentHandover> => {
  const response = await apiClient.post<{ data: ShipmentHandover }>(`/shipment-handovers/${id}/cancel`, { reason });
  return response.data.data;
};

/**
 * Lấy số lượng còn lại có thể bàn giao của lô hàng.
 * GET /api/v1/shipments/{id}/remaining-handover-quantity
 */
export const getRemainingQuantity = async (shipmentId: string): Promise<number> => {
  const response = await apiClient.get<{ data: number }>(`/shipments/${shipmentId}/remaining-handover-quantity`);
  return response.data.data;
};

/**
 * Kiểm tra lô hàng có phiếu bàn giao đang chờ xác nhận hay không.
 * Dùng để hiển thị nhãn "Đang bàn giao" (NCL-05-CN-008).
 * GET /api/v1/shipments/{id}/has-pending-handover
 */
export const hasPendingHandover = async (shipmentId: string): Promise<boolean> => {
  const response = await apiClient.get<{ data: boolean }>(`/shipments/${shipmentId}/has-pending-handover`);
  return response.data.data;
};

/**
 * Lấy danh sách phiếu bàn giao đã nhận.
 * GET /api/v1/shipment-handovers/received
 */
export const getReceivedHandovers = async (): Promise<HandoverDetailResponse[]> => {
  const response = await apiClient.get<{ data: HandoverDetailResponse[] }>('/shipment-handovers/received');
  return response.data.data;
};

/**
 * Lấy danh sách phiếu bàn giao đã gửi.
 * GET /api/v1/shipment-handovers/sent
 */
export const getSentHandovers = async (): Promise<HandoverDetailResponse[]> => {
  const response = await apiClient.get<{ data: HandoverDetailResponse[] }>('/shipment-handovers/sent');
  return response.data.data;
};

/**
 * Lấy chi tiết phiếu bàn giao.
 * GET /api/v1/shipment-handovers/{id}
 */
export const getHandoverById = async (id: string): Promise<HandoverDetailResponse> => {
  const response = await apiClient.get<{ data: HandoverDetailResponse }>(`/shipment-handovers/${id}`);
  return response.data.data;
};

/**
 * Xác nhận nhận bàn giao.
 * POST /api/v1/shipment-handovers/{id}/accept
 */
export const acceptHandover = async (id: string): Promise<ShipmentHandover> => {
  const response = await apiClient.post<{ data: ShipmentHandover }>(`/shipment-handovers/${id}/accept`);
  return response.data.data;
};

/**
 * Từ chối nhận bàn giao.
 * POST /api/v1/shipment-handovers/{id}/reject
 */
export const rejectHandover = async (id: string, reason: string): Promise<ShipmentHandover> => {
  const response = await apiClient.post<{ data: ShipmentHandover }>(`/shipment-handovers/${id}/reject`, { reason });
  return response.data.data;
};

/**
 * Tải lên chứng từ giao hàng trước khi tạo phiếu bàn giao.
 * POST /api/v1/shipment-handovers/attachment (multipart)
 *
 * Trả về đường dẫn file (filePath) để gửi kèm trong attachmentPath
 * khi tạo phiếu bàn giao (NCL-05-CN-008: thay ô nhập tay URL).
 */
export const uploadHandoverAttachment = async (file: File): Promise<string> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await apiClient.post<{
    data: { filePath: string };
  }>('/shipment-handovers/attachment', formData, {
    headers: {
      /**
       * Override Content-Type mặc định 'application/json' của apiClient.
       * Axios tự thay bằng multipart/form-data kèm boundary khi gửi đi
       * (giống uploadAttachment / uploadInspectionResultFile).
       */
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data.data.filePath;
};
