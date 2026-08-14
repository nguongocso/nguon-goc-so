export interface ActivityLog {
  id: string;
  userId: string;
  username: string;
  fullName: string;
  action: string;
  description: string;
  entityType: string;
  entityId: string;
  ipAddress: string;
  createdAt: string;
}

export interface ActivityLogParams {
  page?: number;
  size?: number;
  action?: string;
  actorName?: string;
  startDate?: string;
  endDate?: string;
}