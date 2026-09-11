import type { ChainEventType } from '@/enums/chainEventType';

export interface RecordPackagingRequest {
  productionLotId: string;
  packagingSpecification: string;
  packagingDate: string;
  latitude?: number;
  longitude?: number;
}

export interface CorrectPackagingRequest {
  packagingSpecification: string;
  packagingDate: string;
  latitude?: number;
  longitude?: number;
  correctionReason: string;
}

export interface ChainEventResponse {
  id: string;
  shipmentId: string | null;
  eventType: ChainEventType;
  eventData: Record<string, any>;
  latitude: number | null;
  longitude: number | null;
  recordedAt: string;
  recordedByName: string;
  createdAt: string;
  lineageLevel?: 'PRODUCTION_LOT' | 'SOURCE_SHIPMENT' | 'CHILD_SHIPMENT' | null;
  sourceShipmentId?: string | null;
  inherited?: boolean;
}
