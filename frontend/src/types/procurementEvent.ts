/** Payload yêu cầu ghi nhận sự kiện thu mua nông sản từ HTX/Nông dân. */
export interface RecordProcurementEventRequest {
  shipmentId: string;
  receivedQuantity: number;
  notes?: string;
  latitude?: number;
  longitude?: number;
}

/** Cấu trúc dữ liệu chi tiết của sự kiện thu mua nông sản. */
export interface ProcurementEventData {
  shipmentId: string;
  shipmentName: string;
  receivedQuantity: number;
  notes?: string;
}

/** Dữ liệu phản hồi sự kiện thu mua trong chuỗi cung ứng. */
export interface ChainEventResponse {
  id: string;
  shipmentId: string;
  eventType: string;
  eventData: ProcurementEventData;
  latitude?: number;
  longitude?: number;
  recordedAt: string;
  recordedByName: string;
  createdAt: string;
}