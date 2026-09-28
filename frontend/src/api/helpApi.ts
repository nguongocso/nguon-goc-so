// src/api/helpApi.ts
// NCL-01-CN-006 - Hướng dẫn sử dụng trong ứng dụng
import apiClient from './axiosConfig';
import type { ApiResponse } from '@/types/api';
import type { HelpContent } from '@/types/help';

/**
 * Lấy nội dung hướng dẫn cho một màn hình theo vai trò người dùng hiện tại.
 * GET /api/v1/help?screenKey=<key>
 * Lỗi được ném về caller (useHelp); caller dùng try/finally + toApiError để tránh treo UI.
 *
 * @returns nội dung hướng dẫn, hoặc null nếu chưa có.
 */
export const getHelp = async (
  screenKey: string,
): Promise<HelpContent | null> => {
  const response = await apiClient.get<ApiResponse<HelpContent | null>>(
    '/help',
    { params: { screenKey } },
  );
  return response.data.data;
};