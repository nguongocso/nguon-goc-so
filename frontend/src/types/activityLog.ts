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
}