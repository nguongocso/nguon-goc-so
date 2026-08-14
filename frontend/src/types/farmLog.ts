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
  createdAt: string;
  attachmentCount?: number;
  attachments?: Attachment[];
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