export type CodeRangeSupplementStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface SupplementUserInfo {
  userId: string;
  fullName: string;
}

export interface CodeRangeSupplementRequest {
  id: string;
  organizationId: string;
  organizationName: string;
  requestedBy: SupplementUserInfo | null;
  requestedAt: string;
  requestedQuantity: number;
  approvedQuantity: number | null;
  status: CodeRangeSupplementStatus;
  reason: string;
  evidenceEventIds: string[];
  approvedBy: SupplementUserInfo | null;
  approvedAt: string | null;
  approvalRemarks: string | null;
  rejectedBy: SupplementUserInfo | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  notifiedCount?: number;
}

export interface CreateSupplementRequestPayload {
  requestedQuantity: number;
  reason: string;
  evidenceEventIds: string[];
}

export interface ApproveSupplementRequestPayload {
  approvedQuantity: number;
  remarks?: string;
}

export interface RejectSupplementRequestPayload {
  rejectionReason: string;
}

export interface SupplementRequestListParams {
  status?: CodeRangeSupplementStatus;
  page?: number;
  size?: number;
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
