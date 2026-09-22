import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CreateTestApiKeyModal } from '../CreateTestApiKeyModal';
import { createTestApiKey } from '@/api/apiKeyApi';
import { toast } from 'sonner';

vi.mock('@/api/apiKeyApi', () => ({
  createTestApiKey: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('CreateTestApiKeyModal (NCL-12-CN-004)', () => {
  const mockOnClose = vi.fn();
  const mockOnSuccess = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders modal dialog correctly with initial values', () => {
    render(
      <CreateTestApiKeyModal
        open={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    expect(screen.getByText('Cấp khóa API thử nghiệm')).toBeInTheDocument();
    expect(screen.getByText(/Môi trường Thử nghiệm \(Sandbox\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Tên đối tác/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Thời hạn hiệu lực/i)).toHaveValue(14);
    expect(screen.getByLabelText(/Hạn mức gọi API/i)).toHaveValue(30);
  });

  it('validates empty partnerName on submit', async () => {
    render(
      <CreateTestApiKeyModal
        open={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    const submitBtn = screen.getByRole('button', { name: /Cấp khóa thử nghiệm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Vui lòng nhập tên đối tác hoặc đơn vị thử nghiệm')).toBeInTheDocument();
    });
    expect(createTestApiKey).not.toHaveBeenCalled();
  });

  it('validates expireDays exceeding maximum 15 days', async () => {
    render(
      <CreateTestApiKeyModal
        open={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    const partnerInput = screen.getByLabelText(/Tên đối tác/i);
    const expireInput = screen.getByLabelText(/Thời hạn hiệu lực/i);

    fireEvent.change(partnerInput, { target: { value: 'Công ty Đối tác Test' } });
    fireEvent.change(expireInput, { target: { value: '20', valueAsNumber: 20 } });

    const submitBtn = screen.getByRole('button', { name: /Cấp khóa thử nghiệm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Thời hạn thử nghiệm tối đa là 15 ngày theo quy định')).toBeInTheDocument();
    });
    expect(createTestApiKey).not.toHaveBeenCalled();
  });

  it('validates rateLimitPerHour exceeding maximum 50 requests/hour', async () => {
    render(
      <CreateTestApiKeyModal
        open={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    const partnerInput = screen.getByLabelText(/Tên đối tác/i);
    const rateLimitInput = screen.getByLabelText(/Hạn mức gọi API/i);

    fireEvent.change(partnerInput, { target: { value: 'Công ty Đối tác Test' } });
    fireEvent.change(rateLimitInput, { target: { value: '75', valueAsNumber: 75 } });

    const submitBtn = screen.getByRole('button', { name: /Cấp khóa thử nghiệm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Hạn mức thử nghiệm tối đa là 50 lượt/giờ')).toBeInTheDocument();
    });
    expect(createTestApiKey).not.toHaveBeenCalled();
  });

  it('submits form successfully, calls createTestApiKey and triggers callbacks', async () => {
    const mockCreatedKey = {
      id: 'key-test-uuid-123',
      organizationId: 'org-uuid-456',
      partnerName: 'Công ty Logistics Thử Nghiệm',
      keyPrefix: 'nks_test_logi',
      rawApiKey: 'nks_test_logi_1234567890abcdef',
      rateLimitPerHour: 30,
      expiresAt: '2026-10-14T10:00:00Z',
      status: 'ACTIVE' as const,
      isTest: true,
      totalCalls: 0,
      failedCalls: 0,
      createdByUserId: 'user-01',
      createdByFullName: 'Nguyễn Quản Lý',
      createdAt: '2026-09-14T10:00:00Z',
    };

    vi.mocked(createTestApiKey).mockResolvedValue(mockCreatedKey);

    render(
      <CreateTestApiKeyModal
        open={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    const partnerInput = screen.getByLabelText(/Tên đối tác/i);
    fireEvent.change(partnerInput, { target: { value: 'Công ty Logistics Thử Nghiệm' } });

    const submitBtn = screen.getByRole('button', { name: /Cấp khóa thử nghiệm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createTestApiKey).toHaveBeenCalledWith(
        expect.objectContaining({
          partnerName: 'Công ty Logistics Thử Nghiệm',
          expireDays: 14,
          rateLimitPerHour: 30,
        })
      );
      expect(toast.success).toHaveBeenCalledWith('Cấp khóa thử nghiệm thành công!');
      expect(mockOnSuccess).toHaveBeenCalledWith(mockCreatedKey);
      expect(mockOnClose).toHaveBeenCalled();
    });
  });

  it('handles API error (such as 403 Forbidden) and displays error toast', async () => {
    vi.mocked(createTestApiKey).mockRejectedValue({
      response: {
        status: 403,
        data: {
          message: 'Chỉ Quản lý HTX mới có quyền cấp khóa thử nghiệm',
        },
      },
    });

    render(
      <CreateTestApiKeyModal
        open={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );

    const partnerInput = screen.getByLabelText(/Tên đối tác/i);
    fireEvent.change(partnerInput, { target: { value: 'Đơn vị kiểm thử' } });

    const submitBtn = screen.getByRole('button', { name: /Cấp khóa thử nghiệm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Chỉ Quản lý HTX mới có quyền cấp khóa thử nghiệm');
    });
    expect(mockOnSuccess).not.toHaveBeenCalled();
    expect(mockOnClose).not.toHaveBeenCalled();
  });
});
