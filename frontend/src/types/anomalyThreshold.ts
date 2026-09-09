export interface AnomalyThresholdConfig {
  id: string | null;
  productCategoryId: string | null;
  productCategoryName: string | null;
  maxScansPerHour: number;
  maxScansPerDay: number;
  maxDistanceKmPer30Min: number;
  minTimeBetweenScansMinutes: number;
  activationAgeDays: number;
  isActive: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
  createdByName?: string | null;
  updatedByName?: string | null;
}

export interface AllThresholdsResponse {
  global: AnomalyThresholdConfig;
  categoryOverrides: AnomalyThresholdConfig[];
}

export interface UpdateGlobalThresholdRequest {
  maxScansPerHour: number;
  maxScansPerDay: number;
  maxDistanceKmPer30Min: number;
  minTimeBetweenScansMinutes: number;
  activationAgeDays: number;
}

export interface CategoryThresholdOverrideRequest {
  productCategoryId: string;
  maxScansPerHour: number;
  maxScansPerDay: number;
  maxDistanceKmPer30Min: number;
  minTimeBetweenScansMinutes: number;
  activationAgeDays: number;
}

export interface ImpactEstimationRequest {
  productCategoryId?: string | null;
  maxScansPerHour: number;
  maxScansPerDay: number;
  maxDistanceKmPer30Min: number;
  minTimeBetweenScansMinutes: number;
  activationAgeDays: number;
}

export interface ImpactEstimationResult {
  estimatedAnomaliesCount: number;
  totalScansAnalyzed: number;
  totalTraceCodesAnalyzed: number;
  highFrequencyCount: number;
  impossibleTravelCount: number;
  activationAgeCount: number;
  analysisPeriodDays: number;
  message: string;
}
