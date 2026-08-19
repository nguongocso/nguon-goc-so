export interface EventVerificationItem {
  index: number;
  eventId: string;
  eventType: string;
  recordedAt: string;
  hash?: string;
  previousHash?: string;
  isValid: boolean;
  expectedHash?: string;
}

export interface ChainVerificationResponse {
  shipmentId: string;
  shipmentName: string;
  totalEvents: number;
  isIntegrityVerified: boolean;
  verificationStatus: 'INTACT' | 'BROKEN';
  failedEventIndex: number | null;
  failedEventId: string | null;
  failureReason: string | null;
  verifiedAt: string;
  hashAlgorithm: string;
  events: EventVerificationItem[];
}