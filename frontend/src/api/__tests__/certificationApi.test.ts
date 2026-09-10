import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/api/axiosConfig', () => ({
  default: {
    post: vi.fn(),
  },
}));

import apiClient from '@/api/axiosConfig';
import { createCertification } from '@/api/certificationApi';

describe('certificationApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('gửi metadata và tài liệu chứng nhận bằng multipart', async () => {
    const payload = {
      standardId: '21c4445c-36a4-4103-bf0b-c283339d22b5',
      code: 'VGP-2026-00125',
      issuedBy: 'Trung tâm Chứng nhận Chất lượng',
      issueDate: '2026-01-15',
      expiryDate: '2027-01-14',
    };
    const file = new File(['%PDF-test'], 'vietgap.pdf', { type: 'application/pdf' });
    const response = { id: 'certificate-1', ...payload, name: 'VietGAP', isValid: true };
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { data: response } });

    const result = await createCertification(payload, file);

    expect(apiClient.post).toHaveBeenCalledOnce();
    const [url, body, config] = vi.mocked(apiClient.post).mock.calls[0];
    expect(url).toBe('/certifications');
    expect(body).toBeInstanceOf(FormData);
    expect((body as FormData).get('file')).toBe(file);
    expect((body as FormData).get('data')).toBeInstanceOf(Blob);
    expect(config).toMatchObject({
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    expect(result).toEqual(response);
  });
});
