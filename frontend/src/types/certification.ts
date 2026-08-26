export interface Certification {
  id: string;
  name: string;
  code: string;
  issuedBy: string;
  issueDate: string;  // YYYY-MM-DD
  expiryDate: string; // YYYY-MM-DD
  isValid: boolean;
}

export interface ProductionLotCertification {
  id: string;
  certificationId: string;
  certificationName: string;
  certificationCode: string;
  issuedBy: string;
  issueDate: string;
  expiryDate: string;
  isValid: boolean;
  attachedAt: string; // ISO datetime
  attachedBy: string;
  note: string | null;
}

export interface AttachCertificationRequest {
  certificationId: string;
  note?: string;
}

export interface CreateCertificationRequest {
  standardId: string;
  code: string;
  issuedBy?: string;
  issueDate: string;   // YYYY-MM-DD
  expiryDate: string;  // YYYY-MM-DD
}

// CertificationResponse đã có (hoặc thêm nếu chưa có)
export interface CertificationResponse {
  id: string;
  name: string;        // tên standard
  code: string;
  issuedBy: string;
  issueDate: string;
  expiryDate: string;
  isValid: boolean;
}

// ============================================================
// Kiểm nghiệm (Inspection Request)
// ============================================================

/**
 * Một chỉ tiêu kiểm nghiệm của lô.
 * GET /api/v1/production-lots/{lotId}/test-criteria
 */
export interface TestCriterionItem {
  criteriaId: number;
  code: string;
  name: string;
}

/**
 * Kết quả lấy chỉ tiêu kiểm nghiệm của một lô.
 */
export interface LotTestCriteriaResult {
  lotId: string;
  standardId: string | null;
  standardName: string | null;
  criteria: TestCriterionItem[];
}

/**
 * Payload tạo yêu cầu kiểm nghiệm.
 * POST /api/v1/production-lots/{lotId}/test-requests
 */
export interface CreateInspectionRequestPayload {
  testingUnit: string;
  sampleSentDate: string; // YYYY-MM-DD
  criteriaIds: number[];
  confirmDuplicate: boolean;
}

/**
 * Response tạo yêu cầu kiểm nghiệm.
 */
export interface InspectionRequestCreatedResponse {
  testRequestId: string;
  lotId: string;
  lotCode: string;
  status: string;
  testingUnit: string;
  sampleSentDate: string;
  criteria: InspectionRequestCriterionResponse[];
  createdBy: string;
  createdAt: string;
}

export interface InspectionRequestCriterionResponse {
  criteriaId: number | null;
  code: string;
  name: string;
  standardId: string | null;
  standardName: string | null;
}

/**
 * Trạng thái yêu cầu trong response từ backend.
 * Backend map PENDING_RESULT -> PENDING; khi tất cả chỉ tiêu đạt -> PASSED,
 * có chỉ tiêu không đạt -> FAILED.
 */
export type InspectionRequestStatusDisplay =
  | 'PENDING'
  | 'PASSED'
  | 'FAILED'
  | 'CANCELLED';

/**
 * Trạng thái yêu cầu dùng để query backend.
 * Backend enum: PENDING_RESULT / PASSED / FAILED / CANCELLED (không có RESULTED).
 */
export type InspectionRequestStatusQuery =
  | 'PENDING_RESULT'
  | 'PASSED'
  | 'FAILED'
  | 'CANCELLED';

/**
 * Một yêu cầu kiểm nghiệm trong danh sách.
 * GET /api/v1/test-requests?lotId=...
 */
export interface InspectionRequestListItem {
  testRequestId: string;
  lotCode: string;
  status: InspectionRequestStatusDisplay;
  testingUnit: string;
  sampleSentDate: string;
  criteriaCount: number;
  /** Số chỉ tiêu không đạt (passed = false); không tính chưa có kết quả/hết hạn. */
  failedCriteriaCount: number;
  /** Tỷ lệ chỉ tiêu không đạt trên tổng số chỉ tiêu (%), 1 chữ số thập phân. */
  failedRatio: number;
}

