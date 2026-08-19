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
  region: string;
  fromDate: string;
  toDate: string;
}