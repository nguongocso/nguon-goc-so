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

/**
 * Ghi nhận một lượt quét mã QR thực tế.
 *
 * Được gọi bởi luồng quét QR trong ứng dụng sau khi giải mã thành công
 * payload QR. Khác với GET /public/trace/{codeValue} (đọc thuần túy),
 * endpoint này tạo TraceCodeScanLog và kích hoạt phát hiện nghi vấn
 * NCL-08-CN-007.
 */
export const recordPublicScan = async (
  codeValue: string,
  latitude?: number,
  longitude?: number,
): Promise<PublicTraceResponse> => {
  const response = await apiClient.post<{
    data: PublicTraceResponse;
  }>(`/public/trace/${codeValue}/scan`, null, {
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