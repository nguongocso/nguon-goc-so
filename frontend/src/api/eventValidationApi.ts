import apiClient from './axiosConfig';
import type { FailedEventLog, LotValidationResponse } from '@/types/eventValidation';
import type { PageResponse } from '@/types/common';

/**
 * Kiểm tra tính hợp lệ của lô sản xuất trước khi ghi sự kiện
 * GET /api/v1/chain-events/validate-lot?lotId={lotId}&eventType={eventType}
 */
export const validateLot = async (lotId: string, eventType: string): Promise<LotValidationResponse> => {
  const response = await apiClient.get<{ data: LotValidationResponse }>('/chain-events/validate-lot', {
    params: { lotId, eventType },
  });
  return response.data.data;
};
/**
 * Xóa bản nháp sự kiện chuỗi
 * DELETE /api/v1/chain-events/drafts/{draftId}
 */
export const deleteDraft = async (draftId: string): Promise<void> => {
  await apiClient.delete(`chain-events/drafts/${draftId}`);
};

/**
 * Lấy danh sách nhật ký sự kiện thất bại có phân trang
 * GET /api/v1/chain-events/failed-logs?page={page}&size={size}
 */
export const getFailedLogs = async (page: number, size: number): Promise<PageResponse<FailedEventLog>> => {
  const response = await apiClient.get<{ data: PageResponse<FailedEventLog> }>('/chain-events/failed-logs', {
    params: { page, size },
  });
  return response.data.data;
};
