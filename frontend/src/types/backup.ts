export type BackupOperationType = 'BACKUP' | 'RESTORE';
export type BackupStatus = 'IN_PROGRESS' | 'SUCCESS' | 'FAILED';
export type BackupType = 'SCHEDULED' | 'MANUAL';

export interface BackupSchedule {
  id: number;
  cronExpression: string;
  description: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  updatedBy: string;
}

export interface BackupHistoryItem {
  id: number;
  operationType: BackupOperationType;
  fileName: string | null;
  fileSize: number | null;
  backupType: BackupType | null;
  status: BackupStatus;
  errorMessage: string | null;
  referenceId: number | null;
  createdAt: string;
  createdBy: string;
}

export interface BackupHistoryResponse {
  content: BackupHistoryItem[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface BackupScheduleRequest {
  cronExpression: string;
  description?: string;
  isActive: boolean;
}