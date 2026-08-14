// src/api/recallApi.ts
// Theo tài liệu API: Thu hồi lô (NCL-08-CN-003)
import apiClient from './axiosConfig';
import type { ApiResult } from '@/types/auth';
import type { RecallRequest, RecallResponse, RecallInfoResponse } from '@/types/recall';

/**
 * Thu hồi một lô hàng đang hiệu lực.
 * POST /api/v1/shipments/{shipmentId}/recall
 */
export const recallShipment = async (
  shipmentId: string,
  reason: string,
): Promise<RecallResponse> => {
  const payload: RecallRequest = { reason };
  const response = await apiClient.post<ApiResult<RecallResponse>>(
    `/shipments/${shipmentId}/recall`,
    payload,
  );
  return response.data.data;
};

/**
 * Xem thông tin thu hồi hiện tại của một lô hàng (nếu có).
 * GET /api/v1/shipments/{shipmentId}/recall
 */
export const getRecallInfo = async (
  shipmentId: string,
): Promise<RecallInfoResponse> => {
  const response = await apiClient.get<ApiResult<RecallInfoResponse>>(
    `/shipments/${shipmentId}/recall`,
  );
  return response.data.data;
};