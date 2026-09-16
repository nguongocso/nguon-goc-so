import { openDB, type IDBPDatabase } from 'idb';
import { v4 as uuidv4 } from 'uuid';
import type { OfflineEvent } from '@/types/offlineEvent';

/**
 * Lớp lưu trữ IndexedDB cho nhật ký canh tác ngoại tuyến (NCL-10-CN-012, MVP).
 *
 * - DB `nong-san-offline`, version 1.
 * - Chỉ lưu dữ liệu văn bản; ảnh/đính kèm để phase 2.
 * - Hàng chờ tối đa 100 bản ghi; cache lô hết hạn sau 7 ngày.
 */

export const DB_NAME = 'nong-san-offline';
export const DB_VERSION = 1;

/** Số bản ghi chờ tối đa trong hàng chờ ngoại tuyến. */
export const MAX_BAN_GHI_CHO = 100;

/** Thời hạn hiệu lực của cache lô được phân công (ngày). */
export const TTL_LO_NGAY = 7;

const STORE_NHAT_KY_CHO = 'nhat-ky-cho';
const STORE_LO_CACHE = 'lo-cache';
const STORE_CAU_HINH = 'cau-hinh';

const KHOA_LAN_DONG_BO_LO = 'lan-dong-bo-lo';

/** Lô sản xuất được phân công, lưu gọn để chọn khi ngoại tuyến. */
export interface LoDuocPhanCong {
  id: string;
  ten: string;
  trangThai: string;
}

/** Dữ liệu đầu vào khi ghi nhật ký chờ (chưa có id/trạng thái). */
export type NhatKyChoMoi = Omit<OfflineEvent, 'offlineEventId' | 'status' | 'retryCount'> & {
  offlineEventId?: string;
};

/** Kết quả kiểm tra hạn hiệu lực của cache lô. */
export interface HanDanhMuc {
  conHan: boolean;
  soNgayConLai: number;
}

interface CauHinh {
  key: string;
  giaTri: string;
}

type FarmLogDb = IDBPDatabase<{
  [STORE_NHAT_KY_CHO]: {
    key: string;
    value: OfflineEvent;
  };
  [STORE_LO_CACHE]: {
    key: string;
    value: LoDuocPhanCong;
  };
  [STORE_CAU_HINH]: {
    key: string;
    value: CauHinh;
  };
}>;

let dbPromise: Promise<FarmLogDb> | null = null;
let dbHienTai: FarmLogDb | null = null;

/**
 * Khởi tạo (mở) IndexedDB. Gọi ngầm bởi mọi API bên dưới.
 */
