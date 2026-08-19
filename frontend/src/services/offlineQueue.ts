import { v4 as uuidv4 } from 'uuid';
import type { OfflineEvent } from '@/types/offlineEvent';

const STORAGE_KEY = 'offline_events_queue';
const MAX_RETRIES = 3;

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

export const addOfflineEvent = (eventData: Omit<OfflineEvent, 'offlineEventId'>): string | null => {
  const validationError = validateOfflineEvent(eventData);
  if (validationError) {
    console.warn('❌ Invalid offline event, not saving:', validationError, eventData);
    return validationError;
  }

  try {
    const queue = getOfflineEvents();
    const newEvent: OfflineEvent = {
      offlineEventId: uuidv4(),
      ...eventData,
      status: 'pending',
      retryCount: 0,
    };
    queue.push(newEvent);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
    return null; // success
  } catch (error) {
    // console.error("❌ Failed to save offline event:", error);
    throw error;
  }
};

export const removeOfflineEvent = (offlineEventId: string): void => {
  const queue = getOfflineEvents();
  const filtered = queue.filter((e) => e.offlineEventId !== offlineEventId);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(filtered));
};

export const clearOfflineQueue = (): void => {
  localStorage.removeItem(STORAGE_KEY);
};

export const getOfflineQueueCount = (): number => {
  return getOfflineEvents().length;
};

/**
 * Cập nhật trạng thái và lỗi cho một event
 */
export const updateOfflineEventStatus = (
  offlineEventId: string,
  updates: Partial<Pick<OfflineEvent, 'status' | 'errorMessage' | 'retryCount'>>
): void => {
  const queue = getOfflineEvents();
  const event = queue.find((e) => e.offlineEventId === offlineEventId);
  if (!event) return;
  Object.assign(event, updates);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
};