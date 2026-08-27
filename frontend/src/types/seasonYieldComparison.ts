export interface SeasonYieldComparisonParams {
  years: number[];
  farmAreaId?: string;
  productCategoryId?: string;
  organizationId?: string;
  /** NCL-742 §8 — lặp query param `unitIds=a&unitIds=b`. */
  unitIds?: string[];
}

export interface SeasonYieldItem {
  year: number;
  seasonCode: string;
  seasonName: string;
  lotCount: number;
  totalQuantity: number;
  delta: number;
  deltaPercent: number | null;
}

export interface SeasonYieldComparisonResponse {
  hasData: boolean;
  message: string | null;
  baselineYear: number | null;
  baselineSeasonCode: string | null;
  baselineSeasonName: string | null;
  seasons: SeasonYieldItem[];
}