export function moDb(): Promise<FarmLogDb> {
  if (!dbPromise) {
    dbPromise = openDB(DB_NAME, DB_VERSION, {
      upgrade(db) {
        if (!db.objectStoreNames.contains(STORE_NHAT_KY_CHO)) {
          db.createObjectStore(STORE_NHAT_KY_CHO, { keyPath: 'offlineEventId' });
        }
        if (!db.objectStoreNames.contains(STORE_LO_CACHE)) {
          db.createObjectStore(STORE_LO_CACHE, { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains(STORE_CAU_HINH)) {
          db.createObjectStore(STORE_CAU_HINH, { keyPath: 'key' });
        }
      },
    }).then((db) => {
      dbHienTai = db;
      return db;
    });
  }
  return dbPromise;
}

/**
 * Đóng kết nối hiện tại (chỉ dùng cho kiểm thử).
 * @internal
 */
export function dongDbChoKiemThu(): void {
  dbHienTai?.close();
  dbHienTai = null;
  dbPromise = null;
}

function laLoiHetDungLuong(error: unknown): boolean {
  return (
    error instanceof DOMException &&
    (error.name === 'QuotaExceededError' || error.name === 'NS_ERROR_DOM_QUOTA_REACHED')
  );
}

/**
 * Thêm một nhật ký vào hàng chờ đồng bộ.
 *
 * @returns offlineEventId của bản ghi vừa tạo.
 * @throws Lỗi khi hàng chờ đã đầy 100 bản ghi hoặc hết dung lượng lưu trữ.
 */
export async function themNhatKyCho(nhatKy: NhatKyChoMoi): Promise<string> {
  const db = await moDb();
  const soBanGhi = await db.count(STORE_NHAT_KY_CHO);
  if (soBanGhi >= MAX_BAN_GHI_CHO) {
    throw new Error(
      `Hàng chờ đã đầy (${MAX_BAN_GHI_CHO} bản ghi). Vui lòng kết nối mạng để đồng bộ trước khi ghi tiếp.`,
    );
  }
  const banGhi: OfflineEvent = {
    ...nhatKy,
    offlineEventId: nhatKy.offlineEventId ?? uuidv4(),
    status: 'pending',
    retryCount: 0,
  };
  try {
    await db.add(STORE_NHAT_KY_CHO, banGhi);
  } catch (error) {
    if (laLoiHetDungLuong(error)) {
      throw new Error('Bộ nhớ thiết bị đã đầy. Vui lòng đồng bộ để giải phóng dung lượng.');
    }
    throw error;
  }
  return banGhi.offlineEventId;
}

/**
 * Lấy danh sách bản ghi chờ, mới nhất trước.
 */
export async function layDanhSachCho(
  trangThai?: OfflineEvent['status'],
): Promise<OfflineEvent[]> {
  const db = await moDb();
  const tatCa = await db.getAll(STORE_NHAT_KY_CHO);
  const loc = trangThai ? tatCa.filter((b) => b.status === trangThai) : tatCa;
  return loc.sort((a, b) => b.recordedAt.localeCompare(a.recordedAt));
}

/**
 * Lấy một bản ghi chờ theo offlineEventId.
 */
export async function layMotNhatKyCho(offlineEventId: string): Promise<OfflineEvent | undefined> {
  const db = await moDb();
  return db.get(STORE_NHAT_KY_CHO, offlineEventId);
}

/**
 * Cập nhật trạng thái (và lý do lỗi) của một bản ghi chờ.
 */
export async function capNhatTrangThai(
  offlineEventId: string,
  trangThai: NonNullable<OfflineEvent['status']>,
  lyDo?: string,
): Promise<void> {
  const db = await moDb();
  const tx = db.transaction(STORE_NHAT_KY_CHO, 'readwrite');
  const banGhi = await tx.store.get(offlineEventId);
  if (!banGhi) return;
  banGhi.status = trangThai;
  if (lyDo !== undefined) banGhi.errorMessage = lyDo;
  if (trangThai === 'syncing') banGhi.lastSyncAttempt = Date.now();
  await tx.store.put(banGhi);
  await tx.done;
}

/**
 * Xóa một bản ghi chờ khỏi hàng chờ.
 */
export async function xoaNhatKyCho(offlineEventId: string): Promise<void> {
  const db = await moDb();
  await db.delete(STORE_NHAT_KY_CHO, offlineEventId);
}

/**
 * Đếm số bản ghi trong hàng chờ.
 */
export async function demSoBanGhiCho(): Promise<number> {
  const db = await moDb();
  return db.count(STORE_NHAT_KY_CHO);
}

/**
 * Xóa toàn bộ bản ghi lỗi (failed) khỏi hàng chờ.
 *
 * @returns Số bản ghi đã xóa.
 */
export async function xoaBanGhiLoi(): Promise<number> {
  const db = await moDb();
  const tx = db.transaction(STORE_NHAT_KY_CHO, 'readwrite');
  const banLoi = (await tx.store.getAll()).filter((b) => b.status === 'failed');
  for (const banGhi of banLoi) {
    await tx.store.delete(banGhi.offlineEventId);
  }
  await tx.done;
  return banLoi.length;
}

/**
 * Lưu danh sách lô được phân công (gọi khi online) và đánh dấu thời điểm tải.
 */
export async function luuLoDuocPhanCong(danhSach: LoDuocPhanCong[]): Promise<void> {
  const db = await moDb();
  const tx = db.transaction([STORE_LO_CACHE, STORE_CAU_HINH], 'readwrite');
  await tx.objectStore(STORE_LO_CACHE).clear();
  for (const lo of danhSach) {
    await tx.objectStore(STORE_LO_CACHE).put(lo);
  }
  await tx.objectStore(STORE_CAU_HINH).put({
    key: KHOA_LAN_DONG_BO_LO,
    giaTri: new Date().toISOString(),
  });
  await tx.done;
}

/**
 * Lấy danh sách lô đã lưu để chọn khi ngoại tuyến.
 */
export async function layLoDuocPhanCong(): Promise<LoDuocPhanCong[]> {
  const db = await moDb();
  return db.getAll(STORE_LO_CACHE);
}

/**
 * Kiểm tra cache lô còn hạn 7 ngày không.
 */
export async function kiemTraHanDanhMuc(): Promise<HanDanhMuc> {
  const db = await moDb();
  const cauHinh = await db.get(STORE_CAU_HINH, KHOA_LAN_DONG_BO_LO);
  if (!cauHinh) return { conHan: false, soNgayConLai: 0 };
  const soNgayDaQua =
    (Date.now() - new Date(cauHinh.giaTri).getTime()) / (1000 * 60 * 60 * 24);
  const soNgayConLai = TTL_LO_NGAY - soNgayDaQua;
  return { conHan: soNgayConLai > 0, soNgayConLai: Math.max(0, Math.floor(soNgayConLai)) };
}

/**
 * Ước tính dung lượng lưu trữ đã dùng (nếu trình duyệt hỗ trợ).
 */
export async function uocTinhDungLuong(): Promise<{ daDung: number; tong: number } | null> {
  if (!('storage' in navigator) || !navigator.storage.estimate) return null;
  const { usage = 0, quota = 0 } = await navigator.storage.estimate();
  return { daDung: usage, tong: quota };
}
