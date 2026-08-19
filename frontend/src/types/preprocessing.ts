export interface RecordPreprocessingRequest {
  productionLotId: string;
  inputQuantity: number;
  outputQuantity: number;
  grade?: string;
  processingMethod?: string;
  preprocessingDate: string;
  images?: string[];
  latitude?: number;
  longitude?: number;
  deviceSource?: "WEB" | "MOBILE";
}

export interface CorrectPreprocessingRequest {
  inputQuantity: number;
  outputQuantity: number;
  grade?: string;
  processingMethod?: string;
  preprocessingDate: string;
  correctionReason: string;
  latitude?: number;
  longitude?: number;
}

export interface PreprocessingEventData {
  productionLotId: string;
  productionLotName: string;
  inputQuantity: number;
  outputQuantity: number;
  lossRate: number;
  grade?: string;
  processingMethod?: string;
  preprocessingDate: string;
  images?: string[];
  deviceSource?: string;
  correctionReason?: string;
  parentEventId?: string;
}

export interface PreprocessingEventResponse {
  id: string;
  shipmentId: string | null;
  eventType: "PREPROCESSING";
  eventData: PreprocessingEventData;
  latitude: number | null;
  longitude: number | null;
  recordedAt: string;
  recordedByName: string;
  createdAt: string;
}
