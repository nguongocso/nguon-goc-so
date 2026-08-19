import apiClient from "./axiosConfig";
import type {
  CorrectPreprocessingRequest,
  PreprocessingEventResponse,
  RecordPreprocessingRequest,
} from "@/types/preprocessing";

interface ApiDataResponse<T> {
  data: T;
}

export const recordPreprocessingEvent = async (
  payload: RecordPreprocessingRequest,
): Promise<PreprocessingEventResponse> => {
  const response = await apiClient.post<
    ApiDataResponse<PreprocessingEventResponse>
  >("/chain-events/preprocessing", payload);

  return response.data.data;
};
export const correctPreprocessingEvent = async (
  originalEventId: string,
  payload: CorrectPreprocessingRequest,
): Promise<PreprocessingEventResponse> => {
  const response = await apiClient.post<
    ApiDataResponse<PreprocessingEventResponse>
  >(`/chain-events/preprocessing/${originalEventId}/correct`, payload);

  return response.data.data;
};
