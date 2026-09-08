export interface FarmAreaTraceDto {
  id: string;
  code: string;
  name: string;
  location: string;
  areaSize?: number;
}

export interface ProductionLotTraceDto {
  id: string;
  code: string;
  name: string;
  status: string;
  expectedQuantity: number;
  expectedQuantityUnit: string;
  actualQuantity?: number;
  plantingDate?: string;
  harvestDate?: string;
}

export interface ScanStatsTraceDto {
  totalScans: number;
  recentScanAt?: string;
  suspectCount: number;
}

export interface ChainEventTraceDto {
  id: string;
  eventType: string;
  eventTypeName: string;
  recordedAt: string;
  location?: string;
  isCorrection: boolean;
}

export interface ReceivingOrganizationTraceDto {
  organizationId: string;
  organizationName: string;
  receivedAt: string;
  receivedQuantity?: number;
  eventType: string;
  eventTypeName?: string;
}

export interface ShipmentTraceDto {
  id: string;
  code: string;
  name: string;
  status: string;
  totalQuantity: number;
  packagingInfo?: string;
  createdAt: string;
  activatedStampsCount: number;
  scanStats?: ScanStatsTraceDto;
  events?: ChainEventTraceDto[];
  receivingOrganizations: ReceivingOrganizationTraceDto[];
}

export interface ImpactScopeSummaryDto {
  totalShipments: number;
  totalActivatedStamps: number;
  totalReceivingOrganizations: number;
  totalRecalledShipments: number;
}

export interface ImpactScopeTraceResponse {
  rootNodeType: 'PRODUCTION_LOT' | 'SHIPMENT' | 'TRACE_CODE';
  searchedCode: string;
  farmArea?: FarmAreaTraceDto;
  productionLot: ProductionLotTraceDto;
  shipments: ShipmentTraceDto[];
  summary: ImpactScopeSummaryDto;
}
