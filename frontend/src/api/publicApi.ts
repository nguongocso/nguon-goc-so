import apiClient from './axiosConfig';
import type { PublicTraceResponse } from '@/types/publicTrace';
import type { PublicLotCertificationsResponse } from '@/types/publicCertification';
import type { PublicInspectionResponse } from '@/types/publicInspection';
import {
  USE_MOCK_INSPECTION_RESULT,
  mockFetchPublicInspections,
} from '@/services/inspectionResultMock';

/** Tra cứu thông tin công khai của mã tem. */
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

/** Ghi nhận một lượt quét mã QR thực tế. */
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

/** Lấy danh sách chứng nhận công khai của lô mã tem. */
export const getPublicCertifications = async (
  codeValue: string,
): Promise<PublicLotCertificationsResponse> => {
  const response = await apiClient.get<{
    data: PublicLotCertificationsResponse;
  }>(`/public/trace/${codeValue}/certifications`);

  return response.data.data;
};

/** Lấy kết quả kiểm nghiệm công khai của lô mã tem. */
export const getPublicInspections = async (
  codeValue: string,
): Promise<PublicInspectionResponse> => {
  if (USE_MOCK_INSPECTION_RESULT) {
    return mockFetchPublicInspections(codeValue);
  }
  const response = await apiClient.get<{
    data: PublicInspectionResponse;
  }>(`/public/trace/${codeValue}/inspections`);

  return response.data.data;
};
