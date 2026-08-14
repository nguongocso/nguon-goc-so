export interface LotValidationResponse {
  lotId: string;
  eventType: string;
  valid: boolean;
  message: string;
  details: {
    lotType: 'PRODUCTION_LOT' | 'SHIPMENT';
    currentStatus: string;
    organizationId: string;
  };
}

export interface FailedEventLog {
  id: string;
  userId: string;
  userFullName: string;
  eventType: string;
  lotId: string;
  lotCode: string;
  failureReason: string;
  attemptedAt: string;
}