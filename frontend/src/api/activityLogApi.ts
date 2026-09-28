import type {
  ActivityLog,
  ActivityLogParams,
  ActivityLogExportFilterRequest,
  ActivityLogExportJobResponse,
  ActivityLogExportPreviewResponse,
  ActivityLogExportRequestResult,
} from '@/types/activityLog';
import apiClient from './axiosConfig';
import type { PageResponse } from '@/types/common';

interface ApiResult<T> {
  data: T;
  message?: string;
}

export const getActivityLogs = async (params: ActivityLogParams): Promise<PageResponse<ActivityLog>> => {
  const response = await apiClient.get<ApiResult<PageResponse<ActivityLog>>>('/organizations/activity-logs', { params });
  return response.data.data;
};

export const previewExportActivityLogs = async (
  filter: ActivityLogExportFilterRequest,
): Promise<ActivityLogExportPreviewResponse> => {
  const response = await apiClient.post<ApiResult<ActivityLogExportPreviewResponse>>(
    '/organizations/activity-logs/exports/preview', filter,
  );
  return response.data.data;
};

const parseFileName = (contentDisposition?: string): string => {
  const utf8 = contentDisposition?.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (utf8) return decodeURIComponent(utf8.replace(/^"|"$/g, ''));
  return contentDisposition?.match(/filename="?([^";]+)"?/i)?.[1]
    ?? `activity-logs-${new Date().toISOString().slice(0, 10)}.csv`;
};

const triggerBlobDownload = (blob: Blob, contentDisposition?: string) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = parseFileName(contentDisposition);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const requestActivityLogExport = async (
  filter: ActivityLogExportFilterRequest,
): Promise<ActivityLogExportRequestResult> => {
  const response = await apiClient.post<Blob>('/organizations/activity-logs/exports', filter, {
    responseType: 'blob',
  });
  if (response.status === 202) {
    const body = JSON.parse(await response.data.text()) as ApiResult<ActivityLogExportJobResponse>;
    return { mode: 'ASYNC', job: body.data };
  }
  triggerBlobDownload(response.data, response.headers['content-disposition']);
  return { mode: 'DIRECT' };
};

export const getActivityLogExportJob = async (exportId: string): Promise<ActivityLogExportJobResponse> => {
  const response = await apiClient.get<ApiResult<ActivityLogExportJobResponse>>(
    `/organizations/activity-logs/exports/${exportId}`,
  );
  return response.data.data;
};

export const downloadActivityLogExportJob = async (exportId: string): Promise<void> => {
  const response = await apiClient.get<Blob>(`/organizations/activity-logs/exports/${exportId}/download`, {
    responseType: 'blob',
  });
  triggerBlobDownload(response.data, response.headers['content-disposition']);
};

export const getActivityLogApiError = async (error: unknown, fallback: string): Promise<string> => {
  const responseData = (error as { response?: { data?: unknown } })?.response?.data;
  if (responseData instanceof Blob) {
    try {
      const body = JSON.parse(await responseData.text()) as { message?: string };
      return body.message || fallback;
    } catch {
      return fallback;
    }
  }
  if (responseData && typeof responseData === 'object' && 'message' in responseData) {
    return String((responseData as { message?: unknown }).message || fallback);
  }
  return error instanceof Error && error.message ? error.message : fallback;
};

export const downloadActivityLogsCsv = async (filter: ActivityLogExportFilterRequest): Promise<void> => {
  await requestActivityLogExport(filter);
};
