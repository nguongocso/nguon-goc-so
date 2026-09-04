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
  status: 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'HARVESTED' | 'PREPROCESSED' | 'PACKAGED' | 'CLOSED' | 'RECALLED' | 'CANCELLED';
  approvalNotes: string | null;
  createdByName: string | null;
  // FIX: was `approvebyName` (typo, inconsistent casing) — corrected to approvedByName
  approvedByName: string | null;
  // NCL-02-CN-006: thông tin hủy lô
  cancellationReason: string | null;
  cancellationNote: string | null;
  cancelledByName: string | null;
  cancelledAt: string | null;
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
  approvedByName?: string | null;
  approvalNotes?: string | null;
}
export interface ProductionLot {
  id: string;
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
  status: 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'HARVESTED' | 'PREPROCESSED' | 'PACKAGED' | 'CLOSED' | 'RECALLED' | 'CANCELLED';
  approvalNotes: string | null;
  createdByName: string | null;
  // FIX: was `approvebyName` (typo, inconsistent casing) — corrected to approvedByName
  approvedByName: string | null;
  createdAt: string;
  updatedAt: string;
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
  approvedByName?: string | null;
  approvalNotes?: string | null;
}
