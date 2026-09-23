/** Dữ liệu yêu cầu yêu cầu ghi nhận sự kiện sơ chế và phân loại nông sản. */
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
  deviceSource?: 'WEB' | 'MOBILE';
}

/** Dữ liệu yêu cầu yêu cầu đính chính sự kiện sơ chế đã ghi nhận. */
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

/** Cấu trúc dữ liệu chi tiết của sự kiện sơ chế. */
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

/** Phản hồi chi tiết sau khi ghi nhận sự kiện sơ chế. */
export interface PreprocessingEventResponse {
  id: string;
  shipmentId: string | null;
  eventType: 'PREPROCESSING';
  eventData: PreprocessingEventData;
  latitude: number | null;
  longitude: number | null;
  recordedAt: string;
  recordedByName: string;
  createdAt: string;
}
