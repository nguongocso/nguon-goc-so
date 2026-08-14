export interface RecordProcurementEventRequest {
  shipmentId: string;
  receivedQuantity: number;
  notes?: string;
  latitude?: number;
  longitude?: number;
}

export interface ProcurementEventData {
  shipmentId: string;
  shipmentName: string;
  receivedQuantity: number;
  notes?: string;
}

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