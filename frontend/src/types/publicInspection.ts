export interface PublicInspectionResult {
  requestId: string;
  overallResult: "PASSED" | "FAILED";
  overallResultLabel: string;
  issueDate: string | null;
  expiryDate: string;
  statusLabel: string;
}

export interface PublicInspectionResponse {
  productionLotId: string;
  lotName: string;
  hasInspection: boolean;
  inspections: PublicInspectionResult[];
}