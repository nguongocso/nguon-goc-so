import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import '@testing-library/jest-dom/vitest';

import { AlertIndicatorBadge } from '../AlertIndicatorBadge';
import * as aggregateAlertApi from '@/api/aggregateAlertApi';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

let mockUserRole = 'VT-02';
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      userId: 'user-01',
      roleCode: mockUserRole,
      organizationId: 'org-01',
    },
  }),
}));

describe('AlertIndicatorBadge component (CV-04)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUserRole = 'VT-02';
  });

  it('Hiển thị số lượng cảnh báo chưa xử lý khi unviewedCount > 0', async () => {
    vi.spyOn(aggregateAlertApi, 'getUnviewedAlertCount').mockResolvedValueOnce({
      unviewedCount: 5,
      hasHighSeverity: true,
    });

    render(
      <MemoryRouter>
        <AlertIndicatorBadge />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('5')).toBeInTheDocument();
    });

    const button = screen.getByRole('button');
    expect(button).toHaveAttribute(
      'aria-label',
      expect.stringContaining('Có 5 cảnh báo đang mở cần xử lý')
    );

    await userEvent.click(button);
    expect(mockNavigate).toHaveBeenCalledWith('/alerts');
  });

  it('Không hiển thị số đếm badge khi unviewedCount = 0', async () => {
    vi.spyOn(aggregateAlertApi, 'getUnviewedAlertCount').mockResolvedValueOnce({
      unviewedCount: 0,
      hasHighSeverity: false,
    });

    render(
      <MemoryRouter>
        <AlertIndicatorBadge />
      </MemoryRouter>
    );

    await waitFor(() => {
      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();
      expect(button).toHaveAttribute('aria-label', 'Không có cảnh báo nào đang mở');
    });

    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('Ẩn hoàn toàn component đối với vai trò không có quyền (VT-03)', () => {
    mockUserRole = 'VT-03';

    const { container } = render(
      <MemoryRouter>
        <AlertIndicatorBadge />
      </MemoryRouter>
    );

    expect(container.firstChild).toBeNull();
  });
});
