import { useCallback, useEffect, useRef, useState } from 'react';
import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { v4 as uuidv4 } from 'uuid';
import {
  getBackoffDelay,
  getOfflineEvents,
  removeOfflineEvent,
  updateOfflineEventStatus,
} from '@/services/offlineQueue';
import { syncOfflineEvents } from '@/api/chainEventApi';
import { uploadAttachment } from '@/api/attachmentApi';
import {
  capNhatTrangThaiTep,
  xoaTepDinhKem,
  xoaTepTheoNhatKy,
} from '@/lib/offline/farmLogDb';
import { layAnhChuaGui } from '@/lib/offline/farmLogAttachmentQueue';
import { diChuyenHangChoFarmLogCu } from '@/lib/offline/migrateLegacyFarmLogQueue';
import { ChainEventType } from '@/enums/chainEventType';
import type { OfflineEvent, OfflineSyncResultDto } from '@/types/offlineEvent';

const SYNC_POLL_INTERVAL = 10_000;
const AUTO_SYNC_DEBOUNCE = 15_000;
const MAX_RETRIES = 3;

/**
 * Chu kỳ thử lại cho bản ghi `FARM_LOG` đã hết lượt (ms).
 * Bản ghi canh tác lỗi được giữ lại chờ xử lý (dead-letter, không tự xóa),
 * lượt tự động giãn chu kỳ thử để tránh gọi dồn.
 */
const FARM_LOG_RETRY_SAU_HET_LUOT = 60_000;

