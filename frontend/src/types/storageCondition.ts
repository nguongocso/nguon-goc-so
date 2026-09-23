/**
 * Yêu cầu ghi nhận điều kiện lưu trữ / bảo quản
 */
export interface StorageConditionRequest {
  codeValue: string;
  temperature: number;
  humidity: number;
  recordedAt?: string;
}

/**
 * Thông tin ngưỡng nhiệt độ và độ ẩm cho phép
 */
export interface ThresholdInfo {
  tempMin?: number;
  tempMax?: number;
  humidityMin?: number;
  humidityMax?: number;
}

/**
 * Phản hồi sau khi ghi nhận điều kiện lưu trữ
 */
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
