import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ExportOpenDataPage from '../ExportOpenDataPage';
import * as exportApi from '@/api/exportApi';
import * as productCategoryApi from '@/api/productCategoryApi';

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}));

vi.mock('@/hooks/useHelp', () => ({
  useHelp: () => ({ data: null, isLoading: false, error: null, refetch: vi.fn() }),
}));

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      userId: 'test-user-id',
      username: 'regulator_vt05',
      roleCode: 'VT-05',
      organizationId: 'cc5872d4-a3be-11f1-9ca5-e00af63e88f4',
    },
    isAuthenticated: true,
  }),
}));

vi.mock('@/hooks/usePermission', () => ({
  usePermission: (roles: string[]) => roles.includes('VT-05'),
}));

vi.mock('@/hooks/useProfileTemplates', () => ({
  useProfileTemplates: () => ({
    templates: [],
    availableFields: [],
    defaultTemplate: null,
    loading: false,
    error: null,
  }),
}));

describe('ExportOpenDataPage UI/UX Synchronization', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(productCategoryApi, 'getProductCategories').mockResolvedValue([
      { id: 'cat-1', code: 'CHE', name: 'Chè búp', description: 'Chè búp tươi' },
      { id: 'cat-2', code: 'LUA', name: 'Lúa gạo', description: 'Lúa chất lượng cao' },
    ]);
  });

  it('renders standard ListPageHeader with title and description', async () => {
    render(
      <MemoryRouter>
        <ExportOpenDataPage />
      </MemoryRouter>
    );

    // Tiêu đề trang chuẩn ListPageHeader
    expect(await screen.findByRole('heading', { level: 1, name: /Xuất dữ liệu mở/i })).toBeInTheDocument();
    expect(
      screen.getByText(/Trích xuất và tải xuống dữ liệu mở phục vụ phân tích, đối soát/i)
    ).toBeInTheDocument();
  });

  it('renders synchronous 2-column layout cards: Scope and Export Options', async () => {
    render(
      <MemoryRouter>
        <ExportOpenDataPage />
      </MemoryRouter>
    );

    // Thẻ phạm vi dữ liệu
    expect(await screen.findByText('Phạm vi địa bàn & Tổ chức')).toBeInTheDocument();
    expect(screen.getByText('Khoảng thời gian & Danh mục nông sản')).toBeInTheDocument();

    // Thẻ định dạng & tải xuống bên cột phải
    expect(screen.getByText('Định dạng & Tải xuống')).toBeInTheDocument();
    expect(screen.getByText('Dữ liệu JSON')).toBeInTheDocument();
    expect(screen.getByText('Bảng tính CSV')).toBeInTheDocument();
    expect(screen.getByText('Tóm tắt thiết lập xuất')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tải xuống dữ liệu mở/i })).toBeInTheDocument();
  });

  it('allows switching format between JSON and CSV', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <ExportOpenDataPage />
      </MemoryRouter>
    );

    const csvButton = await screen.findByText('Bảng tính CSV');
    await user.click(csvButton);

    expect(screen.getByRole('button', { name: /Tải xuống dữ liệu mở \(CSV\)/i })).toBeInTheDocument();
  });
});
