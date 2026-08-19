export type RecallRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface RecallUserInfo {
  userId: string;
  fullName: string;
}

export interface RecallRequest {
  id: string;
  lotId: string;
  lotName: string;
  requestedBy: RecallUserInfo | null;
  requestedAt: string;
  status: RecallRequestStatus;
  reason: string;
  evidence: string | null;
  approvedBy: RecallUserInfo | null;
  approvedAt: string | null;
  approvalRemarks: string | null;
  rejectedBy: RecallUserInfo | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  notifiedBuyerCount?: number;
}

export interface CreateRecallRequestPayload {
  lotId: string;
  reason: string;
  evidence?: string;
}

export interface ApproveRecallRequestPayload {
  remarks?: string;
}

export interface RejectRecallRequestPayload {
  rejectionReason: string;
}

export interface RecallRequestListParams {
  status?: RecallRequestStatus;
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