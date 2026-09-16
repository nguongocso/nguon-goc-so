import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { MobileOnlyRoute } from '../MobileOnlyRoute';

vi.mock('@/hooks/useIsMobileDevice', () => ({
  useIsMobileDevice: vi.fn(),
}));

import { useIsMobileDevice } from '@/hooks/useIsMobileDevice';

const mockUseIsMobileDevice = vi.mocked(useIsMobileDevice);

const renderVoiRouter = (children: React.ReactNode) =>
  render(
    <MemoryRouter>
      <MobileOnlyRoute>{children}</MobileOnlyRoute>
    </MemoryRouter>,
  );

describe('MobileOnlyRoute', () => {
  it('render nội dung khi dùng thiết bị mobile', () => {
    mockUseIsMobileDevice.mockReturnValue(true);

    renderVoiRouter(<p>Nội dung ghi nhật ký</p>);

    expect(screen.getByText('Nội dung ghi nhật ký')).toBeInTheDocument();
    expect(
      screen.queryByText('Chỉ khả dụng trên thiết bị di động'),
    ).not.toBeInTheDocument();
  });

  it('hiển thị trang báo khi dùng desktop', () => {
    mockUseIsMobileDevice.mockReturnValue(false);

    renderVoiRouter(<p>Nội dung ghi nhật ký</p>);

    expect(
      screen.getByText('Chỉ khả dụng trên thiết bị di động'),
    ).toBeInTheDocument();
    expect(screen.queryByText('Nội dung ghi nhật ký')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Quay lại' })).toBeInTheDocument();
  });
});
