export interface PublicChainEventItem {
  eventType: string;
  eventData: Record<string, any>;
  recordedAt: string;
  latitude: number | null;
  longitude: number | null;
}

export interface PublicTraceResponse {
  codeValue: string;
  productionLotId: string | null;
  lotName?: string | null;
  lotCode?: string | null;
  productName: string;
  shipmentCode: string;
  shipmentStatus: string;
  recalled: boolean;
  recallMessage: string | null;
  locked: boolean;
  lockReason: string | null;
  lockedAt: string | null;
  events: PublicChainEventItem[];
}

export interface ApiError {
  success: false;
  status: number;
  message: string;
  path?: string;
  timestamp?: string;
}