export interface ShipmentCodeRange {
  fromCode: string;
  toCode: string;
  quantity: number;
}

export interface ShipmentSplitPreview {
  shipmentId: string;
  shipmentName: string;
  status: string;
  productionLotId: string;
  productionLotName: string;
  declaredQuantity: number;
  assignableQuantity: number;
  nonInactiveQuantity: number;
  availableCodeRange: ShipmentCodeRange;
  canSplit: boolean;
  blockReasonCode: string | null;
  blockMessage: string | null;
}

export interface PartnerOrganization {
  id: string;
  code: string;
  name: string;
}

export interface ShipmentSplitAllocation {
  recipientOrganizationId: string;
  name: string;
  quantity: number;
  fromCode: string;
  toCode: string;
  packagingInfo?: string;
}

export interface SplitShipmentRequest {
  allocations: ShipmentSplitAllocation[];
}

export interface SplitShipmentResult {
  sourceShipment: {
    id: string;
    name: string;
    status: 'SPLIT';
    declaredQuantity: number;
    allocatedQuantity: number;
  };
  children: Array<{
    id: string;
    parentShipmentId: string;
    name: string;
    status: 'CODE_PRINTED';
    recipientOrganization: PartnerOrganization;
    totalQuantity: number;
    firstCode: string;
    lastCode: string;
  }>;
  totalChildren: number;
  totalAllocatedQuantity: number;
  splitByName: string;
  splitAt: string;
}
