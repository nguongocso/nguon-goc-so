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

  it('hiển thị phản hồi HTML của đối tác dưới dạng văn bản thuần, không lộ thẻ HTML', async () => {
    const rawBody =
      'This URL has no default content configured. <a href="https://webhook.site/#!/edit/abc">Change response in Webhook.site</a>.';

    vi.mocked(testPingPartnerWebhook).mockResolvedValueOnce({
      targetUrl: 'https://webhook.site/abc',
      httpStatus: 200,
      durationMs: 1027,
      isSuccess: true,
      responseBody: rawBody,
    });

    const { container } = render(
      <WebhookConfigModal
        open={true}
        apiKey={mockApiKey}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /Gửi thử nghiệm kết nối/i }));

    await waitFor(() => {
      expect(screen.getByText(/Kết nối thành công!/i)).toBeInTheDocument();
    });

    // Không được render thẻ HTML thô từ phản hồi của đối tác
    expect(container.querySelectorAll('a')).toHaveLength(0);

    // Nội dung phải hiển thị văn bản thuần đã làm sạch thẻ HTML, không bị rò rỉ thẻ <a href...>
    expect(screen.getByText(/Phản hồi từ máy chủ đối tác:/i)).toBeInTheDocument();
    const responseBlock = screen.getByText(/This URL has no default content configured. Change response in Webhook.site./i);
    expect(responseBlock.textContent).not.toContain('<a href=');
    expect(responseBlock.className).not.toContain('truncate');
    expect(responseBlock.className).toContain('break-all');
  });

  it('không làm tràn khung nhìn: nội dung modal có thể cuộn và chân trang luôn hiển thị', () => {
    render(
      <WebhookConfigModal
        open={true}
        apiKey={mockApiKey}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    // Vùng nội dung phải có giới hạn chiều cao + cho phép cuộn
    const scrollable = document.querySelector('[data-slot="dialog-content"] > .overflow-y-auto');
    expect(scrollable).not.toBeNull();
    expect(scrollable?.className).toContain('overflow-y-auto');

    // Chân trang (nút Lưu) vẫn phải truy cập được
    expect(screen.getByRole('button', { name: /Lưu cấu hình/i })).toBeInTheDocument();
  });
});
