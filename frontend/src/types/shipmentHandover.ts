export interface CreateHandoverPayload {
  shipmentId: string;
  toOrganizationId: string;
  quantity: number;
  plannedAt?: string;
  vehicleInfo?: string;
  carrierName?: string;
  note?: string;
  attachmentPath?: string;
}

export interface ShipmentHandover {
  id: string;
  shipmentId: string;
  fromOrganizationId: string;
  toOrganizationId: string;
  quantity: number;
  status: 'PENDING_CONFIRMATION' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED';
  plannedAt?: string;
  vehicleInfo?: string;
  carrierName?: string;
  note?: string;
  attachmentPath?: string;
  expiresAt: string;
  createdAt: string;
  confirmedAt?: string;
  rejectedAt?: string;
  cancelReason?: string;
  cancelledAt?: string;
}

export interface HandoverDetailResponse extends ShipmentHandover {
  shipmentName: string;
  fromOrganizationName: string;
  toOrganizationName: string;
}