/**
 * Tải ảnh/đính kèm của một nhật ký đã có ID trên máy chủ (pha 2).
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
    } catch {
      await capNhatTrangThaiTep(anh.id, 'loi', 'Lỗi kết nối máy chủ');
      conLai += 1;
    }
  }

  return { daGui, conLai };
}

export const useOfflineSync = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [pendingCount, setPendingCount] = useState(0);
  // Số nhật ký canh tác chờ trong HÀNG CHỜ CHUNG (lọc theo eventType),
  // giữ tên cũ để các màn hình không phải sửa.
  const [farmLogPendingCount, setFarmLogPendingCount] = useState(0);
  const [isSyncing, setIsSyncing] = useState(false);
  const [lastError, setLastError] = useState<string | null>(null);

  const isSyncingRef = useRef(false);
  const lastAutoSyncRef = useRef(0);
  const toastShownRef = useRef(new Set<string>());

  // Đếm hàng chờ chung: `pendingCount` gồm mọi sự kiện chưa xong (kể cả bản
  // lỗi giữ lại kèm lý do), `farmLogPendingCount` là tập con FARM_LOG.
  const refreshCount = useCallback(() => {
    const events = getOfflineEvents();
    setPendingCount(events.filter((e) => e.status !== 'success').length);
    setFarmLogPendingCount(
      events.filter(
        (e) => e.eventType === ChainEventType.FARM_LOG && e.status !== 'success',
      ).length,
    );
  }, []);

  // Cập nhật trạng thái mạng
  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      refreshCount();
    };
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    refreshCount();
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [refreshCount]);

  const sync = useCallback(async (tuDong = false): Promise<void> => {
    if (isSyncingRef.current) return;
    // Lượt tự động giãn cách tối thiểu 15s để tránh gọi dồn; lượt bấm tay chạy ngay.
    if (tuDong) {
      const bayGio = Date.now();
      if (bayGio - lastAutoSyncRef.current < AUTO_SYNC_DEBOUNCE) return;
      lastAutoSyncRef.current = bayGio;
    }
    isSyncingRef.current = true;
    setIsSyncing(true);
    setLastError(null);

    try {
      // Di chuyển một lần dữ liệu cũ từ kho IndexedDB riêng sang hàng chờ chung.
      try {
        await diChuyenHangChoFarmLogCu();
      } catch {
        // Migration lỗi: bỏ qua, không chặn luồng sync chuẩn.
      }

      const allEvents = getOfflineEvents();

      const activeEvents = allEvents.filter(
        (e) =>
          e.status !== 'success' &&
          (e.status !== 'invalid' || e.eventType === ChainEventType.FARM_LOG),
      );

      // Tách sự kiện chain-event hết lượt thử (sẽ xóa) khỏi bản ghi canh tác
      // (luôn giữ lại chờ xử lý) và các sự kiện đến hạn gửi.
      const dueEvents: OfflineEvent[] = [];
      const exhaustedEvents: OfflineEvent[] = [];

      for (const e of activeEvents) {
        const laFarmLog = e.eventType === ChainEventType.FARM_LOG;
        if (e.status === 'da-ghi') continue;
        const retries = e.retryCount ?? 0;
        if (!laFarmLog && retries >= MAX_RETRIES) {
          exhaustedEvents.push(e);
          continue;
        }
        const delay =
          laFarmLog && retries >= MAX_RETRIES
            ? FARM_LOG_RETRY_SAU_HET_LUOT
            : getBackoffDelay(retries);
        if (!e.lastSyncAttempt || Date.now() - e.lastSyncAttempt >= delay) {
          dueEvents.push(e);
        }
        // else: còn trong thời gian backoff
      }

      // Remove exhausted events (chỉ chain-event; FARM_LOG không bao giờ tự xóa)
      for (const event of exhaustedEvents) {
        const label = getEventLabel(event);
        const key = `exhausted-${event.offlineEventId}`;
        if (!toastShownRef.current.has(key)) {
          toastShownRef.current.add(key);
          toast.error(`Sự kiện ${label} đã thất bại sau 3 lần thử và bị xóa.`, {
            duration: 5000,
          });
        }
        removeOfflineEvent(event.offlineEventId);
      }

      // Lượt chỉ tải ảnh (pha 2): bản ghi canh tác đã có nội dung trên máy chủ.
      let anhDaGui = 0;
      let anhCho = 0;
      const choTaiAnh = activeEvents.filter(
        (e) =>
          e.eventType === ChainEventType.FARM_LOG &&
          e.status === 'da-ghi' &&
          e.farmLogId,
      );
      for (const banGhi of choTaiAnh) {
        const { daGui, conLai } = await taiAnhLen(
          banGhi.offlineEventId,
          banGhi.farmLogId as string,
        );
        anhDaGui += daGui;
        anhCho += conLai;
        if (conLai === 0) {
          await xoaTepTheoNhatKy(banGhi.offlineEventId).catch(() => undefined);
          removeOfflineEvent(banGhi.offlineEventId);
        } else {
          updateOfflineEventStatus(banGhi.offlineEventId, {
            status: 'da-ghi',
            errorMessage: `Còn ${conLai} ảnh chưa tải lên được, sẽ thử lại sau.`,
            lastSyncAttempt: Date.now(),
          });
        }
      }

      if (dueEvents.length === 0) {
        if (anhDaGui > 0) toast.info(`Đã tải lên ${anhDaGui} ảnh đính kèm.`);
        if (anhCho > 0) toast.warning(`Còn ${anhCho} ảnh chưa tải lên được, sẽ thử lại sau.`);
        refreshCount();
        return;
      }

      // Mark as syncing
      const now = Date.now();
      for (const e of dueEvents) {
        updateOfflineEventStatus(e.offlineEventId, {
          status: 'syncing',
          lastSyncAttempt: now,
        });
      }

      try {
        const syncId = uuidv4();
        const payload = { syncId, events: dueEvents };
        const response = await syncOfflineEvents(payload);

        const results: OfflineSyncResultDto[] = response.results || [];
        const theoId = new Map(dueEvents.map((e) => [e.offlineEventId, e]));
        let permanentFailures = 0;
        let farmChoXuLy = 0;

        for (const r of results) {
          const gốc = theoId.get(r.offlineEventId);
          const laFarmLog = gốc?.eventType === ChainEventType.FARM_LOG;

          if (r.status === 'SUCCESS' || r.status === 'DUPLICATE') {
            if (laFarmLog) {
              const farmLogId = r.eventId ?? gốc?.farmLogId ?? null;
              const conAnh = (await layAnhChuaGui(r.offlineEventId)).length;
              if (r.status === 'DUPLICATE') {
                if (conAnh > 0 && !farmLogId) {
                  updateOfflineEventStatus(r.offlineEventId, {
                    status: 'invalid',
                    errorMessage:
                      'Nội dung đã đồng bộ nhưng máy chủ chưa trả mã nhật ký. Ảnh được giữ lại, chưa thể tải lên.',
                    lastSyncAttempt: Date.now(),
                  });
                  farmChoXuLy += 1;
                  anhCho += conAnh;
                  continue;
                }
                await xoaTepTheoNhatKy(r.offlineEventId).catch(() => undefined);
                removeOfflineEvent(r.offlineEventId);
                continue;
              }
              // SUCCESS
              if (conAnh > 0 && farmLogId) {
                const { daGui, conLai } = await taiAnhLen(r.offlineEventId, farmLogId);
                anhDaGui += daGui;
                anhCho += conLai;
                if (conLai === 0) {
                  await xoaTepTheoNhatKy(r.offlineEventId).catch(() => undefined);
                  removeOfflineEvent(r.offlineEventId);
                } else {
                  updateOfflineEventStatus(r.offlineEventId, {
                    status: 'da-ghi',
                    farmLogId,
                    errorMessage: `Còn ${conLai} ảnh chưa tải lên được, sẽ thử lại sau.`,
                    lastSyncAttempt: Date.now(),
                  });
                }
                continue;
              }
              await xoaTepTheoNhatKy(r.offlineEventId).catch(() => undefined);
              removeOfflineEvent(r.offlineEventId);
              continue;
            }
            removeOfflineEvent(r.offlineEventId);
          } else {
            // FAILED nghiệp vụ: nhật ký canh tác giữ lại kèm lý do (dead-letter).
            if (laFarmLog) {
              updateOfflineEventStatus(r.offlineEventId, {
                status: 'invalid',
                errorMessage: r.message || 'Lỗi không xác định',
                lastSyncAttempt: Date.now(),
              });
              farmChoXuLy += 1;
              continue;
            }
            const event = gốc;
            const newRetryCount = (event?.retryCount ?? 0) + 1;
            if (newRetryCount >= MAX_RETRIES) {
              removeOfflineEvent(r.offlineEventId);
              permanentFailures++;
            } else {
              updateOfflineEventStatus(r.offlineEventId, {
                status: 'failed',
                errorMessage: r.message || 'Lỗi không xác định',
                retryCount: newRetryCount,
                lastSyncAttempt: Date.now(),
              });
            }
          }
        }

        if (response.successCount > 0) {
          toast.success(`Đồng bộ thành công ${response.successCount} sự kiện.`);
        }
        if (response.duplicateCount > 0) {
          toast.info(`Bỏ qua ${response.duplicateCount} sự kiện đã tồn tại.`);
        }
        if (anhDaGui > 0) {
          toast.info(`Đã tải lên ${anhDaGui} ảnh đính kèm.`);
        }
        if (farmChoXuLy > 0) {
          toast.warning(
            `Còn ${farmChoXuLy} nhật ký chưa đồng bộ được, được giữ lại kèm lý do để bạn xử lý.`,
          );
        }
        if (anhCho > 0) {
          toast.warning(`Còn ${anhCho} ảnh chưa tải lên được, sẽ thử lại sau.`);
        }
        if (permanentFailures > 0) {
          toast.error(`${permanentFailures} sự kiện đã thất bại vĩnh viễn và bị xóa.`);
        }
        const transient = response.failedCount - permanentFailures - farmChoXuLy;
        if (transient > 0) {
          toast.warning(`Còn ${transient} sự kiện chưa đồng bộ được, sẽ thử lại sau.`);
        }
      } catch (error: unknown) {
        const status = isAxiosError(error) ? error.response?.status : undefined;

        if (status === 400) {
          // Dữ liệu không hợp lệ: chain-event xóa, nhật ký canh tác giữ lại kèm lý do.
          const serverMessage =
            isAxiosError(error)
              ? ((error.response?.data as any)?.message ?? 'Dữ liệu không hợp lệ.')
              : 'Dữ liệu không hợp lệ.';

          let farmGiuLai = 0;
          for (const e of dueEvents) {
            if (e.eventType === ChainEventType.FARM_LOG) {
              updateOfflineEventStatus(e.offlineEventId, {
                status: 'invalid',
                errorMessage: serverMessage,
                lastSyncAttempt: Date.now(),
              });
              farmGiuLai += 1;
            } else {
              removeOfflineEvent(e.offlineEventId);
            }
          }

          const soXoa = dueEvents.length - farmGiuLai;
          if (soXoa > 0) {
            toast.error(`Đã xóa ${soXoa} sự kiện không hợp lệ khỏi hàng chờ.`, {
              description: serverMessage,
              duration: 6000,
            });
          }
          if (farmGiuLai > 0) {
            toast.warning(
              `Còn ${farmGiuLai} nhật ký chưa đồng bộ được, được giữ lại kèm lý do để bạn xử lý.`,
              { description: serverMessage, duration: 6000 },
            );
          }
          setLastError(serverMessage);
        } else {
          // Lỗi mạng hoặc 5xx: chain-event thử lại tối đa 3 lần rồi xóa,
          // nhật ký canh tác giữ lại và thử lại (không bao giờ tự xóa).
          for (const e of dueEvents) {
            const laFarmLog = e.eventType === ChainEventType.FARM_LOG;
            const newRetryCount = Math.min((e.retryCount ?? 0) + 1, MAX_RETRIES);
            const loiMang = error instanceof Error ? error.message : 'Lỗi kết nối máy chủ';
            if (!laFarmLog && newRetryCount >= MAX_RETRIES) {
              removeOfflineEvent(e.offlineEventId);
            } else {
              updateOfflineEventStatus(e.offlineEventId, {
                // Bản ghi canh tác đang ở `invalid` (lỗi nghiệp vụ cũ) thì giữ
                // nguyên trạng thái đó, chỉ dập mốc thử để giãn chu kỳ.
                status: e.status === 'invalid' ? 'invalid' : 'failed',
                errorMessage: e.status === 'invalid' ? e.errorMessage : loiMang,
                retryCount: newRetryCount,
                lastSyncAttempt: Date.now(),
              });
            }
          }

          const msg =
            status === 500
              ? 'Máy chủ gặp lỗi, sẽ thử lại sau.'
              : 'Đồng bộ thất bại. Sẽ thử lại khi có kết nối.';
          toast.error(msg);
        }
      }
    } finally {
      isSyncingRef.current = false;
      setIsSyncing(false);
      refreshCount();
    }
  }, [refreshCount]);

  // Tự động đồng bộ với debounce (kể cả bản ghi canh tác lỗi được giữ lại:
  // thử lại khi hết chu kỳ giãn cách, phòng trạng thái máy chủ đã đổi).
  useEffect(() => {
    if (!isOnline || isSyncingRef.current) return;

    const now = Date.now();
    if (now - lastAutoSyncRef.current < AUTO_SYNC_DEBOUNCE) return;

    const events = getOfflineEvents();
    const hasActionable = events.some((e) => {
      if (e.status === 'success') return false;
      const laFarmLog = e.eventType === ChainEventType.FARM_LOG;
      if (e.status === 'da-ghi') return laFarmLog && Boolean(e.farmLogId);
      if (!laFarmLog && e.status === 'invalid') return false;
      if (e.status !== 'pending' && e.status !== 'failed' && e.status !== 'invalid') {
        return false;
      }
      const retries = e.retryCount ?? 0;
      if (!laFarmLog && retries >= MAX_RETRIES) return false;
      if (e.status === 'failed' || e.status === 'invalid') {
        const delay =
          laFarmLog && retries >= MAX_RETRIES
            ? FARM_LOG_RETRY_SAU_HET_LUOT
            : getBackoffDelay(retries);
        if (e.lastSyncAttempt && now - e.lastSyncAttempt < delay) return false;
      }
      return true;
    });

    if (!hasActionable) return;

    lastAutoSyncRef.current = now;
    sync(true);
  }, [isOnline, sync]);

  const forceSync = useCallback(() => {
    if (isSyncingRef.current) return;
    sync();
  }, [sync]);

  // Hẹn giờ đồng bộ tự động: có mạng là gửi, không cần nút bấm tay (NCL-10-CN-012).
  useEffect(() => {
    const interval = setInterval(() => {
      refreshCount();
      if (navigator.onLine) void sync(true);
    }, SYNC_POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [refreshCount, sync]);

  return {
    isOnline,
    pendingCount,
    farmLogPendingCount,
    isSyncing,
    lastError,
    sync: forceSync,
  };
};

function getEventLabel(event: OfflineEvent): string {
  const typeLabels: Record<string, string> = {
    HARVEST: 'Thu hoạch',
    TRANSPORT: 'Vận chuyển',
    PACKAGING: 'Đóng gói',
    PROCUREMENT: 'Thu mua',
    MOBILE: 'Ngoài đồng',
    FARM_LOG: 'Nhật ký canh tác',
  };
  return typeLabels[event.eventType] ?? event.eventType;
}
