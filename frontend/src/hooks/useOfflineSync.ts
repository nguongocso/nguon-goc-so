import { useCallback, useEffect, useRef, useState } from 'react';
import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { v4 as uuidv4 } from 'uuid';
import {
  getOfflineEvents,
  removeOfflineEvent,
  updateOfflineEventStatus,
} from '@/services/offlineQueue';
import { syncOfflineEvents } from '@/api/chainEventApi';
import type { OfflineEvent, OfflineSyncResultDto } from '@/types/offlineEvent';

const SYNC_POLL_INTERVAL = 10_000;
const AUTO_SYNC_DEBOUNCE = 15_000;
const MAX_RETRIES = 3;

export const useOfflineSync = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [pendingCount, setPendingCount] = useState(0);
  const [isSyncing, setIsSyncing] = useState(false);
  const [lastError, setLastError] = useState<string | null>(null);

  const isSyncingRef = useRef(false);
  const lastAutoSyncRef = useRef(0);
  const toastShownRef = useRef(new Set<string>());

  // Refresh pending count periodically
  const refreshCount = useCallback(() => {
    const events = getOfflineEvents();
    const count = events.filter(
      (e) => e.status !== 'success' && e.status !== 'invalid' && (e.retryCount ?? 0) < MAX_RETRIES,
    ).length;
    setPendingCount(count);
  }, []);

  // Cập nhật trạng thái mạng
  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    refreshCount();
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [refreshCount]);

  useEffect(() => {
    const interval = setInterval(refreshCount, SYNC_POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [refreshCount]);

  const sync = useCallback(async (): Promise<void> => {
    if (isSyncingRef.current) return;
    isSyncingRef.current = true;
    setIsSyncing(true);
    setLastError(null);

    try {
      const allEvents = getOfflineEvents();

      const activeEvents = allEvents.filter(
        (e) => e.status !== 'success' && e.status !== 'invalid',
      );

      // Separate events that still have retry budget and are due
      const dueEvents: OfflineEvent[] = [];
      const exhaustedEvents: OfflineEvent[] = [];

      for (const e of activeEvents) {
        const retries = e.retryCount ?? 0;
        if (retries >= MAX_RETRIES) {
          exhaustedEvents.push(e);
        } else if (
          !e.lastSyncAttempt ||
          Date.now() - e.lastSyncAttempt >= getBackoffDelay(retries)
        ) {
          dueEvents.push(e);
        }
        // else: still in backoff
      }

      // Remove exhausted events
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

      if (dueEvents.length === 0) {
        refreshCount();
        return;
      }

      // Mark as syncing
      const now = Date.now();
      for (const e of dueEvents) {
        updateOfflineEventStatus(e.offlineEventId, {
          status: 'syncing',
          lastSyncAttempt: now,
        } as any);
      }

      try {
        const syncId = uuidv4();
        const payload = { syncId, events: dueEvents };
        const response = await syncOfflineEvents(payload);

        const results: OfflineSyncResultDto[] = response.results || [];
        let permanentFailures = 0;

        for (const r of results) {
          if (r.status === 'SUCCESS' || r.status === 'DUPLICATE') {
            removeOfflineEvent(r.offlineEventId);
          } else {
            const event = dueEvents.find((e) => e.offlineEventId === r.offlineEventId);
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
              } as any);
            }
          }
        }

        if (response.successCount > 0) {
          toast.success(`Đồng bộ thành công ${response.successCount} sự kiện.`);
        }
        if (response.duplicateCount > 0) {
          toast.info(`Bỏ qua ${response.duplicateCount} sự kiện đã tồn tại.`);
        }
        if (permanentFailures > 0) {
          toast.error(`${permanentFailures} sự kiện đã thất bại vĩnh viễn và bị xóa.`);
        }
        const transient = response.failedCount - permanentFailures;
        if (transient > 0) {
          toast.warning(`Còn ${transient} sự kiện chưa đồng bộ được, sẽ thử lại sau.`);
        }
      } catch (error: unknown) {
        const status = isAxiosError(error) ? error.response?.status : undefined;

        if (status === 400) {
          // Invalid data — never retry
          const serverMessage =
            isAxiosError(error)
              ? ((error.response?.data as any)?.message ?? 'Dữ liệu không hợp lệ.')
              : 'Dữ liệu không hợp lệ.';

          for (const e of dueEvents) {
            removeOfflineEvent(e.offlineEventId);
          }

          toast.error(`Đã xóa ${dueEvents.length} sự kiện không hợp lệ khỏi hàng chờ.`, {
            description: serverMessage,
            duration: 6000,
          });
          setLastError(serverMessage);
        } else {
          // Network error or 5xx — retry with backoff
          for (const e of dueEvents) {
            const newRetryCount = (e.retryCount ?? 0) + 1;
            if (newRetryCount >= MAX_RETRIES) {
              removeOfflineEvent(e.offlineEventId);
            } else {
              updateOfflineEventStatus(e.offlineEventId, {
                status: 'failed',
                errorMessage:
                  error instanceof Error ? error.message : 'Lỗi kết nối máy chủ',
                retryCount: newRetryCount,
                lastSyncAttempt: Date.now(),
              } as any);
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

  // Tự động đồng bộ với debounce
  useEffect(() => {
    if (!isOnline || isSyncingRef.current) return;

    const now = Date.now();
    if (now - lastAutoSyncRef.current < AUTO_SYNC_DEBOUNCE) return;

    const events = getOfflineEvents();
    const hasActionable = events.some((e) => {
      if (e.status === 'success' || e.status === 'invalid') return false;
      const retries = e.retryCount ?? 0;
      if (retries >= MAX_RETRIES) return false;
      if (e.status === 'failed') {
        const delay = getBackoffDelay(retries);
        if (e.lastSyncAttempt && now - e.lastSyncAttempt < delay) return false;
      }
      return true;
    });

    if (!hasActionable) return;

    lastAutoSyncRef.current = now;
    sync();
  }, [isOnline, sync]);

  const forceSync = useCallback(() => {
    if (isSyncingRef.current) return;
    sync();
  }, [sync]);

  return {
    isOnline,
    pendingCount,
    isSyncing,
    lastError,
    sync: forceSync,
  };
};

function getBackoffDelay(retryCount: number): number {
  const delays = [5_000, 15_000, 30_000];
  return delays[Math.min(retryCount, delays.length - 1)] ?? 30_000;
}

function getEventLabel(event: OfflineEvent): string {
  const typeLabels: Record<string, string> = {
    HARVEST: 'Thu hoạch',
    TRANSPORT: 'Vận chuyển',
    PACKAGING: 'Đóng gói',
    PROCUREMENT: 'Thu mua',
    MOBILE: 'Ngoài đồng',
  };
  return typeLabels[event.eventType] ?? event.eventType;
}