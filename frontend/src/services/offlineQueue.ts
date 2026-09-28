import { v4 as uuidv4 } from 'uuid';
import type { OfflineEvent } from '@/types/offlineEvent';

const STORAGE_KEY = 'offline_events_queue';
const MAX_RETRIES = 3;

/**
 * Số sự kiện chờ tối đa trong hàng chờ chung.
 *
 * Áp dụng cho mọi loại sự kiện, kể cả `FARM_LOG` (kế thừa giới hạn 100 bản ghi
 * của hàng chờ nhật ký canh tác cũ): khi đầy, bản ghi mới bị chặn kèm thông báo,
 * không tự xóa bản ghi cũ.
 */
export const MAX_OFFLINE_EVENTS = 100;

/** Tên sự kiện DOM khi hàng chờ chung thay đổi (để các danh sách cập nhật ngay). */
export const OFFLINE_QUEUE_CHANGED_EVENT = 'offline-queue-changed';

/**
 * Tên sự kiện DOM cũ của hàng chờ nhật ký canh tác.
 * Giữ lại để tương thích với các component còn lắng nghe tên cũ.
 */
export const LEGACY_FARM_LOG_QUEUE_CHANGED_EVENT = 'farm-log-queue-changed';

/** Phát tín hiệu cho mọi danh sách chờ (mới và cũ) cập nhật lại. */
export const thongBaoDoiHangCho = (): void => {
  if (typeof window === 'undefined' || typeof window.dispatchEvent !== 'function') return;
  window.dispatchEvent(new Event(OFFLINE_QUEUE_CHANGED_EVENT));
  window.dispatchEvent(new Event(LEGACY_FARM_LOG_QUEUE_CHANGED_EVENT));
};

/**
 * Backoff delays in milliseconds: 5s, 15s, 30s
 */
export const BACKOFF_DELAYS = [5_000, 15_000, 30_000];

export const getOfflineEvents = (): OfflineEvent[] => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

/**
 * Validate an offline event before saving.
 * Returns an error message if invalid, or null if valid.
 */
export const validateOfflineEvent = (event: Omit<OfflineEvent, 'offlineEventId'>): string | null => {
  if (!event.eventType) {
    return 'Thiếu loại sự kiện.';
  }
  if (!event.eventData || Object.keys(event.eventData).length === 0) {
    return 'Thiếu dữ liệu sự kiện.';
  }
  return null;
};

/**
 * Check if an event has exceeded the maximum retry count.
 */
export const hasExceededRetries = (event: OfflineEvent): boolean => {
  return (event.retryCount ?? 0) >= MAX_RETRIES;
};

/**
 * Get the backoff delay for an event based on its retry count.
 */
export const getBackoffDelay = (retryCount: number): number => {
  const index = Math.min(retryCount, BACKOFF_DELAYS.length - 1);
  return BACKOFF_DELAYS[index];
};

/**
 * Check if an event is due for retry (enough time has passed since last attempt).
 */
export const isDueForRetry = (event: OfflineEvent): boolean => {
  if (!event.lastSyncAttempt) return true;
  const delay = getBackoffDelay(event.retryCount ?? 0);
  return Date.now() - event.lastSyncAttempt >= delay;
};

/**
 * Đầu vào khi xếp hàng một sự kiện chờ. Cho phép gọi phía truyền sẵn
 * `offlineEventId` (ví dụ form cần gắn ảnh đính kèm theo cùng mã trước khi lưu).
 */
export type OfflineEventMoi = Omit<OfflineEvent, 'offlineEventId' | 'status' | 'retryCount'> & {
  offlineEventId?: string;
};

export const addOfflineEvent = (eventData: OfflineEventMoi): string | null => {
  const validationError = validateOfflineEvent(eventData);
  if (validationError) {
    console.warn('❌ Invalid offline event, not saving:', validationError, eventData);
    return validationError;
  }

  try {
    const queue = getOfflineEvents();
    if (queue.length >= MAX_OFFLINE_EVENTS) {
      return `Hàng chờ đã đầy (${MAX_OFFLINE_EVENTS} bản ghi). Vui lòng kết nối mạng để đồng bộ trước khi ghi tiếp.`;
    }
    const newEvent: OfflineEvent = {
      ...eventData,
      offlineEventId: eventData.offlineEventId ?? uuidv4(),
      status: 'pending',
      retryCount: 0,
    };
    queue.push(newEvent);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
    thongBaoDoiHangCho();
    return null; // success
  } catch (error) {
    // console.error("❌ Failed to save offline event:", error);
    throw error;
  }
};

/**
 * Khôi phục nguyên trạng một sự kiện đã có (dùng khi di chuyển dữ liệu từ
 * kho IndexedDB cũ sang hàng chờ chung): giữ nguyên trạng thái, số lần thử
 * và lý do lỗi, bỏ qua khi trùng mã.
 *
 * @returns 'ok' khi đã thêm, 'duplicate' khi mã đã tồn tại, 'full' khi hàng chờ đã đầy.
 */
export const restoreOfflineEvent = (event: OfflineEvent): 'ok' | 'duplicate' | 'full' => {
  const queue = getOfflineEvents();
  if (queue.some((e) => e.offlineEventId === event.offlineEventId)) return 'duplicate';
  if (queue.length >= MAX_OFFLINE_EVENTS) return 'full';
  queue.push({ ...event });
  localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
  return 'ok';
};

export const removeOfflineEvent = (offlineEventId: string): void => {
  const queue = getOfflineEvents();
  const filtered = queue.filter((e) => e.offlineEventId !== offlineEventId);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(filtered));
  thongBaoDoiHangCho();
};

export const clearOfflineQueue = (): void => {
  localStorage.removeItem(STORAGE_KEY);
  thongBaoDoiHangCho();
};

export const getOfflineQueueCount = (): number => {
  return getOfflineEvents().length;
};

/**
 * Cập nhật trạng thái và lỗi cho một event.
 * Nhận cả `lastSyncAttempt` (mốc backoff) và `farmLogId` (pha 2 tải ảnh nhật ký).
 */
export const updateOfflineEventStatus = (
  offlineEventId: string,
  updates: Partial<
    Pick<OfflineEvent, 'status' | 'errorMessage' | 'retryCount' | 'lastSyncAttempt' | 'farmLogId'>
  >
): void => {
  const queue = getOfflineEvents();
  const event = queue.find((e) => e.offlineEventId === offlineEventId);
  if (!event) return;
  Object.assign(event, updates);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
};