import apiClient from './axiosConfig';
import type { ProductionLotCertification, AttachCertificationRequest, Certification, CreateCertificationRequest, CertificationResponse, LotTestCriteriaResult, CreateInspectionRequestPayload, InspectionRequestCreatedResponse, InspectionRequestListItem, InspectionRequestStatusQuery, InspectionRequestDetailResponse, InspectionCriterionResult, RecordCriterionResultPayload, RecordInspectionResultsPayload, InspectionResultFileUploadResponse, CanActivateSealCheck } from '@/types/certification';
import type { PageResponse } from '@/types/common';

/**
 * Lấy danh sách chứng nhận đã gắn của một lô sản xuất
 */
export const getLotCertifications = async (lotId: string): Promise<ProductionLotCertification[]> => {
  const response = await apiClient.get<{ data: ProductionLotCertification[] }>(
    `/production-lots/${lotId}/certifications`
  );
  return response.data.data;
};

/**
 * Gắn chứng nhận cho lô sản xuất
 */
export const attachCertification = async (
  lotId: string,
  payload: AttachCertificationRequest
): Promise<ProductionLotCertification> => {
  const response = await apiClient.post<{ data: ProductionLotCertification }>(
    `/production-lots/${lotId}/certifications`,
    payload
  );
  return response.data.data;
};

/**
 * Gỡ chứng nhận khỏi lô sản xuất
 */
export const detachCertification = async (lotId: string, certificationId: string): Promise<void> => {
  await apiClient.delete(`/production-lots/${lotId}/certifications/${certificationId}`);
};

/**
 * Lấy danh sách chứng nhận còn hiệu lực của tổ chức hiện tại
 * GET /api/v1/certifications/valid
 */
export const getValidCertifications = async (): Promise<Certification[]> => {
  const response = await apiClient.get<{ data: Certification[] }>('/certifications/valid');
  return response.data.data;
};

/**
 * Tạo mới chứng nhận cho tổ chức
 * POST /api/v1/certifications
 */
export const createCertification = async (
  data: CreateCertificationRequest
): Promise<CertificationResponse> => {
  const response = await apiClient.post<{ data: CertificationResponse }>(
    '/certifications',
    data
  );
  return response.data.data;
};

/**
 * Lấy danh sách tất cả chứng nhận của tổ chức
 * GET /api/v1/certifications
 */
export const getAllCertifications = async (): Promise<CertificationResponse[]> => {
  const response = await apiClient.get<{ data: CertificationResponse[] }>(
    '/certifications'
  );
  return response.data.data;
};

/**
 * Lấy chỉ tiêu kiểm nghiệm áp dụng cho lô
 * GET /api/v1/production-lots/{lotId}/test-criteria
 */
export const getLotTestCriteria = async (lotId: string): Promise<LotTestCriteriaResult> => {
  const response = await apiClient.get<{ data: LotTestCriteriaResult }>(
    `/production-lots/${lotId}/test-criteria`
  );
  return response.data.data;
};

/**
 * Tạo yêu cầu kiểm nghiệm cho lô
 * POST /api/v1/production-lots/{lotId}/test-requests
 *
 * Trả HTTP 201 khi thành công.
 * Trả HTTP 409 khi trùng yêu cầu đang chờ kết quả và confirmDuplicate=false.
 */
export const createInspectionRequest = async (
  lotId: string,
  payload: CreateInspectionRequestPayload
): Promise<InspectionRequestCreatedResponse> => {
  const response = await apiClient.post<{ data: InspectionRequestCreatedResponse }>(
    `/production-lots/${lotId}/test-requests`,
    payload
  );
  return response.data.data;
};

export interface GetInspectionRequestsParams {
  lotId: string;
  status?: InspectionRequestStatusQuery;
  page?: number;
  size?: number;
}

/**
 * Lấy danh sách yêu cầu kiểm nghiệm theo lô.
 * GET /api/v1/test-requests?lotId=...&status=...&page=...&size=...
 *
 * Luôn truyền lotId từ frontend; backend scope theo organization hiện tại.
 */
export const getInspectionRequests = async (
  params: GetInspectionRequestsParams
): Promise<PageResponse<InspectionRequestListItem>> => {
  const searchParams = new URLSearchParams({ lotId: params.lotId });
  if (params.status) {
    searchParams.set('status', params.status);
  }
  if (params.page !== undefined) {
    searchParams.set('page', String(params.page));
  }
  if (params.size !== undefined) {
    searchParams.set('size', String(params.size));
  }
  const response = await apiClient.get<{ data: PageResponse<InspectionRequestListItem> }>(
    `/test-requests?${searchParams.toString()}`
  );
  return response.data.data;
};

