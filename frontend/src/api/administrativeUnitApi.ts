import type { AdministrativeUnitNode } from '@/types/administrativeUnit';
import { MOCK_ADMIN_UNITS } from '@/mocks/administrativeUnits';

/**
 * Lấy cây đơn vị hành chính 2 cấp (tỉnh → xã/phường).
 * Hiện trả mock; giai đoạn 3 thay bằng API thật theo docs/NCL-742-api-contract.md.
 */
export async function getAdministrativeUnitTree(): Promise<AdministrativeUnitNode[]> {
  // TODO(NCL-742-giai-doan-3): thay bằng apiClient.get('/administrative-units/tree')
  await new Promise<void>((resolve) => setTimeout(resolve, 150));
  return MOCK_ADMIN_UNITS;
}
