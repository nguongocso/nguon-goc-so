import apiClient from './axiosConfig';
import type { ChainVerificationResponse } from '@/types/eventChainVerification';

/** Xác thực tính toàn vẹn chuỗi sự kiện của lô hàng GET /api/v1/shipments/{shipmentId}/verify-chain */
export const verifyChainIntegrity = async (
  shipmentId: string
): Promise<ChainVerificationResponse> => {
  const response = await apiClient.get<{ data: ChainVerificationResponse }>(
    `/shipments/${shipmentId}/verify-chain`
  );
  return response.data.data;
};
