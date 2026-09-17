import 'fake-indexeddb/auto';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { deleteDB } from 'idb';
import { ChainEventType } from '@/enums/chainEventType';

const { mockSync, mockUpload } = vi.hoisted(() => ({ mockSync: vi.fn(), mockUpload: vi.fn() }));
vi.mock('@/api/chainEventApi', () => ({ syncOfflineEvents: mockSync }));
vi.mock('@/api/attachmentApi', () => ({ uploadAttachment: mockUpload }));
vi.mock('sonner', () => ({ toast: { success: vi.fn(), info: vi.fn(), warning: vi.fn(), error: vi.fn() } }));

import {
  DB_NAME, capNhatTrangThai, demSoBanGhiCho, dongDbChoKiemThu,
  layMotNhatKyCho, themNhatKyCho, luuTepDinhKem, layTepTheoNhatKy,
} from '@/lib/offline/farmLogDb';
import { dongBoNhatKyCho } from '@/lib/offline/farmLogSync';
import { luuNhatKyKemTep } from '@/lib/offline/farmLogAttachmentQueue';

const taoNhatKyMoi = () => ({
  productionLotId: '85d91b0c-c3b8-4c1f-bcb0-2b86737d1406',
  eventType: ChainEventType.FARM_LOG, recordedAt: '2026-09-16T08:30:00',
  latitude: 0, longitude: 0, images: [], deviceSource: 'WEB',
  eventData: { activityType: 'FERTILIZING', executedDate: '2026-09-15' },
});
const phanHoi = (id: string, status = 'SUCCESS', eventId?: string) => ({
  results: [{ offlineEventId: id, status, eventId, message: status === 'FAILED' ? 'Lô đã hủy' : undefined }],
});
const themAnh = (id: string) => luuTepDinhKem({
  offlineEventId: id, ten: 'anh.jpg', loai: 'image/jpeg', kichThuoc: 3,
  blob: new Blob(['anh'], { type: 'image/jpeg' }),
});

beforeEach(async () => {
  dongDbChoKiemThu();
  await deleteDB(DB_NAME);
  mockSync.mockReset();
  mockUpload.mockReset();
});

describe('dongBoNhatKyCho', () => {
  it('xóa bản ghi sau SUCCESS', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    mockSync.mockResolvedValue(phanHoi(id));
    expect(await dongBoNhatKyCho()).toMatchObject({ thanhCong: 1, trung: 0, thatBai: 0 });
    expect(await demSoBanGhiCho()).toBe(0);
  });
  it('xóa bản ghi sau DUPLICATE mà không tạo lại nội dung', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    mockSync.mockResolvedValue(phanHoi(id, 'DUPLICATE'));
    expect((await dongBoNhatKyCho()).trung).toBe(1);
    expect(await demSoBanGhiCho()).toBe(0);
  });
  it('giữ lỗi nghiệp vụ cùng lý do sau nhiều lượt đồng bộ', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    mockSync.mockResolvedValue(phanHoi(id, 'FAILED'));
    expect((await dongBoNhatKyCho()).choXuLy).toBe(1);
    for (let i = 0; i < 5; i++) await dongBoNhatKyCho();
    expect(await layMotNhatKyCho(id)).toMatchObject({ status: 'invalid', errorMessage: 'Lô đã hủy', retryCount: 0 });
    expect(mockSync).toHaveBeenCalledTimes(1);
  });
  it('giữ bản ghi khi lỗi mạng và khi hết lượt tự động', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    mockSync.mockRejectedValue(new Error('Network Error'));
    await dongBoNhatKyCho();
    expect(await layMotNhatKyCho(id)).toMatchObject({ status: 'failed', retryCount: 1 });
    await capNhatTrangThai(id, 'failed', 'Network Error', 3);
    await dongBoNhatKyCho();
    expect(await demSoBanGhiCho()).toBe(1);
    expect(mockSync).toHaveBeenCalledTimes(1);
  });
  it('bỏ qua bản ghi đang backoff', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    await capNhatTrangThai(id, 'failed', 'Lỗi tạm', 1);
    await dongBoNhatKyCho();
    expect(mockSync).not.toHaveBeenCalled();
    expect(await demSoBanGhiCho()).toBe(1);
  });
  it('không gửi khi hàng chờ trống', async () => {
    expect(await dongBoNhatKyCho()).toMatchObject({ thanhCong: 0, thatBai: 0 });
    expect(mockSync).not.toHaveBeenCalled();
  });
  it('gửi lần lượt ba bản ghi, giữ nguyên lô và ngày thực hiện', async () => {
    for (let i = 0; i < 3; i++) await themNhatKyCho({ ...taoNhatKyMoi(), productionLotId: `lo-${i}` });
    mockSync.mockImplementation(async ({ events }) => {
      expect(events).toHaveLength(1);
      expect(events[0].eventData.executedDate).toBe('2026-09-15');
      return phanHoi(events[0].offlineEventId);
    });
    expect((await dongBoNhatKyCho()).thanhCong).toBe(3);
    expect(new Set(mockSync.mock.calls.map(([r]) => r.events[0].productionLotId)).size).toBe(3);
    expect(await demSoBanGhiCho()).toBe(0);
  });
  it('giữ ảnh khi phản hồi thiếu ID, không dò ID theo nội dung', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    await themAnh(id);
    mockSync.mockResolvedValue(phanHoi(id, 'DUPLICATE'));
    await dongBoNhatKyCho();
    expect(await layMotNhatKyCho(id)).toMatchObject({ status: 'invalid' });
    expect(await layTepTheoNhatKy(id)).toHaveLength(1);
    expect(mockUpload).not.toHaveBeenCalled();
  });
  it('ảnh lỗi được giữ; lượt sau chỉ tải ảnh vào đúng ID', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());
    await themAnh(id);
    mockSync.mockResolvedValue(phanHoi(id, 'SUCCESS', 'farm-1'));
    mockUpload.mockRejectedValueOnce(new Error('Network Error'));
    await dongBoNhatKyCho();
    expect(await layMotNhatKyCho(id)).toMatchObject({ status: 'da-ghi', farmLogId: 'farm-1' });
    mockUpload.mockResolvedValue({});
    await dongBoNhatKyCho();
    expect(mockSync).toHaveBeenCalledTimes(1);
    expect(mockUpload).toHaveBeenLastCalledWith('farm-1', expect.any(File));
    expect(await demSoBanGhiCho()).toBe(0);
  });
  it('lưu nội dung và chứng từ cùng giao dịch cho form hiện có', async () => {
    const id = await luuNhatKyKemTep(taoNhatKyMoi(), [new File(['pdf'], 'ct.pdf', { type: 'application/pdf' })]);
    expect(await layMotNhatKyCho(id)).toMatchObject({ status: 'pending', eventData: taoNhatKyMoi().eventData });
    expect(await layTepTheoNhatKy(id)).toHaveLength(1);
  });
});
