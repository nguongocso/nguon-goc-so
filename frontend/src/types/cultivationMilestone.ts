/**
 * Một mốc canh tác trong bảng hợp nhất.
 * GET /api/v1/cultivation-milestones
 */
export interface CultivationMilestone {
  id: number;
  name: string;
  description: string | null;
  activityType: string;
  expectedDaysFromPlanting: number | null;
  productCategoryId: string | null;
  productCategoryName: string | null;
  standardId: string | null;
  standardName: string | null;
  isMandatory: boolean;
  createdAt: string;
  updatedAt: string | null;
}

/** Payload tạo/cập nhật mốc canh tác. */
export interface CultivationMilestoneRequest {
  name: string;
  description?: string;
  activityType: string;
  expectedDaysFromPlanting?: number;
  productCategoryId?: string | null;
  standardId?: string | null;
  isMandatory: boolean;
}

export interface CultivationMilestoneQueryParams {
  keyword?: string;
  activityType?: string;
  productCategoryId?: string;
  standardId?: string;
  globalOnly?: boolean;
  page?: number;
  size?: number;
}

/** Một mốc canh tác bắt buộc còn thiếu khi đóng gói (NCL-09-CN-011). */
export interface MissingMilestoneItem {
  name: string;
  activityType: string | null;
}

/**
 * Kết quả kiểm tra lô đã đủ mốc canh tác bắt buộc (loại nông sản + tiêu chuẩn
 * của lô) để ghi sự kiện đóng gói.
 * GET /api/v1/cultivation-milestones/eligibility?productionLotId=...
 */
export interface MilestoneEligibilityResponse {
  productionLotId: string;
  eligible: boolean;
  missingMilestones: MissingMilestoneItem[];
}
