import apiClient from './axiosConfig';
import type {
  CreateShipmentPayload,
  PageResponse,
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
 * Lấy danh sách lô hàng theo lô sản xuất với phân trang
 * GET /api/v1/shipments/production-lots/{productionLotId}/paged?page=0&size=10
 */
export const getShipmentsByProductionLotPaged = async (
  productionLotId: string,
  page: number,
  size = 10,
): Promise<PageResponse<Shipment>> => {
  const response = await apiClient.get<{ data: PageResponse<Shipment> }>(
    `/shipments/production-lots/${productionLotId}/paged`,
    { params: { page, size } },
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

/** Các trường tùy chọn in trên tem QR (NCL-04-CN-005). */
export interface LabelIncludeFields {
  productName?: boolean;
  cooperativeName?: boolean;
  lotCode?: boolean;
  packagingDate?: boolean;
}

/** Payload xuất tem QR cho lô hàng (NCL-04-CN-005). */
export interface ExportLabelsPayload {
  /** Chỉ số bắt đầu trong danh sách mã đã sinh (mặc định 0). */
  startIndex?: number;
  /** Số tem cần xuất. */
  count: number;
  /** Khổ tem: "40x30" | "50x40" | "70x50" (mm). */
  labelSize: string;
  /** Các trường tùy chọn in trên tem. */
  includeFields?: LabelIncludeFields;
}

/**
 * Xuất file PDF chứa tem QR của lô hàng và tải về.
 * POST /api/v1/shipments/{shipmentId}/labels/export
 * Chỉ dành cho VT-02 (Quản lý HTX).
 */
export const exportQrLabels = async (
  shipmentId: string,
  payload: ExportLabelsPayload,
): Promise<Blob> => {
  try {
    const response = await apiClient.post(
      `/shipments/${shipmentId}/labels/export`,
      payload,
      {
        responseType: 'blob',
        timeout: 60000,
      },
    );
    return response.data as Blob;
  } catch (error: any) {
    if (
      error.response?.data instanceof Blob &&
      error.response.data.type?.includes('application/json')
    ) {
      const text = await error.response.data.text();
      let message = text || 'Lỗi khi xuất tem QR';
      try {
        const errJson = JSON.parse(text);
        if (errJson?.message) {
          message = errJson.message;
        }
      } catch {
        // Không phải JSON hợp lệ → giữ nguyên text
      }
      throw new Error(message);
    }
    throw error;
  }
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