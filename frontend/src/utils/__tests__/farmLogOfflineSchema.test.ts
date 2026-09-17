import { describe, expect, it } from 'vitest';
import { farmLogOfflineSchema } from '@/utils/validators';
import { getLocalDateString } from '@/utils/dateTime';

const hopLe = {
  productionLotId: '85d91b0c-c3b8-4c1f-bcb0-2b86737d1406',
  activityType: 'FERTILIZING',
  material: 'NPK 16-16-8',
  quantity: 25,
  unit: 'kg',
  executedDate: '2026-09-15',
  notes: 'Bón phân lần 1',
};

describe('farmLogOfflineSchema', () => {
  it('chấp nhận bản ghi hợp lệ đầy đủ', () => {
    expect(() => farmLogOfflineSchema.parse(hopLe)).not.toThrow();
  });

  it('chấp nhận bản ghi tối thiểu (không vật tư/số lượng/ghi chú)', () => {
    expect(() =>
      farmLogOfflineSchema.parse({
        productionLotId: hopLe.productionLotId,
        activityType: 'WATERING',
        executedDate: '2026-09-15',
      }),
    ).not.toThrow();
  });

  it('báo lỗi khi thiếu lô và loại hoạt động', () => {
    const ketQua = farmLogOfflineSchema.safeParse({
      executedDate: '2026-09-15',
    });
    expect(ketQua.success).toBe(false);
  });

  it('báo lỗi khi số lượng <= 0', () => {
    const ketQua = farmLogOfflineSchema.safeParse({ ...hopLe, quantity: 0 });
    expect(ketQua.success).toBe(false);
  });

  it('báo lỗi khi ngày thực hiện ở tương lai', () => {
    const ketQua = farmLogOfflineSchema.safeParse({
      ...hopLe,
      executedDate: '2999-01-01',
    });
    expect(ketQua.success).toBe(false);
    if (!ketQua.success) {
      expect(ketQua.error.issues[0]?.message).toContain('tương lai');
    }
  });

  it('chấp nhận ngày hôm nay theo giờ local (không phụ thuộc múi giờ)', () => {
    const ketQua = farmLogOfflineSchema.safeParse({
      ...hopLe,
      executedDate: getLocalDateString(),
    });
    expect(ketQua.success).toBe(true);
  });

  it('báo lỗi khi ngày thực hiện là ngày mai', () => {
    const ngayMai = new Date();
    ngayMai.setDate(ngayMai.getDate() + 1);
    const ketQua = farmLogOfflineSchema.safeParse({
      ...hopLe,
      executedDate: getLocalDateString(ngayMai),
    });
    expect(ketQua.success).toBe(false);
  });

  it('báo lỗi khi ghi chú vượt 1000 ký tự', () => {
    const ketQua = farmLogOfflineSchema.safeParse({
      ...hopLe,
      notes: 'a'.repeat(1001),
    });
    expect(ketQua.success).toBe(false);
  });
});
