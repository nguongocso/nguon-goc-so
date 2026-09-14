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

/**
 * Xem trước hồ sơ xuất theo mẫu
 */
export const getExportPreview = async (
  shipmentId: string,
  templateId?: string
): Promise<Record<string, unknown>> => {
  const params: Record<string, string> = {};
  if (templateId) {
    params.templateId = templateId;
  }
  const response = await apiClient.get<Record<string, unknown>>(
    `/export/open-data/shipments/${shipmentId}/preview`,
    { params }
  );
  return response.data;
};