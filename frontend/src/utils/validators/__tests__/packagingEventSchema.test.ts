import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { recordPackagingSchema, correctPackagingSchema } from '../packagingEventSchema';
import { getLocalDateString } from '@/utils/dateTime';

describe('packagingEventSchema', () => {
  const validUuid = '123e4567-e89b-12d3-a456-426614174000';

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('recordPackagingSchema - packagingDate validation', () => {
    it('accepts today date at early morning (06:00)', () => {
      // Mock local system time at 06:00 on 2026-09-07
      const mockDate = new Date(2026, 8, 7, 6, 0, 0); // month is 0-indexed: 8 = Sep
      vi.useFakeTimers();
      vi.setSystemTime(mockDate);

      const todayStr = getLocalDateString();
      expect(todayStr).toBe('2026-09-07');

      const result = recordPackagingSchema.safeParse({
        productionLotId: validUuid,
        packagingSpecification: 'Túi 500g',
        packagingDate: todayStr,
      });

      expect(result.success).toBe(true);
    });

    it('accepts today date at noon (12:00)', () => {
      const mockDate = new Date(2026, 8, 7, 12, 0, 0);
      vi.useFakeTimers();
      vi.setSystemTime(mockDate);

      const todayStr = getLocalDateString();
      const result = recordPackagingSchema.safeParse({
        productionLotId: validUuid,
        packagingSpecification: 'Túi 500g',
        packagingDate: todayStr,
      });

      expect(result.success).toBe(true);
    });

    it('accepts today date at late evening (23:59)', () => {
      const mockDate = new Date(2026, 8, 7, 23, 59, 59);
      vi.useFakeTimers();
      vi.setSystemTime(mockDate);

      const todayStr = getLocalDateString();
      const result = recordPackagingSchema.safeParse({
        productionLotId: validUuid,
        packagingSpecification: 'Túi 500g',
        packagingDate: todayStr,
      });

      expect(result.success).toBe(true);
    });

    it('rejects tomorrow date as future date', () => {
      const mockDate = new Date(2026, 8, 7, 10, 0, 0);
      vi.useFakeTimers();
      vi.setSystemTime(mockDate);

      const tomorrowStr = '2026-09-08';
      const result = recordPackagingSchema.safeParse({
        productionLotId: validUuid,
        packagingSpecification: 'Túi 500g',
        packagingDate: tomorrowStr,
      });

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors.some(e => e.message === 'Ngày đóng gói không được là ngày ở tương lai')).toBe(true);
      }
    });

    it('rejects invalid date format', () => {
      const result = recordPackagingSchema.safeParse({
        productionLotId: validUuid,
        packagingSpecification: 'Túi 500g',
        packagingDate: '07/09/2026',
      });

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors.some(e => e.message === 'Ngày không đúng định dạng YYYY-MM-DD')).toBe(true);
      }
    });
  });

  describe('correctPackagingSchema - packagingDate validation', () => {
    it('accepts today date and valid correction reason', () => {
      const mockDate = new Date(2026, 8, 7, 5, 30, 0);
      vi.useFakeTimers();
      vi.setSystemTime(mockDate);

      const todayStr = getLocalDateString();
      const result = correctPackagingSchema.safeParse({
        packagingSpecification: 'Túi 1kg',
        packagingDate: todayStr,
        correctionReason: 'Đính chính quy cách đóng gói',
      });

      expect(result.success).toBe(true);
    });

    it('rejects future date for correction', () => {
      const mockDate = new Date(2026, 8, 7, 5, 30, 0);
      vi.useFakeTimers();
      vi.setSystemTime(mockDate);

      const result = correctPackagingSchema.safeParse({
        packagingSpecification: 'Túi 1kg',
        packagingDate: '2026-09-08',
        correctionReason: 'Đính chính quy cách đóng gói',
      });

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors.some(e => e.message === 'Ngày đóng gói không được là ngày ở tương lai')).toBe(true);
      }
    });
  });
});
