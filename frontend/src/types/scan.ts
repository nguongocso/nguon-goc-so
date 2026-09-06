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
  /** Lô đủ điều kiện ghi mốc bảo quản chưa (VT-03: có TRANSPORT; VT-04: đã thu mua; null với vai trò khác). */
  storageEligible?: boolean | null;
}
