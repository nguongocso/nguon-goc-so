/**
 * Định nghĩa kiểu dữ liệu cho Cổng nhập kết quả kiểm nghiệm (NCL-11-CN-007).
 */

export type InspectionResultEntryLinkStatus = 'ACTIVE' | 'USED' | 'REVOKED' | 'EXPIRED';

export type InspectionResultEntrySource = 'TESTING_UNIT_PORTAL' | 'COOPERATIVE_MANUAL';

/**
 * Thông tin liên kết nhập kết quả nhận được từ backend.
 */
export interface InspectionResultEntryLinkResponse {
  id: string;
  status: InspectionResultEntryLinkStatus;
  recipientEmail: string;
  tokenPrefix?: string;
  expiresAt: string;
  usedAt?: string;
  createdAt: string;
  entryUrl?: string;
}

/**
 * Yêu cầu cấp liên kết nhập kết quả cho đơn vị kiểm nghiệm.
 */
export interface IssueInspectionResultEntryLinkRequest {
  recipientEmail: string;
  expiryDays?: number;
}

/**
 * Chỉ tiêu kiểm nghiệm hiển thị trên cổng công khai.
 */
export interface PublicInspectionResultEntryCriterion {
  criterionId: string;
  code: string;
  name: string;
  standardName?: string;
}

/**
 * Dữ liệu cổng công khai tải về theo mã token.
 */
export interface PublicInspectionResultEntryData {
  testingUnitName?: string;
  testingUnit?: string;
  lotCode: string;
  lotName?: string;
  sampleSentDate?: string;
  expiresAt: string;
  criteria: PublicInspectionResultEntryCriterion[];
}

/**
 * Dữ liệu kết quả nhập cho từng chỉ tiêu.
 */
export interface InspectionCriterionResultItemInput {
  criterionId: string;
  passed: boolean;
  resultDate?: string;
  expiryDate?: string;
  filePath?: string;
}

/**
 * Payload nộp toàn bộ kết quả kiểm nghiệm.
 */
export interface RecordInspectionResultsPayload {
  results: InspectionCriterionResultItemInput[];
}
