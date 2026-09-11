export type HandoverStatus =
  | 'PENDING'
  | 'PENDING_CONFIRMATION'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'EXPIRED'
  | 'CANCELLED';

export interface HandoverSummary {
  id: string;
  shipmentId: string;
  shipmentName: string;
  fromOrganizationName: string;
  toOrganizationName: string;
  quantity: number;
  unit: string;
  status: HandoverStatus;
  createdAt: string;
  confirmedAt?: string;
  rejectionReason?: string;
}

export interface HandoverListParams {
  status?: string;
  search?: string;
  page?: number;
  size?: number;
}
