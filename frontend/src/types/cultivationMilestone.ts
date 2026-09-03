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
