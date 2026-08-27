export interface CropAreaAnalysisResponse {
  summary: SummaryStats;
  byArea: AreaAnalysisStats[];
  bySeason: SeasonAnalysisStats[];
  /** NCL-742 §8 — message khi dữ liệu rỗng (vd VT-05 chưa gán địa bàn). */
  message?: string | null;
}

export interface SummaryStats {
  totalLots: number;
  totalExpectedYield: number;
  totalActualYield: number;
  totalArea: number;
}

export interface AreaAnalysisStats {
  farmAreaId: string;
  farmAreaName: string;
  areaSize: number;
  organizationName: string;
  totalLots: number;
  expectedYield: number;
  actualYield: number;
  seasons: AreaSeasonStats[];
}

export interface AreaSeasonStats {
  seasonCode: string; // DONG_XUAN, HE_THU, THU_DONG
  seasonName: string;
  year: number;
  lotCount: number;
  expectedYield: number;
  actualYield: number;
}

export interface SeasonAnalysisStats {
  seasonCode: string;
  seasonName: string;
  year: number;
  totalLots: number;
  expectedYield: number;
  actualYield: number;
  areas: SeasonAreaStats[];
}

export interface SeasonAreaStats {
  farmAreaId: string;
  farmAreaName: string;
  lotCount: number;
  expectedYield: number;
  actualYield: number;
}

export interface CropAreaAnalysisParams {
  year?: number;
  farmAreaId?: string;
  productCategoryId?: string;
  organizationId?: string;
  /** NCL-742 §8 — lặp query param `unitIds=a&unitIds=b`. */
  unitIds?: string[];
}