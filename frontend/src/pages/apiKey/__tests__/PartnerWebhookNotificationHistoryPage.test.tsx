import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { PartnerWebhookNotificationHistoryPage } from '../PartnerWebhookNotificationHistoryPage';
import { getPartnerWebhookNotifications, getApiKeys } from '@/api/apiKeyApi';
import type { PartnerApiKeyResponse, PartnerWebhookNotificationResponse } from '@/types/apiKey';

vi.mock('@/api/apiKeyApi', () => ({
  getPartnerWebhookNotifications: vi.fn(),
  getApiKeys: vi.fn(),
}));

vi.mock('@/components/common/AppBreadcrumb', () => ({
  useSetBreadcrumb: vi.fn(),
}));

describe('PartnerWebhookNotificationHistoryPage (NCL-12-CN-006)', () => {
  const mockApiKey: PartnerApiKeyResponse = {
    id: 'test-key-uuid-1',
    organizationId: 'org-uuid-1',
    partnerName: 'Công ty thực phẩm Aigu',
    keyPrefix: 'nks_live_2fa373e5',
    rateLimitPerHour: 1000,
    expiresAt: '2026-12-31T23:59:59',
    status: 'ACTIVE',
    totalCalls: 12,
    failedCalls: 0,
    createdAt: '2026-09-01T10:00:00',
    webhookUrl: 'https://webhook.site/b868c229-165b-4cf3-8b72-4111b84b9c55',
    webhookSecret: 'sec_wh_1234567890abcdef',
    isWebhookActive: true,
  };

  const mockNotifications: PartnerWebhookNotificationResponse[] = [
    {
      id: 'notif-uuid-1',
      partnerApiKeyId: 'test-key-uuid-1',
      partnerName: 'Công ty thực phẩm Aigu',
      lotCode: 'GGJ00000005',
      newStatus: 'RECALLED',
      deliveryStatus: 'SUCCESS',
      attemptCount: 1,
      maxAttempts: 5,
      targetUrl: 'https://webhook.site/b868c229-165b-4cf3-8b72-4111b84b9c55',
      lastHttpStatus: 200,
      publicReason: 'Dư lượng thuốc bảo vệ thực vật',
      createdAt: '2026-09-15T14:10:50',
      attempts: [
        {
          attemptNumber: 1,
          attemptedAt: '2026-09-15T14:10:51',
          httpStatus: 200,
          responseBody: '{"status":"ok"}',
          errorMessage: null,
          durationMs: 85,
        },
      ],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getApiKeys).mockResolvedValue({
      content: [mockApiKey],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
    });
  });

  const renderPage = (initialEntries = ['/integration/api-keys/test-key-uuid-1/notifications']) =>
    render(
      <MemoryRouter initialEntries={initialEntries}>
        <Routes>
          <Route
            path="/integration/api-keys/:id/notifications"
            element={<PartnerWebhookNotificationHistoryPage />}
          />
        </Routes>
      </MemoryRouter>
    );

  it('renders full page with header, partner info, and summary stats', async () => {
    vi.mocked(getPartnerWebhookNotifications).mockResolvedValue({
      content: mockNotifications,
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
    });

    renderPage();

    expect(screen.getByText(/Lịch sử gửi thông báo thu hồi/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('GGJ00000005')).toBeInTheDocument();
      expect(screen.getAllByText(/Thành công/i).length).toBeGreaterThan(0);
    });

    // Stats cards
    expect(screen.getByText(/Tổng gói tin gửi đi/i)).toBeInTheDocument();
    expect(screen.getByText(/Gửi thành công/i)).toBeInTheDocument();
  });

  it('allows expanding and collapsing the attempt details log in the data table', async () => {
    vi.mocked(getPartnerWebhookNotifications).mockResolvedValue({
      content: mockNotifications,
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('GGJ00000005')).toBeInTheDocument();
    });

    const expandBtn = screen.getByRole('button', { name: /Chi tiết/i });
    expect(expandBtn).toBeInTheDocument();
    fireEvent.click(expandBtn);

    await waitFor(() => {
      expect(screen.getByText(/Nhật ký chi tiết các lần gửi thông báo/i)).toBeInTheDocument();
      expect(screen.getByText(/Lần #1/i)).toBeInTheDocument();
      expect(screen.getByText(/85 ms/i)).toBeInTheDocument();
      expect(screen.getAllByText(/HTTP 200/i).length).toBeGreaterThan(0);
    });
  });

  it('displays empty state when no webhook notifications exist for the partner', async () => {
    vi.mocked(getPartnerWebhookNotifications).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Chưa có thông báo thu hồi nào được gửi tới đối tác này/i)).toBeInTheDocument();
    });
  });
});
