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
  /** Tổng số lần kiểm nghiệm đã thực hiện trên lô (bao gồm kiểm nghiệm lại). */
  roundCount?: number;
  /** Lịch sử kiểm nghiệm theo từng lần gửi mẫu, sắp xếp từ cũ đến mới. */
  history?: PublicInspectionRound[];
}

/** Một lần kiểm nghiệm (round) trong lịch sử — mỗi lần gửi mẫu đi kiểm là một phần tử. */
export interface PublicInspectionRound {
  /** Số thứ tự lần kiểm nghiệm, tính từ 1 cho lần cũ nhất. */
  round: number;
  /** Tên phòng/đơn vị kiểm nghiệm thực hiện lần này. */
  laboratoryName?: string | null;
  /** Ngày gửi mẫu đi kiểm nghiệm. */
  sampleSentDate?: string | null;
  /** Trạng thái lần kiểm nghiệm: PENDING_RESULT | PASSED | FAILED | CANCELLED. */
  status?: string | null;
  /** Tổng số chỉ tiêu của lần kiểm nghiệm này. */
  totalCriteria: number;
  /** Số chỉ tiêu đạt của lần này. */
  passedCriteria: number;
  /** Số chỉ tiêu không đạt của lần này. */
  failedCriteriaCount: number;
  /** Kết quả chi tiết các chỉ tiêu của lần này. */
  results: PublicInspectionResult[];
}