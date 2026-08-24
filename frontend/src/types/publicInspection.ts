export interface PublicInspectionResult {
  id: string;
  criterionName: string;
  standardValue: string;
  measuredValue: string;
  passed: boolean;
  inspectorName?: string;
  inspectionDate: string;
  expiryDate: string;
  laboratoryName?: string;
}

export interface PublicInspectionResponse {
  productionLotId?: string | null;
  lotName?: string | null;
  hasInspection: boolean;
  inspections: PublicInspectionResult[];
}