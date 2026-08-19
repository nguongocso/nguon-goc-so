import apiClient from "@/api/axiosConfig";
import type {
  RecordTransportEventPayload,
  TransportEvent,
  TransportEventResponse,
} from "@/types/transportEvent";

/**
 * Ghi sự kiện vận chuyển cho lô hàng được xác định bằng mã truy xuất.
 * POST /api/v1/chain-events/transport
 */
export const recordTransportEvent = async (
  payload: RecordTransportEventPayload,
): Promise<TransportEvent> => {
  const response = await apiClient.post<TransportEventResponse>(
    "/chain-events/transport",
    payload,
  );

  return response.data.data;
};