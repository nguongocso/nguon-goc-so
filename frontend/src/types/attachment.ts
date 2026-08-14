export interface Attachment {
  id: string;
  farmLogId: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  fileUrl: string;
  description: string | null;
  uploadedBy: string;
  uploadedAt: string; // ISO datetime
}