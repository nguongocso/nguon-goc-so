/** Dữ liệu yêu cầu yêu cầu ghi nhận sự kiện vận chuyển lô hàng. */
export interface RecordTransportEventPayload {
  codeValue: string;
  fromLocation: string;
  toLocation: string;
  transportTime: string;
}

/** Cấu trúc dữ liệu chi tiết của sự kiện vận chuyển. */
export interface TransportEventData {
  fromLocation: string;
  toLocation: string;
}

/** Đối tượng sự kiện vận chuyển trong chuỗi cung ứng. */
export interface TransportEvent {
  id: string;
  shipmentId: string;
  eventType: 'TRANSPORT';
  eventData: TransportEventData;
  recordedAt: string;
  recordedByName: string;
  createdAt: string;
}

/** Phản hồi chi tiết sau khi ghi nhận sự kiện vận chuyển. */
export interface TransportEventResponse {
  success: boolean;
  status: number;
  data: TransportEvent;
  timestamp: string;
}
