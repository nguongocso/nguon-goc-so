export interface LookupStatisticsResponse {
  summary: SummaryStats;
  byLocation: LocationScanStats[];
  byProductionLot: LotScanStats[];
  timeSeries: TimeSeriesData[];
}

export interface SummaryStats {
  totalScans: number;
  totalUniqueCodes: number;
  abnormalScansCount: number;
}

export interface LocationScanStats {
  location: string;
  scanCount: number;
}

export interface LotScanStats {
  lotId: string;
  lotName: string;
  scanCount: number;
  abnormalScansCount: number;
}

export interface TimeSeriesData {
  period: string;
  scanCount: number;
}

export interface AbnormalScanResponse {
  scanId: string;
  codeValue: string;
  lotName: string;
  scannedAt: string;
  ipAddress: string;
  userAgent: string;
  location: string;
  latitude: number | null;
  longitude: number | null;
  reason: string;
}

export interface AbnormalScansPage {
  content: AbnormalScanResponse[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
  };
}

export interface LookupStatisticsParams {
  startDate?: string;
  endDate?: string;
  productionLotId?: string;
  shipmentId?: string;
  organizationId?: string;
  groupBy?: 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';
}