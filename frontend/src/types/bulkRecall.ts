// NCL-08-CN-011 - Thu hồi theo phạm vi ảnh hưởng

export type BulkRecallRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

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
