import type { FarmActivityType } from './farmLog';

export type MilestoneReminderStatus = 'OPEN' | 'COMPLETED';

export interface MilestoneReminder {
  id: string;
  lotId: string;
  lotName: string;
  milestoneId: number;
  milestoneName: string;
  activityType: FarmActivityType | string;
  overdueDays: number;
  expectedDate?: string | null;
  status: MilestoneReminderStatus;
  reminderDate: string;
  completedAt?: string | null;
  createdAt: string;
}

export interface MilestoneScanResult {
  scannedLotsCount: number;
  remindersCreatedCount: number;
  message: string;
}

export interface MilestoneReminderQueryParams {
  status?: MilestoneReminderStatus;
  lotId?: string;
  page?: number;
  size?: number;
}
