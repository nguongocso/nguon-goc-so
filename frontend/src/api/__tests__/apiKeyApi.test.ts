import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getPartnerWebhookNotifications } from '../apiKeyApi';
import apiClient from '@/api/axiosConfig';

vi.mock('@/api/axiosConfig', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('apiKeyApi webhook notification contract', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('gửi đúng query param deliveryStatus khi lọc theo trạng thái (NCL-12-CN-006)', async () => {
    const mockData = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
    };

    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: {
        success: true,
        data: mockData,
      },
    });

    const apiKeyId = 'uuid-key-123';
    const result = await getPartnerWebhookNotifications(apiKeyId, 'FAILED', 1, 20);

    expect(apiClient.get).toHaveBeenCalledWith(
      `/organization/api-keys/${apiKeyId}/notifications`,
      {
        params: {
          page: 1,
          size: 20,
          deliveryStatus: 'FAILED',
        },
      }
    );
    expect(result).toEqual(mockData);
  });

  it('không truyền deliveryStatus khi status là undefined', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: {
        success: true,
        data: { content: [] },
      },
    });

    await getPartnerWebhookNotifications('uuid-key-456', undefined, 0, 10);

    expect(apiClient.get).toHaveBeenCalledWith(
      '/organization/api-keys/uuid-key-456/notifications',
      {
        params: {
          page: 0,
          size: 10,
        },
      }
    );
  });
});
