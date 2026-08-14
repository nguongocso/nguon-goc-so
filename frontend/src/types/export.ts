export interface ExportOpenDataRequest {
  organizationId?: string;
  fromDate?: string; // ISO datetime
  toDate?: string;
  productCategoryIds?: string[];
  shipmentIds?: string[];
  format?: 'JSON' | 'CSV' | 'XML';
}

export interface ExportOpenDataResponse {
  success: boolean;
  status: number;
  message?: string;
}