/**
 * Lấy chi tiết yêu cầu kiểm nghiệm để nhập kết quả.
 * GET /api/v1/inspection-requests/{requestId}
 *
 * Trả về danh sách chỉ tiêu (UUID snapshot) kèm kết quả đã ghi (nếu có).
 */
export const getInspectionRequestDetail = async (
  requestId: string
): Promise<InspectionRequestDetailResponse> => {
  const response = await apiClient.get<{ data: InspectionRequestDetailResponse }>(
    `/inspection-requests/${requestId}`
  );
  return response.data.data;
};

/**
 * Lấy danh sách kết quả kiểm nghiệm của các chỉ tiêu thuộc một yêu cầu.
 * GET /api/v1/inspection-requests/{requestId}/results
 */
export const getInspectionRequestResults = async (
  requestId: string
): Promise<InspectionCriterionResult[]> => {
  const response = await apiClient.get<{ data: InspectionCriterionResult[] }>(
    `/inspection-requests/${requestId}/results`
  );
  return response.data.data;
};

/**
 * Ghi nhận toàn bộ kết quả kiểm nghiệm của một yêu cầu trong một lần gọi.
 * PUT /api/v1/inspection-requests/{requestId}/results
 *
 * Payload phải chứa kết quả cho TẤT CẢ chỉ tiêu của yêu cầu.
 * Backend validate toàn bộ rồi lưu trong một giao dịch (all-or-nothing):
 * nếu có chỉ tiêu không hợp lệ, không chỉ tiêu nào được lưu.
 */
export const recordInspectionRequestResults = async (
  requestId: string,
  payload: RecordInspectionResultsPayload
): Promise<InspectionCriterionResult[]> => {
  const response = await apiClient.put<{ data: InspectionCriterionResult[] }>(
    `/inspection-requests/${requestId}/results`,
    payload
  );
  return response.data.data;
};

/**
 * Ghi nhận / cập nhật kết quả kiểm nghiệm cho một chỉ tiêu.
 * POST /api/v1/inspection-criteria/{criterionId}/results
 *
 * criterionId là UUID snapshot của chỉ tiêu thuộc yêu cầu (inspection_criteria.id).
 * Trả HTTP 201 khi thành công.
 */
export const recordOrUpdateCriterionResult = async (
  criterionId: string,
  payload: RecordCriterionResultPayload
): Promise<InspectionCriterionResult> => {
  const response = await apiClient.post<{ data: InspectionCriterionResult }>(
    `/inspection-criteria/${criterionId}/results`,
    payload
  );
  return response.data.data;
};

/**
 * Tải lên phiếu kết quả kiểm nghiệm cho một chỉ tiêu.
 * POST /api/v1/inspection-criteria/{criterionId}/result-file
 *
 * Trả về filePath để gửi kèm khi ghi nhận kết quả.
 */
export const uploadInspectionResultFile = async (
  criterionId: string,
  file: File
): Promise<InspectionResultFileUploadResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post<{
    data: InspectionResultFileUploadResponse;
  }>(`/inspection-criteria/${criterionId}/result-file`, formData);
  return response.data.data;
};

/**
 * Lấy kết quả kiểm nghiệm của một chỉ tiêu.
 * GET /api/v1/inspection-criteria/{criterionId}/result
 */
export const getCriterionResult = async (
  criterionId: string
): Promise<InspectionCriterionResult> => {
  const response = await apiClient.get<{ data: InspectionCriterionResult }>(
    `/inspection-criteria/${criterionId}/result`
  );
  return response.data.data;
};

/**
 * Xóa kết quả kiểm nghiệm.
 * DELETE /api/v1/inspection-results/{resultId}
 */
export const deleteInspectionResult = async (resultId: string): Promise<void> => {
  await apiClient.delete(`/inspection-results/${resultId}`);
};

/**
 * Kiểm tra lô sản xuất có đủ điều kiện kích hoạt tem dựa trên kết quả kiểm nghiệm.
 * POST /api/v1/production-lots/{lotId}/can-activate-seal
 */
export const checkCanActivateSeal = async (
  lotId: string
): Promise<CanActivateSealCheck> => {
  const response = await apiClient.post<{ data: CanActivateSealCheck }>(
    `/production-lots/${lotId}/can-activate-seal`
  );
  return response.data.data;
};