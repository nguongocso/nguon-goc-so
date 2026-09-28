// NCL-08-CN-011, NCL-08-CN-012 - Thu hồi theo phạm vi ảnh hưởng & Kết thúc vụ việc

export type BulkRecallRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';

export type LotResolution = 'DESTROYED' | 'RETURNED' | 'REPROCESSED' | 'UNRECOVERABLE';

export interface BulkRecallUserInfo {
  userId: string;
  fullName: string;
}

export interface BulkRecallShipmentItem {
  id: string;
  shipmentId: string;
  shipmentCode: string;
  shipmentName: string;
  shipmentStatus: string;
  included: boolean;
  exclusionReason: string | null;
  unit?: string | null;
  totalQuantity?: number | null;
  resolution?: LotResolution | null;
  recoveredQuantity?: number | null;
  notes?: string | null;
}

export interface RecallEvidenceFile {
  id: string;
  fileName: string;
  fileSize: number;
  contentType: string;
  downloadUrl?: string;
  uploadedAt: string;
}

export interface BulkRecallRequest {
  id: string;
  productionLotId: string;
  productionLotName: string;
  reason: string;
  evidence: string | null;
  status: BulkRecallRequestStatus;
  requestedBy: BulkRecallUserInfo | null;
  requestedAt: string;
  approvedBy: BulkRecallUserInfo | null;
  approvedAt: string | null;
  approvalRemarks: string | null;
  rejectedBy: BulkRecallUserInfo | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  closedBy?: BulkRecallUserInfo | null;
  closedAt?: string | null;
  remediationMeasures?: string | null;
  evidenceFileIds?: string[] | null;
  evidenceFiles?: RecallEvidenceFile[] | null;
  caseCode?: string | null;
  shipments: BulkRecallShipmentItem[];
  createdAt: string;
  updatedAt: string;
}

export interface ExcludedShipment {
  shipmentId: string;
  exclusionReason: string;
}

export interface CreateBulkRecallRequestPayload {
  productionLotId: string;
  reason: string;
  evidence?: string;
  includedShipmentIds: string[];
  excludedShipments?: ExcludedShipment[];
}

export interface ApproveBulkRecallRequestPayload {
  remarks?: string;
}

export interface RejectBulkRecallRequestPayload {
  reason: string;
}

export interface BulkRecallRequestListParams {
  status?: BulkRecallRequestStatus;
  page?: number;
  size?: number;
}

export interface CloseBulkRecallLotResultPayload {
  shipmentId: string;
  resolution: LotResolution;
  recoveredQuantity: number;
  notes?: string;
}

export interface CloseBulkRecallRequestPayload {
  remediationMeasures: string;
  evidenceFileIds?: string[];
  lotResults: CloseBulkRecallLotResultPayload[];
}
