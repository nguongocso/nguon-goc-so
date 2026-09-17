import apiClient from './axiosConfig';

interface ApiResponse<T> {
  success?: boolean;
  status?: number;
  message?: string;
  data: T;
}
import type {
  InspectionResultEntryLinkResponse,
  IssueInspectionResultEntryLinkRequest,
  PublicInspectionResultEntryData,
  RecordInspectionResultsPayload,
} from '@/types/inspectionResultPortal';
import type { InspectionCriterionResult } from '@/types/certification';

/**
 * Cấp liên kết nhập kết quả kiểm nghiệm cho đơn vị kiểm nghiệm (vai trò VT-02).
 */
export const issueInspectionResultEntryLink = async (
  requestId: string,
  data: IssueInspectionResultEntryLinkRequest
): Promise<InspectionResultEntryLinkResponse> => {
  const response = await apiClient.post<ApiResponse<InspectionResultEntryLinkResponse>>(
    `/inspection-requests/${requestId}/result-entry-links`,
    data
  );
  return response.data.data;
};

/**
 * Lấy thông tin liên kết gần nhất đã cấp cho yêu cầu kiểm nghiệm.
 */
export const getLatestInspectionResultEntryLink = async (
  requestId: string
): Promise<InspectionResultEntryLinkResponse> => {
  const response = await apiClient.get<ApiResponse<InspectionResultEntryLinkResponse>>(
    `/inspection-requests/${requestId}/result-entry-links/latest`
  );
  return response.data.data;
};

/**
 * Lấy dữ liệu công khai của cổng nhập kết quả theo mã token bí mật.
 */
export const getPublicPortalData = async (
  token: string
): Promise<PublicInspectionResultEntryData> => {
  const response = await apiClient.get<ApiResponse<PublicInspectionResultEntryData>>(
    `/public/inspection-result-entry/${token}`
  );
  return response.data.data;
};

/**
 * Tải lên phiếu kết quả kiểm nghiệm cho một chỉ tiêu trên cổng công khai.
 */
export const uploadPortalResultFile = async (
  token: string,
  criterionId: string,
  file: File
): Promise<string> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await apiClient.post<ApiResponse<{ filePath: string }>>(
    `/public/inspection-result-entry/${token}/criteria/${criterionId}/file`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data.data.filePath;
};

/**
 * Gửi toàn bộ kết quả kiểm nghiệm từ cổng công khai (dùng một lần, atomic consume).
 */
export const submitPortalResults = async (
  token: string,
  data: RecordInspectionResultsPayload
): Promise<InspectionCriterionResult[]> => {
  const response = await apiClient.put<ApiResponse<InspectionCriterionResult[]>>(
    `/public/inspection-result-entry/${token}/results`,
    data
  );
  return response.data.data;
};
