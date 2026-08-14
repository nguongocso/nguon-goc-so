export interface ProductionLotImportRowError {
  rowNumber: number;
  reason: string;
}

export interface ProductionLotImportResultResponse {
  importHistoryId: string;
  status: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
  fileName: string;
  totalRows: number;
  successCount: number;
  failedCount: number;
  savedLotIds: string[];
  errors: ProductionLotImportRowError[];
  importedAt: string;
}

export interface ProductionLotImportHistory {
  id: string;
  fileName: string;
  totalRows: number;
  successCount: number;
  failedCount: number;
  status: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
  importedAt: string;
}