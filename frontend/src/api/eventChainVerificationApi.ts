import type { ChainVerificationResponse } from '@/types/eventChainVerification';

import apiClient from './axiosConfig';

/** Xác thực tính toàn vẹn chuỗi sự kiện của lô hàng. */
export const verifyChainIntegrity = async (
  shipmentId: string
): Promise<ChainVerificationResponse> => {
  const response = await apiClient.get<{ data: ChainVerificationResponse }>(
    `/shipments/${shipmentId}/verify-chain`
  );
  return response.data.data;
};
