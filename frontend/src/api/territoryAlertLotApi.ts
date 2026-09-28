import apiClient from './axiosConfig';
import { toApiError } from './apiError';
import type { PageResponse } from '@/types/common';
import type {
  AlertLotDetailResponse,
  AlertLotQueryParams,
  AlertLotSummaryResponse,
} from '@/types/territoryAlertLot';

export interface AlertLotListResponse {
  success: boolean;
  status: number;
  message: string;
  data: PageResponse<AlertLotSummaryResponse>;
}

export interface DownloadedAlertReport {
  blob: Blob;
  fileName: string;
}

/**
 * Chuẩn hóa tham số ngày sang ISO LocalDateTime (YYYY-MM-DDTHH:mm:ss)
 * nếu người dùng nhập YYYY-MM-DD từ date input.
 */
function normalizeDateTimeParam(dateStr?: string, isEndOfDay = false): string | undefined {
  if (!dateStr || !dateStr.trim()) return undefined;
  const trimmed = dateStr.trim();
  if (trimmed.length === 10 && /^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
    return isEndOfDay ? `${trimmed}T23:59:59` : `${trimmed}T00:00:00`;
  }
  return trimmed;
}

/**
 * Xây dựng URLSearchParams hỗ trợ đầy đủ các tham số lọc và mảng unitIds.
 */
export function buildAlertLotSearchParams(params: AlertLotQueryParams = {}): URLSearchParams {
  const search = new URLSearchParams();

  if (params.alertType) {
    search.set('alertType', params.alertType);
  }
  if (params.organizationId) {
    search.set('organizationId', params.organizationId);
  }

  const normalizedFromDate = normalizeDateTimeParam(params.fromDate, false);
  if (normalizedFromDate) {
    search.set('fromDate', normalizedFromDate);
  }

  const normalizedToDate = normalizeDateTimeParam(params.toDate, true);
  if (normalizedToDate) {
    search.set('toDate', normalizedToDate);
  }

  if (typeof params.page === 'number') {
    search.set('page', String(params.page));
  }
  if (typeof params.size === 'number') {
    search.set('size', String(params.size));
  }
  if (params.sortBy) {
    search.set('sortBy', params.sortBy);
  }
  if (params.sortDir) {
    search.set('sortDir', params.sortDir);
  }

  params.unitIds?.forEach((unitId) => {
    if (unitId) search.append('unitIds', unitId);
  });

  return search;
}

/**
 * Trích xuất tên file từ header Content-Disposition.
 */
function extractFileName(contentDisposition?: string): string {
  if (!contentDisposition) return 'Danh_sach_lo_canh_bao.pdf';

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1].replace(/"/g, ''));
    } catch {
      // Bỏ qua lỗi decode
    }
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return plainMatch?.[1]?.replace(/"/g, '') || 'Danh_sach_lo_canh_bao.pdf';
}

/**
 * Lấy danh sách lô có cảnh báo theo địa bàn quản lý.
 * GET /api/v1/reports/alert-lots
 */
export async function getAlertLots(
  params: AlertLotQueryParams = {}
): Promise<AlertLotListResponse> {
  try {
    const search = buildAlertLotSearchParams(params);
    const response = await apiClient.get<AlertLotListResponse>(
      `/reports/alert-lots?${search.toString()}`
    );
    return response.data;
  } catch (err) {
    throw toApiError(err);
  }
}

/**
 * Lấy chi tiết một lô có cảnh báo ở chế độ chỉ đọc.
 * GET /api/v1/reports/alert-lots/{lotId}
 */
export async function getAlertLotDetail(
  lotId: string
): Promise<AlertLotDetailResponse> {
  try {
    const response = await apiClient.get<{
      success: boolean;
      status: number;
      message: string;
      data: AlertLotDetailResponse;
    }>(`/reports/alert-lots/${lotId}`);
    return response.data.data;
  } catch (err) {
    throw toApiError(err);
  }
}

/**
 * Xuất danh sách lô có cảnh báo theo địa bàn ra file PDF.
 * GET /api/v1/reports/alert-lots/export
 */
export async function exportAlertLots(
  params: AlertLotQueryParams = {}
): Promise<DownloadedAlertReport> {
  try {
    const search = buildAlertLotSearchParams(params);
    // Xóa phân trang khi export để tải toàn bộ danh sách phù hợp với filter
    search.delete('page');
    search.delete('size');

    const response = await apiClient.get(`/reports/alert-lots/export?${search.toString()}`, {
      responseType: 'blob',
    });

    const blob = response.data as Blob;
    const fileName = extractFileName(response.headers?.['content-disposition']);

    return { blob, fileName };
  } catch (err) {
    throw toApiError(err);
  }
}
