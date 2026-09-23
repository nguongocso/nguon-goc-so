import type { PublicInspectionResult } from './publicInspection';

export interface PublicChainEventItem {
  eventType: string;
  eventData: Record<string, any>;
  recordedAt: string;
  latitude: number | null;
  longitude: number | null;
}

export interface FarmAreaBoundaryPoint {
  latitude: number;
  longitude: number;
}

export interface PublicFarmAreaBoundary {
  id: string;
  name: string;
  calculatedArea: number | null;
  points: FarmAreaBoundaryPoint[];
}

export interface PublicTraceResponse {
  codeValue: string;
  productionLotId: string | null;
  lotName?: string | null;
  lotCode?: string | null;
  productName: string;
  productNameEn?: string | null;
  shipmentCode: string;
  shipmentStatus: string;
  recalled: boolean;
  recallMessage: string | null;
  recallMessageEn?: string | null;
  locked: boolean;
  lockReason: string | null;
  lockedAt: string | null;
  verificationNote?: string | null;
  unlockedAt?: string | null;
  events: PublicChainEventItem[];
  inspections?: PublicInspectionResult[];
  farmAreaBoundary?: PublicFarmAreaBoundary | null;
}

export interface ApiError {
  success: false;
  status: number;
  message: string;
  path?: string;
  timestamp?: string;
}
