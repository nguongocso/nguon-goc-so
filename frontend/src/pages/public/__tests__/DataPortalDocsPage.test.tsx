import '@testing-library/jest-dom/vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { DataPortalDocsPage } from '../DataPortalDocsPage';
import { toast } from 'sonner';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

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
  const writeTextMock = vi.fn().mockResolvedValue(undefined);

  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: writeTextMock,
      },
      writable: true,
      configurable: true,
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
    expect(screen.getByText(/Cổng tích hợp B2B & Chuỗi cung ứng nông sản/i)).toBeInTheDocument();
    expect(screen.getByText(/1\. Tổng quan & Cơ chế Xác thực/i)).toBeInTheDocument();
  });

  it('displays authentication details, Base URL, usage guide, and rate limit highlights', () => {
    renderPage();

    expect(screen.getByText(/Hướng dẫn sử dụng Cổng dữ liệu Nguồn Gốc Số/i)).toBeInTheDocument();
    expect(screen.getByText(/Lấy Khóa API \(API Key\)/i)).toBeInTheDocument();
    expect(screen.getByText('https://agri-trace.online')).toBeInTheDocument();
    expect(screen.getByText(/X-API-KEY: <chuỗi_khóa>/i)).toBeInTheDocument();
    expect(screen.getByText(/Mặc định 30 lượt \/ giờ/i)).toBeInTheDocument();
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

  it('sets default API key value to "Ví dụ", reveals snippets by default, and hides them when cleared', () => {
    renderPage();

    const inputKey = screen.getByPlaceholderText(/Nhập khóa API/i) as HTMLInputElement;
    expect(inputKey.value).toBe('Ví dụ');

    // Mặc định hiển thị ví dụ lệnh gọi với key 'Ví dụ'
    expect(screen.getAllByText(/X-API-KEY: Ví dụ/i).length).toBe(3);
    expect(screen.getAllByText(/Ví dụ dữ liệu phản hồi mẫu/i).length).toBe(3);

    // Người dùng bấm nút "Xóa" để xóa giá trị mặc định
    const clearBtn = screen.getByRole('button', { name: /Xóa/i });
    fireEvent.click(clearBtn);

    expect(inputKey.value).toBe('');
    expect(screen.queryByText(/curl -s -X GET/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Ví dụ dữ liệu phản hồi mẫu/i)).not.toBeInTheDocument();
  });

  it('reveals request snippet and response payload sample for each endpoint when user types API key', () => {
    renderPage();

    // Nhập key mới vào input
    const inputKey = screen.getByPlaceholderText(/Nhập khóa API/i);
    fireEvent.change(inputKey, { target: { value: 'nks_test_custom_key_999' } });

    // Hiển thị code snippet với key đã nhập cho cả 3 endpoints
    expect(screen.getAllByText(/X-API-KEY: nks_test_custom_key_999/i).length).toBe(3);
    expect(
      screen.getByText(/curl -s -X GET "https:\/\/agri-trace\.online\/api\/publicapi\/v1\/lots\/sample-lot-001"/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/curl -s -X GET "https:\/\/agri-trace\.online\/api\/v1\/partner\/production-lots\/sample-lot-001\/dossier"/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/curl -s -X GET "https:\/\/agri-trace\.online\/api\/v1\/partner\/shipments\/sample-shipment-001\/dossier\/gs1"/i)
    ).toBeInTheDocument();

    // Hiển thị khối Dữ liệu phản hồi mẫu cho cả 3 endpoints
    expect(screen.getAllByText(/Ví dụ dữ liệu phản hồi mẫu/i).length).toBe(3);
    expect(screen.getAllByText(/Lô Xoài Cát Chu Thử Nghiệm/i).length).toBe(2);
    expect(screen.getByText(/Lô Hàng Xoài Cát Xuất Khẩu Thử Nghiệm/i)).toBeInTheDocument();
    expect(screen.getByText(/GS1_SIMULATED_V1/i)).toBeInTheDocument();
  });

  it('allows independent snippet tab switching between cURL, JavaScript, and Python for each endpoint', () => {
    renderPage();

    const inputKey = screen.getByPlaceholderText(/Nhập khóa API/i);
    fireEvent.change(inputKey, { target: { value: 'nks_test_my_key' } });

    // Ban đầu cả 3 đều là cURL
    expect(screen.getAllByText(/curl -s -X GET/i).length).toBe(3);

    // Chuyển endpoint 1 sang JavaScript
    const jsBtns = screen.getAllByRole('button', { name: 'JavaScript' });
    fireEvent.click(jsBtns[0]);

    // Chỉ endpoint 1 chuyển sang fetch, 2 endpoint còn lại vẫn là cURL
    expect(screen.getAllByText(/fetch\("https:\/\/agri-trace\.online/i).length).toBe(1);
    expect(screen.getAllByText(/curl -s -X GET/i).length).toBe(2);

    // Chuyển endpoint 2 sang Python
    const pythonBtns = screen.getAllByRole('button', { name: 'Python' });
    fireEvent.click(pythonBtns[1]);

    // Endpoint 1 là fetch, endpoint 2 là requests, endpoint 3 vẫn là cURL
    expect(screen.getAllByText(/fetch\("https:\/\/agri-trace\.online/i).length).toBe(1);
    expect(screen.getAllByText(/import requests/i).length).toBe(1);
    expect(screen.getAllByText(/curl -s -X GET/i).length).toBe(1);
  });

  it('allows independent environment switching between Production, Staging, and Localhost for each endpoint', () => {
    renderPage();

    const inputKey = screen.getByPlaceholderText(/Nhập khóa API/i);
    fireEvent.change(inputKey, { target: { value: 'nks_test_my_key' } });

    // Mặc định cả 3 đều là Production curl
    expect(screen.queryByText(/curl -s -X GET "https:\/\/staging\.agri-trace\.online/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/curl -s -X GET "http:\/\/localhost:8080/i)).not.toBeInTheDocument();

    // Chuyển endpoint 1 sang Staging
    const stagingBtns = screen.getAllByRole('button', { name: 'Staging' });
    fireEvent.click(stagingBtns[0]);

    // Chỉ endpoint 1 có URL staging
    expect(screen.getByText(/curl -s -X GET "https:\/\/staging\.agri-trace\.online\/api\/publicapi\/v1\/lots\/sample-lot-001"/i)).toBeInTheDocument();
    // Endpoint 2 & 3 vẫn là Production
    expect(screen.getByText(/curl -s -X GET "https:\/\/agri-trace\.online\/api\/v1\/partner\/production-lots\/sample-lot-001\/dossier"/i)).toBeInTheDocument();
    expect(screen.getByText(/curl -s -X GET "https:\/\/agri-trace\.online\/api\/v1\/partner\/shipments\/sample-shipment-001\/dossier\/gs1"/i)).toBeInTheDocument();

    // Chuyển endpoint 2 sang Localhost
    const localBtns = screen.getAllByRole('button', { name: 'Localhost' });
    fireEvent.click(localBtns[1]);

    // Endpoint 2 đổi sang localhost:8080, endpoint 1 vẫn staging, endpoint 3 vẫn production
    expect(screen.getByText(/curl -s -X GET "https:\/\/staging\.agri-trace\.online\/api\/publicapi\/v1\/lots\/sample-lot-001"/i)).toBeInTheDocument();
    expect(screen.getByText(/curl -s -X GET "http:\/\/localhost:8080\/api\/v1\/partner\/production-lots\/sample-lot-001\/dossier"/i)).toBeInTheDocument();
    expect(screen.getByText(/curl -s -X GET "https:\/\/agri-trace\.online\/api\/v1\/partner\/shipments\/sample-shipment-001\/dossier\/gs1"/i)).toBeInTheDocument();
  });

  it('supports copying code sample and response JSON to clipboard with toast feedback when key is entered', async () => {
    renderPage();

    const inputKey = screen.getByPlaceholderText(/Nhập khóa API/i);
    fireEvent.change(inputKey, { target: { value: 'nks_test_my_key' } });

    const copyBtns = screen.getAllByRole('button', { name: /^Sao chép$/i });
    expect(copyBtns.length).toBe(3);

    fireEvent.click(copyBtns[0]);

    await waitFor(() => {
      expect(writeTextMock).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('Đã sao chép vào khay nhớ tạm!');
    });

    const copyJsonBtns = screen.getAllByRole('button', { name: /Sao chép JSON/i });
    expect(copyJsonBtns.length).toBe(3);

    fireEvent.click(copyJsonBtns[0]);

    await waitFor(() => {
      expect(writeTextMock).toHaveBeenCalledTimes(2);
    });
  });

  it('navigates to /login when clicking "Đăng nhập"', () => {
    renderPage();

    const loginBtn = screen.getByRole('button', { name: /Đăng nhập/i });
    fireEvent.click(loginBtn);

    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('renders GS1 EPCIS mapping table and HTTP error codes reference', () => {
    renderPage();

    expect(screen.getByText('3. Bảng Ánh xạ Thuộc tính theo Chuẩn GS1 EPCIS')).toBeInTheDocument();
    expect(screen.getByText('Mã lô sản xuất (GTIN)')).toBeInTheDocument();
    expect(screen.getByText('4. Bảng Mã Lỗi Tổng hợp (HTTP Error Codes)')).toBeInTheDocument();
    expect(screen.getAllByText('401').length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Khóa thử nghiệm đã hết hạn/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Khóa truy cập đã hết thời gian hiệu lực/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText('403').length).toBeGreaterThan(0);
  });

  it('allows unauthenticated users to access /portal and displays login button instead of dashboard', () => {
    renderPage();

    // Người dùng chưa đăng nhập thấy nút "Đăng nhập"
    expect(screen.getByRole('button', { name: /Đăng nhập/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Bảng điều khiển/i })).not.toBeInTheDocument();

    // Hiển thị phần Quy tắc Tiền tố Khóa & Chế độ Sandbox
    expect(screen.getByText(/Quy tắc Tiền tố Khóa & Phạm vi Thử nghiệm/i)).toBeInTheDocument();
    expect(screen.getByText(/Mặc định 30 lượt \/ giờ/i)).toBeInTheDocument();
    expect(screen.getAllByText(/sample-shipment-001/i).length).toBeGreaterThan(0);

    // Kiểm tra tiêu đề khung Ví dụ Gọi Thử nghiệm và hướng dẫn thay thế khóa
    expect(screen.getByText(/^Ví dụ Gọi Thử nghiệm$/i)).toBeInTheDocument();
    expect(screen.getByText(/Hãy thay thế khóa/i)).toBeInTheDocument();
  });
});
