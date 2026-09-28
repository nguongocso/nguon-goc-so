import apiClient from './axiosConfig';
import type { ApiResult } from '@/types/auth';
import type {
  CertificateVerification,
  CertificateVerificationListParams,
  CertificateVerificationPage,
  RejectCertificatePayload,
  VerifyCertificatePayload,
} from '@/types/certificateVerification';

export const getCertificateVerifications = async (
  params: CertificateVerificationListParams,
): Promise<CertificateVerificationPage> => {
  const response = await apiClient.get<ApiResult<CertificateVerificationPage>>(
    '/admin/certifications',
    { params },
  );
  return response.data.data;
};

export const getCertificateVerification = async (
  certificateId: string,
): Promise<CertificateVerification> => {
  const response = await apiClient.get<ApiResult<CertificateVerification>>(
    `/admin/certifications/${certificateId}`,
  );
  return response.data.data;
};

export const getCertificateDocument = async (certificateId: string): Promise<Blob> => {
  const response = await apiClient.get<Blob>(
    `/admin/certifications/${certificateId}/document`,
    { responseType: 'blob' },
  );
  return response.data;
};

export const verifyCertificate = async (
  certificateId: string,
  payload: VerifyCertificatePayload,
): Promise<CertificateVerification> => {
  const response = await apiClient.put<ApiResult<CertificateVerification>>(
    `/admin/certifications/${certificateId}/verify`,
    payload,
  );
  return response.data.data;
};

export const rejectCertificate = async (
  certificateId: string,
  payload: RejectCertificatePayload,
): Promise<CertificateVerification> => {
  const response = await apiClient.put<ApiResult<CertificateVerification>>(
    `/admin/certifications/${certificateId}/reject`,
    payload,
  );
  return response.data.data;
};
