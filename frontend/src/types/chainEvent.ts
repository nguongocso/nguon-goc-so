import type { ChainEventType } from "@/enums/chainEventType";

export interface RecordMobileEventRequest {
  productionLotId: string;
  eventType: ChainEventType;
  recordedAt: string; // ISO 8601
  latitude: number;
  longitude: number;
  images: string[]; // danh sách ảnh base64
  deviceSource?: string; // mặc định "MOBILE"
  eventData: {
    quantity?: number;
    harvestDate?: string; // YYYY-MM-DD
    packagingSpecification?: string;
    packagingDate?: string; // YYYY-MM-DD
  };
}

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