export interface RecordTransportEventPayload {
  codeValue: string;
  fromLocation: string;
  toLocation: string;
  transportTime: string;
}

export interface TransportEventData {
  fromLocation: string;
  toLocation: string;
}

export interface TransportEvent {
  id: string;
  shipmentId: string;
  eventType: "TRANSPORT";
  eventData: TransportEventData;
  recordedAt: string;
  recordedByName: string;
  createdAt: string;
}

export interface TransportEventResponse {
  success: boolean;
  status: number;
  data: TransportEvent;
  timestamp: string;
}