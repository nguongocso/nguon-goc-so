export interface RecallRequest {
  reason: string;
}

export interface RecallResponse {
  id: string;
  shipmentId: string;
  reason: string;
  recalledBy: string;
  recalledAt: string;
  status: string;
  shipmentStatus: string;
  traceCodesUpdated: number;
  auditLogId: string;
}

export interface RecallInfoResponse {
  shipmentId: string;
  recalled: boolean;
  reason: string;
  recalledAt: string;
}