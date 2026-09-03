// src/types/report.ts
export interface ProductBreakdownItem {
  productCategoryName: string;
  shipmentCount: number;
  totalQuantity: number;
}

export interface IndustryReportResponse {
  region: string;
  fromDate: string;
  toDate: string;
  hasData: boolean;
  totalOrganizations: number;
  totalShipments: number;
  totalQuantity: number;
  productBreakdown: ProductBreakdownItem[];
  message: string | null;
}

export interface IndustryReportExportResponse {
  fileUrl: string;
  format: string;
  exportedAt: string;
  auditLogId: string;
}

export interface IndustryReportParams {
  /** Bỏ bắt buộc từ NCL-670/742 §8 — lọc theo địa bàn qua `unitIds`. */
  region?: string;
  fromDate: string;
  toDate: string;
  /** UUID đơn vị hành chính; axios phải serialize thành `unitIds=a&unitIds=b`. */
  unitIds?: string[];
}