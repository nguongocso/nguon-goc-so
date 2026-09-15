import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { WebhookNotificationHistoryModal } from '../WebhookNotificationHistoryModal';
import { getPartnerWebhookNotifications } from '@/api/apiKeyApi';
import type { PartnerApiKeyResponse, PartnerWebhookNotificationResponse } from '@/types/apiKey';

vi.mock('@/api/apiKeyApi', () => ({
  getPartnerWebhookNotifications: vi.fn(),
}));

describe('WebhookNotificationHistoryModal (NCL-12-CN-006)', () => {
  const mockApiKey: PartnerApiKeyResponse = {
    id: 'test-key-uuid-1',
    organizationId: 'org-uuid-1',
    partnerName: 'Doanh Nghiệp Thu Mua Lúa Gạo',
    keyPrefix: 'nks_live_abc123',
    rateLimitPerHour: 1000,
    expiresAt: '2026-12-31T23:59:59',
    status: 'ACTIVE',
    totalCalls: 12,
    failedCalls: 0,
    createdAt: '2026-09-01T10:00:00',
    webhookUrl: 'https://partner.com/webhook',
    webhookSecret: 'sec_wh_1234567890abcdef',
    isWebhookActive: true,
  };

  const mockNotifications: PartnerWebhookNotificationResponse[] = [
    {
      id: 'notif-uuid-1',
      partnerApiKeyId: 'test-key-uuid-1',
      partnerName: 'Doanh Nghiệp Thu Mua Lúa Gạo',
      lotCode: 'LOT-2026-GAO-ST25',
      newStatus: 'RECALLING',
      deliveryStatus: 'SUCCESS',
      attemptCount: 1,
      maxAttempts: 5,
      targetUrl: 'https://partner.com/webhook',
      lastHttpStatus: 200,
      createdAt: '2026-09-15T10:00:00',
      attempts: [
        {
          attemptNumber: 1,
          attemptedAt: '2026-09-15T10:00:02',
          httpStatus: 200,
          responseBody: '{"received": true}',
          errorMessage: null,
          durationMs: 120,
        },
      ],
    },
    {
      id: 'notif-uuid-2',
      partnerApiKeyId: 'test-key-uuid-1',
      partnerName: 'Doanh Nghiệp Thu Mua Lúa Gạo',
      lotCode: 'LOT-2026-XOAI-CAT',
      newStatus: 'RECALLED',
      deliveryStatus: 'FAILED',
      attemptCount: 5,
      maxAttempts: 5,
      targetUrl: 'https://partner.com/webhook',
      lastHttpStatus: 500,
      lastErrorMessage: 'HTTP 500 Internal Server Error',
      createdAt: '2026-09-14T08:00:00',
      attempts: [
        {
          attemptNumber: 1,
          attemptedAt: '2026-09-14T08:00:02',
          httpStatus: 500,
          responseBody: 'Internal Error',
          errorMessage: 'Server 500',
          durationMs: 350,
        },
        {
          attemptNumber: 2,
          attemptedAt: '2026-09-14T08:05:02',
          httpStatus: 500,
          responseBody: 'Internal Error',
          errorMessage: 'Server 500',
          durationMs: 320,
        },
      ],
    },
  ];

  const mockOnClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders history modal and displays notification list', async () => {
    vi.mocked(getPartnerWebhookNotifications).mockResolvedValue({
      content: mockNotifications,
      totalElements: 2,
      totalPages: 1,
      size: 10,
      number: 0,
    });

    render(
      <WebhookNotificationHistoryModal
        open={true}
        apiKey={mockApiKey}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText(/Lịch sử gửi thông báo Webhook thu hồi/i)).toBeInTheDocument();
    expect(screen.getByText(/Doanh Nghiệp Thu Mua Lúa Gạo/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('LOT-2026-GAO-ST25')).toBeInTheDocument();
      expect(screen.getByText('LOT-2026-XOAI-CAT')).toBeInTheDocument();
    });

    // Check delivery badges
    expect(screen.getByText(/Thành công/i)).toBeInTheDocument();
    expect(screen.getByText(/Thất bại/i)).toBeInTheDocument();
  });

  it('allows expanding a notification item to view attempt log details', async () => {
    vi.mocked(getPartnerWebhookNotifications).mockResolvedValue({
      content: mockNotifications,
      totalElements: 2,
      totalPages: 1,
      size: 10,
      number: 0,
    });

    render(
      <WebhookNotificationHistoryModal
        open={true}
        apiKey={mockApiKey}
        onClose={mockOnClose}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('LOT-2026-GAO-ST25')).toBeInTheDocument();
    });

    // Find and click the button to expand attempts
    const expandBtn = screen.getByText(/Xem chi tiết 1 lần gửi/i);
    expect(expandBtn).toBeInTheDocument();
    fireEvent.click(expandBtn);

    await waitFor(() => {
      expect(screen.getByText(/Lần #1/i)).toBeInTheDocument();
      expect(screen.getByText('120ms')).toBeInTheDocument();
      expect(screen.getByText(/HTTP 200/i)).toBeInTheDocument();
    });
  });

  it('displays empty state when no notifications are found', async () => {
    vi.mocked(getPartnerWebhookNotifications).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
    });

    render(
      <WebhookNotificationHistoryModal
        open={true}
        apiKey={mockApiKey}
        onClose={mockOnClose}
      />
    );

    await waitFor(() => {
      expect(screen.getByText(/Chưa có thông báo thu hồi nào được gửi tới đối tác này/i)).toBeInTheDocument();
    });
  });
});
