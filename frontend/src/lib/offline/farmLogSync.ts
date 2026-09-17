import { v4 as uuidv4 } from 'uuid';
import { toast } from 'sonner';
import { uploadAttachment } from '@/api/attachmentApi';
import { layAnhChuaGui } from '@/lib/offline/farmLogAttachmentQueue';
import { syncOfflineEvents } from '@/api/chainEventApi';
import { getBackoffDelay } from '@/services/offlineQueue';
import {
  capNhatTrangThai,
  capNhatTrangThaiTep,
  layDanhSachCho,
  luuKetQuaDaGhi,
  thuHoiBanGhiTreoSyncing,
  xoaNhatKyCho,
  xoaTepDinhKem,
  xoaTepTheoNhatKy,
} from '@/lib/offline/farmLogDb';
import type { OfflineEvent } from '@/types/offlineEvent';

/**
 * Đồng bộ hàng chờ nhật ký canh tác lên server (NCL-10-CN-012).
 *
 * Đúng mô tả story: "Khi có mạng, ứng dụng **gửi lần lượt** các bản ghi chờ, hệ thống
 * dùng mã định danh để bỏ qua bản gửi lại". Vì vậy mỗi bản ghi là một request riêng.
 *
 * - Lỗi mạng: giữ bản ghi, tăng lượt thử, backoff 5s → 15s → 30s (tối đa 3 lần tự động).
 * - Lỗi nghiệp vụ (sai quyền, lô đã hủy...): chuyển `invalid`, **giữ nguyên kèm lý do** —
 *   không tự xoá (đúng TC-04 và điều kiện sau hoàn thành của NCL-10-CN-006).
 * - Ảnh: sau khi nội dung đã lên máy chủ, tải ảnh lên endpoint đính kèm (pha 2). Nếu ảnh
 *   lỗi, bản ghi giữ trạng thái `da-ghi` kèm `farmLogId` để lần sau chỉ tải ảnh.
 *
 * Module tự xử lý lỗi/toast nên không làm hỏng luồng sync chain-event.
 */

/** Số lần tự động thử lại cho một nhật ký chờ (đồng nhất với hàng chờ chain-event). */
export const MAX_LAN_THU_FARM_LOG = 3;

export interface KetQuaDongBoFarmLog {
  thanhCong: number;
  trung: number;
  thatBai: number;
  /** Số bản ghi bị giữ lại kèm lý do (lỗi nghiệp vụ) — chờ người dùng xử lý. */
  choXuLy: number;
  /** Số ảnh đã tải lên thành công trong lượt này. */
  anhDaGui: number;
  /** Số ảnh còn lại chưa tải lên được. */
  anhCho: number;
}

const taoKetQuaRong = (): KetQuaDongBoFarmLog => ({
  thanhCong: 0,
  trung: 0,
  thatBai: 0,
  choXuLy: 0,
  anhDaGui: 0,
  anhCho: 0,
});

/**
 * Ghi nhận lỗi nghiệp vụ: giữ bản ghi kèm lý do, không tăng lượt thử, không tự xoá.
 */
async function danhDauChoXuLy(banGhi: OfflineEvent, lyDo: string): Promise<void> {
  await capNhatTrangThai(banGhi.offlineEventId, 'invalid', lyDo, banGhi.retryCount ?? 0);
}

/**
 * Ghi nhận lỗi mạng: giữ bản ghi và tăng lượt thử để backoff lần sau.
 */
async function danhDauLoiMang(banGhi: OfflineEvent, lyDo: string): Promise<void> {
  await capNhatTrangThai(
    banGhi.offlineEventId,
    'failed',
    lyDo,
    (banGhi.retryCount ?? 0) + 1,
  );
}

/**
 * Tải ảnh của một nhật ký đã có ID trên máy chủ (pha 2).
 *
 * @returns số ảnh đã gửi và số ảnh còn lại
 */
