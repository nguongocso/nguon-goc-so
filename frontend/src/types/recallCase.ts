// NCL-08-CN-012 — Kết thúc vụ việc thu hồi và ghi nhận biện pháp khắc phục

export type RecallCaseStatus = 'OPEN' | 'CLOSED';

/** Kết quả xử lý từng lô trong vụ việc thu hồi. */
export type LotResolution =
  | 'DESTROYED'
  | 'RETURNED'
  | 'REPROCESSED'
  | 'UNRECOVERABLE';

/**
 * Kết quả xử lý của một lô hàng trong phạm vi vụ việc.
 *
 * Với lô chưa nhập kết quả, `resolution`, `recoveredQuantity`, `notes`
 * và `createdAt` là `null`.
 */
export interface RecallLotResult {
  id: string | null;
  shipmentId: string;
  shipmentName: string;
  unit: string | null;
  resolution: LotResolution | null;
  recoveredQuantity: number | null;
  notes: string | null;
  createdAt: string | null;
}

export interface RecallCase {
  id: string;
  caseCode: string;
  status: RecallCaseStatus;
  productionLotId: string;
  productionLotName: string;
  organizationId: string;
  createdAt: string;
  updatedAt: string;
  closedAt: string | null;
  closedBy: string | null;
  /** Biện pháp khắc phục phòng ngừa chung cho vụ việc. */
  remediationMeasures: string | null;
  evidenceFileIds: string[] | null;
  /** Danh sách lô trong phạm vi vụ việc (bao gồm cả lô chưa nhập kết quả). */
  lotResults: RecallLotResult[];
  shipmentCount: number;
}

export interface RecallLotResultPayload {
  shipmentId: string;
  resolution: LotResolution;
  recoveredQuantity: number;
  notes?: string;
}

export interface CloseRecallCasePayload {
  remediationMeasures: string;
  evidenceFileIds?: string[];
  lotResults: RecallLotResultPayload[];
}