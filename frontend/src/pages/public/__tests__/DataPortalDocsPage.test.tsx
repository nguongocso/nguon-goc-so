import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { DataPortalDocsPage } from '../DataPortalDocsPage';
import { toast } from 'sonner';

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: null,
    isLoading: false,
  }),
}));

describe('DataPortalDocsPage (NCL-12-CN-004)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    });
  });

  const renderPage = () =>
    render(
      <MemoryRouter>
        <DataPortalDocsPage />
      </MemoryRouter>
    );

  it('renders public documentation page without crashing and shows header and title', () => {
    renderPage();

    expect(
      screen.getByRole('heading', { level: 1, name: /Tài liệu Cổng dữ liệu Nguồn Gốc Số/i })
    ).toBeInTheDocument();
    expect(screen.getByText(/Môi trường Sandbox Thử nghiệm/i)).toBeInTheDocument();
    expect(screen.getByText(/1\. Tổng quan & Cơ chế Xác thực/i)).toBeInTheDocument();
  });

  it('displays authentication details, Base URL, and Sandbox mode highlights', () => {
    renderPage();

    expect(screen.getByText('https://api.nguongocso.vn')).toBeInTheDocument();
    expect(screen.getByText(/X-API-KEY: <chuỗi_khóa>/i)).toBeInTheDocument();
    expect(screen.getByText(/Chế độ Thử nghiệm \(Sandbox Mode - is_test: true\)/i)).toBeInTheDocument();
  });

  it('renders endpoint list including public lots and GS1 endpoints', () => {
    renderPage();

    expect(screen.getByText('/api/publicapi/v1/lots/{lotId}')).toBeInTheDocument();
    expect(
      screen.getByText('/api/v1/partner/production-lots/{lotId}/dossier')
    ).toBeInTheDocument();
    expect(
      screen.getByText('/api/v1/partner/shipments/{shipmentId}/dossier/gs1')
    ).toBeInTheDocument();
  });

  it('renders sandbox sample payload with is_test: true highlighted', () => {
    renderPage();

    expect(screen.getByText(/4\. Dữ liệu Phản hồi Mẫu \(Sandbox Response Payload\)/i)).toBeInTheDocument();
    expect(screen.getByText(/★ Chú ý trường nhận diện: "is_test": true/i)).toBeInTheDocument();
  });

  it('allows switching code snippet tabs between cURL, JavaScript, and Python', () => {
    renderPage();

    const fetchTabBtn = screen.getByRole('button', { name: 'JavaScript' });
    fireEvent.click(fetchTabBtn);

    expect(screen.getByText(/fetch\("https:\/\/api\.nguongocso\.vn/i)).toBeInTheDocument();

    const pythonTabBtn = screen.getByRole('button', { name: 'Python' });
    fireEvent.click(pythonTabBtn);

    expect(screen.getByText(/import requests/i)).toBeInTheDocument();
  });

  it('supports copying code sample to clipboard with toast feedback', async () => {
    renderPage();

    const copyBtns = screen.getAllByRole('button', { name: /Sao chép/i });
    expect(copyBtns.length).toBeGreaterThan(0);

    fireEvent.click(copyBtns[0]);

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('Đã sao chép vào khay nhớ tạm!');
    });
  });

  it('renders GS1 EPCIS mapping table and HTTP error codes reference', () => {
    renderPage();

    expect(screen.getByText('5. Bảng Ánh xạ Thuộc tính theo Chuẩn GS1 EPCIS')).toBeInTheDocument();
    expect(screen.getByText('Mã sản phẩm (GTIN)')).toBeInTheDocument();
    expect(screen.getByText('6. Bảng Mã Lỗi Tổng hợp (HTTP Error Codes)')).toBeInTheDocument();
    expect(screen.getByText('401')).toBeInTheDocument();
    expect(screen.getByText(/API Key đã hết hạn/i)).toBeInTheDocument();
  });
});

