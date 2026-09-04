import { describe, expect, it, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { Toaster } from 'sonner';
import { CategoryOverridePage } from '@/pages/admin/CategoryOverridePage';
import type { AnomalyThresholdConfig, ImpactEstimationResult } from '@/types/anomalyThreshold';

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

const MOCK_CATEGORIES = [
  { id: 'cat-id-1', name: 'Sầu riêng Ri6', isActive: true },
  { id: 'cat-id-2', name: 'Xoài Cát Chu', isActive: true },
];

const MOCK_GLOBAL: AnomalyThresholdConfig = {
  id: 'global-id-1',
  productCategoryId: null,
  productCategoryName: null,
  maxScansPerHour: 5,
  maxScansPerDay: 10,
  maxDistanceKmPer30Min: 50.0,
  minTimeBetweenScansMinutes: 30,
  activationAgeDays: 365,
  isActive: true,
};

const MOCK_OVERRIDES: AnomalyThresholdConfig[] = [
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
];

const MOCK_IMPACT: ImpactEstimationResult = {
  estimatedAnomaliesCount: 2,
  totalScansAnalyzed: 100,
  totalTraceCodesAnalyzed: 25,
  highFrequencyCount: 1,
  impossibleTravelCount: 1,
  activationAgeCount: 0,
  analysisPeriodDays: 30,
  message: 'Dự kiến có 2 mã tem bị gắn cờ bất thường.',
};

describe('CategoryOverridePage (NCL-08-CN-014)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    productCategoryApi.getProductCategories.mockResolvedValue(MOCK_CATEGORIES);
    thresholdApi.getGlobalThreshold.mockResolvedValue(MOCK_GLOBAL);
    thresholdApi.getCategoryOverrides.mockResolvedValue(MOCK_OVERRIDES);
    thresholdApi.estimateImpact.mockResolvedValue(MOCK_IMPACT);
    thresholdApi.saveCategoryOverride.mockResolvedValue(MOCK_OVERRIDES[0]);
  });

  it('hiển thị form tạo mới cấu hình với tiêu đề và các trường nhập liệu', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/anomaly-thresholds/categories/create']}>
        <Toaster />
        <Routes>
          <Route
            path="/admin/anomaly-thresholds/categories/create"
            element={<CategoryOverridePage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Thêm mới cấu hình theo loại nông sản')).toBeInTheDocument();
      expect(screen.getByText('Loại nông sản')).toBeInTheDocument();
      expect(screen.getByText('Số lượt quét tối đa / giờ (1 mã)')).toBeInTheDocument();
      expect(screen.getByText('Lưu cấu hình')).toBeInTheDocument();
    });
  });

  it('tải dữ liệu cấu hình khi ở chế độ chỉnh sửa', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/anomaly-thresholds/categories/override-id-1/edit']}>
        <Toaster />
        <Routes>
          <Route
            path="/admin/anomaly-thresholds/categories/:id/edit"
            element={<CategoryOverridePage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Chỉnh sửa cấu hình theo loại nông sản')).toBeInTheDocument();
      expect(screen.getByText('Sầu riêng Ri6')).toBeInTheDocument();
    });
  });

  it('thực hiện ước lượng tác động thành công khi click nút Ước lượng', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/admin/anomaly-thresholds/categories/override-id-1/edit']}>
        <Toaster />
        <Routes>
          <Route
            path="/admin/anomaly-thresholds/categories/:id/edit"
            element={<CategoryOverridePage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Ước lượng tác động (30 ngày)')).toBeInTheDocument();
    });

    const estimateButton = screen.getByText('Ước lượng tác động (30 ngày)');
    await user.click(estimateButton);

    await waitFor(() => {
      expect(thresholdApi.estimateImpact).toHaveBeenCalled();
      expect(screen.getByText('Dự kiến có 2 mã tem bị gắn cờ bất thường.')).toBeInTheDocument();
    });
  });
});
