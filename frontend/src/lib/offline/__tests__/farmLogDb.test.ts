import 'fake-indexeddb/auto';
import { beforeEach, describe, expect, it } from 'vitest';
import { deleteDB } from 'idb';
import {
  DB_NAME,
  demTepCho,
  dongDbChoKiemThu,
  kiemTraHanDanhMuc,
  kiemTraHanTatCaDanhMuc,
  layDanhMucHoatDong,
  layDanhMucVatTu,
  layLoDuocPhanCong,
  layTepTheoNhatKy,
  luuDanhMucHoatDong,
  luuDanhMucVatTu,
  luuLoDuocPhanCong,
  luuTepDinhKem,
  xoaTepTheoNhatKy,
} from '@/lib/offline/farmLogDb';

beforeEach(async () => {
  dongDbChoKiemThu();
  await deleteDB(DB_NAME);
});

describe('farmLogDb (danh mục tải sẵn)', () => {
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

  it('lưu danh mục vật tư và loại hoạt động, đủ cả ba mới cho ghi ngoại tuyến', async () => {
    await luuLoDuocPhanCong([{ id: 'lo-1', ten: 'Lô chè xuân', trangThai: 'APPROVED' }]);
    await luuDanhMucVatTu([
      { id: 'vt-1', ten: 'NPK', donVi: 'kg', nhomVatTu: 'PHAN_BON', soNgayCachLy: 7 },
    ]);
    await luuDanhMucHoatDong([{ ma: 'FERTILIZING', nhan: 'Bón phân' }]);

    expect(await layDanhMucVatTu()).toHaveLength(1);
    expect(await layDanhMucHoatDong()).toHaveLength(1);

    const han = await kiemTraHanTatCaDanhMuc();
    expect(han.conHan).toBe(true);
    expect(han.thieu).toHaveLength(0);
  });
});

describe('farmLogDb (kho ảnh đính kèm pha 2)', () => {
  const tepMoi = (offlineEventId: string) => ({
    offlineEventId,
    ten: 'anh.jpg',
    loai: 'image/jpeg',
    kichThuoc: 3,
    blob: new Blob(['anh'], { type: 'image/jpeg' }),
  });

  it('lưu và đọc ảnh theo nhật ký', async () => {
    await luuTepDinhKem(tepMoi('evt-1'));
    await luuTepDinhKem(tepMoi('evt-2'));

    expect(await layTepTheoNhatKy('evt-1')).toHaveLength(1);
    expect(await demTepCho()).toBe(2);
  });

  it('xóa toàn bộ ảnh của một nhật ký sau khi gửi xong', async () => {
    await luuTepDinhKem(tepMoi('evt-1'));
    await luuTepDinhKem(tepMoi('evt-1'));

    expect(await xoaTepTheoNhatKy('evt-1')).toBe(2);
    expect(await demTepCho()).toBe(0);
  });
});
