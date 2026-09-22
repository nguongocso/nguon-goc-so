import apiClient from './axiosConfig';
import type { ProductionLotCertification, AttachCertificationRequest, Certification, CreateCertificationRequest, CertificationResponse, LotTestCriteriaResult, CreateInspectionRequestPayload, InspectionRequestCreatedResponse, InspectionRequestListItem, InspectionRequestStatusQuery, InspectionRequestDetailResponse, InspectionCriterionResult, RecordCriterionResultPayload, RecordInspectionResultsPayload, InspectionResultFileUploadResponse, CanActivateSealCheck, TestingUnit, CreateTestingUnitRequest, UpdateTestingUnitRequest, AccreditationScopeSummary, UpdateAccreditationScopeRequest } from '@/types/certification';
import type { PageResponse } from '@/types/common';

export const getAccreditationScopes = async (
  unitId: string
): Promise<AccreditationScopeSummary> => {
  const response = await apiClient.get<{ data: AccreditationScopeSummary }>(
    `/testing-units/${unitId}/accreditation-scopes`
  );
  return response.data.data;
};

export const updateAccreditationScopes = async (
  unitId: string,
  payload: UpdateAccreditationScopeRequest
): Promise<AccreditationScopeSummary> => {
  const response = await apiClient.put<{ data: AccreditationScopeSummary }>(
    `/testing-units/${unitId}/accreditation-scopes`,
    payload
  );
  return response.data.data;
};

export const getTestingUnits = async (params?: {
  isActive?: boolean;
  page?: number;
  size?: number;
}): Promise<PageResponse<TestingUnit>> => {
  const searchParams = new URLSearchParams();
  if (params?.isActive !== undefined) {
    searchParams.set('isActive', String(params.isActive));
  }
  searchParams.set('page', String(params?.page ?? 0));
  searchParams.set('size', String(params?.size ?? 200));
  const response = await apiClient.get<{ data: PageResponse<TestingUnit> }>(
    `/testing-units?${searchParams.toString()}`
  );
  return response.data.data;
};

export const createTestingUnit = async (
  data: CreateTestingUnitRequest
): Promise<TestingUnit> => {
  const response = await apiClient.post<{ data: TestingUnit }>(
    '/testing-units',
    data
  );
  return response.data.data;
};

export const updateTestingUnit = async (
  testingUnitId: string,
  data: UpdateTestingUnitRequest
): Promise<TestingUnit> => {
  const response = await apiClient.put<{ data: TestingUnit }>(
    `/testing-units/${testingUnitId}`,
    data
  );
  return response.data.data;
};

export const deactivateTestingUnit = async (
  testingUnitId: string
): Promise<void> => {
  await apiClient.delete(`/testing-units/${testingUnitId}`);
};

export const getLotCertifications = async (lotId: string): Promise<ProductionLotCertification[]> => {
  const response = await apiClient.get<{ data: ProductionLotCertification[] }>(
    `/production-lots/${lotId}/certifications`
  );
  return response.data.data;
};

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

export const detachCertification = async (lotId: string, certificationId: string): Promise<void> => {
  await apiClient.delete(`/production-lots/${lotId}/certifications/${certificationId}`);
};

export const getValidCertifications = async (): Promise<Certification[]> => {
  const response = await apiClient.get<{ data: Certification[] }>('/certifications/valid');
  return response.data.data;
};

export const createCertification = async (
  data: CreateCertificationRequest,
  file: File
): Promise<CertificationResponse> => {
  const formData = new FormData();
  formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
  formData.append('file', file);
  const response = await apiClient.post<{ data: CertificationResponse }>(
    '/certifications',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data.data;
};

export interface GetCertificationsParams {
  keyword?: string;
  status?: 'valid' | 'expiring' | 'expired';
  sortBy?: 'name' | 'issueDate' | 'expiryDate';
  sortDir?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

export const getCertifications = async (
  params: GetCertificationsParams
): Promise<PageResponse<CertificationResponse>> => {
  const searchParams = new URLSearchParams();
  if (params.keyword?.trim()) {
    searchParams.set('keyword', params.keyword.trim());
  }
  if (params.status) {
    searchParams.set('status', params.status);
  }
  if (params.sortBy) {
    searchParams.set('sortBy', params.sortBy);
  }
  if (params.sortDir) {
    searchParams.set('sortDir', params.sortDir);
  }
  if (params.page !== undefined) {
    searchParams.set('page', String(params.page));
  }
  if (params.size !== undefined) {
    searchParams.set('size', String(params.size));
  }
  const response = await apiClient.get<{ data: PageResponse<CertificationResponse> }>(
    `/certifications?${searchParams.toString()}`
  );
  return response.data.data;
};

export const getLotTestCriteria = async (lotId: string): Promise<LotTestCriteriaResult> => {
  const response = await apiClient.get<{ data: LotTestCriteriaResult }>(
    `/production-lots/${lotId}/test-criteria`
  );
  return response.data.data;
};

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

export const getInspectionRequestDetail = async (
  requestId: string
): Promise<InspectionRequestDetailResponse> => {
  const response = await apiClient.get<{ data: InspectionRequestDetailResponse }>(
    `/inspection-requests/${requestId}`
  );
  return response.data.data;
};

export const getInspectionRequestResults = async (
  requestId: string
): Promise<InspectionCriterionResult[]> => {
  const response = await apiClient.get<{ data: InspectionCriterionResult[] }>(
    `/inspection-requests/${requestId}/results`
  );
  return response.data.data;
};

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

export const uploadInspectionResultFile = async (
  criterionId: string,
  file: File
): Promise<InspectionResultFileUploadResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post<{
    data: InspectionResultFileUploadResponse;
  }>(
    `/inspection-criteria/${criterionId}/result-file`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data.data;
};

export const getCriterionResult = async (
  criterionId: string
): Promise<InspectionCriterionResult> => {
  const response = await apiClient.get<{ data: InspectionCriterionResult }>(
    `/inspection-criteria/${criterionId}/result`
  );
  return response.data.data;
};

export const deleteInspectionResult = async (resultId: string): Promise<void> => {
  await apiClient.delete(`/inspection-results/${resultId}`);
};

export const checkCanActivateSeal = async (
  lotId: string
): Promise<CanActivateSealCheck> => {
  const response = await apiClient.post<{ data: CanActivateSealCheck }>(
    `/production-lots/${lotId}/can-activate-seal`
  );
  return response.data.data;
};
