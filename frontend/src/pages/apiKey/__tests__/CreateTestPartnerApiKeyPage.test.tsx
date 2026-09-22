import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CreateTestPartnerApiKeyPage } from '../CreateTestPartnerApiKeyPage';
import { createTestApiKey } from '@/api/apiKeyApi';
import { toast } from 'sonner';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('@/api/apiKeyApi', () => ({
  createTestApiKey: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

vi.mock('@/components/common/AppBreadcrumb', () => ({
  useSetBreadcrumb: vi.fn(),
}));

describe('CreateTestPartnerApiKeyPage (NCL-12-CN-004)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderPage = () =>
    render(
      <MemoryRouter>
        <CreateTestPartnerApiKeyPage />
      </MemoryRouter>
    );

  it('renders page header, sandbox banner, and form fields properly', () => {
    renderPage();

    expect(
      screen.getByRole('heading', { name: /Cấp khóa API thử nghiệm/i })
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Tên đối tác \/ Đơn vị thử nghiệm/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Hạn mức gọi API/i)).toHaveValue(30);
    expect(screen.getByLabelText(/Thời gian hết hạn khóa/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /\+3 ngày/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /\+7 ngày/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /\+15 ngày/i })).toBeInTheDocument();
  });

  it('validates empty partner name and shows toast error', async () => {
    renderPage();

    const submitBtn = screen.getByRole('button', { name: /Cấp khóa thử nghiệm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Vui lòng nhập tên đối tác / đơn vị thử nghiệm');
    });
    expect(createTestApiKey).not.toHaveBeenCalled();
  });

  it('validates rate limit exceeding 50 requests per hour', async () => {
    renderPage();

    const partnerInput = screen.getByLabelText(/Tên đối tác \/ Đơn vị thử nghiệm/i);
    const rateLimitInput = screen.getByLabelText(/Hạn mức gọi API/i);

    fireEvent.change(partnerInput, { target: { value: 'Công ty Đối tác ERP' } });
    fireEvent.change(rateLimitInput, { target: { value: '75' } });

    const submitBtn = screen.getByRole('button', { name: /Cấp khóa thử nghiệm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        'Hạn mức thử nghiệm tối đa là 50 lượt/giờ theo quy định'
      );
    });
    expect(createTestApiKey).not.toHaveBeenCalled();
  });

  it('validates expiry date exceeding 15 days', async () => {
    renderPage();

    const partnerInput = screen.getByLabelText(/Tên đối tác \/ Đơn vị thử nghiệm/i);
    const dateInput = screen.getByLabelText(/Thời gian hết hạn khóa/i);

    fireEvent.change(partnerInput, { target: { value: 'Công ty Đối tác ERP' } });

    // Set 20 days in future (exceeds 15 days)
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 20);
    const isoString = futureDate.toISOString().slice(0, 16);
    fireEvent.change(dateInput, { target: { value: isoString } });

    const submitBtn = screen.getByRole('button', { name: /Cấp khóa thử nghiệm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        'Thời hạn thử nghiệm tối đa là 15 ngày theo quy định bảo mật'
      );
    });
    expect(createTestApiKey).not.toHaveBeenCalled();
  });

  it('allows clicking quick select buttons (+3, +7, +15 days)', () => {
    renderPage();

    const quick15Btn = screen.getByRole('button', { name: /\+15 ngày/i });
    fireEvent.click(quick15Btn);

    const dateInput = screen.getByLabelText(/Thời gian hết hạn khóa/i) as HTMLInputElement;
    expect(dateInput.value).toBeTruthy();
  });

  it('submits form successfully and displays raw key result card', async () => {
    const mockCreatedResponse = {
      id: 99,
      keyPrefix: 'nks_test_abcdef',
      partnerName: 'Công ty Big C Test',
      isTest: true,
      rateLimitPerHour: 30,
      status: 'ACTIVE',
      createdAt: '2026-09-14T10:00:00Z',
      expiresAt: '2026-09-28T10:00:00Z',
      rawApiKey: 'nks_test_secret_raw_key_full_sample_12345',
    };

    vi.mocked(createTestApiKey).mockResolvedValueOnce(mockCreatedResponse as any);

    renderPage();

    const partnerInput = screen.getByLabelText(/Tên đối tác \/ Đơn vị thử nghiệm/i);
    fireEvent.change(partnerInput, { target: { value: 'Công ty Big C Test' } });

    const submitBtn = screen.getByRole('button', { name: /Cấp khóa thử nghiệm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createTestApiKey).toHaveBeenCalledWith(
        expect.objectContaining({
          partnerName: 'Công ty Big C Test',
          rateLimitPerHour: 30,
        })
      );
    });

    // Verification of result view
    await waitFor(() => {
      expect(
        screen.getByText(/Khóa API thử nghiệm đã được tạo thành công cho Công ty Big C Test!/i)
      ).toBeInTheDocument();
    });

    expect(screen.getByDisplayValue('nks_test_secret_raw_key_full_sample_12345')).toBeInTheDocument();
    expect(screen.getByText(/Sandbox \(is_test=true\)/i)).toBeInTheDocument();

    // Test clipboard copy
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: {
        writeText: writeTextMock,
      },
    });

    const copyBtn = screen.getByRole('button', { name: /Sao chép khóa/i });
    fireEvent.click(copyBtn);

    await waitFor(() => {
      expect(writeTextMock).toHaveBeenCalledWith('nks_test_secret_raw_key_full_sample_12345');
      expect(toast.success).toHaveBeenCalledWith(
        'Đã sao chép khóa API thử nghiệm vào khay nhớ tạm!'
      );
    });

    // Test back button
    const backBtn = screen.getByRole('button', { name: /Hoàn tất & Quay lại danh sách/i });
    fireEvent.click(backBtn);
    expect(mockNavigate).toHaveBeenCalledWith('/integration/api-keys');
  });

  it('navigates back to /integration/api-keys when clicking Cancel button', () => {
    renderPage();

    const cancelBtn = screen.getByRole('button', { name: /Hủy/i });
    fireEvent.click(cancelBtn);

    expect(mockNavigate).toHaveBeenCalledWith('/integration/api-keys');
  });
});
