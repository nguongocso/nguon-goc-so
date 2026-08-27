import type { Attachment } from './attachment';

export type FarmActivityType =
  | 'PLANTING'
  | 'WATERING'
  | 'FERTILIZING'
  | 'PESTICIDE'
  | 'WEEDING'
  | 'HARVESTING'
  | 'OTHER';

export interface FarmLog {
  id: string;
  productionLotId: string;
  productionLotName: string;
  activityType: FarmActivityType;
  material: string | null;
  quantity: number | null;
  unit: string | null;
  executedDate: string;
  notes: string | null;
  createdByName: string;
  createdById?: string;
  createdAt: string;
  attachmentCount?: number;
  attachments?: Attachment[];
  // NCL-03-CN-006: Đính chính nhật ký canh tác
  originalFarmLogId?: string | null;
  isCorrection?: boolean | null;
  correctionReason?: string | null;
  correctedByName?: string | null;
  isCorrected?: boolean | null;
}

export interface FarmLogQueryParams {
  productionLotId: string;
  page?: number;
  size?: number;
}

export interface CreateFarmLogRequest {
  productionLotId: string;
  activityType: FarmActivityType;
  material: string | null;
  quantity: number | null;
  unit: string | null;
  executedDate: string;
  notes: string | null;
}

export type FarmLogResponse = FarmLog;

export interface FarmLogCorrectionData {
  activityType?: FarmActivityType;
  material?: string;
  quantity?: number;
  unit?: string;
  executedDate?: string;
  notes?: string;
}

export interface CorrectFarmLogRequest {
  correctionData: FarmLogCorrectionData;
  reason: string;
}