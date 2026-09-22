/**
 * Kết quả kiểm tra tính hợp lệ của lô trước khi ghi nhận sự kiện chuỗi.
 */
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

/**
 * Bản ghi nhật ký các lần ghi nhận sự kiện không hợp lệ / thất bại.
 */
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