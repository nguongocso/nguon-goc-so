import apiClient from './axiosConfig';
import type { ProductionLotCertification, AttachCertificationRequest, Certification, CreateCertificationRequest, CertificationResponse, LotTestCriteriaResult, CreateInspectionRequestPayload, InspectionRequestCreatedResponse, InspectionRequestListItem, InspectionRequestStatusQuery } from '@/types/certification';
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
 * Lưu ý: luôn truyền lotId từ frontend vì backend chưa giới hạn
 * organization khi thiếu lotId.
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