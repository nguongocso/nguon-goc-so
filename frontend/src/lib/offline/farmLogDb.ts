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
export const DB_VERSION = 2;

/** Số bản ghi chờ tối đa trong hàng chờ ngoại tuyến. */
export const MAX_BAN_GHI_CHO = 100;

/**
 * Thời hạn hiệu lực của danh mục tải sẵn về thiết bị (ngày).
 * Áp dụng cho cả ba danh mục: lô sản xuất, vật tư và loại hoạt động.
 */
export const TTL_DANH_MUC_NGAY = 7;

/** @deprecated Dùng `TTL_DANH_MUC_NGAY` — giữ tên cũ để tương thích. */
export const TTL_LO_NGAY = TTL_DANH_MUC_NGAY;

/** Số ảnh tối đa lưu cho một nhật ký ghi ngoại tuyến. */
export const MAX_ANH_MOI_NHAT_KY = 5;

/** Bản ghi ở trạng thái `syncing` lâu hơn mốc này được coi là bị treo (đóng tab giữa lúc gửi). */
export const SYNCHING_TREO_MS = 2 * 60 * 1000;

const STORE_NHAT_KY_CHO = 'nhat-ky-cho';
const STORE_LO_CACHE = 'lo-cache';
const STORE_CAU_HINH = 'cau-hinh';
const STORE_VAT_TU_CACHE = 'vat-tu-cache';
const STORE_HOAT_DONG_CACHE = 'hoat-dong-cache';
const STORE_TEP_DINH_KEM = 'tep-dinh-kem';

const KHOA_LAN_DONG_BO_LO = 'lan-dong-bo-lo';
const KHOA_LAN_DONG_BO_VAT_TU = 'lan-dong-bo-vat-tu';
const KHOA_LAN_DONG_BO_HOAT_DONG = 'lan-dong-bo-hoat-dong';

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

/** Danh mục vật tư tải sẵn để chọn khi ngoại tuyến. */
export interface VatTuCache {
  id: string;
  ten: string;
  donVi: string;
  nhomVatTu: string;
  /** Số ngày cách ly của vật tư (0 = không yêu cầu). */
  soNgayCachLy: number;
}

/** Loại hoạt động canh tác tải sẵn (mã + nhãn tiếng Việt). */
export interface HoatDongCache {
  ma: string;
  nhan: string;
}

/** Ảnh/đính kèm chờ tải lên máy chủ sau khi nhật ký đã được ghi. */
export interface TepDinhKem {
  id: string;
  offlineEventId: string;
  ten: string;
  loai: string;
  kichThuoc: number;
  blob: Blob;
  trangThai: 'cho' | 'dang-gui' | 'loi';
  lyDo?: string;
}

/** Kết quả kiểm tra hạn của toàn bộ danh mục bắt buộc. */
export interface HanTatCaDanhMuc {
  conHan: boolean;
  soNgayConLai: number;
  /** Các danh mục chưa tải hoặc đã hết hạn. */
  thieu: string[];
}

interface CauHinh {
  key: string;
  giaTri: string;
}

export interface FarmLogDbSchema {
  'nhat-ky-cho': {
    key: string;
    value: OfflineEvent;
  };
  'lo-cache': {
    key: string;
    value: LoDuocPhanCong;
  };
  'cau-hinh': {
    key: string;
    value: CauHinh;
  };
  'vat-tu-cache': {
    key: string;
    value: VatTuCache;
  };
  'hoat-dong-cache': {
    key: string;
    value: HoatDongCache;
  };
  'tep-dinh-kem': {
    key: string;
    value: TepDinhKem;
    indexes: { 'theo-nhat-ky': string };
  };
}

export type FarmLogDb = IDBPDatabase<FarmLogDbSchema>;

let dbPromise: Promise<FarmLogDb> | null = null;
let dbHienTai: FarmLogDb | null = null;

/**
 * Khởi tạo (mở) IndexedDB. Gọi ngầm bởi mọi API bên dưới.
 */
