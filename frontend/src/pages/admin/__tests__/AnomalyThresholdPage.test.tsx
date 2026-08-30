import { describe, expect, it, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Toaster } from 'sonner';
import { AnomalyThresholdPage } from '@/pages/admin/AnomalyThresholdPage';
import type { AllThresholdsResponse, ImpactEstimationResult } from '@/types/anomalyThreshold';

const thresholdApi = vi.hoisted(() => ({
  getAllThresholds: vi.fn(),
  getGlobalThreshold: vi.fn(),
  updateGlobalThreshold: vi.fn(),
  getCategoryOverrides: vi.fn(),
  saveCategoryOverride: vi.fn(),
  deleteCategoryOverride: vi.fn(),
  estimateImpact: vi.fn(),
}));

const productCategoryApi = vi.hoisted(() => ({
  getProductCategories: vi.fn(),
}));

vi.mock('@/api/anomalyThresholdApi', () => thresholdApi);
vi.mock('@/api/productCategoryApi', () => productCategoryApi);

const MOCK_DATA: AllThresholdsResponse = {
  global: {
    id: 'global-id-1',
    productCategoryId: null,
    productCategoryName: null,
    maxScansPerHour: 5,
    maxScansPerDay: 10,
    maxDistanceKmPer30Min: 50.0,
    minTimeBetweenScansMinutes: 30,
    activationAgeDays: 365,
    isActive: true,
  },
  categoryOverrides: [
    {
      id: 'override-id-1',
      productCategoryId: 'cat-id-1',
      productCategoryName: 'Sầu riêng Ri6',
      maxScansPerHour: 3,
      maxScansPerDay: 7,
      maxDistanceKmPer30Min: 35.0,
      minTimeBetweenScansMinutes: 20,
      activationAgeDays: 180,
      isActive: true,
    },
  ],
};

const MOCK_IMPACT: ImpactEstimationResult = {
  estimatedAnomaliesCount: 4,
  totalScansAnalyzed: 150,
  totalTraceCodesAnalyzed: 45,
  highFrequencyCount: 2,
  impossibleTravelCount: 2,
  activationAgeCount: 0,
  analysisPeriodDays: 30,
  message: 'Dự kiến có 4 mã tem bị gắn cờ bất thường.',
};

describe('AnomalyThresholdPage (NCL-08-CN-014)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    thresholdApi.getAllThresholds.mockResolvedValue(MOCK_DATA);
    thresholdApi.estimateImpact.mockResolvedValue(MOCK_IMPACT);
    productCategoryApi.getProductCategories.mockResolvedValue([
      { id: 'cat-id-1', name: 'Sầu riêng Ri6', isActive: true },
      { id: 'cat-id-2', name: 'Xoài Cát Chu', isActive: true },
    ]);
  });

  it('hiển thị tiêu đề trang và danh sách cấu hình toàn cục & ghi đè danh mục', async () => {
    render(
      <>
        <Toaster />
        <AnomalyThresholdPage />
      </>,
    );

    await waitFor(() => {
      expect(screen.getByText('Cấu hình ngưỡng quét bất thường')).toBeInTheDocument();
      expect(screen.getByText('Cấu hình ngưỡng mặc định toàn cục')).toBeInTheDocument();
      expect(screen.getByText('Sầu riêng Ri6')).toBeInTheDocument();
    });
  });

  it('kích hoạt ước lượng tác động khi bấm nút Xem ước lượng', async () => {
    const user = userEvent.setup();
    render(
      <>
        <Toaster />
        <AnomalyThresholdPage />
      </>,
    );

    await waitFor(() => {
      expect(screen.getByText('Xem ước lượng tác động (30 ngày)')).toBeInTheDocument();
    });

    const estimateButton = screen.getByText('Xem ước lượng tác động (30 ngày)');
    await user.click(estimateButton);

    await waitFor(() => {
      expect(thresholdApi.estimateImpact).toHaveBeenCalled();
      expect(screen.getByText('Dự kiến có 4 mã tem bị gắn cờ bất thường.')).toBeInTheDocument();
    });
  });

  it('cập nhật cấu hình toàn cục thành công khi nhấn Lưu', async () => {
    const user = userEvent.setup();
    thresholdApi.updateGlobalThreshold.mockResolvedValue({
      ...MOCK_DATA.global,
      maxScansPerHour: 8,
    });

    render(
      <>
        <Toaster />
        <AnomalyThresholdPage />
      </>,
    );

    await waitFor(() => {
      expect(screen.getByText('Lưu cấu hình toàn cục')).toBeInTheDocument();
    });

    const saveButton = screen.getByText('Lưu cấu hình toàn cục');
    await user.click(saveButton);

    await waitFor(() => {
      expect(thresholdApi.updateGlobalThreshold).toHaveBeenCalled();
    });
  });
});
