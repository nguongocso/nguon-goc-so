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

export const getLatestInspectionResultEntryLink = async (
  requestId: string
): Promise<InspectionResultEntryLinkResponse> => {
  const response = await apiClient.get<ApiResponse<InspectionResultEntryLinkResponse>>(
    `/inspection-requests/${requestId}/result-entry-links/latest`
  );
  return response.data.data;
};

export const getPublicPortalData = async (
  token: string
): Promise<PublicInspectionResultEntryData> => {
  const response = await apiClient.get<ApiResponse<PublicInspectionResultEntryData>>(
    `/public/inspection-result-entry/${token}`
  );
  return response.data.data;
};

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
