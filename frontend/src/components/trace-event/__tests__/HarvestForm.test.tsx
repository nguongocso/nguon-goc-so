import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AxiosError, AxiosHeaders } from 'axios';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';

import { HarvestForm } from '../HarvestForm';

import { recordHarvestEvent } from '@/api/traceEventApi';
import { getHarvestEligibility } from '@/api/farmLogApi';

vi.mock('@/api/traceEventApi', () => ({
  recordHarvestEvent: vi.fn(),
}));

vi.mock('@/api/farmLogApi', () => ({
  getHarvestEligibility: vi.fn(),
}));

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 'usr-1',
      username: 'test_user',
      roleCode: 'VT-02',
    },
  }),
}));

vi.mock('@/hooks/useAutoGeolocation', () => ({
  useAutoGeolocation: vi.fn(),
}));

vi.mock('@/hooks/useOfflineSync', () => ({
  useOfflineSync: () => ({
    isOnline: true,
  }),
}));

vi.mock('@/pages/packaging-event/components/LocationPicker', () => ({
  LocationPicker: () => <div data-testid="mock-location-picker">LocationPicker</div>,
}));

describe('HarvestForm (Trace Event UI)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getHarvestEligibility).mockResolvedValue({
      determined: true,
      eligibleHarvestDate: '2026-09-01',
      unmatchedMaterials: [],
    });
  });

  it('render form với thông tin lô sản xuất và các trường bắt buộc', async () => {
    render(
      <HarvestForm
        productionLotId="lot-123"
        productionLotName="Lô Chè Ô Long A1"
        onSuccess={vi.fn()}
      />,
    );

    expect(screen.getAllByText('Ghi nhận thu hoạch').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Lô Chè Ô Long A1')).toBeInTheDocument();
    expect(screen.getByLabelText(/Ngày thu hoạch/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Sản lượng thu hoạch/i)).toBeInTheDocument();
  });

  it('hiển thị trạng thái đã đảm bảo thời gian cách ly khi eligibility hợp lệ', async () => {
    render(
      <HarvestForm
        productionLotId="lot-123"
        productionLotName="Lô Chè Ô Long A1"
        onSuccess={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText(/Đã đảm bảo thời gian cách ly thuốc BVTV/i)).toBeInTheDocument();
    });
  });

  it('hiển thị cảnh báo thu hoạch sớm khi thời hạn cách ly ở tương lai', async () => {
    vi.mocked(getHarvestEligibility).mockResolvedValueOnce({
      determined: true,
      eligibleHarvestDate: '2026-10-15',
      unmatchedMaterials: [],
    });

    render(
      <HarvestForm
        productionLotId="lot-123"
        productionLotName="Lô Chè Ô Long A1"
        onSuccess={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText(/Cảnh báo thu hoạch trước thời gian cách ly/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Lý do thu hoạch sớm/i)).toBeInTheDocument();
    });
  });

  it('submit thành công gọi API recordHarvestEvent với payload đúng', async () => {
    const handleSuccess = vi.fn();
    vi.mocked(recordHarvestEvent).mockResolvedValueOnce({
      id: 'event-1',
      productionLotId: 'lot-123',
      eventType: 'HARVEST',
      eventData: { harvestDate: '2026-09-20', quantity: 250 },
      recordedAt: '2026-09-20T10:00:00',
      recordedByName: 'test_user',
      createdAt: '2026-09-20T10:00:00',
    });

    render(
      <HarvestForm
        productionLotId="lot-123"
        productionLotName="Lô Chè Ô Long A1"
        onSuccess={handleSuccess}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText(/Đã đảm bảo thời gian cách ly thuốc BVTV/i)).toBeInTheDocument();
    });

    const qtyInput = screen.getByPlaceholderText('Nhập sản lượng thực tế');
    fireEvent.change(qtyInput, { target: { value: '250' } });

    const submitBtn = screen.getByRole('button', { name: /Ghi nhận thu hoạch/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(recordHarvestEvent).toHaveBeenCalledTimes(1);
    });

    expect(recordHarvestEvent).toHaveBeenCalledWith(
      expect.objectContaining({
        productionLotId: 'lot-123',
        quantity: 250,
      }),
    );
    expect(handleSuccess).toHaveBeenCalled();
  });

  it('hiển thị thông báo lỗi từ backend khi API thất bại', async () => {
    const error = new AxiosError('Conflict');
    error.response = {
      data: { status: 409, message: 'Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.' },
      status: 409,
      statusText: 'Conflict',
      headers: {},
      config: { headers: new AxiosHeaders() },
    };
    vi.mocked(recordHarvestEvent).mockRejectedValueOnce(error);

    render(
      <HarvestForm
        productionLotId="lot-123"
        productionLotName="Lô Chè Ô Long A1"
      />,
    );

    await waitFor(() => {
      expect(screen.getByText(/Đã đảm bảo thời gian cách ly thuốc BVTV/i)).toBeInTheDocument();
    });

    const qtyInput = screen.getByPlaceholderText('Nhập sản lượng thực tế');
    fireEvent.change(qtyInput, { target: { value: '150' } });

    const submitBtn = screen.getByRole('button', { name: /Ghi nhận thu hoạch/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText('Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.'),
      ).toBeInTheDocument();
    });
  });
});
