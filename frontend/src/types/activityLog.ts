export interface ActivityLog {
  id: string;
  userId: string;
  username: string;
  fullName: string;
  actorName?: string;
  action: string;
  actionType?: string;
  description: string;
  entityType?: string;
  targetType?: string;
  entityId?: string;
  targetId?: string;
  ipAddress?: string;
  details?: string;
  createdAt: string;
}

export interface ActivityLogParams {
  page?: number;
  size?: number;
  action?: string;
  actorName?: string;
  startDate?: string;
  endDate?: string;
  objectType?: string;
}

export interface ActivityLogExportFilterRequest {
  startDate?: string;
  endDate?: string;
  action?: string;
  actorName?: string;
  objectType?: string;
}

export interface ActivityLogExportPreviewResponse {
  count: number;
  mode: 'DIRECT' | 'ASYNC';
}

export type ActivityLogExportStatus = 'IN_PROGRESS' | 'SUCCESS' | 'FAILED';

export interface ActivityLogExportJobResponse {
  exportId: string;
  mode: 'ASYNC';
  status: ActivityLogExportStatus;
  recordCount: number;
  fileName?: string;
  fileSize?: number;
  createdAt: string;
  completedAt?: string;
  downloadUrl?: string;
}

export type ActivityLogExportRequestResult =
  | { mode: 'DIRECT' }
  | { mode: 'ASYNC'; job: ActivityLogExportJobResponse };
