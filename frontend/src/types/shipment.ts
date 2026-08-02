export interface TraceCode {
  id: string;
  codeValue: string;
  qrImage: string;
  status: 'INACTIVE' | 'ACTIVE' | 'RECALLED';
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

export interface RecallShipmentPayload {
  reason: string;
}

export interface RecallShipmentResponse {
  id: string;
  shipmentId: string;
  reason: string;
  recalledBy: string;
  recalledAt: string;
  status: string;
  shipmentStatus: "RECALLED";
  traceCodesUpdated: number;
}

export interface ShipmentRecallInfo {
  shipmentId: string;
  recalled: boolean;
  reason: string | null;
  recalledAt: string | null;
}