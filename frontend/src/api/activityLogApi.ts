import type {
  ActivityLog,
  ActivityLogParams,
  ActivityLogExportFilterRequest,
  ActivityLogExportPreviewResponse,
} from "@/types/activityLog";
import apiClient from "./axiosConfig";
import type { PageResponse } from "@/types/common";

export const getActivityLogs = async (
  params: ActivityLogParams
): Promise<PageResponse<ActivityLog>> => {
  const response = await apiClient.get<{ data: PageResponse<ActivityLog>}>(
    '/organizations/activity-logs',
    { params }
  );
  return response.data.data;
};

/**
 * Đếm số bản ghi nhật ký hoạt động khớp bộ lọc trước khi xuất.
 */
export const previewExportActivityLogs = async (
  filter: ActivityLogExportFilterRequest
): Promise<ActivityLogExportPreviewResponse> => {
  const response = await apiClient.post<{ data: ActivityLogExportPreviewResponse }>(
    '/organizations/activity-logs/exports/preview',
    filter
  );
  return response.data.data;
};

/**
 * Tải xuống tệp CSV snapshot nhật ký hoạt động khớp bộ lọc.
 */
export const downloadActivityLogsCsv = async (
  filter: ActivityLogExportFilterRequest
): Promise<void> => {
  const res = await apiClient.post('/organizations/activity-logs/exports', filter, {
    responseType: 'blob',
  });
  const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8;' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  const contentDisposition = res.headers['content-disposition'];
  let fileName = `activity-logs-${new Date().toISOString().slice(0, 10)}.csv`;
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^";]+)"?/);
    if (match && match[1]) fileName = match[1];
  }
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};