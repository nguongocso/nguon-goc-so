// src/api/helpApi.ts
// NCL-01-CN-006 - Hướng dẫn sử dụng trong ứng dụng
import apiClient from './axiosConfig';
import type { ApiResult } from '@/types/auth';
import type { HelpContent } from '@/types/help';

/**
 * Lấy nội dung hướng dẫn cho một màn hình theo vai trò người dùng hiện tại.
 * GET /api/v1/help?screenKey=<key>
 *
 * @returns nội dung hướng dẫn, hoặc null nếu chưa có.
 */
export const getHelp = async (
  screenKey: string,
): Promise<HelpContent | null> => {
  const response = await apiClient.get<ApiResult<HelpContent | null>>(
    '/help',
    { params: { screenKey } },
  );
  return response.data.data;
};