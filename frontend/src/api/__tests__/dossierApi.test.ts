import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/api/axiosConfig', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import apiClient from '@/api/axiosConfig';
import { exportBatchDossier, exportDossier, exportGs1Dossier } from '@/api/dossierApi';

function createAxiosError(data?: unknown) {
  return {
    isAxiosError: true,
    message: 'Request failed',
    response: data === undefined ? undefined : { data, status: 500 },
  };
}

describe('dossierApi export errors', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('giữ nguyên lỗi mạng khi xuất hồ sơ', async () => {
    const error = createAxiosError();
    vi.mocked(apiClient.get).mockRejectedValueOnce(error);

    await expect(exportDossier('shipment-1')).rejects.toBe(error);
  });

  it('giữ nguyên lỗi khi phản hồi không phải Blob', async () => {
    const error = createAxiosError({ message: 'Server error' });
    vi.mocked(apiClient.get).mockRejectedValueOnce(error);

    await expect(exportGs1Dossier('shipment-1')).rejects.toBe(error);
  });

  it('giữ nguyên lỗi khi Blob không chứa JSON', async () => {
    const error = createAxiosError(new Blob(['PDF error'], { type: 'application/pdf' }));
    vi.mocked(apiClient.post).mockRejectedValueOnce(error);

    await expect(exportBatchDossier({ shipmentIds: ['shipment-1'] })).rejects.toBe(error);
  });

  it('đọc thông báo từ Blob JSON và giữ lỗi gốc làm nguyên nhân', async () => {
    const error = createAxiosError(
      new Blob([JSON.stringify({ message: 'Không đủ điều kiện xuất hồ sơ' })], {
        type: 'application/json;charset=utf-8',
      }),
    );
    vi.mocked(apiClient.get).mockRejectedValueOnce(error);

    try {
      await exportDossier('shipment-1');
      throw new Error('Expected exportDossier to reject');
    } catch (result) {
      expect(result).toBeInstanceOf(Error);
      expect((result as Error).message).toBe('Không đủ điều kiện xuất hồ sơ');
      expect((result as Error).cause).toBe(error);
    }
  });

  it('giữ nội dung Blob khi phản hồi khai báo JSON nhưng không parse được', async () => {
    const error = createAxiosError(
      new Blob(['Nội dung lỗi từ máy chủ'], { type: 'application/json' }),
    );
    vi.mocked(apiClient.get).mockRejectedValueOnce(error);

    await expect(exportDossier('shipment-1')).rejects.toThrow('Nội dung lỗi từ máy chủ');
  });
});
