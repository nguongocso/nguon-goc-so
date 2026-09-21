import apiClient from './axiosConfig';

import {
  mockFetchPublicInspections,
  USE_MOCK_INSPECTION_RESULT,
} from '@/services/inspectionResultMock';
import type { PublicLotCertificationsResponse } from '@/types/publicCertification';
import type { PublicInspectionResponse } from '@/types/publicInspection';
import type { PublicTraceResponse } from '@/types/publicTrace';

interface RequestOptions {
  signal?: AbortSignal;
}

/**
 * Tra cứu thông tin công khai của mã truy xuất nguồn gốc.
 * GET /api/v1/public/trace/{codeValue}
 */
export async function getPublicTrace(
  codeValue: string,
  latitude?: number,
  longitude?: number,
  { signal }: RequestOptions = {},
): Promise<PublicTraceResponse> {
  const response = await apiClient.get<{
    data: PublicTraceResponse;
  }>(`/public/trace/${codeValue}`, {
    params: {
      latitude,
      longitude,
    },
    signal,
  });

  return response.data.data;
}

/**
 * Ghi nhận một lượt quét mã QR thực tế.
 * Được gọi bởi luồng quét QR trong ứng dụng sau khi giải mã thành công payload QR.
 * POST /api/v1/public/trace/{codeValue}/scan
 */
export async function recordPublicScan(
  codeValue: string,
  latitude?: number,
  longitude?: number,
  { signal }: RequestOptions = {},
): Promise<PublicTraceResponse> {
  const response = await apiClient.post<{
    data: PublicTraceResponse;
  }>(`/public/trace/${codeValue}/scan`, null, {
    params: {
      latitude,
      longitude,
    },
    signal,
  });

  return response.data.data;
}

/**
 * Lấy danh sách chứng nhận công khai của lô sản xuất theo mã truy xuất.
 * GET /api/v1/public/trace/{codeValue}/certifications
 */
export async function getPublicCertifications(
  codeValue: string,
  { signal }: RequestOptions = {},
): Promise<PublicLotCertificationsResponse> {
  const response = await apiClient.get<{
    data: PublicLotCertificationsResponse;
  }>(`/public/trace/${codeValue}/certifications`, { signal });

  return response.data.data;
}

/**
 * Lấy kết quả kiểm nghiệm công khai của lô (CV-04).
 * GET /api/v1/public/trace/{codeValue}/inspections
 */
export async function getPublicInspections(
  codeValue: string,
  { signal }: RequestOptions = {},
): Promise<PublicInspectionResponse> {
  if (USE_MOCK_INSPECTION_RESULT) {
    return mockFetchPublicInspections(codeValue);
  }
  const response = await apiClient.get<{
    data: PublicInspectionResponse;
  }>(`/public/trace/${codeValue}/inspections`, { signal });

  return response.data.data;
}
