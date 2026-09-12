import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/api/axiosConfig', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import apiClient from '@/api/axiosConfig';
import {
  cloneProductionLot,
  getCloneProductionLotPreview,
} from '@/api/productionLotApi';
import type {
  CloneProductionLotPreview,
  CloneProductionLotResponse,
} from '@/types/productionLot';

describe('productionLotApi clone (NCL-02-CN-007)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('tải clone preview từ đúng endpoint và bóc data', async () => {
    const preview: CloneProductionLotPreview = {
      sourceLotId: 'source-1',
      sourceLotName: 'Lô lúa vụ hè 2025',
      farmAreaId: 'area-1',
      farmAreaName: 'Vùng trồng số 1',
      productCategoryId: 'cat-1',
      productCategoryName: 'Lúa',
      name: 'Lô lúa vụ hè 2025',
      expectedQuantity: 1000,
      expectedQuantityUnit: 'kg',
      plantingDate: '2025-05-01',
      activeCertifications: [
        { id: 'cert-1', name: 'VietGAP', code: 'VG-001', expiryDate: '2027-01-01' },
      ],
      skippedCertifications: [
        { id: 'cert-2', name: 'GlobalGAP', code: 'GG-002', expiryDate: '2024-01-01' },
      ],
      warnings: [
        "Chứng nhận 'GlobalGAP' đã hết hạn nên không được sao chép sang lô mới.",
      ],
    };
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: { data: preview } });

    const result = await getCloneProductionLotPreview('source-1');

    expect(apiClient.get).toHaveBeenCalledOnce();
    expect(apiClient.get).toHaveBeenCalledWith(
      '/production-lots/source-1/clone-preview',
    );
    expect(result).toEqual(preview);
    expect(result.skippedCertifications).toHaveLength(1);
    expect(result.warnings).toHaveLength(1);
  });

  it('gửi clone request tới đúng endpoint kèm cảnh báo chứng nhận hết hạn', async () => {
    const response: CloneProductionLotResponse = {
      lot: {
        id: 'new-lot-1',
        farmAreaId: 'area-1',
        productCategoryId: 'cat-1',
        organizationName: 'HTX Test',
        farmAreaName: 'Vùng trồng số 1',
        productCategoryName: 'Lúa',
        name: 'Lô lúa vụ đông xuân 2026',
        expectedQuantity: 1200,
        expectedQuantityUnit: 'kg',
        actualQuantity: null,
        plantingDate: '2026-01-10',
        harvestDate: null,
        status: 'DRAFT',
        approvalNotes: null,
        createdByName: 'Nguyen Van A',
        approvedByName: null,
        createdAt: '2026-01-10T00:00:00',
        updatedAt: '2026-01-10T00:00:00',
      },
      copiedCertifications: [
        { id: 'cert-1', name: 'VietGAP', code: 'VG-001', expiryDate: '2027-01-01' },
      ],
      skippedCertifications: [
        { id: 'cert-2', name: 'GlobalGAP', code: 'GG-002', expiryDate: '2024-01-01' },
      ],
      warnings: [
        "Chứng nhận 'GlobalGAP' đã hết hạn nên không được sao chép sang lô mới.",
      ],
    };
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { data: response } });

    const payload = {
      name: 'Lô lúa vụ đông xuân 2026',
      expectedQuantity: 1200,
      expectedQuantityUnit: 'kg',
      plantingDate: '2026-01-10',
    };
    const result = await cloneProductionLot('source-1', payload);

    expect(apiClient.post).toHaveBeenCalledOnce();
    expect(apiClient.post).toHaveBeenCalledWith(
      '/production-lots/source-1/clone',
      payload,
    );
    expect(result.lot.status).toBe('DRAFT');
    expect(result.copiedCertifications).toHaveLength(1);
    expect(result.skippedCertifications).toHaveLength(1);
  });
});
