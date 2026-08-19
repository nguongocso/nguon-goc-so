import type { Attachment } from '@/types/attachment';
import apiClient from './axiosConfig';

export const getAttachments = async (logId: string): Promise<Attachment[]> => {
  const response = await apiClient.get<{ success: boolean; data: Attachment[] }>(
    `/farm-logs/${logId}/attachments`
  );
  return response.data.data;
};

export const uploadAttachment = async (
  logId: string,
  file: File,
  description?: string
): Promise<Attachment> => {
  const formData = new FormData();
  formData.append('file', file);
  if (description) formData.append('description', description);

  const response = await apiClient.post<{ success: boolean; data: Attachment }>(
    `/farm-logs/${logId}/attachments`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    }
  );
  return response.data.data;
};

export const deleteAttachment = async (attachmentId: string): Promise<void> => {
  await apiClient.delete(`/farm-logs/attachments/${attachmentId}`);
};

/** Xem file đính kèm (trả về Blob để hiển thị trong dialog). */
export const viewAttachment = async (attachmentId: string): Promise<{ blob: Blob; contentType: string }> => {
  const response = await apiClient.get(`/farm-logs/attachments/${attachmentId}/view`, {
    responseType: 'blob',
  });
  const contentTypeHeader = response.headers['content-type'];
  return {
    blob: response.data,
    contentType: typeof contentTypeHeader === 'string' ? contentTypeHeader : 'application/octet-stream',
  };
};

/** Tải xuống file đính kèm. */
export const downloadAttachment = async (attachmentId: string, fileName: string): Promise<void> => {
  const response = await apiClient.get(`/farm-logs/attachments/${attachmentId}/download`, {
    responseType: 'blob',
  });

  const blob = response.data;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};
