export interface WarehouseReceiptRequest {
  codeValue: string;
  receivedQuantity: number;
  conditionNote?: string;
  receiptDate?: string;
  reason?: string;
}

export interface WarehouseReceiptResponse {
  id: string;
  eventType: 'WAREHOUSE_RECEIPT';
  shipmentId: string;
  shipmentName: string;
  traceCode?: string;
  declaredQuantity: number;
  receivedQuantity: number;
  discrepancy: number;
  discrepancyPercent: number;
  isDiscrepancyExceeded: boolean;
  reasonRequired: boolean;
  reason?: string;
  conditionNote?: string;
  receiptDate: string;
  recordedAt: string;
  recordedBy: string;
  notificationSent?: boolean;
}
