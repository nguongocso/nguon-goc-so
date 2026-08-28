export type InspectionCriterionStatus = 'ACTIVE' | 'INACTIVE';

/**
 * Một chỉ tiêu kiểm nghiệm trong danh mục dùng chung.
 * GET /api/v1/inspection-criteria
 */
export interface InspectionCriterion {
  id: number;
  name: string;
  unit: string;
  maxThreshold: number;
  referenceStandard: string | null;
  status: InspectionCriterionStatus;
  /**
   * Đã được yêu cầu kiểm nghiệm tham chiếu hay chưa.
   * Khi true, chỉ tiêu không được xóa cứng mà chỉ ngừng sử dụng (BR-5).
   */
  referenced: boolean;
  createdAt: string;
  updatedAt: string | null;
}

/** Payload tạo/cập nhật chỉ tiêu kiểm nghiệm. */
export interface InspectionCriterionRequest {
  name: string;
  unit: string;
  maxThreshold: number;
  referenceStandard?: string;
}

export interface InspectionCriterionQueryParams {
  keyword?: string;
  status?: InspectionCriterionStatus;
  page?: number;
  size?: number;
}
