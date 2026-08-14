import type { FailedEventLog, LotValidationResponse } from "@/types/eventValidation";
import apiClient from "./axiosConfig";
import type { PageResponse } from "@/types/common";

export const validateLot = async (lotId: string, eventType: string): Promise<LotValidationResponse> => {
  const response = await apiClient.get<{ data: LotValidationResponse }>('/chain-events/validate-lot', {
    params: { lotId, eventType },
  });
  return response.data.data;
};  

export const deleteDraft = async (draftId: string): Promise<void> => {
  await apiClient.delete(`chain-events/drafts/${draftId}`);
};

export const getFailedLogs = async (page: number, size: number): Promise<PageResponse<FailedEventLog>> => {
  const response = await apiClient.get<{ data: PageResponse<FailedEventLog> }>('/chain-events/failed-logs', {
    params: { page, size },
  });
  return response.data.data;
};