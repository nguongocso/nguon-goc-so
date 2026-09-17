import { describe, it, expect } from 'vitest';
import { normalizeVietnamese, sanitizeResponseBody } from '../string';

describe('normalizeVietnamese', () => {
  it('loại bỏ dấu tiếng Việt và chuyển chữ thường', () => {
    expect(normalizeVietnamese('Lô Trà Tân Cương Đạt Chuẩn')).toBe('lo tra tan cuong dat chuan');
  });

  it('xử lý chuỗi rỗng hoặc undefined', () => {
    expect(normalizeVietnamese('')).toBe('');
    expect(normalizeVietnamese(null as any)).toBe('');
  });
});

describe('sanitizeResponseBody', () => {
  it('loại bỏ thẻ HTML và giữ nguyên văn bản thuần', () => {
    const raw =
      'This URL has no default content configured. <a href="https://webhook.site/#!/edit/2dcb171e">Change response in Webhook.site</a>.';
    expect(sanitizeResponseBody(raw)).toBe(
      'This URL has no default content configured. Change response in Webhook.site.'
    );
  });

  it('giữ nguyên chuỗi JSON hoặc văn bản không chứa HTML', () => {
    const json = '{"status":"success","received":true}';
    expect(sanitizeResponseBody(json)).toBe(json);
  });

  it('xử lý chuỗi HTML lồng nhau phức tạp', () => {
    const html = '<div class="alert"><p>Lỗi máy chủ: <strong>500 Internal Error</strong></p></div>';
    expect(sanitizeResponseBody(html)).toBe('Lỗi máy chủ: 500 Internal Error');
  });

  it('xử lý giá trị rỗng, null hoặc undefined an toàn', () => {
    expect(sanitizeResponseBody('')).toBe('');
    expect(sanitizeResponseBody(null)).toBe('');
    expect(sanitizeResponseBody(undefined)).toBe('');
  });
});
