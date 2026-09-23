export interface PublicInspectionResult {
  id: string;
  criterionName: string;
  criterionNameEn?: string | null;
  standardValue: string;
  standardValueEn?: string | null;
  measuredValue: string;
  passed: boolean;
  inspectorName?: string;
  inspectionDate: string;
  expiryDate: string;
  laboratoryName?: string;
  entrySource?: 'TESTING_UNIT_PORTAL' | 'COOPERATIVE_MANUAL';
}

export interface PublicInspectionResponse {
  productionLotId?: string | null;
  lotName?: string | null;
  hasInspection: boolean;
  totalCriteria: number;
  passedCriteria: number;
  failedCriteriaCount: number;
  failedRatio: number;
  inspections: PublicInspectionResult[];
  roundCount?: number;
  history?: PublicInspectionRound[];
}

export interface PublicInspectionRound {
  round: number;
  laboratoryName?: string | null;
  sampleSentDate?: string | null;
  status?: string | null;
  totalCriteria: number;
  passedCriteria: number;
  failedCriteriaCount: number;
  results: PublicInspectionResult[];
}
