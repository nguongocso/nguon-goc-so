import apiClient from './axiosConfig';
import type {
  BackupSchedule,
  BackupHistoryItem,
  BackupHistoryResponse,
  BackupScheduleRequest,
  BackupOperationType,
  BackupStatus,
} from '@/types/backup';

const BASE_URL = '/backups';

export const getSchedules = async (): Promise<BackupSchedule[]> => {
  const res = await apiClient.get<{ data: BackupSchedule[] }>(`${BASE_URL}/schedules`);
  return res.data.data;
};

export const configureSchedule = async (payload: BackupScheduleRequest): Promise<BackupSchedule> => {
  const res = await apiClient.post<{ data: BackupSchedule }>(`${BASE_URL}/schedules`, payload);
  return res.data.data;
};

export const triggerBackup = async (): Promise<BackupHistoryItem> => {
  const res = await apiClient.post<{ data: BackupHistoryItem }>(`${BASE_URL}/trigger`);
  return res.data.data;
};

export const getHistory = async (
  page: number = 0,
  size: number = 10,
  operationType?: BackupOperationType,
  status?: BackupStatus,
): Promise<BackupHistoryResponse> => {
  const params = new URLSearchParams();
  params.set('page', String(page));
  params.set('size', String(size));
  if (operationType) params.set('operationType', operationType);
  if (status) params.set('status', status);
  const res = await apiClient.get<{ data: BackupHistoryResponse }>(`${BASE_URL}/history?${params.toString()}`);
  return res.data.data;
};

export const downloadBackup = async (id: number): Promise<void> => {
  const res = await apiClient.get(`${BASE_URL}/history/${id}/download`, {
    responseType: 'blob',
  });
  const blob = new Blob([res.data], { type: 'application/octet-stream' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  const contentDisposition = res.headers['content-disposition'];
  let fileName = `backup_${id}.sql.gz`;
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="(.+?)"/);
    if (match) fileName = match[1];
  }
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};

export const deleteBackup = async (id: number): Promise<void> => {
  await apiClient.delete(`${BASE_URL}/history/${id}`);
};

export const restoreBackup = async (id: number): Promise<BackupHistoryItem> => {
  const res = await apiClient.post<{ data: BackupHistoryItem }>(`${BASE_URL}/history/${id}/restore`);
  return res.data.data;
};