import { isAxiosError } from 'axios';
import apiClient from './axiosConfig';
import type {
  OrganizationUsageDashboard,
  OrganizationUsageQueryParams,
} from '@/types/organizationUsage';

export interface OrganizationUsageResponse {
  success: boolean;
  status: number;
  message?: string;
  data: OrganizationUsageDashboard;
}

export interface DownloadedUsageReport {
  blob: Blob;
  fileName: string;
}

/** Kiểu xuất báo cáo được hỗ trợ. */
export type OrganizationUsageExportFormat = 'csv' | 'pdf';

/**
 * Lỗi API mang theo HTTP status để UI phân biệt 403 (không có quyền)
 * với lỗi tải dữ liệu thông thường.
 */
export class OrganizationUsageApiError extends Error {
  status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = 'OrganizationUsageApiError';
    this.status = status;
  }
}

function toUsageError(err: unknown, fallback: string): OrganizationUsageApiError {
  if (isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    return new OrganizationUsageApiError(
      data?.message || fallback,
      err.response?.status
    );
  }
  if (err instanceof Error && err.message) {
    return new OrganizationUsageApiError(err.message);
  }
  return new OrganizationUsageApiError(fallback);
}

function buildUsageSearchParams(params: OrganizationUsageQueryParams = {}): URLSearchParams {
  const search = new URLSearchParams();
  if (params.startDate) search.set('startDate', params.startDate);
  if (params.endDate) search.set('endDate', params.endDate);
  if (params.organizationId) search.set('organizationId', params.organizationId);
  return search;
}

/**
 * Lấy mức độ sử dụng nền tảng của từng tổ chức trong kỳ.
 * GET /api/v1/reports/organization-usage
 */
export async function getOrganizationUsage(
  params: OrganizationUsageQueryParams = {}
): Promise<OrganizationUsageDashboard> {
  try {
    const search = buildUsageSearchParams(params);
    const query = search.toString();
    const response = await apiClient.get<OrganizationUsageResponse>(
      `/reports/organization-usage${query ? `?${query}` : ''}`
    );
    return response.data.data;
  } catch (err) {
    throw toUsageError(err, 'Không thể tải dữ liệu mức độ sử dụng. Vui lòng thử lại.');
  }
}

/**
 * Trích xuất tên file từ header Content-Disposition,
 * dùng đuôi file theo kiểu xuất khi header không có sẵn.
 */
function extractFileName(
  contentDisposition?: string,
  format: OrganizationUsageExportFormat = 'csv'
): string {
  const fallback = `Bao_cao_muc_do_su_dung.${format}`;

  if (!contentDisposition) return fallback;

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1].replace(/"/g, ''));
    } catch {
      // Bỏ qua lỗi decode
    }
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return plainMatch?.[1]?.replace(/"/g, '') || fallback;
}

/**
 * Xuất báo cáo mức độ sử dụng theo kỳ ra file CSV hoặc PDF.
 * GET /api/v1/reports/organization-usage/export
 */
export async function exportOrganizationUsage(
  params: OrganizationUsageQueryParams = {},
  format: OrganizationUsageExportFormat = 'csv'
): Promise<DownloadedUsageReport> {
  try {
    const search = buildUsageSearchParams(params);
    search.set('format', format);
    const response = await apiClient.get(
      `/reports/organization-usage/export?${search.toString()}`,
      { responseType: 'blob' }
    );

    const blob = response.data as Blob;
    const fileName = extractFileName(response.headers?.['content-disposition'], format);

    return { blob, fileName };
  } catch (err) {
    throw toUsageError(err, 'Không thể xuất báo cáo. Vui lòng thử lại.');
  }
}
