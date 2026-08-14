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
  isMandatory: boolean;
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
 * Backend map PENDING_RESULT -> PENDING.
 */
export type InspectionRequestStatusDisplay =
  | 'PENDING'
  | 'PASSED'
  | 'FAILED'
  | 'CANCELLED';

/**
 * Trạng thái yêu cầu dùng để query backend.
 * Backend nhận enum PENDING_RESULT, không nhận PENDING.
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
}