export function moDb(): Promise<FarmLogDb> {
  if (!dbPromise) {
    dbPromise = openDB<FarmLogDbSchema>(DB_NAME, DB_VERSION, {
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
        if (!db.objectStoreNames.contains(STORE_VAT_TU_CACHE)) {
          db.createObjectStore(STORE_VAT_TU_CACHE, { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains(STORE_HOAT_DONG_CACHE)) {
          db.createObjectStore(STORE_HOAT_DONG_CACHE, { keyPath: 'ma' });
        }
        if (!db.objectStoreNames.contains(STORE_TEP_DINH_KEM)) {
          const store = db.createObjectStore(STORE_TEP_DINH_KEM, {
            keyPath: 'id',
          });
          store.createIndex('theo-nhat-ky', 'offlineEventId');
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

/** Lưu nguyên tử nội dung và tệp; không để đồng bộ nhìn thấy bản ghi chưa đủ ảnh. */
export async function luuNhatKyVaTepNguyenTu(
  nhatKy: NhatKyChoMoi & { offlineEventId: string },
  danhSachTep: TepDinhKem[],
): Promise<void> {
  const db = await moDb();
  const tx = db.transaction([STORE_NHAT_KY_CHO, STORE_TEP_DINH_KEM], 'readwrite');
  try {
    const store = tx.objectStore(STORE_NHAT_KY_CHO);
    if (await store.count() >= MAX_BAN_GHI_CHO) {
      throw new Error(`Hàng chờ đã đầy (${MAX_BAN_GHI_CHO} bản ghi). Vui lòng đồng bộ trước khi ghi tiếp.`);
    }
    await store.add({ ...nhatKy, status: 'pending', retryCount: 0 });
    for (const tep of danhSachTep) await tx.objectStore(STORE_TEP_DINH_KEM).add(tep);
    await tx.done;
  } catch (error) {
    try { tx.abort(); } catch { /* Giao dịch có thể đã tự hủy khi hết dung lượng. */ }
    await tx.done.catch(() => undefined);
    if (laLoiHetDungLuong(error)) throw new Error('Bộ nhớ thiết bị đã đầy. Nhật ký chưa được lưu, vui lòng giữ lại nội dung trên form.');
    throw error;
  }
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
 * Cập nhật trạng thái (và lý do lỗi, số lần thử) của một bản ghi chờ.
 */
export async function capNhatTrangThai(
  offlineEventId: string,
  trangThai: NonNullable<OfflineEvent['status']>,
  lyDo?: string,
  lanThuLai?: number,
): Promise<void> {
  const db = await moDb();
  const tx = db.transaction(STORE_NHAT_KY_CHO, 'readwrite');
  const banGhi = await tx.store.get(offlineEventId);
  if (!banGhi) return;
  banGhi.status = trangThai;
  if (lyDo !== undefined) banGhi.errorMessage = lyDo;
  if (lanThuLai !== undefined) banGhi.retryCount = lanThuLai;
  if (trangThai === 'pending') banGhi.lastSyncAttempt = undefined;
  if (trangThai === 'syncing' || trangThai === 'failed') {
    banGhi.lastSyncAttempt = Date.now();
  }
  await tx.store.put(banGhi);
  await tx.done;
}

/**
 * Xóa một bản ghi chờ khỏi hàng chờ.
 */
export async function xoaNhatKyCho(offlineEventId: string): Promise<void> {
  const db = await moDb();
  const tx = db.transaction([STORE_NHAT_KY_CHO, STORE_TEP_DINH_KEM], 'readwrite');
  const teps = await tx.objectStore(STORE_TEP_DINH_KEM).index('theo-nhat-ky').getAllKeys(offlineEventId);
  for (const id of teps) await tx.objectStore(STORE_TEP_DINH_KEM).delete(id);
  await tx.objectStore(STORE_NHAT_KY_CHO).delete(offlineEventId);
  await tx.done;
}

/**
 * Đếm số bản ghi trong hàng chờ.
 */
export async function demSoBanGhiCho(): Promise<number> {
  const db = await moDb();
  return db.count(STORE_NHAT_KY_CHO);
}

/**
 * Xóa toàn bộ bản ghi lỗi (`failed`) và bản ghi không hợp lệ (`invalid`) khỏi hàng chờ.
 *
 * Bản ghi lỗi **không** bao giờ bị tự xoá trong luồng đồng bộ; chỉ xoá khi người dùng
 * chủ động bấm nút này (QTN-16: giữ lại kèm lý do không gửi được).
 *
 * @returns Số bản ghi đã xóa.
 */
export async function xoaBanGhiLoi(): Promise<number> {
  const db = await moDb();
  const tx = db.transaction(STORE_NHAT_KY_CHO, 'readwrite');
  const banLoi = (await tx.store.getAll()).filter(
    (b) => b.status === 'failed' || b.status === 'invalid',
  );
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
  return tinhHanTheoKhoa(db, KHOA_LAN_DONG_BO_LO);
}

/**
 * Tính hạn hiệu lực của một mốc đồng bộ danh mục trong cửa hàng `cau-hinh`.
 *
 * @param db   kết nối IndexedDB đang mở
 * @param khoa khoá mốc đồng bộ (ví dụ `lan-dong-bo-lo`)
 * @returns hạn còn lại tính theo ngày
 */
async function tinhHanTheoKhoa(db: FarmLogDb, khoa: string): Promise<HanDanhMuc> {
  const cauHinh = await db.get(STORE_CAU_HINH, khoa);
  if (!cauHinh) return { conHan: false, soNgayConLai: 0 };
  const soNgayDaQua =
    (Date.now() - new Date(cauHinh.giaTri).getTime()) / (1000 * 60 * 60 * 24);
  const soNgayConLai = TTL_DANH_MUC_NGAY - soNgayDaQua;
  return {
    conHan: soNgayConLai > 0,
    soNgayConLai: Math.max(0, Math.floor(soNgayConLai)),
  };
}

/**
 * Ước tính dung lượng lưu trữ đã dùng (nếu trình duyệt hỗ trợ).
 */
export async function uocTinhDungLuong(): Promise<{ daDung: number; tong: number } | null> {
  if (!('storage' in navigator) || !navigator.storage.estimate) return null;
  const { usage = 0, quota = 0 } = await navigator.storage.estimate();
  return { daDung: usage, tong: quota };
}

/**
 * Lưu danh mục vật tư tải sẵn (gọi khi online) và đánh dấu thời điểm tải.
 *
 * NCL-10-CN-012-CV-01: danh mục vật tư được tải về thiết bị khi còn mạng và có ngày hết hạn.
 */
export async function luuDanhMucVatTu(danhSach: VatTuCache[]): Promise<void> {
  const db = await moDb();
  const tx = db.transaction([STORE_VAT_TU_CACHE, STORE_CAU_HINH], 'readwrite');
  await tx.objectStore(STORE_VAT_TU_CACHE).clear();
  for (const vatTu of danhSach) {
    await tx.objectStore(STORE_VAT_TU_CACHE).put(vatTu);
  }
  await tx.objectStore(STORE_CAU_HINH).put({
    key: KHOA_LAN_DONG_BO_VAT_TU,
    giaTri: new Date().toISOString(),
  });
  await tx.done;
}

/**
 * Lấy danh mục vật tư đã tải sẵn để chọn khi ngoại tuyến (sắp xếp theo tên tiếng Việt).
 */
export async function layDanhMucVatTu(): Promise<VatTuCache[]> {
  const db = await moDb();
  const danhSach = await db.getAll(STORE_VAT_TU_CACHE);
  return danhSach.sort((a, b) => a.ten.localeCompare(b.ten, 'vi'));
}

/**
 * Lưu danh mục loại hoạt động canh tác tải sẵn và đánh dấu thời điểm tải.
 */
export async function luuDanhMucHoatDong(danhSach: HoatDongCache[]): Promise<void> {
  const db = await moDb();
  const tx = db.transaction([STORE_HOAT_DONG_CACHE, STORE_CAU_HINH], 'readwrite');
  await tx.objectStore(STORE_HOAT_DONG_CACHE).clear();
  for (const hoatDong of danhSach) {
    await tx.objectStore(STORE_HOAT_DONG_CACHE).put(hoatDong);
  }
  await tx.objectStore(STORE_CAU_HINH).put({
    key: KHOA_LAN_DONG_BO_HOAT_DONG,
    giaTri: new Date().toISOString(),
  });
  await tx.done;
}

/**
 * Lấy danh mục loại hoạt động canh tác đã tải sẵn.
 */
export async function layDanhMucHoatDong(): Promise<HoatDongCache[]> {
  const db = await moDb();
  return db.getAll(STORE_HOAT_DONG_CACHE);
}

/**
 * Kiểm tra hạn hiệu lực của **tất cả** danh mục bắt buộc (lô, vật tư, loại hoạt động).
 *
 * Dùng để quyết định có cho ghi ngoại tuyến hay không, đúng Precondition của
 * NCL-10-CN-012: "Người ghi đã đăng nhập trên thiết bị di động và đã đồng bộ danh mục
 * khi còn mạng".
 */
export async function kiemTraHanTatCaDanhMuc(): Promise<HanTatCaDanhMuc> {
  const db = await moDb();
  const [lo, vatTu, hoatDong] = await Promise.all([
    tinhHanTheoKhoa(db, KHOA_LAN_DONG_BO_LO),
    tinhHanTheoKhoa(db, KHOA_LAN_DONG_BO_VAT_TU),
    tinhHanTheoKhoa(db, KHOA_LAN_DONG_BO_HOAT_DONG),
  ]);

  const thieu: string[] = [];
  if (!lo.conHan) thieu.push('lô sản xuất');
  if (!vatTu.conHan) thieu.push('danh mục vật tư');
  if (!hoatDong.conHan) thieu.push('loại hoạt động');

  return {
    conHan: thieu.length === 0,
    soNgayConLai: Math.min(
      lo.soNgayConLai,
      vatTu.soNgayConLai,
      hoatDong.soNgayConLai,
    ),
    thieu,
  };
}

/**
 * Ghi nhận nội dung nhật ký đã lên máy chủ nhưng ảnh chưa tải xong.
 *
 * Bản ghi giữ trạng thái `da-ghi` kèm `farmLogId` để lần đồng bộ sau chỉ tải ảnh,
 * không gửi lại nội dung (tránh mất ảnh đã chụp ngoài đồng).
 */
export async function luuKetQuaDaGhi(
  offlineEventId: string,
  farmLogId: string,
): Promise<void> {
  const db = await moDb();
  const banGhi = await db.get(STORE_NHAT_KY_CHO, offlineEventId);
  if (!banGhi) return;
  banGhi.status = 'da-ghi';
  banGhi.farmLogId = farmLogId;
  banGhi.errorMessage = undefined;
  await db.put(STORE_NHAT_KY_CHO, banGhi);
}

/**
 * Thu hồi các bản ghi bị kẹt ở trạng thái `syncing` (đóng tab giữa lúc gửi).
 *
 * @returns Số bản ghi đã đưa về `pending`.
 */
export async function thuHoiBanGhiTreoSyncing(): Promise<number> {
  const db = await moDb();
  const tx = db.transaction(STORE_NHAT_KY_CHO, 'readwrite');
  const tatCa = await tx.store.getAll();
  const bayGio = Date.now();
  let soThuHoi = 0;

  for (const banGhi of tatCa) {
    if (banGhi.status !== 'syncing') continue;
    if (bayGio - (banGhi.lastSyncAttempt ?? 0) < SYNCHING_TREO_MS) continue;
    banGhi.status = 'pending';
    await tx.store.put(banGhi);
    soThuHoi += 1;
  }

  await tx.done;
  return soThuHoi;
}

/**
 * Lưu một ảnh/đính kèm chờ tải lên (trạng thái `cho`).
 *
 * @returns ID của tệp vừa lưu.
 */
export async function luuTepDinhKem(
  tep: Omit<TepDinhKem, 'id' | 'trangThai'> & { id?: string },
): Promise<string> {
  const db = await moDb();
  const banGhi: TepDinhKem = {
    ...tep,
    id: tep.id ?? uuidv4(),
    trangThai: 'cho',
  };
  try {
    await db.put(STORE_TEP_DINH_KEM, banGhi);
  } catch (error) {
    if (laLoiHetDungLuong(error)) {
      throw new Error(
        'Bộ nhớ thiết bị đã đầy. Vui lòng đồng bộ để giải phóng dung lượng trước khi chụp thêm ảnh.',
      );
    }
    throw error;
  }
  return banGhi.id;
}

/**
 * Lấy danh sách ảnh/đính kèm của một nhật ký chờ.
 */
export async function layTepTheoNhatKy(offlineEventId: string): Promise<TepDinhKem[]> {
  const db = await moDb();
  return db.getAllFromIndex(STORE_TEP_DINH_KEM, 'theo-nhat-ky', offlineEventId);
}

/**
 * Lấy toàn bộ ảnh/đính kèm còn phải tải lên (chờ hoặc lỗi).
 */
export async function layTepChuaXong(): Promise<TepDinhKem[]> {
  const db = await moDb();
  const tatCa = await db.getAll(STORE_TEP_DINH_KEM);
  return tatCa.filter((t) => t.trangThai !== 'dang-gui');
}

/**
 * Cập nhật trạng thái tải lên của một tệp đính kèm.
 */
export async function capNhatTrangThaiTep(
  id: string,
  trangThai: TepDinhKem['trangThai'],
  lyDo?: string,
): Promise<void> {
  const db = await moDb();
  const banGhi = await db.get(STORE_TEP_DINH_KEM, id);
  if (!banGhi) return;
  banGhi.trangThai = trangThai;
  if (lyDo !== undefined) banGhi.lyDo = lyDo;
  await db.put(STORE_TEP_DINH_KEM, banGhi);
}

/**
 * Xóa một tệp đính kèm khỏi hàng chờ.
 */
export async function xoaTepDinhKem(id: string): Promise<void> {
  const db = await moDb();
  await db.delete(STORE_TEP_DINH_KEM, id);
}

/**
 * Xóa toàn bộ ảnh/đính kèm của một nhật ký.
 *
 * @returns Số tệp đã xóa.
 */
export async function xoaTepTheoNhatKy(offlineEventId: string): Promise<number> {
  const db = await moDb();
  const danhSach = await db.getAllFromIndex(
    STORE_TEP_DINH_KEM,
    'theo-nhat-ky',
    offlineEventId,
  );
  for (const tep of danhSach) {
    await db.delete(STORE_TEP_DINH_KEM, tep.id);
  }
  return danhSach.length;
}

/**
 * Đếm số ảnh/đính kèm còn trong hàng chờ tải lên.
 */
export async function demTepCho(): Promise<number> {
  const db = await moDb();
  return db.count(STORE_TEP_DINH_KEM);
}
