import type { PublicInspectionResult } from './publicInspection';

export interface PublicChainEventItem {
  eventType: string;
  eventData: Record<string, unknown>;
  recordedAt: string;
  latitude: number | null;
  longitude: number | null;
}

/** Tọa độ một đỉnh polygon ranh giới vùng trồng. */
export interface FarmAreaBoundaryPoint {
  latitude: number;
  longitude: number;
}

/**
 * Ranh giới vùng trồng công khai (CV-05, QTN-12).
 * Null trong PublicTraceResponse khi lô sản xuất chưa gắn vùng trồng
 * hoặc vùng trồng chưa được khoanh ranh giới.
 */
export interface PublicFarmAreaBoundary {
  id: string;
  name: string;
  /** Diện tích (ha), null khi chưa tính. */
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
  /** Ranh giới vùng trồng (CV-05). Null khi chưa khoanh ranh giới. */
  farmAreaBoundary?: PublicFarmAreaBoundary | null;
}

export interface ApiError {
  success: false;
  status: number;
  message: string;
  path?: string;
  timestamp?: string;
}
