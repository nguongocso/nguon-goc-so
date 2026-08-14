import apiClient from './axiosConfig';
import type { ProductionLotCertification, AttachCertificationRequest, Certification, CreateCertificationRequest, CertificationResponse } from '@/types/certification';

/**
 * Lấy danh sách chứng nhận đã gắn của một lô sản xuất
 */
export const getLotCertifications = async (lotId: string): Promise<ProductionLotCertification[]> => {
  const response = await apiClient.get<{ data: ProductionLotCertification[] }>(
    `/production-lots/${lotId}/certifications`
  );
  return response.data.data;
};

/**
 * Gắn chứng nhận cho lô sản xuất
 */
export const attachCertification = async (
  lotId: string,
  payload: AttachCertificationRequest
): Promise<ProductionLotCertification> => {
  const response = await apiClient.post<{ data: ProductionLotCertification }>(
    `/production-lots/${lotId}/certifications`,
    payload
  );
  return response.data.data;
};

/**
 * Gỡ chứng nhận khỏi lô sản xuất
 */
export const detachCertification = async (lotId: string, certificationId: string): Promise<void> => {
  await apiClient.delete(`/production-lots/${lotId}/certifications/${certificationId}`);
};

/**
 * Lấy danh sách chứng nhận còn hiệu lực của tổ chức hiện tại
 * GET /api/v1/certifications/valid
 */
export const getValidCertifications = async (): Promise<Certification[]> => {
  const response = await apiClient.get<{ data: Certification[] }>('/certifications/valid');
  return response.data.data;
};

/**
 * Tạo mới chứng nhận cho tổ chức
 * POST /api/v1/certifications
 */
export const createCertification = async (
  data: CreateCertificationRequest
): Promise<CertificationResponse> => {
  const response = await apiClient.post<{ data: CertificationResponse }>(
    '/certifications',
    data
  );
  return response.data.data;
};

/**
 * Lấy danh sách tất cả chứng nhận của tổ chức
 * GET /api/v1/certifications
 */
export const getAllCertifications = async (): Promise<CertificationResponse[]> => {
  const response = await apiClient.get<{ data: CertificationResponse[] }>(
    '/certifications'
  );
  return response.data.data;
};