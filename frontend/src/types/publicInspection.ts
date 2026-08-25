export interface PublicInspectionResult {
  id: string;
  criterionName: string;
  standardValue: string;
  measuredValue: string;
  passed: boolean;
  inspectorName?: string;
  inspectionDate: string;
  expiryDate: string;
  laboratoryName?: string;
}

export interface PublicInspectionResponse {
  productionLotId?: string | null;
  lotName?: string | null;
  hasInspection: boolean;
  /** Tổng số chỉ tiêu kiểm nghiệm đã công bố của lô. */
  totalCriteria: number;
  /** Số chỉ tiêu đạt. */
  passedCriteria: number;
  /** Số chỉ tiêu không đạt. */
  failedCriteriaCount: number;
  /** Tỷ lệ chỉ tiêu không đạt (%), 1 chữ số thập phân. */
  failedRatio: number;
  inspections: PublicInspectionResult[];
}