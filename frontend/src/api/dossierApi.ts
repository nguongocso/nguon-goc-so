import apiClient from './axiosConfig';

export interface DossierCheckResponse {
  shipmentId: string;
  eligible: boolean;
  missingDocuments: string[];
}

/**
 * Kiểm tra điều kiện xuất hồ sơ
 */
export const checkDossierEligibility = async (shipmentId: string): Promise<DossierCheckResponse> => {
  const response = await apiClient.get<{ data: DossierCheckResponse }>(
    `/shipments/${shipmentId}/dossier/check`
  );
  return response.data.data;
};

/**
 * Xuất và tải hồ sơ PDF
 */
export const exportDossier = async (shipmentId: string): Promise<Blob> => {
  const response = await apiClient.get(`/shipments/${shipmentId}/dossier/export`, {
    responseType: 'blob',
  });
  return response.data;
};