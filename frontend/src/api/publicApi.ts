import apiClient from './axiosConfig';

import type { PublicTraceResponse } from '@/types/publicTrace';
import type { PublicLotCertificationsResponse } from '@/types/publicCertification';

export const getPublicTrace = async (
  codeValue: string,
  latitude?: number,
  longitude?: number,
): Promise<PublicTraceResponse> => {
  const response = await apiClient.get<{
    data: PublicTraceResponse;
  }>(`/public/trace/${codeValue}`, {
    params: {
      latitude,
      longitude,
    },
  });

  return response.data.data;
};

export const getPublicCertifications = async (
  codeValue: string
): Promise<PublicLotCertificationsResponse> => {
  const response = await apiClient.get<{
    data: PublicLotCertificationsResponse;
  }>(
    `/public/trace/${codeValue}/certifications`
  );

  return response.data.data;
};