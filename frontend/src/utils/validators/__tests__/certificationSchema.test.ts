// @vitest-environment jsdom

import { describe, expect, it } from 'vitest';
import { createCertificationSchema } from '@/utils/validators';

const currentYear = new Date().getFullYear();
const validMetadata = {
  standardId: '21c4445c-36a4-4103-bf0b-c283339d22b5',
  code: 'VGP-2026-00125',
  issuedBy: 'Trung tâm Chứng nhận Chất lượng',
  issueDate: `${currentYear}-01-15`,
  expiryDate: `${currentYear + 1}-01-14`,
};

describe('createCertificationSchema', () => {
  it('chấp nhận chứng nhận có tệp PDF hợp lệ', () => {
    const result = createCertificationSchema.safeParse({
      ...validMetadata,
      document: new File(['%PDF-test'], 'vietgap.pdf', { type: 'application/pdf' }),
    });

    expect(result.success).toBe(true);
  });

  it('từ chối loại tệp không hỗ trợ', () => {
    const result = createCertificationSchema.safeParse({
      ...validMetadata,
      document: new File(['test'], 'vietgap.txt', { type: 'text/plain' }),
    });

    expect(result.success).toBe(false);
  });
});
