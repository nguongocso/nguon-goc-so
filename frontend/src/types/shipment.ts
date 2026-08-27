export interface TraceCode {
  id: string;
  codeValue: string;
  qrImage: string;
  status: 'INACTIVE' | 'ACTIVE' | 'RECALLED' | 'LOCKED' | 'SUSPECT' | 'CANCELLED';
}

export interface CancelTraceCodesPayload {
  cancelType: 'RANGE' | 'SINGLE';
  fromCode?: string;
  toCode?: string;
  codeValues?: string[];
  reasonType: 'PRINT_ERROR' | 'PRINT_MISALIGNED' | 'PEELED_OFF_DAMAGED' | 'OTHER';
  reasonNote?: string;
}

export interface CancelTraceCodesResult {
  shipmentId: string;
  totalCancelled: number;
  refundedQuota: number;
  remainingQuota: number;
  cancelledAt: string;
  cancelledBy: string;
  message: string;
}

export interface LabelCancellationHistoryItem {
  id: string;
  shipmentId: string;
  shipmentName: string;
  cancelledByName: string;
  cancelledAt: string;
  quantity: number;
  cancellationType: 'RANGE' | 'SINGLE';
  rangeFromCode?: string;
  rangeToCode?: string;
  reasonType: string;
  reasonNote?: string;
}

export interface Shipment {
  id: string;
  productionLotId: string;
  productionLotName: string;
  name: string;
  totalQuantity: number;
  packagingInfo?: string;
  status: 'DRAFT' | 'CODE_PRINTED' | 'ACTIVATED' | 'RECALLED';
  traceCodes: TraceCode[];
  createdByName: string;
  createdAt: string;
}

export interface ShipmentSummary {
  id: string;
  name: string;
  status: 'DRAFT' | 'CODE_PRINTED' | 'ACTIVATED' | 'RECALLED';
  productionLotName: string | null;
  totalQuantity: number | null;
}

export interface ProcurementShipment {
  id: string;
  name: string;
  status: 'DRAFT' | 'CODE_PRINTED' | 'ACTIVATED' | 'RECALLED';
  productionLotName: string | null;
  productCategoryName: string | null;
  totalQuantity: number | null;
}

export interface CreateShipmentPayload {
  productionLotId: string;
  name: string;
  totalQuantity: number;
  packagingInfo?: string;
}

export interface ShipmentResponse {
  success: boolean;
  status: number;
  data: Shipment;
  timestamp: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
