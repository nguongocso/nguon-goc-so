import { isAxiosError } from 'axios';
import apiClient from './axiosConfig';
import type { ExportOpenDataRequest } from '@/types/export';

/**
 * Cấu trúc bao bọc ApiResult trả về từ backend Spring Boot.
 */
interface ApiResult<T> {
  code?: number;
  status?: string;
  data: T;
  message?: string;
}

/**
 * Trích xuất an toàn thuộc tính data từ đối tượng ApiResult hoặc chính payload thô.
 */
function extractData<T>(resData: ApiResult<T> | T): T {
  if (resData && typeof resData === 'object' && 'data' in (resData as Record<string, unknown>)) {
    return (resData as ApiResult<T>).data;
  }
  return resData as T;
}

/**
 * Kết xuất dữ liệu mở dưới dạng tệp tin nhị phân (Blob).
 *
 * @param data Tham số lọc kết xuất dữ liệu mở.
 * @returns Promise chứa dữ liệu nhị phân của tệp kết xuất (JSON, CSV hoặc XML).
 */
export const exportOpenData = async (
  data: ExportOpenDataRequest,
): Promise<Blob> => {
  const response = await apiClient.post('/export/open-data', data, {
    responseType: 'blob',
  });
  return response.data;
};

/**
 * Lấy dữ liệu xem trước của hồ sơ xuất theo mẫu.
 *
 * @param shipmentId Mã định danh lô hàng cần xem trước.
 * @param templateId Mã mẫu hồ sơ áp dụng (tùy chọn).
 * @returns Promise chứa dữ liệu xem trước dạng JSON object.
 */
export const getExportPreview = async (
  shipmentId: string,
  templateId?: string,
): Promise<Record<string, unknown>> => {
  const params: Record<string, string> = {};
  if (templateId) {
    params.templateId = templateId;
  }
  const response = await apiClient.get<ApiResult<Record<string, unknown>> | Record<string, unknown>>(
    `/export/shipments/${shipmentId}/preview`,
    { params },
  );
  return extractData(response.data);
};

/**
 * Tải tệp hồ sơ xuất theo mẫu đối tác định dạng JSON, CSV hoặc PDF.
 *
 * @param shipmentId Mã định danh lô hàng cần xuất.
 * @param templateId Mã mẫu hồ sơ áp dụng.
 * @param format Định dạng tệp xuất ('json' | 'csv' | 'pdf'). Mặc định là 'json'.
 * @returns Promise chứa dữ liệu Blob của tệp xuất.
 */
export const exportShipmentWithTemplate = async (
  shipmentId: string,
  templateId?: string,
  format: 'json' | 'csv' | 'pdf' = 'json',
): Promise<Blob> => {
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
  } catch (error: unknown) {
    if (
      isAxiosError(error) &&
      error.response?.data instanceof Blob &&
      error.response.data.type?.includes('application/json')
    ) {
      const text = await error.response.data.text();
      let message = text || 'Có lỗi xảy ra khi tạo hồ sơ xuất';
      try {
        const errJson = JSON.parse(text) as { message?: string };
        if (errJson?.message) {
          message = errJson.message;
        }
      } catch {
        // Giữ nguyên text nếu không phải JSON
      }
      throw new Error(message);
    }
    throw error;
  }
};