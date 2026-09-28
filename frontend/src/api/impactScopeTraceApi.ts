import apiClient from './axiosConfig';
import type { ImpactScopeTraceResponse } from '@/types/impactScopeTrace';

/**
 * Lấy dữ liệu cây truy vết phạm vi ảnh hưởng hai chiều
 * GET /api/v1/trace/impact-scope?code={code}
 */
export const getImpactScopeTrace = async (code: string): Promise<ImpactScopeTraceResponse> => {
  const response = await apiClient.get<{ data: ImpactScopeTraceResponse }>(
    `/trace/impact-scope`,
    { params: { code } }
  );
  return response.data.data;
};

/**
 * Xuất tệp báo cáo phạm vi ảnh hưởng (Excel / PDF)
 * GET /api/v1/trace/impact-scope/export?code={code}&format={format}
 */
export const exportImpactScopeReport = async (code: string, format: 'EXCEL' | 'PDF' = 'EXCEL'): Promise<Blob> => {
  const response = await apiClient.get(`/trace/impact-scope/export`, {
    params: { code, format },
    responseType: 'blob',
  });
  return response.data;
};