async function taiAnhLen(
  offlineEventId: string,
  farmLogId: string,
): Promise<{ daGui: number; conLai: number }> {
  const danhSach = await layAnhChuaGui(offlineEventId);
  let daGui = 0;
  let conLai = 0;

  for (const anh of danhSach) {
    await capNhatTrangThaiTep(anh.id, 'dang-gui');
    try {
      const tep = new File([anh.blob], anh.ten, { type: anh.loai });
      await uploadAttachment(farmLogId, tep);
      await xoaTepDinhKem(anh.id);
      daGui += 1;
    } catch (error) {
      await capNhatTrangThaiTep(
        anh.id,
        'loi',
        error instanceof Error ? error.message : 'Lỗi kết nối máy chủ',
      );
      conLai += 1;
    }
  }

  return { daGui, conLai };
}

/**
 * Ảnh của một nhật ký còn phải gửi hay không.
 */
async function conAnhCho(offlineEventId: string): Promise<boolean> {
  const conLai = await layAnhChuaGui(offlineEventId);
  return conLai.length > 0;
}


/**
 * Đồng bộ hàng chờ nhật ký canh tác: thu hồi bản kẹt, tải ảnh còn lại, gửi lần lượt
 * từng bản ghi chờ và giữ lại bản ghi lỗi kèm lý do.
 */
let dongBoDangChay: Promise<KetQuaDongBoFarmLog> | null = null;

/** Dùng chung lượt đồng bộ giữa các component trong cùng một trang. */
export function dongBoNhatKyCho(): Promise<KetQuaDongBoFarmLog> {
  if (!dongBoDangChay) {
    dongBoDangChay = thucHienDongBo().finally(() => { dongBoDangChay = null; });
  }
  return dongBoDangChay;
}

