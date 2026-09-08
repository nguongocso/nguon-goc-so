import apiClient from './axiosConfig';
import type { CreateHandoverPayload, ShipmentHandover, HandoverDetailResponse } from '@/types/shipmentHandover';

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
