import { describe, expect, it } from 'vitest';
import { AxiosError, AxiosHeaders } from 'axios';

import {
  formatDateTimeWithSeconds,
  getCoopWarehouseErrorMessage,
  getCurrentDatetimeString,
} from '../coopWarehouseUtils';

describe('coopWarehouseUtils', () => {
  it('getCurrentDatetimeString trả về chuỗi ISO dạng YYYY-MM-DDTHH:mm', () => {
    const result = getCurrentDatetimeString();
    expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
  });

  it('formatDateTimeWithSeconds bổ sung :00 cho chuỗi 16 ký tự', () => {
    expect(formatDateTimeWithSeconds('2026-09-22T14:30')).toBe('2026-09-22T14:30:00');
    expect(formatDateTimeWithSeconds('2026-09-22T14:30:45')).toBe('2026-09-22T14:30:45');
    expect(formatDateTimeWithSeconds('')).toBe('');
    expect(formatDateTimeWithSeconds(undefined)).toBe('');
  });

  it('getCoopWarehouseErrorMessage ưu tiên thông báo từ Axios response', () => {
    const error = new AxiosError('Network Error');
    error.response = {
      data: { message: 'Lô hàng không tồn tại trong kho' },
      status: 400,
      statusText: 'Bad Request',
      headers: {},
      config: { headers: new AxiosHeaders() },
    };

    expect(getCoopWarehouseErrorMessage(error, 'Lỗi mặc định')).toBe('Lô hàng không tồn tại trong kho');
  });

  it('getCoopWarehouseErrorMessage trả về Error.message khi không có Axios response', () => {
    const error = new Error('Lỗi kết nối nội bộ');
    expect(getCoopWarehouseErrorMessage(error, 'Lỗi mặc định')).toBe('Lỗi kết nối nội bộ');
  });

  it('getCoopWarehouseErrorMessage trả về fallback khi lỗi không xác định', () => {
    expect(getCoopWarehouseErrorMessage('unknown', 'Lỗi mặc định')).toBe('Lỗi mặc định');
    expect(getCoopWarehouseErrorMessage(null, 'Lỗi mặc định')).toBe('Lỗi mặc định');
  });
});
