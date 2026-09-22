import type { ChainEventType } from '@/enums/chainEventType';

/**
 * Payload yêu cầu ghi nhận sự kiện từ ứng dụng di động (Mobile).
 */
export interface RecordMobileEventRequest {
  productionLotId: string;
  eventType: ChainEventType;
  recordedAt: string; // ISO 8601
  latitude: number;
  longitude: number;
  images: string[]; // Danh sách ảnh base64
  deviceSource?: string; // Mặc định 'MOBILE'
  eventData: {
    quantity?: number;
    harvestDate?: string; // YYYY-MM-DD
    packagingSpecification?: string;
    packagingDate?: string; // YYYY-MM-DD
  };
}

/**
 * Dữ liệu phản hồi chi tiết của một sự kiện trong chuỗi cung ứng.
 */
export interface ChainEventResponse {
  id: string;
  eventType: ChainEventType;
  eventData: Record<string, any>;
  latitude: number | null;
  longitude: number | null;
  recordedAt: string;
  recordedByName: string;
  createdAt: string;
}