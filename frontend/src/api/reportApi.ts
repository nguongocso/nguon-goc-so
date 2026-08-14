import apiClient from './axiosConfig';
import type { IndustryReportResponse, IndustryReportParams } from '@/types/report';

export interface DownloadedReport {
  blob: Blob;
  fileName: string;
}

export interface DashboardStatistics {
  summary: {
    totalLots: number;
    totalExpectedYield: number;
    totalActualYield: number;
  };
  byStatus: Record<string, number>;
  timeSeries: Array<{
    period: string;
    lotCount: number;
    expectedYield: number;
    actualYield: number;
  }>;
}

export interface DashboardQueryParams {
  startDate?: string;      // yyyy-MM-dd
  endDate?: string;        // yyyy-MM-dd
  organizationId?: string; // UUID
  groupBy?: 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';
}

// --- API cho Dashboard ---

/**
 * Lấy dữ liệu bảng điều khiển sản lượng và số lô
 * GET /api/v1/production-lots/dashboard
 */
export const getDashboardStatistics = async (params: DashboardQueryParams = {}): Promise<DashboardStatistics> => {
  const response = await apiClient.get<{ success: boolean; data: DashboardStatistics }>(
    '/production-lots/dashboard',
    { params }
  );
  return response.data.data;
};

// --- API cho Export Report (NCL-07-CN-003) ---

/**
 * Ghi chú: backend hiện trả DTO trực tiếp ở top-level (không bọc trong
 * { success, data } như tài liệu mô tả). Hàm này chấp nhận cả 2 dạng để
 * không vỡ khi backend sửa lại đúng theo tài liệu.
 */
function unwrapReportResponse<T>(payload: T | { success: boolean; data: T }): T {
  if (payload && typeof payload === 'object' && 'data' in (payload as any)) {
    return (payload as { data: T }).data;
  }
  return payload as T;
}

export const getIndustrySummary = async (
  params: IndustryReportParams
): Promise<IndustryReportResponse> => {
  const response = await apiClient.get<
    IndustryReportResponse | { success: boolean; data: IndustryReportResponse }
  >('/reports/industry-summary', { params });
  return unwrapReportResponse(response.data);
};
export const exportIndustrySummary = async (
  params: IndustryReportParams & { format?: 'PDF' | 'EXCEL' }
): Promise<DownloadedReport> => {
  const response = await apiClient.get('/reports/industry-summary/export', {
    params,
    responseType: 'blob',
  });

  const blob = response.data as Blob;
  const fileName = extractFileNameFromContentDisposition(
    response.headers?.['content-disposition']
  );

  return { blob, fileName };
};

/**
 * Lấy tên file từ header Content-Disposition.
 * Hỗ trợ cả dạng `attachment; filename="x.pdf"` lẫn `filename*=UTF-8''...`.
 */
function extractFileNameFromContentDisposition(
  contentDisposition?: string
): string {
  if (!contentDisposition) return 'industry-summary';

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1].replace(/"/g, ''));
    } catch {
      // fall through nếu decode thất bại
    }
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return plainMatch?.[1]?.replace(/"/g, '') || 'industry-summary';
}
