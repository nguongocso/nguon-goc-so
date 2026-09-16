import { v4 as uuidv4 } from 'uuid';
import { toast } from 'sonner';
import { syncOfflineEvents } from '@/api/chainEventApi';
import { getBackoffDelay } from '@/services/offlineQueue';
import {
  capNhatTrangThai,
  layDanhSachCho,
  xoaNhatKyCho,
} from '@/lib/offline/farmLogDb';
import type { OfflineEvent } from '@/types/offlineEvent';

/**
 * Đồng bộ hàng chờ nhật ký canh tác lên server (NCL-10-CN-012, MVP).
 *
 * - Gửi qua endpoint sync chung `POST /chain-events/sync` (eventType=FARM_LOG).
 * - Tái dùng chính sách thử lại của hàng chờ chain-event: tối đa 3 lần,
 *   backoff 5s → 15s → 30s; hết lượt thì xóa khỏi hàng chờ.
 * - Được gọi bổ sung trong `useOfflineSync().sync()` nên mọi lỗi đều nuốt
 *   gọn trong toast, không làm hỏng luồng sync chain-event.
 */

/** Số lần thử tối đa cho một nhật ký chờ (đồng nhất với hàng chờ chain-event). */
export const MAX_LAN_THU_FARM_LOG = 3;

export interface KetQuaDongBoFarmLog {
  thanhCong: number;
  trung: number;
  thatBai: number;
}

async function danhDauThatBai(
  banGhi: OfflineEvent | undefined,
  lyDo: string,
): Promise<void> {
  if (!banGhi) return;
  const lanThuMoi = (banGhi.retryCount ?? 0) + 1;
  if (lanThuMoi >= MAX_LAN_THU_FARM_LOG) {
    await xoaNhatKyCho(banGhi.offlineEventId);
  } else {
    await capNhatTrangThai(banGhi.offlineEventId, 'failed', lyDo, lanThuMoi);
  }
}

export async function dongBoNhatKyCho(): Promise<KetQuaDongBoFarmLog> {
  const ketQua: KetQuaDongBoFarmLog = { thanhCong: 0, trung: 0, thatBai: 0 };

  let tatCa: OfflineEvent[];
  try {
    tatCa = await layDanhSachCho();
  } catch {
    return ketQua;
  }
  const dangCho = tatCa.filter(
    (b) => b.status === 'pending' || b.status === 'failed',
  );

  // Xóa bản ghi đã hết lượt thử để hàng chờ không phình.
  const hetLuot = dangCho.filter(
    (b) => (b.retryCount ?? 0) >= MAX_LAN_THU_FARM_LOG,
  );
  for (const banGhi of hetLuot) {
    await xoaNhatKyCho(banGhi.offlineEventId);
  }
  if (hetLuot.length > 0) {
    toast.error(
      `${hetLuot.length} nhật ký thất bại sau ${MAX_LAN_THU_FARM_LOG} lần thử và bị xóa.`,
    );
  }

  // Chỉ gửi bản ghi đã qua thời gian backoff.
  const bayGio = Date.now();
  const denHan = dangCho.filter((b) => {
    if ((b.retryCount ?? 0) >= MAX_LAN_THU_FARM_LOG) return false;
    if (!b.lastSyncAttempt) return true;
    return bayGio - b.lastSyncAttempt >= getBackoffDelay(b.retryCount ?? 0);
  });
  if (denHan.length === 0) return ketQua;

  for (const banGhi of denHan) {
    await capNhatTrangThai(banGhi.offlineEventId, 'syncing');
  }

  try {
    const phanHoi = await syncOfflineEvents({ syncId: uuidv4(), events: denHan });

    for (const r of phanHoi.results ?? []) {
      if (r.status === 'SUCCESS') {
        await xoaNhatKyCho(r.offlineEventId);
        ketQua.thanhCong += 1;
      } else if (r.status === 'DUPLICATE') {
        await xoaNhatKyCho(r.offlineEventId);
        ketQua.trung += 1;
      } else {
        await danhDauThatBai(
          denHan.find((e) => e.offlineEventId === r.offlineEventId),
          r.message || 'Lỗi không xác định',
        );
        ketQua.thatBai += 1;
      }
    }

    if (ketQua.thanhCong > 0) {
      toast.success(`Đã đồng bộ ${ketQua.thanhCong} nhật ký canh tác.`);
    }
    if (ketQua.trung > 0) {
      toast.info(`Bỏ qua ${ketQua.trung} nhật ký đã tồn tại.`);
    }
    if (ketQua.thatBai > 0) {
      toast.warning(
        `Còn ${ketQua.thatBai} nhật ký chưa đồng bộ được, sẽ thử lại sau.`,
      );
    }
  } catch (error) {
    // Lỗi mạng/server: giữ lại hàng chờ, tăng lượt thử để backoff.
    for (const banGhi of denHan) {
      await danhDauThatBai(
        banGhi,
        error instanceof Error ? error.message : 'Lỗi kết nối máy chủ',
      );
    }
    toast.error('Đồng bộ nhật ký thất bại. Sẽ thử lại khi có kết nối.');
  }

  return ketQua;
}