async function thucHienDongBo(): Promise<KetQuaDongBoFarmLog> {
  const ketQua = taoKetQuaRong();

  // 1. Thu hồi bản ghi bị kẹt ở trạng thái `syncing` (đóng tab giữa lúc gửi).
  try {
    await thuHoiBanGhiTreoSyncing();
  } catch {
    // IndexedDB lỗi: bỏ qua, không chặn luồng đồng bộ
  }

  let tatCa: OfflineEvent[];
  try {
    tatCa = await layDanhSachCho();
  } catch {
    return ketQua;
  }

  // 2. Bản ghi đã ghi nội dung nhưng còn ảnh chờ: chỉ tải ảnh (pha 2).
  const choTaiAnh = tatCa.filter((b) => b.status === 'da-ghi' && b.farmLogId);
  for (const banGhi of choTaiAnh) {
    const { daGui, conLai } = await taiAnhLen(
      banGhi.offlineEventId,
      banGhi.farmLogId as string,
    );
    ketQua.anhDaGui += daGui;
    ketQua.anhCho += conLai;

    if (conLai === 0) {
      await xoaTepTheoNhatKy(banGhi.offlineEventId);
      await xoaNhatKyCho(banGhi.offlineEventId);
      ketQua.thanhCong += 1;
    } else {
      await capNhatTrangThai(
        banGhi.offlineEventId,
        'da-ghi',
        `Còn ${conLai} ảnh chưa tải lên được, sẽ thử lại sau.`,
      );
    }
  }

  // 3. Chỉ gửi bản ghi đã qua thời gian backoff (bản `da-ghi` có farmLogId đã xử lý ở bước 2).
  const bayGio = Date.now();
  const denHan = tatCa.filter((b) => {
    if (b.status === 'da-ghi' && b.farmLogId) return false;
    if (b.status !== 'pending' && b.status !== 'failed' && b.status !== 'da-ghi') {
      return false;
    }
    if ((b.retryCount ?? 0) >= MAX_LAN_THU_FARM_LOG) return false;
    if (!b.lastSyncAttempt) return true;
    return bayGio - b.lastSyncAttempt >= getBackoffDelay(b.retryCount ?? 0);
  });

  ketQua.choXuLy = tatCa.filter((b) => b.status === 'invalid').length;

  // 4. Gửi lần lượt từng bản ghi (mỗi bản ghi một request, đúng mô tả story).
  for (const banGhi of denHan) {
    await capNhatTrangThai(banGhi.offlineEventId, 'syncing');

    let phanHoi;
    try {
      phanHoi = await syncOfflineEvents({ syncId: uuidv4(), events: [banGhi] });
    } catch (error) {
      // Lỗi mạng/server: giữ bản ghi, tăng lượt thử và dừng lượt đồng bộ này.
      await danhDauLoiMang(
        banGhi,
        error instanceof Error ? error.message : 'Lỗi kết nối máy chủ',
      );
      ketQua.thatBai += 1;
      toast.error('Mất kết nối khi đồng bộ nhật ký. Sẽ thử lại khi có mạng.');
      break;
    }

    const ketQuaBanGhi = phanHoi.results?.[0];

    // Lỗi nghiệp vụ: giữ nguyên bản ghi kèm lý do, không tự xoá.
    if (!ketQuaBanGhi || ketQuaBanGhi.status === 'FAILED') {
      await danhDauChoXuLy(banGhi, ketQuaBanGhi?.message || 'Lỗi không xác định');
      ketQua.choXuLy += 1;
      continue;
    }

    // SUCCESS hoặc DUPLICATE: nội dung đã có trên máy chủ.
    const farmLogId = ketQuaBanGhi.eventId ?? null;
    const coAnhCho = await conAnhCho(banGhi.offlineEventId);

    if (ketQuaBanGhi.status === 'DUPLICATE') ketQua.trung += 1;
    else ketQua.thanhCong += 1;

    if (coAnhCho && !farmLogId) {
      await danhDauChoXuLy(banGhi, 'Nội dung đã đồng bộ nhưng máy chủ chưa trả mã nhật ký. Ảnh được giữ lại, chưa thể tải lên.');
      ketQua.choXuLy += 1;
      ketQua.anhCho += (await layAnhChuaGui(banGhi.offlineEventId)).length;
      continue;
    }

    if (!coAnhCho || !farmLogId) {
      await xoaTepTheoNhatKy(banGhi.offlineEventId);
      await xoaNhatKyCho(banGhi.offlineEventId);
      continue;
    }

    // Nội dung đã ghi, còn ảnh: giữ bản ghi `da-ghi` rồi tải ảnh lên (pha 2).
    await luuKetQuaDaGhi(banGhi.offlineEventId, farmLogId);
    const { daGui, conLai } = await taiAnhLen(banGhi.offlineEventId, farmLogId);
    ketQua.anhDaGui += daGui;
    ketQua.anhCho += conLai;

    if (conLai === 0) {
      await xoaTepTheoNhatKy(banGhi.offlineEventId);
      await xoaNhatKyCho(banGhi.offlineEventId);
    } else {
      await capNhatTrangThai(
        banGhi.offlineEventId,
        'da-ghi',
        `Còn ${conLai} ảnh chưa tải lên được, sẽ thử lại sau.`,
      );
    }
  }

  // 5. Thông báo kết quả.
  if (ketQua.thanhCong > 0) {
    toast.success(`Đã đồng bộ ${ketQua.thanhCong} nhật ký canh tác.`);
  }
  if (ketQua.trung > 0) {
    toast.info(`Bỏ qua ${ketQua.trung} nhật ký đã đồng bộ trước đó.`);
  }
  if (ketQua.anhDaGui > 0) {
    toast.info(`Đã tải lên ${ketQua.anhDaGui} ảnh đính kèm.`);
  }
  if (ketQua.choXuLy > 0) {
    toast.warning(
      `Còn ${ketQua.choXuLy} nhật ký chưa đồng bộ được, được giữ lại kèm lý do để bạn xử lý.`,
    );
  }
  if (ketQua.anhCho > 0) {
    toast.warning(`Còn ${ketQua.anhCho} ảnh chưa tải lên được, sẽ thử lại sau.`);
  }

  return ketQua;
}
