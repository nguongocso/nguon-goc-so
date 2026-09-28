import apiClient from './axiosConfig';
import type {
  SuspectTraceCodeResponse,
  SuspectTraceCodeDetailResponse,
  LockTraceCodeRequest,
  LockTraceCodeResponse,
  UnlockTraceCodeRequest,
  UnlockTraceCodeResponse,
  PageResponse,
} from '@/types/suspectTraceCode';

const ADMIN_PREFIX = '/admin/trace-codes';

export const getSuspectTraceCodes = async (params: {
  minScore?: number;
  status?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<SuspectTraceCodeResponse>> => {
  const response = await apiClient.get<{
    data: PageResponse<SuspectTraceCodeResponse>;
  }>(`${ADMIN_PREFIX}/suspect`, { params });
  return response.data.data;
};

export const getSuspectDetail = async (
  traceCodeId: string,
): Promise<SuspectTraceCodeDetailResponse> => {
  const response = await apiClient.get<{
    data: SuspectTraceCodeDetailResponse;
  }>(`${ADMIN_PREFIX}/${traceCodeId}/suspect-detail`);
  return response.data.data;
};

export const lockTraceCode = async (
  traceCodeId: string,
  request: LockTraceCodeRequest,
): Promise<LockTraceCodeResponse> => {
  const response = await apiClient.post<{
    data: LockTraceCodeResponse;
  }>(`${ADMIN_PREFIX}/${traceCodeId}/lock`, request);
  return response.data.data;
};

export const unlockTraceCode = async (
  codeOrId: string,
  request: UnlockTraceCodeRequest,
): Promise<UnlockTraceCodeResponse> => {
  const response = await apiClient.post<{
    data: UnlockTraceCodeResponse;
  }>(`${ADMIN_PREFIX}/${codeOrId}/unlock`, request);
  return response.data.data;
};