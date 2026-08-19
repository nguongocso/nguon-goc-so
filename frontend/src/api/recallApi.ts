// src/api/recallApi.ts
// Theo tài liệu API: Thu hồi lô (NCL-08-CN-003) & Thu hồi lô sản xuất 2 bước (NCL-08-CN-008)
import apiClient from './axiosConfig';
import type { ApiResult } from '@/types/auth';
import type { RecallRequest, RecallResponse, RecallInfoResponse } from '@/types/recall';
import type {
  ApproveRecallRequestPayload,
  CreateRecallRequestPayload,
  PageResponse,
  RecallRequest as RecallRequestDetail,
  RecallRequestListParams,
  RejectRecallRequestPayload,
} from '@/types/recallRequest';

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

// =========================================================
// NCL-08-CN-008 - Yêu cầu thu hồi lô sản xuất (2 bước)
// =========================================================

/**
 * Tạo yêu cầu thu hồi lô sản xuất (VT-03).
 * POST /api/v1/recall-requests
 */
export const createRecallRequest = async (
  payload: CreateRecallRequestPayload,
): Promise<RecallRequestDetail> => {
  const response = await apiClient.post<ApiResult<RecallRequestDetail>>(
    '/recall-requests',
    payload,
  );
  return response.data.data;
};

/**
 * Lấy danh sách yêu cầu thu hồi (VT-02), hỗ trợ lọc theo trạng thái + phân trang.
 * GET /api/v1/recall-requests?status=&page=&size=
 */
export const getRecallRequests = async (
  params: RecallRequestListParams = {},
): Promise<PageResponse<RecallRequestDetail>> => {
  const response = await apiClient.get<ApiResult<PageResponse<RecallRequestDetail>>>(
    '/recall-requests',
    { params },
  );
  return response.data.data;
};

/**
 * Lấy chi tiết một yêu cầu thu hồi (VT-02).
 * GET /api/v1/recall-requests/{id}
 */
export const getRecallRequest = async (id: string): Promise<RecallRequestDetail> => {
  const response = await apiClient.get<ApiResult<RecallRequestDetail>>(
    `/recall-requests/${id}`,
  );
  return response.data.data;
};

/**
 * Duyệt một yêu cầu thu hồi (VT-02).
 * PUT /api/v1/recall-requests/{id}/approve
 */
export const approveRecallRequest = async (
  id: string,
  payload: ApproveRecallRequestPayload = {},
): Promise<RecallRequestDetail> => {
  const response = await apiClient.put<ApiResult<RecallRequestDetail>>(
    `/recall-requests/${id}/approve`,
    payload,
  );
  return response.data.data;
};

/**
 * Từ chối một yêu cầu thu hồi (VT-02).
 * PUT /api/v1/recall-requests/{id}/reject
 */
export const rejectRecallRequest = async (
  id: string,
  payload: RejectRecallRequestPayload,
): Promise<RecallRequestDetail> => {
  const response = await apiClient.put<ApiResult<RecallRequestDetail>>(
    `/recall-requests/${id}/reject`,
    payload,
  );
  return response.data.data;
};
