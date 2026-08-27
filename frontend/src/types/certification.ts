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
 * Đơn vị kiểm nghiệm trong danh mục dùng chung (NCL-11-CN-006 Phase 1).
 * GET /api/v1/testing-units
 */
export interface TestingUnit {
  id: string;
  name: string;
  accreditationCode: string;
  contactInfo: string | null;
  /** YYYY-MM-DD, null nếu không có ngày hết hạn. */
  accreditationExpiryDate: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string | null;
}

export interface CreateTestingUnitRequest {
  name: string;
  accreditationCode: string;
  contactInfo?: string | null;
  accreditationExpiryDate?: string | null;
  isActive?: boolean;
}

export interface UpdateTestingUnitRequest {
  name: string;
  accreditationCode: string;
  contactInfo?: string | null;
  accreditationExpiryDate?: string | null;
  isActive?: boolean;
}

/**
 * Một dòng phạm vi công nhận của đơn vị kiểm nghiệm
 * (NCL-11-CN-006 Phase 2).
 */
export interface AccreditationScope {
  id: string;
  testingUnitId: string;
  testingUnitName: string;
  /** Id chỉ tiêu trong danh mục dùng chung (inspection_criterion_catalog.id). */
  criterionDefinitionId: number;
  criterionCode: string;
  criterionName: string;
  createdAt: string;
}

/**
 * Tóm tắt phạm vi công nhận của một đơn vị kiểm nghiệm.
 * GET /api/v1/testing-units/{unitId}/accreditation-scopes
 */
export interface AccreditationScopeSummary {
  testingUnitId: string;
  testingUnitName: string;
  accreditedCriteria: {
    id: number;
    code: string;
    name: string;
  }[];
}

/** Payload cập nhật phạm vi công nhận (REPLACE-ALL). */
export interface UpdateAccreditationScopeRequest {
  criterionDefinitionIds: number[];
}

/**
 * Payload tạo yêu cầu kiểm nghiệm.
 * POST /api/v1/production-lots/{lotId}/test-requests
 *
 * testingUnitId ưu tiên khi có (chọn từ danh mục đơn vị kiểm nghiệm).
 * testingUnit giữ lại để tương thích ngược (nhập tự do khi không có danh mục).
 */
export interface CreateInspectionRequestPayload {
  testingUnitId?: string | null;
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
  /** Ngày cấp kết quả; null khi chỉ tiêu Không đạt (kết quả không có hiệu lực). */
  resultDate: string | null; // YYYY-MM-DD
  /** Ngày hết hiệu lực; null khi chỉ tiêu Không đạt. */
  expiryDate: string | null; // YYYY-MM-DD
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
  /** Bắt buộc khi passed = true; được phép null khi passed = false. */
  resultDate: string | null; // YYYY-MM-DD
  /** Bắt buộc khi passed = true; được phép null khi passed = false. */
  expiryDate: string | null; // YYYY-MM-DD
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