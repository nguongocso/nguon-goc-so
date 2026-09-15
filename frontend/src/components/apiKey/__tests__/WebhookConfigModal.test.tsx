import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { WebhookConfigModal } from '../WebhookConfigModal';
import { updatePartnerWebhook, testPingPartnerWebhook } from '@/api/apiKeyApi';
import { toast } from 'sonner';
import type { PartnerApiKeyResponse } from '@/types/apiKey';

vi.mock('@/api/apiKeyApi', () => ({
  updatePartnerWebhook: vi.fn(),
  testPingPartnerWebhook: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}));

describe('WebhookConfigModal (NCL-12-CN-006)', () => {
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

  const mockOnClose = vi.fn();
  const mockOnSuccess = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders modal with partner name and existing webhook URL', () => {
    render(
      <WebhookConfigModal
        open={true}
        apiKey={mockApiKey}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    expect(screen.getByText(/Khai báo thông tin nhận thông báo thu hồi/i)).toBeInTheDocument();
    expect(screen.getByText(/Đối tác: Doanh Nghiệp Thu Mua Lúa Gạo/i)).toBeInTheDocument();
    const urlInput = screen.getByLabelText(/Địa chỉ tiếp nhận thông báo/i) as HTMLInputElement;
    expect(urlInput.value).toBe('https://partner.com/webhook');
  });

  it('shows validation error when non-HTTPS URL is entered (TC-02 security)', async () => {
    render(
      <WebhookConfigModal
        open={true}
        apiKey={mockApiKey}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    const urlInput = screen.getByLabelText(/Địa chỉ tiếp nhận thông báo/i);
    fireEvent.change(urlInput, { target: { value: 'http://insecure-partner.com/webhook' } });

    expect(
      await screen.findByText(/Địa chỉ Webhook bắt buộc phải sử dụng giao thức bảo mật HTTPS/i)
    ).toBeInTheDocument();
  });

  it('calls updatePartnerWebhook on save with valid HTTPS URL', async () => {
    vi.mocked(updatePartnerWebhook).mockResolvedValueOnce({
      id: 'test-key-uuid-1',
      partnerName: 'Doanh Nghiệp Thu Mua Lúa Gạo',
      keyPrefix: 'nks_live_abc123',
      webhookUrl: 'https://partner.com/new-webhook',
      isWebhookActive: true,
      webhookSecret: 'sec_wh_1234567890abcdef',
    });

    render(
      <WebhookConfigModal
        open={true}
        apiKey={mockApiKey}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    const urlInput = screen.getByLabelText(/Địa chỉ tiếp nhận thông báo/i);
    fireEvent.change(urlInput, { target: { value: 'https://partner.com/new-webhook' } });

    const saveButton = screen.getByRole('button', { name: /Lưu cấu hình/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(updatePartnerWebhook).toHaveBeenCalledWith('test-key-uuid-1', {
        webhookUrl: 'https://partner.com/new-webhook',
        isActive: true,
      });
      expect(mockOnSuccess).toHaveBeenCalled();
      expect(mockOnClose).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('Lưu thông tin nhận thông báo thành công!');
    });
  });

  it('performs test ping and displays response details', async () => {
    vi.mocked(testPingPartnerWebhook).mockResolvedValueOnce({
      targetUrl: 'https://partner.com/webhook',
      httpStatus: 200,
      durationMs: 145,
      isSuccess: true,
      responseBody: '{"status":"ok"}',
    });

    render(
      <WebhookConfigModal
        open={true}
        apiKey={mockApiKey}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    const pingButton = screen.getByRole('button', { name: /Gửi thử nghiệm kết nối/i });
    fireEvent.click(pingButton);

    await waitFor(() => {
      expect(testPingPartnerWebhook).toHaveBeenCalledWith('test-key-uuid-1');
      expect(screen.getByText(/Kết nối thành công!/i)).toBeInTheDocument();
      expect(screen.getByText(/HTTP 200/i)).toBeInTheDocument();
      expect(screen.getByText(/145ms/i)).toBeInTheDocument();
    });
  });
});
