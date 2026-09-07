// src/api/codeRangeSupplementApi.ts
// NCL-04-CN-007 - Yêu cầu cấp bổ sung dải mã truy xuất (VT-02 tạo, VT-01 duyệt)
import apiClient from './axiosConfig';
import type { ApiResult } from '@/types/auth';
import type {
  ApproveSupplementRequestPayload,
  CodeRangeSupplementRequest,
  CreateSupplementRequestPayload,
  PageResponse,
  RejectSupplementRequestPayload,
  SupplementRequestListParams,
} from '@/types/codeRangeSupplement';

/**
 * Tạo yêu cầu cấp bổ sung dải mã (VT-02).
 * POST /api/v1/code-range-supplement-requests
 */
export const createSupplementRequest = async (
  payload: CreateSupplementRequestPayload,
): Promise<CodeRangeSupplementRequest> => {
  const response = await apiClient.post<ApiResult<CodeRangeSupplementRequest>>(
    '/code-range-supplement-requests',
    payload,
  );
  return response.data.data;
};

/**
 * Lấy danh sách yêu cầu của tổ chức mình (VT-02).
 * GET /api/v1/code-range-supplement-requests/my?status=&page=&size=
 */
export const getMySupplementRequests = async (
  params: SupplementRequestListParams = {},
): Promise<PageResponse<CodeRangeSupplementRequest>> => {
  const response = await apiClient.get<ApiResult<PageResponse<CodeRangeSupplementRequest>>>(
    '/code-range-supplement-requests/my',
    { params },
  );
  return response.data.data;
};

/**
 * Lấy danh sách tất cả yêu cầu (VT-01), hỗ trợ lọc theo trạng thái + phân trang.
 * GET /api/v1/code-range-supplement-requests?status=&page=&size=
 */
export const getSupplementRequests = async (
  params: SupplementRequestListParams = {},
): Promise<PageResponse<CodeRangeSupplementRequest>> => {
  const response = await apiClient.get<ApiResult<PageResponse<CodeRangeSupplementRequest>>>(
    '/code-range-supplement-requests',
    { params },
  );
  return response.data.data;
};

/**
 * Lấy chi tiết một yêu cầu (VT-01 xem tất cả, VT-02 chỉ xem của tổ chức mình).
 * GET /api/v1/code-range-supplement-requests/{id}
 */
export const getSupplementRequest = async (id: string): Promise<CodeRangeSupplementRequest> => {
  const response = await apiClient.get<ApiResult<CodeRangeSupplementRequest>>(
    `/code-range-supplement-requests/${id}`,
  );
  return response.data.data;
};

/**
 * Duyệt toàn bộ hoặc một phần một yêu cầu (VT-01).
 * PUT /api/v1/code-range-supplement-requests/{id}/approve
 */
export const approveSupplementRequest = async (
  id: string,
  payload: ApproveSupplementRequestPayload,
): Promise<CodeRangeSupplementRequest> => {
  const response = await apiClient.put<ApiResult<CodeRangeSupplementRequest>>(
    `/code-range-supplement-requests/${id}/approve`,
    payload,
  );
  return response.data.data;
};

/**
 * Từ chối một yêu cầu (VT-01).
 * PUT /api/v1/code-range-supplement-requests/{id}/reject
 */
export const rejectSupplementRequest = async (
  id: string,
  payload: RejectSupplementRequestPayload,
): Promise<CodeRangeSupplementRequest> => {
  const response = await apiClient.put<ApiResult<CodeRangeSupplementRequest>>(
    `/code-range-supplement-requests/${id}/reject`,
    payload,
  );
  return response.data.data;
};
