import { beforeEach, describe, expect, it } from 'vitest';
import { ChainEventType } from '@/enums/chainEventType';
import {
  MAX_OFFLINE_EVENTS,
  addOfflineEvent,
  clearOfflineQueue,
  getOfflineEvents,
  getOfflineQueueCount,
  removeOfflineEvent,
  restoreOfflineEvent,
  updateOfflineEventStatus,
} from '@/services/offlineQueue';

const suKienFarmLog = () => ({
  productionLotId: '85d91b0c-c3b8-4c1f-bcb0-2b86737d1406',
  eventType: ChainEventType.FARM_LOG,
  recordedAt: '2026-09-16T08:30:00',
  latitude: 0,
  longitude: 0,
  images: [],
  deviceSource: 'WEB',
  eventData: {
    productionLotId: '85d91b0c-c3b8-4c1f-bcb0-2b86737d1406',
    activityType: 'FERTILIZING',
    material: 'NPK 16-16-8',
    quantity: 25.0,
    unit: 'kg',
    executedDate: '2026-09-15',
    notes: 'Bón phân lần 1',
  },
});

beforeEach(() => {
  clearOfflineQueue();
});

describe('offlineQueue (hàng chờ chung)', () => {
  it('xếp nhật ký canh tác đúng chuẩn OfflineEvent để đồng bộ qua /chain-events/sync', () => {
    expect(addOfflineEvent(suKienFarmLog())).toBeNull();

    const [banGhi] = getOfflineEvents();
    expect(banGhi.offlineEventId).toBeTruthy();
    expect(banGhi.eventType).toBe('FARM_LOG');
    expect(banGhi.images).toEqual([]);
    expect(banGhi.status).toBe('pending');
    expect(banGhi.retryCount).toBe(0);
    expect(banGhi.eventData.activityType).toBe('FERTILIZING');
  });

  it('giữ offlineEventId do phía gọi truyền sẵn (để gắn ảnh pha 2)', () => {
    expect(addOfflineEvent({ ...suKienFarmLog(), offlineEventId: 'evt-codinh' })).toBeNull();
    expect(getOfflineEvents()[0].offlineEventId).toBe('evt-codinh');
  });

  it('chặn bản ghi mới khi hàng chờ đã đầy, không xóa bản ghi cũ', () => {
    for (let i = 0; i < MAX_OFFLINE_EVENTS; i += 1) {
      expect(addOfflineEvent(suKienFarmLog())).toBeNull();
    }
    expect(addOfflineEvent(suKienFarmLog())).toMatch(/đã đầy/);
    expect(getOfflineQueueCount()).toBe(MAX_OFFLINE_EVENTS);
  });

  it('cập nhật trạng thái kèm mốc backoff và mã nhật ký pha 2', () => {
    addOfflineEvent(suKienFarmLog());
    const id = getOfflineEvents()[0].offlineEventId;

    updateOfflineEventStatus(id, {
      status: 'da-ghi',
      farmLogId: 'log-1',
      lastSyncAttempt: 123,
    });

    expect(getOfflineEvents()[0]).toMatchObject({
      status: 'da-ghi',
      farmLogId: 'log-1',
      lastSyncAttempt: 123,
    });

    removeOfflineEvent(id);
    expect(getOfflineQueueCount()).toBe(0);
  });

  it('khôi phục bản ghi cũ giữ nguyên trạng thái, bỏ qua khi trùng mã', () => {
    addOfflineEvent(suKienFarmLog());
    const goc = getOfflineEvents()[0];

    expect(restoreOfflineEvent(goc)).toBe('duplicate');
    expect(
      restoreOfflineEvent({ ...goc, offlineEventId: 'evt-moi', status: 'invalid' }),
    ).toBe('ok');
    expect(getOfflineQueueCount()).toBe(2);
  });
});
