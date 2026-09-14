import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { PartnerApiKeyListPage } from '../PartnerApiKeyListPage';

const mockUsePermission = vi.fn();
vi.mock('@/hooks/usePermission', () => ({
  usePermission: (roles: string[]) => mockUsePermission(roles),
}));

vi.mock('@/api/apiKeyApi', () => ({
  getApiKeys: vi.fn().mockResolvedValue({
    content: [],
    totalElements: 0,
    totalPages: 0,
    size: 10,
    number: 0,
  }),
}));

vi.mock('@/components/common/AppBreadcrumb', () => ({
  useSetBreadcrumb: vi.fn(),
}));

describe('PartnerApiKeyListPage Role Enforcement (TC-04)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderPage = () =>
    render(
      <MemoryRouter>
        <PartnerApiKeyListPage />
      </MemoryRouter>
    );

  it('renders "Cấp khóa thử nghiệm" and "Cấp khóa mới" buttons for COOPERATIVE_MANAGER (VT-02) / Admin (VT-01)', () => {
    mockUsePermission.mockReturnValue(true);

    renderPage();

    expect(screen.getByRole('button', { name: /Cấp khóa thử nghiệm/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cấp khóa mới/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tài liệu cổng dữ liệu/i })).toBeInTheDocument();
  });

  it('HIDES "Cấp khóa thử nghiệm" and "Cấp khóa mới" buttons for EVENT_RECORDER (VT-03) (TC-04 UI enforcement)', () => {
    mockUsePermission.mockReturnValue(false);

    renderPage();

    expect(screen.queryByRole('button', { name: /Cấp khóa thử nghiệm/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Cấp khóa mới/i })).not.toBeInTheDocument();
    // Nút xem tài liệu công khai vẫn hiển thị để tham khảo
    expect(screen.getByRole('button', { name: /Tài liệu cổng dữ liệu/i })).toBeInTheDocument();
  });
});
