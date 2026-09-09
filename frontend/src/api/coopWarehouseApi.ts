import apiClient from "./axiosConfig";

export interface ApiResult<T> {
  success?: boolean;
  message?: string;
  data?: T;
}
import type {
  RecordWarehouseEntryFormValues,
  RecordWarehouseExitFormValues,
} from "@/utils/validators/coopWarehouseEventSchema";

export interface CoopWarehouseEventResponse {
  id: string;
  shipmentId: string;
  shipmentName?: string;
  productionLotId?: string;
  productionLotName?: string;
  eventType: "WAREHOUSE_ENTRY" | "WAREHOUSE_EXIT";
  warehouseName?: string;
  entryTime?: string;
  exitTime?: string;
  storageCondition?: string;
  destination?: string;
  notes?: string;
  storageDurationDays?: number;
  storageDurationHours?: number;
  maxAllowedStorageDays?: number;
  isStorageExceeded?: boolean;
  warningMessage?: string;
  latitude?: number;
  longitude?: number;
  recordedAt: string;
  recordedByName?: string;
  createdAt?: string;
}

export interface ChainEventItem {
  id: string;
  eventType: string;
  recordedAt: string;
  eventData?: string;
}

export async function recordWarehouseEntry(
  data: RecordWarehouseEntryFormValues
): Promise<ApiResult<CoopWarehouseEventResponse>> {
  const response = await apiClient.post<ApiResult<CoopWarehouseEventResponse>>(
    "/api/v1/chain-events/coop-warehouse/entry",
    data
  );
  return response.data;
}

export async function recordWarehouseExit(
  data: RecordWarehouseExitFormValues
): Promise<ApiResult<CoopWarehouseEventResponse>> {
  const response = await apiClient.post<ApiResult<CoopWarehouseEventResponse>>(
    "/api/v1/chain-events/coop-warehouse/exit",
    data
  );
  return response.data;
}

export async function getShipmentChainEvents(
  shipmentId: string
): Promise<ChainEventItem[]> {
  try {
    const response = await apiClient.get<ApiResult<ChainEventItem[]>>(
      `/api/v1/shipments/${shipmentId}/chain-events`
    );
    return response.data?.data || [];
  } catch {
    return [];
  }
}

export async function getShipmentWarehouseStatus(
  shipmentId: string
): Promise<"IN_WAREHOUSE" | "NOT_IN_WAREHOUSE"> {
  const events = await getShipmentChainEvents(shipmentId);
  const warehouseEvents = events.filter(
    (e) => e.eventType === "WAREHOUSE_ENTRY" || e.eventType === "WAREHOUSE_EXIT"
  );
  if (warehouseEvents.length === 0) {
    return "NOT_IN_WAREHOUSE";
  }
  const lastEvent = warehouseEvents[warehouseEvents.length - 1];
  return lastEvent.eventType === "WAREHOUSE_ENTRY" ? "IN_WAREHOUSE" : "NOT_IN_WAREHOUSE";
}