/**
 * Chi tiết yêu cầu kiểm nghiệm để nhập kết quả.
 * GET /api/v1/inspection-requests/{requestId}
 */
export interface InspectionRequestDetailResponse {
  testRequestId: string;
  lotId: string;
  lotCode: string;
  status: InspectionRequestStatusDisplay;
  testingUnit: string;
  sampleSentDate: string;
  /** Tổng số chỉ tiêu kiểm nghiệm của yêu cầu. */
  totalCriteria: number;
  /** Số chỉ tiêu đã có kết quả kiểm nghiệm được ghi nhận. */
  evaluatedCriteria: number;
  /** Số chỉ tiêu đạt (passed = true). */
  passedCriteria: number;
  /** Số chỉ tiêu không đạt (passed = false). */
  failedCriteriaCount: number;
  /** Tỷ lệ chỉ tiêu không đạt trên tổng số chỉ tiêu (%), 1 chữ số thập phân. */
  failedRatio: number;
  criteria: InspectionRequestDetailCriterion[];
}

/**
 * Một chỉ tiêu kiểm nghiệm trong chi tiết yêu cầu.
 * criterionId là UUID snapshot của chỉ tiêu thuộc yêu cầu
 * (inspection_criteria.id), dùng để gọi
 * POST /api/v1/inspection-criteria/{criterionId}/results.
 */
export interface InspectionRequestDetailCriterion {
  criterionId: string;
  code: string;
  name: string;
  standardName: string | null;
  /** Kết quả đã ghi cho chỉ tiêu (null nếu chưa có). */
  result: InspectionCriterionResult | null;
}

// ============================================================
// Kết quả kiểm nghiệm theo chỉ tiêu (InspectionCriterionResultController)
// ============================================================

/**
 * Kết quả kiểm nghiệm của một chỉ tiêu.
 * GET /api/v1/inspection-requests/{requestId}/results
 * POST /api/v1/inspection-criteria/{criterionId}/results
 */
export interface InspectionCriterionResult {
  resultId: string;
  /** UUID snapshot của chỉ tiêu thuộc yêu cầu (inspection_criteria.id). */
  criterionId: string;
  criterionCode: string;
  criterionName: string;
  resultDate: string; // YYYY-MM-DD
  expiryDate: string; // YYYY-MM-DD
  /** true = đạt, false = không đạt. */
  passed: boolean;
  filePath: string | null;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Payload ghi nhận kết quả kiểm nghiệm cho một chỉ tiêu.
 * POST /api/v1/inspection-criteria/{criterionId}/results
 */
export interface RecordCriterionResultPayload {
  criterionId: string;
  resultDate: string; // YYYY-MM-DD
  expiryDate: string; // YYYY-MM-DD
  passed: boolean;
  filePath?: string | null;
}

/**
 * Payload ghi nhận toàn bộ kết quả kiểm nghiệm của một yêu cầu.
 * PUT /api/v1/inspection-requests/{requestId}/results
 *
 * Phải chứa kết quả cho tất cả chỉ tiêu của yêu cầu; backend lưu
 * trong một giao dịch (all-or-nothing).
 */
export interface RecordInspectionResultsPayload {
  results: RecordCriterionResultPayload[];
}

/**
 * Phản hồi tải lên phiếu kết quả kiểm nghiệm.
 * POST /api/v1/inspection-criteria/{criterionId}/result-file
 */
export interface InspectionResultFileUploadResponse {
  filePath: string;
}

/**
 * Kết quả kiểm tra điều kiện kích hoạt tem của lô.
 * POST /api/v1/production-lots/{lotId}/can-activate-seal
 */
export interface CanActivateSealCheck {
  productionLotId: string;
  canActivate: boolean;
  reason: string | null;
  earliestExpiryDate: string | null; // YYYY-MM-DD
  totalCriteria: number;
  passedCriteria: number;
  failedOrExpiredCriteria: number;
}