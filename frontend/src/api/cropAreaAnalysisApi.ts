import apiClient from './axiosConfig';
import type { CropAreaAnalysisResponse, CropAreaAnalysisParams } from '@/types/cropAreaAnalysis';

export const getCropAreaAnalysis = async (
  params: CropAreaAnalysisParams
): Promise<CropAreaAnalysisResponse> => {
  const response = await apiClient.get<{ data: CropAreaAnalysisResponse }>(
    '/reports/crop-area-analysis',
    { params }
  );
  return response.data.data;
};