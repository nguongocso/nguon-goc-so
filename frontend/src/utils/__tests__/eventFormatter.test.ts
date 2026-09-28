import { describe, expect, it } from 'vitest';
import {
  formatFieldLabel,
  getDisplayEventDataEntries,
  getTranslatedEventData,
} from '@/utils/eventFormatter';

describe('eventFormatter cho sự kiện tách lô', () => {
  it('chuyển toàn bộ trường nghiệp vụ sang nhãn tiếng Việt', () => {
    expect(formatFieldLabel('sourceShipmentName')).toBe('Tên lô hàng nguồn');
    expect(formatFieldLabel('recipientOrganizationName')).toBe('Đối tác nhận hàng');
    expect(formatFieldLabel('allocatedQuantity')).toBe('Số lượng phân bổ');
    expect(formatFieldLabel('fromCode')).toBe('Mã bắt đầu');
    expect(formatFieldLabel('toCode')).toBe('Mã kết thúc');
  });

  it('ẩn trường kỹ thuật và sắp xếp thông tin theo thứ tự nghiệp vụ', () => {
    const eventData = {
      toCode: 'PPT00000020',
      sourceShipmentId: 'source-id',
      allocatedQuantity: 5,
      recipientOrganizationName: 'Công ty Thái Nguyên',
      fromCode: 'PPT00000016',
      sourceLastEventHash: 'event-hash',
      recipientOrganizationId: 'recipient-id',
      sourceShipmentName: 'Lô chuối Tân Cương',
    };

    expect(getDisplayEventDataEntries('SPLIT', eventData).map(([key]) => key)).toEqual([
      'sourceShipmentName',
      'recipientOrganizationName',
      'allocatedQuantity',
      'fromCode',
      'toCode',
    ]);
    expect(getTranslatedEventData('SPLIT', eventData)).toEqual({
      'Tên lô hàng nguồn': 'Lô chuối Tân Cương',
      'Đối tác nhận hàng': 'Công ty Thái Nguyên',
      'Số lượng phân bổ': '5',
      'Mã bắt đầu': 'PPT00000016',
      'Mã kết thúc': 'PPT00000020',
    });
  });
});
