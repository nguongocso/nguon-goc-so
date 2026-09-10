import { describe, expect, it } from 'vitest';

import type { ShipmentSplitAllocation, ShipmentSplitPreview } from '@/types/shipmentSplit';
import { validateShipmentSplit } from '@/utils/shipmentSplitValidation';

const preview: ShipmentSplitPreview = {
  shipmentId: 'shipment-1',
  shipmentName: 'Lô thanh long',
  status: 'CODE_PRINTED',
  productionLotId: 'lot-1',
  productionLotName: 'Thanh long tháng 9',
  declaredQuantity: 1000,
  assignableQuantity: 1000,
  nonInactiveQuantity: 0,
  availableCodeRange: { fromCode: 'HTX00000001', toCode: 'HTX00001000', quantity: 1000 },
  canSplit: true,
  blockReasonCode: null,
  blockMessage: null,
};

function allocation(overrides: Partial<ShipmentSplitAllocation>): ShipmentSplitAllocation {
  return {
    recipientOrganizationId: 'partner-1',
    name: 'Lô con',
    quantity: 500,
    fromCode: 'HTX00000001',
    toCode: 'HTX00000500',
    ...overrides,
  };
}

describe('validateShipmentSplit', () => {
  it('chấp nhận hai lô con phủ đủ 1.000 tem liên tục', () => {
    const result = validateShipmentSplit(preview, [
      allocation({}),
      allocation({
        recipientOrganizationId: 'partner-2',
        fromCode: 'HTX00000501',
        toCode: 'HTX00001000',
      }),
    ]);

    expect(result.isValid).toBe(true);
    expect(result.allocatedQuantity).toBe(1000);
    expect(result.errors).toEqual([]);
  });

  it('chặn tổng số lượng lô con vượt số lượng lô cha', () => {
    const result = validateShipmentSplit(preview, [
      allocation({ quantity: 600, toCode: 'HTX00000600' }),
      allocation({
        recipientOrganizationId: 'partner-2',
        quantity: 600,
        fromCode: 'HTX00000601',
        toCode: 'HTX00001200',
      }),
    ]);

    expect(result.isValid).toBe(false);
    expect(result.errors).toContain('Đã phân bổ vượt 200 tem.');
  });

  it('chặn phân bổ nhiều lô con cho cùng một đối tác', () => {
    const result = validateShipmentSplit(preview, [
      allocation({}),
      allocation({ fromCode: 'HTX00000501', toCode: 'HTX00001000' }),
    ]);

    expect(result.errors).toContain('Mỗi đối tác chỉ được nhận một lô con.');
  });

  it('chặn khoảng mã bị hở hoặc chồng lấn', () => {
    const result = validateShipmentSplit(preview, [
      allocation({ quantity: 499, toCode: 'HTX00000499' }),
      allocation({
        recipientOrganizationId: 'partner-2',
        quantity: 501,
        fromCode: 'HTX00000501',
        toCode: 'HTX00001000',
      }),
    ]);

    expect(result.isValid).toBe(false);
    expect(result.errors.some((error) => error.includes('bị hở hoặc chồng lấn'))).toBe(true);
  });
});
