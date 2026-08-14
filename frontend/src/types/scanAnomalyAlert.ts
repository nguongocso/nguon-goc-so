export type ScanAnomalyAlertStatus = 'PENDING' | 'RESOLVED';

export type ScanAnomalyAlertFilterStatus = ScanAnomalyAlertStatus | 'ALL';

export type ScanAnomalyAlertSeverity = 'MEDIUM' | 'HIGH';

export interface ScanPoint {
  latitude: number;
  longitude: number;
  scannedAt: string;
}

export interface ScanAnomalyAlertDetails {
  locations: ScanPoint[];
  scanCount: number;
  thresholdConfigured: number;
}

export interface ScanAnomalyAlert {
  id: string;
  type: 'SCAN_ANOMALY';
  relatedEntityType: 'TraceCode';
  relatedEntityId: string;
  severity: ScanAnomalyAlertSeverity;
  details: ScanAnomalyAlertDetails;
  status: ScanAnomalyAlertStatus;
  createdAt: string;
  resolvedAt: string | null;
  resolvedBy: string | null;
}

export interface ScanAnomalyAlertListResponse {
  content: ScanAnomalyAlert[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ScanAnomalyAlertParams {
  status?: ScanAnomalyAlertFilterStatus;
  fromDate?: string;
  toDate?: string;
  organizationId?: string;
  page?: number;
  size?: number;
}

export interface ResolveScanAnomalyAlertRequest {
  resolutionNote?: string;
}

export interface ResolveScanAnomalyAlertResponse {
  id: string;
  status: 'RESOLVED';
  resolvedAt: string;
  resolvedBy: string;
  auditLogId: string;
}
