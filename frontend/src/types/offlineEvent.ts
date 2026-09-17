import { ChainEventType } from '@/enums/chainEventType';

export interface OfflineEvent {
  offlineEventId: string;
  /** ID của lô sản xuất (dùng cho HARVEST, PACKAGING) */
  productionLotId?: string;
  /** ID của lô hàng (dùng cho TRANSPORT, PROCUREMENT) */
  shipmentId?: string;
  /** Mã truy xuất (dùng cho TRANSPORT để lookup shipment) */
  codeValue?: string;
  eventType: ChainEventType;
  recordedAt: string;
  latitude: number;
  longitude: number;
  images: string[];
  deviceSource?: string;
  eventData: Record<string, any>;
  status?: 'pending' | 'syncing' | 'failed' | 'success' | 'invalid' | 'da-ghi';
  errorMessage?: string;
  retryCount?: number;
  lastSyncAttempt?: number;
  /** ID bản ghi `farm_logs` sau khi nội dung đã đồng bộ, dùng để tải ảnh ở pha 2. */
  farmLogId?: string;
}

export interface OfflineSyncRequest {
  syncId: string;
  events: OfflineEvent[];
}

export interface OfflineSyncResultDto {
  offlineEventId: string;
  status: 'SUCCESS' | 'DUPLICATE' | 'FAILED';
  eventId?: string;
  message?: string;
}

export interface OfflineSyncResponse {
  syncId: string;
  totalEvents: number;
  successCount: number;
  duplicateCount: number;
  failedCount: number;
  results: OfflineSyncResultDto[];
}