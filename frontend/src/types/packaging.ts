import type { ChainEventType } from '@/enums/chainEventType';

/**
 * Payload yêu cầu ghi nhận sự kiện đóng gói nông sản.
 */
export interface RecordPackagingRequest {
  productionLotId: string;
  packagingSpecification: string;
  packagingDate: string;
  latitude?: number;
  longitude?: number;
}

/**
 * Payload yêu cầu đính chính thông tin sự kiện đóng gói đã ghi nhận.
 */
export interface CorrectPackagingRequest {
  packagingSpecification: string;
  packagingDate: string;
  latitude?: number;
  longitude?: number;
  correctionReason: string;
}

/** Giá trị dữ liệu phẳng của sự kiện đóng gói. */
export type PackagingEventDataValue =
  | string
  | number
  | boolean
  | null
  | undefined
  | readonly (string | number | boolean | null)[];

/** Dữ liệu linh hoạt của một sự kiện trong timeline lô hàng. */
export interface ChainEventData
  extends Record<string, PackagingEventDataValue> {
  productionLotId?: string;
  productionLotName?: string;
  inputQuantity?: number;
  outputQuantity?: number;
  lossRate?: number;
  preprocessingDate?: string;
}

/** Dữ liệu phản hồi chi tiết của sự kiện trong chuỗi cung ứng. */
export interface ChainEventResponse {
  id: string;
  shipmentId: string | null;
  eventType: ChainEventType;
  eventData: ChainEventData;
  latitude: number | null;
  longitude: number | null;
  recordedAt: string;
  recordedByName: string;
  createdAt: string;
  lineageLevel?: 'PRODUCTION_LOT' | 'SOURCE_SHIPMENT' | 'CHILD_SHIPMENT' | null;
  sourceShipmentId?: string | null;
  inherited?: boolean;
}
