import apiClient from './axiosConfig';
import type {
  CorrectPreprocessingRequest,
  PreprocessingEventResponse,
  RecordPreprocessingRequest,
} from '@/types/preprocessing';

interface ApiDataResponse<T> {
  data: T;
}

/**
 * Ghi nhận sự kiện sơ chế và phân loại
 * POST /api/v1/chain-events/preprocessing
 */
export const recordPreprocessingEvent = async (
  payload: RecordPreprocessingRequest,
): Promise<PreprocessingEventResponse> => {
  const response = await apiClient.post<
    ApiDataResponse<PreprocessingEventResponse>
  >('/chain-events/preprocessing', payload);

  return response.data.data;
};

/**
 * Đính chính sự kiện sơ chế đã ghi nhận
 * POST /api/v1/chain-events/preprocessing/{originalEventId}/correct
 */
export const correctPreprocessingEvent = async (
  originalEventId: string,
  payload: CorrectPreprocessingRequest,
): Promise<PreprocessingEventResponse> => {
  const response = await apiClient.post<
    ApiDataResponse<PreprocessingEventResponse>
  >(`/chain-events/preprocessing/${originalEventId}/correct`, payload);

  return response.data.data;
};
