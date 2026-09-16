import 'fake-indexeddb/auto';
import { beforeEach, describe, expect, it } from 'vitest';
import { deleteDB } from 'idb';
import { ChainEventType } from '@/enums/chainEventType';
import {
  DB_NAME,
  MAX_BAN_GHI_CHO,
  capNhatTrangThai,
  demSoBanGhiCho,
  dongDbChoKiemThu,
  kiemTraHanDanhMuc,
  layDanhSachCho,
  layLoDuocPhanCong,
  layMotNhatKyCho,
  luuLoDuocPhanCong,
  themNhatKyCho,
  xoaBanGhiLoi,
  xoaNhatKyCho,
  type NhatKyChoMoi,
} from '@/lib/offline/farmLogDb';

const taoNhatKyMoi = (ghiDe?: Partial<NhatKyChoMoi>): NhatKyChoMoi => ({
  productionLotId: '85d91b0c-c3b8-4c1f-bcb0-2b86737d1406',
  eventType: ChainEventType.FARM_LOG,
  recordedAt: '2026-09-16T08:30:00',
  latitude: 20.985412,
  longitude: 105.798541,
  images: [],
  deviceSource: 'WEB',
  eventData: {
    activityType: 'FERTILIZING',
    material: 'NPK 16-16-8',
    quantity: 25.0,
    unit: 'kg',
    executedDate: '2026-09-15',
    notes: 'Bón phân lần 1',
  },
  ...ghiDe,
});

beforeEach(async () => {
  dongDbChoKiemThu();
  await deleteDB(DB_NAME);
});

describe('farmLogDb', () => {
  it('thêm nhật ký chờ và đọc lại được', async () => {
    const id = await themNhatKyCho(taoNhatKyMoi());

    expect(id).toBeTruthy();
    expect(await demSoBanGhiCho()).toBe(1);

    const banGhi = await layMotNhatKyCho(id);
    expect(banGhi?.status).toBe('pending');
    expect(banGhi?.eventData.activityType).toBe('FERTILIZING');
  });

  it('chặn bản ghi thứ 101 khi hàng chờ đã đầy', async () => {
    for (let i = 0; i < MAX_BAN_GHI_CHO; i += 1) {
      await themNhatKyCho(taoNhatKyMoi());
    }

    await expect(themNhatKyCho(taoNhatKyMoi())).rejects.toThrow(/đã đầy/);
    expect(await demSoBanGhiCho()).toBe(MAX_BAN_GHI_CHO);
  });

  it('cập nhật trạng thái và lọc theo trạng thái', async () => {
    const id1 = await themNhatKyCho(taoNhatKyMoi());
    await themNhatKyCho(taoNhatKyMoi());

    await capNhatTrangThai(id1, 'failed', 'Lô đã hủy');

    expect((await layMotNhatKyCho(id1))?.errorMessage).toBe('Lô đã hủy');
    expect(await layDanhSachCho('failed')).toHaveLength(1);
    expect(await layDanhSachCho('pending')).toHaveLength(1);
    expect(await layDanhSachCho()).toHaveLength(2);
  });

  it('xóa một bản ghi và xóa toàn bộ bản ghi lỗi', async () => {
    const id1 = await themNhatKyCho(taoNhatKyMoi());
    const id2 = await themNhatKyCho(taoNhatKyMoi());
    await capNhatTrangThai(id1, 'failed', 'Lỗi');
    await capNhatTrangThai(id2, 'failed', 'Lỗi khác');

    await xoaNhatKyCho(id1);
    expect(await demSoBanGhiCho()).toBe(1);

    expect(await xoaBanGhiLoi()).toBe(1);
    expect(await demSoBanGhiCho()).toBe(0);
  });

  it('lưu và đọc cache lô, còn hạn sau khi lưu', async () => {
    await luuLoDuocPhanCong([
      { id: 'lo-1', ten: 'Lô chè xuân', trangThai: 'APPROVED' },
      { id: 'lo-2', ten: 'Lô lúa hè', trangThai: 'HARVESTED' },
    ]);

    const danhSach = await layLoDuocPhanCong();
    expect(danhSach).toHaveLength(2);

    const han = await kiemTraHanDanhMuc();
    expect(han.conHan).toBe(true);
  });

  it('báo hết hạn khi chưa từng tải lô', async () => {
    const han = await kiemTraHanDanhMuc();
    expect(han.conHan).toBe(false);
    expect(han.soNgayConLai).toBe(0);
  });
});
