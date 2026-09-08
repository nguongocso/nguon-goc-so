export type TraceCodeStatus =
  | 'INACTIVE'
  | 'ACTIVE'
  | 'LOCKED'
  | 'CANCELLED'
  | 'RECALLED'
  | 'SUSPECT';

export interface TraceCodeSummary {
  id: string;
  codeValue: string;
  status: TraceCodeStatus;
  activatedAt?: string | null;
  printedAt?: string | null;
  cancelledAt?: string | null;
  lockedAt?: string | null;
  scanCount: number;
  createdAt: string;
}

export interface HistoryEvent {
  type: string;
  timestamp: string;
  details: string;
  actorName: string;
}

export interface TraceCodeHistory {
  codeValue: string;
  status: TraceCodeStatus;
  shipmentId: string;
  shipmentName: string;
  scanCount: number;
  createdAt: string;
  events: HistoryEvent[];
}

export interface GetShipmentTraceCodesParams {
  status?: string;
  search?: string;
  page?: number;
  size?: number;
}

export interface ExportTraceCodesPayload {
  status?: string;
  search?: string;
}
