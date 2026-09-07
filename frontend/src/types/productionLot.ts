export interface ProductionLot {
  id: string;
  code?: string;
  organizationName: string;
  farmAreaId: string | null;
  farmAreaName: string | null;
  productCategoryId: string;
  productCategoryName: string | null;
  name: string;
  expectedQuantity: number;
  expectedQuantityUnit: string;
  // FIX: was duplicated as `actualQuantyti` (typo) — removed duplicate, kept correct field
  actualQuantity: number | null;
  plantingDate: string;
  harvestDate: string;
  status: 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'HARVESTED' | 'PREPROCESSED' | 'PACKAGED' | 'CLOSED' | 'RECALLED' | 'CANCELLED' | 'DISPOSED';
  approvalNotes: string | null;
  createdByName: string | null;
  // FIX: was `approvebyName` (typo, inconsistent casing) — corrected to approvedByName
  approvedByName: string | null;
  // NCL-02-CN-006: thông tin hủy lô
  cancellationReason: string | null;
  cancellationNote: string | null;
  cancelledByName: string | null;
  cancelledAt: string | null;
  // NCL-11-CN-005: thông tin loại bỏ lô (lô không đạt kiểm nghiệm)
  disposalReason: string | null;
  handlingMeasure: string | null;
  disposalNote: string | null;
  disposedByName: string | null;
  disposedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CancelProductionLotRequest {
  /** Lý do hủy — chọn 1 trong danh sách cố định (TC-03: bắt buộc) */
  reason: string;
  /** "Tại sao?" — diễn giải lý do hủy (không bắt buộc, tối đa 1000 ký tự) */
  note?: string;
}

export interface UpdateProductionLotRequest {
  name: string;
  farmAreaId?: string | null;
  productCategoryId: string;
  expectedQuantity: number;
  expectedQuantityUnit: string;
  plantingDate: string;
}

export interface UpdateProductionLotResponse {
  id: string;
  farmAreaId: string | null;
  productCategoryId: string;
  name: string;
  expectedQuantity: number;
  expectedQuantityUnit: string;
  plantingDate: string;
  status: string;
  updatedAt: string;
}

export interface CreateProductionLotRequest {
  name: string;
  farmAreaId: string | null;
  productCategoryId: string;
  expectedQuantity: number;
  expectedQuantityUnit: string;
  plantingDate: string | null;
}

export interface CreateProductionLotResponse {
  id: string;
  farmAreaId: string | null;
  productCategoryId: string;
  organizationName: string;
  farmAreaName: string | null;
  productCategoryName: string;
  name: string;
  expectedQuantity: number;
  expectedQuantityUnit: string;
  actualQuantity: number | null;
  plantingDate: string | null;
  harvestDate: string | null;
  status: 'DRAFT';
  approvalNotes: string | null;
  createdByName: string;
  approvedByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FarmAreaOption {
  id: string;
  name: string;
  area?: number;
}

export interface ProductCategoryOption {
  id: string;
  name: string;
}

export interface ApproveProductionLotRequest {
  /** true = duyệt, false = từ chối */
  approved: boolean;
  /** Bắt buộc khi approved = false */
  reason?: string;
}

export interface ApproveProductionLotResult {
  id: string;
  status: ProductionLot['status'];
}

// ============================================================
// NCL-11-CN-005: Xử lý lô không đạt kiểm nghiệm
// ============================================================

export interface DisposeProductionLotRequest {
  /** Lý do loại bỏ — bắt buộc (TC-03) */
  reason: string;
  /** Biện pháp xử lý — bắt buộc (TC-03) */
  handlingMeasure: string;
  /** Ghi chú bổ sung (không bắt buộc, tối đa 1000 ký tự) */
  note?: string;
}

export interface DisposeProductionLotResponse {
  id: string;
  status: ProductionLot['status'];
  disposalReason: string;
  handlingMeasure: string;
  disposalNote: string | null;
  disposedByName: string;
  disposedAt: string;
}

/**
 * Trạng thái kiểm nghiệm suy diễn từ dữ liệu kiểm nghiệm.
 * Không phải enum của ProductionLotStatus.
 */
export type InspectionStatus =
  | 'NOT_INSPECTED' // Chưa kiểm nghiệm
  | 'PASSED' // Đạt kiểm nghiệm
  | 'FAILED' // Không đạt kiểm nghiệm
  | 'RE_INSPECTION_PENDING'; // Đang kiểm nghiệm lại

