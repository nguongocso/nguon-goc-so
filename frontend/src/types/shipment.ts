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

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
