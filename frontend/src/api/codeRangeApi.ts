import type { CodeRange, CodeRangeStatusResponse, CreateCodeRangeRequest, RemainingCodesResponse } from "@/types/codeRange";
import apiClient from "./axiosConfig";

export const createCodeRange = async (data: CreateCodeRangeRequest): Promise<CodeRange> => {
  const response = await apiClient.post<{ data: CodeRange }>('/admin/code-ranges', data);
  return response.data.data;
};  

export const getCodeRangeStatus = async (): Promise<CodeRangeStatusResponse[]> => {
  const response = await apiClient.get<{ data: CodeRangeStatusResponse[] }>('/admin/code-ranges/status');
  return response.data.data;
};  

export const getRemainingCodes = async (organizationId: string): Promise<RemainingCodesResponse> => {
  const response = await apiClient.get<{ data: RemainingCodesResponse }>(
    `/admin/code-ranges/organization/${organizationId}/remaining`
  );
  return response.data.data;
};  