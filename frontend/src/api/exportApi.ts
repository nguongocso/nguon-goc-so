import apiClient from './axiosConfig';
import type { ExportOpenDataRequest } from '@/types/export';

interface ApiResult<T> {
  code?: number;
  status?: string;
  data: T;
  message?: string;
}

function extractData<T>(resData: ApiResult<T> | T): T {
  if (resData && typeof resData === 'object' && 'data' in (resData as Record<string, unknown>)) {
    return (resData as ApiResult<T>).data;
  }
  return resData as T;
}

/**
 * Xuất dữ liệu mở
 * POST /api/v1/export/open-data
 * Trả về Blob (file download)
 */
export const exportOpenData = async (
  data: ExportOpenDataRequest
): Promise<Blob> => {
  console.log('[exportApi] exportOpenData:', data);
  const response = await apiClient.post('/export/open-data', data, {
    responseType: 'blob',
  });
  return response.data;
};

/**
 * Xem trước hồ sơ xuất theo mẫu
 * GET /api/v1/export/shipments/{shipmentId}/preview
 */
export const getExportPreview = async (
  shipmentId: string,
  templateId?: string
): Promise<Record<string, unknown>> => {
  console.log('[exportApi] getExportPreview:', { shipmentId, templateId });
  const params: Record<string, string> = {};
  if (templateId) {
    params.templateId = templateId;
  }
  try {
    const response = await apiClient.get<ApiResult<Record<string, unknown>> | Record<string, unknown>>(
      `/export/shipments/${shipmentId}/preview`,
      { params }
    );
    const data = extractData(response.data);
    console.log('[exportApi] getExportPreview - Thành công:', data);
    return data;
  } catch (err) {
    console.error('[exportApi] getExportPreview - Thất bại:', err);
    throw err;
  }
};

/**
 * Tải file hồ sơ xuất theo mẫu đối tác định dạng JSON hoặc CSV
 * GET /api/v1/export/shipments/{shipmentId}?templateId={templateId}&format={format}
 */
export const exportShipmentWithTemplate = async (
  shipmentId: string,
  templateId?: string,
  format: 'json' | 'csv' | 'pdf' = 'json'
): Promise<Blob> => {
  console.log('[exportApi] exportShipmentWithTemplate:', { shipmentId, templateId, format });
  const params: Record<string, string> = { format };
  if (templateId && templateId !== 'default') {
    params.templateId = templateId;
  }
  try {
    const response = await apiClient.get(`/export/shipments/${shipmentId}`, {
      params,
      responseType: 'blob',
      timeout: 30000,
    });
    return response.data;
  } catch (error: any) {
    if (error.response?.data instanceof Blob && error.response.data.type?.includes('application/json')) {
      const text = await error.response.data.text();
      let message = text || 'Có lỗi xảy ra khi tạo hồ sơ xuất';
      try {
        const errJson = JSON.parse(text);
        if (errJson?.message) {
          message = errJson.message;
        }
      } catch {
        // Không phải JSON hợp lệ → giữ nguyên text
      }
      throw new Error(message);
    }
    throw error;
  }
};