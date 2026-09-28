import apiClient from '@/api/axiosConfig';
import type { PageResponse } from '@/types/shipment';
import type {
  PartnerOrganization,
  ShipmentSplitPreview,
  SplitShipmentRequest,
  SplitShipmentResult,
} from '@/types/shipmentSplit';

interface ApiData<T> {
  data: T;
}

/** Lấy dữ liệu xem trước và điều kiện tách của lô hàng. */
export async function getShipmentSplitPreview(
  shipmentId: string,
): Promise<ShipmentSplitPreview> {
  const response = await apiClient.get<ApiData<ShipmentSplitPreview>>(
    `/shipments/${shipmentId}/split-preview`,
  );
  return response.data.data;
}

/** Tìm các doanh nghiệp đang hoạt động có thể nhận lô con. */
export async function getPartnerOrganizations(
  keyword = '',
): Promise<PageResponse<PartnerOrganization>> {
  const response = await apiClient.get<ApiData<PageResponse<PartnerOrganization>>>(
    '/partner-organizations',
    { params: { keyword, page: 0, size: 100 } },
  );
  return response.data.data;
}

/** Gửi toàn bộ phương án phân bổ trong một giao dịch tách lô. */
export async function splitShipment(
  shipmentId: string,
  payload: SplitShipmentRequest,
): Promise<SplitShipmentResult> {
  const response = await apiClient.post<ApiData<SplitShipmentResult>>(
    `/shipments/${shipmentId}/split`,
    payload,
  );
  return response.data.data;
}
