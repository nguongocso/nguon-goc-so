/** Chi tiết trạng thái xác minh băm của từng mắt xích sự kiện trong chuỗi. */
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

/** Kết quả xác minh tính toàn vẹn chuỗi sự kiện truy xuất nguồn gốc (Merkle / Hash Chain). */
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