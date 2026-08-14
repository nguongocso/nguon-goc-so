export interface SuspectTraceCodeResponse {
  id: string;
  codeValue: string;
  shipmentName: string;
  status: 'SUSPECT' | 'LOCKED';
  suspicionScore: number;
  suspicionReason: string | null;
  scanCount: number;
  uniqueLocations: number;
  firstScannedAt: string | null;
  lastScannedAt: string | null;
  lockedAt: string | null;
  lockedBy: string | null;
  lockedByName: string | null;
  lockReason: string | null;
}

export interface ScanLogDetail {
  scannedAt: string;
  latitude: number | null;
  longitude: number | null;
  location: string;
  userAgent: string;
}

export interface ScoreBreakdown {
  highFrequency: number;
  impossibleTravel: number;
  multipleLocations: number;
}

export interface AnomalyDetails {
  totalScans: number;
  uniqueLocations: number;
  impossibleTravelCount: number;
  scoreBreakdown: ScoreBreakdown;
}

export interface SuspectTraceCodeDetailResponse
  extends SuspectTraceCodeResponse {
  scanLogs: ScanLogDetail[];
  anomalyDetails: AnomalyDetails;
}

export interface LockTraceCodeRequest {
  reason: string;
}

export interface LockTraceCodeResponse {
  id: string;
  codeValue: string;
  status: string;
  lockedAt: string;
  lockedBy: string;
  lockedByName: string;
  lockReason: string;
  notificationSent: boolean;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}