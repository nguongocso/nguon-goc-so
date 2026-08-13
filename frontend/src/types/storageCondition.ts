export interface StorageConditionRequest {
  codeValue: string;
  temperature: number;
  humidity: number;
  recordedAt?: string;
}

export interface ThresholdInfo {
  tempMin?: number;
  tempMax?: number;
  humidityMin?: number;
  humidityMax?: number;
}

export interface StorageConditionResponse {
  id: string;
  eventType: 'STORAGE_CONDITION';
  shipmentId: string;
  shipmentName: string;
  temperature: number;
  humidity: number;
  thresholds?: ThresholdInfo;
  isTemperatureExceeded: boolean;
  isHumidityExceeded: boolean;
  alertLevel: 'OK' | 'WARNING' | 'CRITICAL';
  recordedAt: string;
  recordedBy: string;
}