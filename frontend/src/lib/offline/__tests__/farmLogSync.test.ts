import 'fake-indexeddb/auto';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { deleteDB } from 'idb';
import { ChainEventType } from '@/enums/chainEventType';
import type { OfflineEvent } from '@/types/offlineEvent';

const mockSyncOfflineEvents = vi.fn();

vi.mock('@/api/chainEventApi', () => ({
  syncOfflineEvents: (...args: unknown[]) => mockSyncOfflineEvents(...args),
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), info: vi.fn(), warning: vi.fn(), error: vi.fn() },
}));

import {
  DB_NAME,
  capNhatTrangThai,
  demSoBanGhiCho,
  dongDbChoKiemThu,
  layMotNhatKyCho,
  themNhatKyCho,
} from '@/lib/offline/farmLogDb';
import { dongBoNhatKyCho } from '@/lib/offline/farmLogSync';

const taoNhatKyMoi = () => ({
  productionLotId: '85d91b0c-c3b8-4c1f-bcb0-2b86737d1406',
  eventType: ChainEventType.FARM_LOG,
  recordedAt: '2026-09-16T08:30:00',
  latitude: 0,
  longitude: 0,
  images: [],
  deviceSource: 'MOBILE',
  eventData: {
    activityType: 'FERTILIZING',
    executedDate: '2026-09-15',
  },
});

const ketQuaThanhCong = (id: string) => ({
  syncId: 'sync-1',
  totalEvents: 1,
  successCount: 1,
  duplicateCount: 0,
  failedCount: 0,
  results: [{ offlineEventId: id, status: 'SUCCESS' as const }],
});

beforeEach(async () => {
  dongDbChoKiemThu();
  await deleteDB(DB_NAME);
  mockSyncOfflineEvents.mockReset();
});

describe('dongBoNhatKyCho', () => {
  it('xóa bản ghi và báo thành công khi server SUCCESS', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    mockSyncOfflineEvents.mockResolvedValue(ketQuaThanhCong(id));

    const ketQua = await dongBoNhatKyCho();

    expect(ketQua).toEqual({ thanhCong: 1, trung: 0, thatBai: 0, choXuLy: 0, anhDaGui: 0, anhCho: 0 });
    expect(await demSoBanGhiCho()).toBe(0);
  });

  it('xóa bản ghi khi server báo DUPLICATE (chống trùng)', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    mockSyncOfflineEvents.mockResolvedValue({
      syncId: 'sync-1',
      totalEvents: 1,
      successCount: 0,
      duplicateCount: 1,
      failedCount: 0,
      results: [{ offlineEventId: id, status: 'DUPLICATE' as const }],
    });

    const ketQua = await dongBoNhatKyCho();

    expect(ketQua.trung).toBe(1);
    expect(await demSoBanGhiCho()).toBe(0);
  });

  it('giữ lại bản ghi failed kèm lý do khi lỗi nghiệp vụ', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    mockSyncOfflineEvents.mockResolvedValue({
      syncId: 'sync-1',
      totalEvents: 1,
      successCount: 0,
      duplicateCount: 0,
      failedCount: 1,
      results: [{ offlineEventId: id, status: 'FAILED' as const, message: 'Lô đã hủy' }],
    });

    const ketQua = await dongBoNhatKyCho();

    expect(ketQua.choXuLy).toBe(1);
    const banGhi: OfflineEvent | undefined = await layMotNhatKyCho(id);
    expect(banGhi?.status).toBe('invalid');
    expect(banGhi?.errorMessage).toBe('Lô đã hủy');
    expect(banGhi?.retryCount).toBe(0);
  });

  it('tăng lượt thử và giữ hàng chờ khi lỗi mạng', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    mockSyncOfflineEvents.mockRejectedValue(new Error('Network Error'));

    await dongBoNhatKyCho();

    const banGhi: OfflineEvent | undefined = await layMotNhatKyCho(id);
    expect(banGhi?.status).toBe('failed');
    expect(banGhi?.retryCount).toBe(1);
  });

  it('bỏ qua bản ghi đang trong thời gian backoff', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    await capNhatTrangThai(id, 'failed', 'Lỗi tạm', 1);

    const ketQua = await dongBoNhatKyCho();

    expect(mockSyncOfflineEvents).not.toHaveBeenCalled();
    expect(ketQua).toEqual({ thanhCong: 0, trung: 0, thatBai: 0, choXuLy: 0, anhDaGui: 0, anhCho: 0 });
    expect(await demSoBanGhiCho()).toBe(1);
  });

  it('không làm gì khi hàng chờ trống', async () => {
    const ketQua = await dongBoNhatKyCho();

    expect(mockSyncOfflineEvents).not.toHaveBeenCalled();
    expect(ketQua).toEqual({ thanhCong: 0, trung: 0, thatBai: 0, choXuLy: 0, anhDaGui: 0, anhCho: 0 });
  });
});
