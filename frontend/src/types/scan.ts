export interface ScanLookupResponse {
  valid: boolean;
  message: string | null;
  traceCode: string;
  shipmentId: string;
  shipmentName: string;
  shipmentStatus: string;
  productionLotId: string;
  productCategoryName: string;
  farmAreaName: string;
  organizationId: string;
  organizationName: string;
  allowedEventTypes: string[];
  lastEventType: string | null;
  lastEventRecordedAt: string | null;
  totalQuantity?: number;
}
