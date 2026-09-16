import { describe, expect, it } from 'vitest';

import type { SplitShipmentResult } from '@/types/shipmentSplit';

describe('SplitShipmentResult', () => {
  it('mô tả đúng phản hồi POST tách lô thành công', () => {
    const result: SplitShipmentResult = {
      sourceShipment: {
        id: 'source-1',
        name: 'Lô cha',
        status: 'SPLIT',
        declaredQuantity: 1000,
        allocatedQuantity: 1000,
      },
      children: [
        {
          id: 'child-1',
          parentShipmentId: 'source-1',
          name: 'Lô con',
          status: 'CODE_PRINTED',
          recipientOrganization: { id: 'org-1', code: 'DN-01', name: 'Đối tác nhận' },
          totalQuantity: 1000,
          firstCode: 'HTX00000001',
          lastCode: 'HTX00001000',
        },
      ],
      totalChildren: 1,
      totalAllocatedQuantity: 1000,
      splitByName: 'Quản lý HTX',
      splitAt: '2026-09-10T15:10:00',
    };

    expect(result.children).toHaveLength(result.totalChildren);
    expect(result.children[0].recipientOrganization.name).toBe('Đối tác nhận');
  });
});
