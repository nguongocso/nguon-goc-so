// NCL-08-CN-012 — Kết thúc vụ việc thu hồi (RecallCase)
// API docs: docs/api/recall/RecallCase.md
import apiClient from './axiosConfig';
import type { ApiResult } from '@/types/auth';
import type { CloseRecallCasePayload, RecallCase } from '@/types/recallCase';

/**
 * Danh sách vụ việc thu hồi của tổ chức hiện tại.
 * GET /api/v1/recall-cases
 */
export const getRecallCases = async (): Promise<RecallCase[]> => {
  const response = await apiClient.get<ApiResult<RecallCase[]>>(
    '/recall-cases',
  );
  return response.data.data;
};

/**
 * Chi tiết vụ việc thu hồi kèm kết quả xử lý từng lô.
 * GET /api/v1/recall-cases/{id}
 */
export const getRecallCase = async (id: string): Promise<RecallCase> => {
  const response = await apiClient.get<ApiResult<RecallCase>>(
    `/recall-cases/${id}`,
  );
  return response.data.data;
};

/**
 * Kết thúc vụ việc thu hồi (chỉ khi mọi lô đã có kết quả xử lý
 * và đã nhập biện pháp khắc phục phòng ngừa — QTN-27).
 * PUT /api/v1/recall-cases/{id}/close
 */
export const closeRecallCase = async (
  id: string,
  payload: CloseRecallCasePayload,
): Promise<RecallCase> => {
  const response = await apiClient.put<ApiResult<RecallCase>>(
    `/recall-cases/${id}/close`,
    payload,
  );
  return response.data.data;
};