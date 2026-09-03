export type CultivationMilestoneStatus = 'ACTIVE' | 'INACTIVE';

/**
 * Một mốc canh tác trong danh mục dùng chung.
 * GET /api/v1/cultivation-milestones
 */
export interface CultivationMilestone {
  id: number;
  name: string;
  description: string | null;
  activityType: string;
  expectedDaysFromPlanting: number | null;
  status: CultivationMilestoneStatus;
  referenced: boolean;
  createdAt: string;
  updatedAt: string | null;
}

/** Payload tạo/cập nhật mốc canh tác. */
export interface CultivationMilestoneRequest {
  name: string;
  description?: string;
  activityType: string;
  expectedDaysFromPlanting?: number;
}

export interface CultivationMilestoneQueryParams {
  keyword?: string;
  status?: CultivationMilestoneStatus;
  activityType?: string;
  page?: number;
  size?: number;
}

/**
 * Phân công mốc canh tác cho loại nông sản.
 */
export interface ProductCategoryMilestone {
  id: string;
  milestone: CultivationMilestone;
  standardId: string | null;
  standardName: string | null;
  isMandatory: boolean;
}

export interface CategoryMilestoneRequest {
  milestoneIds: number[];
  standardId?: string;
  /** Id các mốc đánh dấu bắt buộc; undefined = tất cả bắt buộc */
  mandatoryMilestoneIds?: number[];
}
