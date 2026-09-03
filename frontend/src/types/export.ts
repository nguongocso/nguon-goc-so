export interface ExportOpenDataRequest {
  organizationId?: string;
  fromDate?: string; // ISO datetime
  toDate?: string;
  productCategoryIds?: string[];
  shipmentIds?: string[];
  /** NCL-742 §8 — lọc theo địa bàn quản lý. */
  unitIds?: string[];
  format?: 'JSON' | 'CSV' | 'XML';
}

export interface ExportOpenDataResponse {
  success: boolean;
  status: number;
  message?: string;
}