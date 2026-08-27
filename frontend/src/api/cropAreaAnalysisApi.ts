import apiClient from './axiosConfig';
import type { CropAreaAnalysisResponse, CropAreaAnalysisParams } from '@/types/cropAreaAnalysis';

export const getCropAreaAnalysis = async (
  params: CropAreaAnalysisParams
): Promise<CropAreaAnalysisResponse> => {
  const response = await apiClient.get<{ data: CropAreaAnalysisResponse }>(
    '/reports/crop-area-analysis',
    { params: buildCropAreaAnalysisParams(params) },
  );
  return response.data.data;
};

/**
 * NCL-742 §8: `unitIds` phải lặp `unitIds=a&unitIds=b`. Serializer mặc định của
 * axios phát `unitIds[]=a` (Spring @RequestParam không bắt được) nên tự build
 * URLSearchParams.
 */
function buildCropAreaAnalysisParams(
  params: CropAreaAnalysisParams,
): URLSearchParams {
  const search = new URLSearchParams();
  if (params.year !== undefined) search.set('year', String(params.year));
  if (params.farmAreaId) search.set('farmAreaId', params.farmAreaId);
  if (params.productCategoryId) {
    search.set('productCategoryId', params.productCategoryId);
  }
  if (params.organizationId) search.set('organizationId', params.organizationId);
  params.unitIds?.forEach((unitId) => search.append('unitIds', unitId));
  return search;
}
