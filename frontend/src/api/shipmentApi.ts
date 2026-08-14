import apiClient from './axiosConfig';
import type {
  CreateShipmentPayload,
  ProcurementShipment,
  Shipment,
  ShipmentResponse,
  ShipmentSummary,
} from '@/types/shipment';

/**
 * Lấy danh sách lô hàng của một lô sản xuất
 * GET /api/v1/shipments/production-lots/{productionLotId}
 */
export const getShipmentsByProductionLot = async (
  productionLotId: string,
): Promise<Shipment[]> => {
  const response = await apiClient.get<{ data: Shipment[] }>(
    `/shipments/production-lots/${productionLotId}`,
  );

  return response.data.data;
};

/**
 * Lấy chi tiết một lô hàng theo ID
 * GET /api/v1/shipments/{id}
 */
export const getShipmentById = async (
  id: string,
): Promise<Shipment> => {
  const response = await apiClient.get<ShipmentResponse>(
    `/shipments/${id}`,
  );

  return response.data.data;
};

/**
 * Tạo lô hàng mới và sinh mã truy xuất
 * POST /api/v1/shipments
 */
export const createShipment = async (
  payload: CreateShipmentPayload,
): Promise<Shipment> => {
  const response = await apiClient.post<ShipmentResponse>(
    '/shipments',
    payload,
  );

  return response.data.data;
};

/**
 * Kích hoạt toàn bộ tem đã được cấp cho một lô hàng.
 * POST /api/v1/shipments/{shipmentId}/activate
 */
export const activateShipmentStamps = async (
  shipmentId: string,
): Promise<Shipment> => {
  const response = await apiClient.post<ShipmentResponse>(
    `/shipments/${shipmentId}/activate`,
  );

  return response.data.data;
};

/**
 * Tra cứu lô hàng bằng mã truy xuất (codeValue in trên tem QR).
 * Dùng bởi VT-04 để xác nhận lô hàng trước khi ghi sự kiện thu mua.
 * GET /api/v1/shipments/by-code?code=...
 */
export const getShipmentByCode = async (
  code: string,
): Promise<ShipmentSummary> => {
  const response = await apiClient.get<{
    success: boolean;
    data: ShipmentSummary;
  }>('/shipments/by-code', {
    params: { code },
  });

  return response.data.data;
};

/**
 * Lấy danh sách lô hàng đủ điều kiện thu mua (status = ACTIVATED).
 * Dùng cho Doanh nghiệp thu mua (VT-04).
 * GET /api/v1/shipments/eligible
 */
export const getEligibleShipments = async (): Promise<
  ProcurementShipment[]
> => {
  const response = await apiClient.get<{
    success: boolean;
    data: ProcurementShipment[];
  }>('/shipments/eligible');

  return response.data.data;
};