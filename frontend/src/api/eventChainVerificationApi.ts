import type { ChainVerificationResponse } from '@/types/eventChainVerification';
import apiClient from './axiosConfig';

export const verifyChainIntegrity = async (
  shipmentId: string
): Promise<ChainVerificationResponse> => {
  const response = await apiClient.get<{ data: ChainVerificationResponse }>(
    `/shipments/${shipmentId}/verify-chain`
  );
  return response.data.data;
};