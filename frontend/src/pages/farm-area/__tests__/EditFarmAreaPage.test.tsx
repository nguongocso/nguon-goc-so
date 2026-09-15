import { useEffect } from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { FarmArea } from '@/types/farmArea';
import { EditFarmAreaPage } from '../EditFarmAreaPage';

const apiMocks = vi.hoisted(() => ({ getFarmAreaById: vi.fn() }));

vi.mock('@/api/farmAreaApi', () => ({
  getFarmAreaById: apiMocks.getFarmAreaById,
}));

vi.mock('@/components/common/AppBreadcrumb', () => ({
  useSetBreadcrumb: vi.fn(),
}));

vi.mock('@/components/help/HelpButton', () => ({
  HelpButton: () => null,
}));

vi.mock('@/components/farm-area/EditFarmAreaForm', () => ({
  EditFarmAreaForm: () => <div>Biểu mẫu thông tin chung</div>,
}));

vi.mock('@/components/farm-area/FarmAreaBoundaryEditor', () => ({
  FarmAreaBoundaryEditor: ({ onDirtyChange }: { onDirtyChange?: (dirty: boolean) => void }) => {
    useEffect(() => {
      onDirtyChange?.(true);
      return () => onDirtyChange?.(false);
    }, [onDirtyChange]);
    return <div>Trình chỉnh sửa ranh giới</div>;
  },
}));

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));

const farmArea: FarmArea = {
  id: 'farm-area-1',
  name: 'Vùng chè thử nghiệm',
  organizationId: 'organization-1',
  organizationName: 'HTX thử nghiệm',
  cropTypeId: 'crop-1',
  cropTypeName: 'Chè',
  latitude: 21,
  longitude: 105,
  area: 1,
  areaUnit: 'HA',
  isActive: true,
  createdAt: '2026-09-15T00:00:00Z',
  updatedAt: '2026-09-15T00:00:00Z',
};

describe('EditFarmAreaPage - bảo vệ draft ranh giới', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.clearAllMocks();
    apiMocks.getFarmAreaById.mockResolvedValue(farmArea);
  });

  it('giữ nguyên tab ranh giới khi người dùng không đồng ý bỏ draft', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

    render(
      <MemoryRouter initialEntries={['/farm-areas/farm-area-1/edit']}>
        <Routes>
          <Route path="/farm-areas/:id/edit" element={<EditFarmAreaPage />} />
        </Routes>
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('tab', { name: /Ranh giới trên bản đồ/i }));
    expect(await screen.findByText('Trình chỉnh sửa ranh giới')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: /Thông tin chung/i }));

    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('tab', { name: /Ranh giới trên bản đồ/i })).toHaveAttribute(
      'data-active'
    );
  });

  it('đăng ký cảnh báo trình duyệt khi draft chưa lưu', async () => {
    render(
      <MemoryRouter initialEntries={['/farm-areas/farm-area-1/edit']}>
        <Routes>
          <Route path="/farm-areas/:id/edit" element={<EditFarmAreaPage />} />
        </Routes>
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('tab', { name: /Ranh giới trên bản đồ/i }));
    await screen.findByText('Trình chỉnh sửa ranh giới');

    await waitFor(() => {
      const event = new Event('beforeunload', { cancelable: true });
      window.dispatchEvent(event);
      expect(event.defaultPrevented).toBe(true);
    });
  });

  it('chặn liên kết nội bộ khi người dùng không đồng ý bỏ draft', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);
    render(
      <MemoryRouter initialEntries={['/farm-areas/farm-area-1/edit']}>
        <Routes>
          <Route path="/farm-areas/:id/edit" element={<EditFarmAreaPage />} />
        </Routes>
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('tab', { name: /Ranh giới trên bản đồ/i }));
    await screen.findByText('Trình chỉnh sửa ranh giới');

    const link = document.createElement('a');
    link.href = '/dashboard';
    document.body.appendChild(link);
    const event = new MouseEvent('click', { bubbles: true, cancelable: true, button: 0 });
    link.dispatchEvent(event);
    link.remove();

    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(event.defaultPrevented).toBe(true);
  });
});
