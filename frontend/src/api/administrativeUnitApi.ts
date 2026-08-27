import apiClient from '@/api/axiosConfig';
import { toApiError } from '@/api/apiError';
import type { ApiResult } from '@/types/member';
import type { AdministrativeUnitNode } from '@/types/administrativeUnit';

/**
 * Lấy cây đơn vị hành chính 2 cấp (tỉnh → xã/phường).
 * GET /administrative-units/tree — contract NCL-742 §1, unwrap ApiResult.data.
 */
export async function getAdministrativeUnitTree(): Promise<AdministrativeUnitNode[]> {
  try {
    const response = await apiClient.get<ApiResult<AdministrativeUnitNode[]>>(
      '/administrative-units/tree',
    );
    return response.data.data ?? [];
  } catch (err) {
    throw toApiError(err);
  }
}
