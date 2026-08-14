import apiClient from './axiosConfig';
import type { ExportOpenDataRequest } from '@/types/export';

/**
 * Xuất dữ liệu mở
 * POST /api/v1/export/open-data
 * Trả về Blob (file download)
 */
export const exportOpenData = async (
  data: ExportOpenDataRequest
): Promise<Blob> => {
  const response = await apiClient.post('/export/open-data', data, {
    responseType: 'blob',
  });
  return response.data;
};