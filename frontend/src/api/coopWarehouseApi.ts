import apiClient from "@/api/apiClient";
import type { ApiResult } from "@/types/api";
import type {
  RecordWarehouseEntryFormValues,
  RecordWarehouseExitFormValues,
} from "@/utils/validators/coopWarehouseEventSchema";

export interface CoopWarehouseEventResponse {
  id: string;
  productionLotId: string;
  productionLotName: string;
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
