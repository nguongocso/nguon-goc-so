import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { FarmArea, FarmAreaBoundaryResponse, LatLng } from '@/types/farmArea';
import { FarmAreaBoundaryEditor } from '../FarmAreaBoundaryEditor';

const apiMocks = vi.hoisted(() => ({
  getBoundary: vi.fn(),
  updateBoundary: vi.fn(),
}));

vi.mock('@/api/farmAreaApi', () => ({
  getFarmAreaBoundary: apiMocks.getBoundary,
  updateFarmAreaBoundary: apiMocks.updateBoundary,
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

vi.mock('../BoundaryMapEditor', () => ({
  BoundaryMapEditor: ({ invalid }: { invalid?: boolean }) => (
    <div data-invalid={invalid ? 'true' : 'false'}>Bản đồ thử nghiệm</div>
  ),
}));

vi.mock('../BoundaryPastePanel', () => ({
  BoundaryPastePanel: ({ onApplyPoints }: { onApplyPoints: (points: LatLng[]) => void }) => (
    <>
      <button
        type="button"
        onClick={() =>
          onApplyPoints([
            { latitude: 21, longitude: 105 },
            { latitude: 21, longitude: 105.001 },
            { latitude: 21.001, longitude: 105.001 },
            { latitude: 21.001, longitude: 105 },
          ])
        }
      >
        Áp dụng tọa độ thử
      </button>
      <button
        type="button"
        onClick={() => onApplyPoints([{ latitude: 21, longitude: 105 }])}
      >
        Áp dụng một điểm
      </button>
      <button
        type="button"
        onClick={() =>
          onApplyPoints([
            { latitude: 21, longitude: 105 },
            { latitude: 21.001, longitude: 105.001 },
          ])
        }
      >
        Áp dụng hai điểm
      </button>
      <button
        type="button"
        onClick={() =>
          onApplyPoints([
            { latitude: 21, longitude: 105 },
            { latitude: 21.001, longitude: 105.001 },
            { latitude: 21.001, longitude: 105 },
            { latitude: 21, longitude: 105.001 },
          ])
        }
      >
        Áp dụng ranh giới tự cắt
      </button>
      <button
        type="button"
        onClick={() =>
          onApplyPoints([
            { latitude: 21, longitude: 105 },
            { latitude: 21.001, longitude: 105.001 },
            { latitude: 21.002, longitude: 105.002 },
          ])
        }
      >
        Áp dụng ranh giới thẳng hàng
      </button>
    </>
  ),
}));

vi.mock('../AreaDeviationConfirmDialog', () => ({
  AreaDeviationConfirmDialog: () => null,
}));

const farmArea: FarmArea = {
  id: 'farm-area-1',
  name: 'Vùng chè thử nghiệm',
  organizationId: 'organization-1',
  organizationName: 'HTX thử nghiệm',
  cropTypeId: 'crop-1',
  cropTypeName: 'Chè',
  latitude: 21,
  longitude: 105,
  area: 0.1,
  areaUnit: 'HA',
  isActive: true,
  createdAt: '2026-09-15T00:00:00Z',
  updatedAt: '2026-09-15T00:00:00Z',
};

const boundaryResponse: FarmAreaBoundaryResponse = {
  id: farmArea.id,
  name: farmArea.name,
  organizationId: farmArea.organizationId,
  declaredArea: farmArea.area,
  declaredAreaUnit: 'HA',
  calculatedArea: null,
  points: [],
  areaDeviationPercentage: null,
  thresholdPercentage: 12.5,
  updatedAt: null,
};

describe('FarmAreaBoundaryEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('khóa trình chỉnh sửa khi tải dữ liệu lỗi và cho phép thử lại', async () => {
    apiMocks.getBoundary
      .mockRejectedValueOnce({ response: { data: { message: 'Mất kết nối máy chủ' } } })
      .mockResolvedValueOnce(boundaryResponse);

    render(<FarmAreaBoundaryEditor farmArea={farmArea} />);

    expect(await screen.findByText('Không thể tải ranh giới vùng trồng')).toBeInTheDocument();
    expect(screen.queryByText('Bản đồ thử nghiệm')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Thử lại/i }));

    expect(await screen.findByText('Bản đồ thử nghiệm')).toBeInTheDocument();
    expect(apiMocks.getBoundary).toHaveBeenCalledTimes(2);
  });

  it('dùng ngưỡng backend và báo trạng thái draft cho trang cha', async () => {
    apiMocks.getBoundary.mockResolvedValue(boundaryResponse);
    const onDirtyChange = vi.fn();

    render(
      <FarmAreaBoundaryEditor farmArea={farmArea} onDirtyChange={onDirtyChange} />
    );

    fireEvent.click(await screen.findByRole('button', { name: 'Áp dụng tọa độ thử' }));

    expect(await screen.findByText(/Chênh lệch > 12.5%/i)).toBeInTheDocument();
    await waitFor(() => expect(onDirtyChange).toHaveBeenLastCalledWith(true));
  });

  it('không cảnh báo chênh lệch khi mới có một hoặc hai điểm', async () => {
    apiMocks.getBoundary.mockResolvedValue(boundaryResponse);

    render(<FarmAreaBoundaryEditor farmArea={farmArea} />);

    fireEvent.click(await screen.findByRole('button', { name: 'Áp dụng một điểm' }));
    expect(screen.queryByText(/Chênh lệch > 12.5%/i)).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Áp dụng hai điểm' }));
    expect(screen.queryByText(/Chênh lệch > 12.5%/i)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Lưu ranh giới' })).toBeDisabled();
  });

  it('cảnh báo và không cho lưu khi ranh giới tự cắt', async () => {
    apiMocks.getBoundary.mockResolvedValue(boundaryResponse);

    render(<FarmAreaBoundaryEditor farmArea={farmArea} />);

    fireEvent.click(await screen.findByRole('button', { name: 'Áp dụng ranh giới tự cắt' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('các cạnh tự cắt nhau');
    expect(screen.queryByText(/Chênh lệch > 12.5%/i)).not.toBeInTheDocument();
    expect(screen.getByText('Bản đồ thử nghiệm')).toHaveAttribute('data-invalid', 'true');
    expect(screen.getByRole('button', { name: 'Lưu ranh giới' })).toBeDisabled();
    expect(apiMocks.updateBoundary).not.toHaveBeenCalled();
  });

  it('cảnh báo và không cho lưu khi các đỉnh thẳng hàng', async () => {
    apiMocks.getBoundary.mockResolvedValue(boundaryResponse);

    render(<FarmAreaBoundaryEditor farmArea={farmArea} />);

    fireEvent.click(await screen.findByRole('button', { name: 'Áp dụng ranh giới thẳng hàng' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('diện tích lớn hơn 0');
    expect(screen.getByText('Bản đồ thử nghiệm')).toHaveAttribute('data-invalid', 'true');
    expect(screen.getByRole('button', { name: 'Lưu ranh giới' })).toBeDisabled();
    expect(apiMocks.updateBoundary).not.toHaveBeenCalled();
  });
});
