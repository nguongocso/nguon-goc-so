import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import SuspectTraceCodeDetailPage from '../SuspectTraceCodeDetailPage';
import * as suspectApi from '@/api/suspectTraceCodeApi';
import type { SuspectTraceCodeDetailResponse } from '@/types/suspectTraceCode';

vi.mock('@/api/suspectTraceCodeApi', () => ({
  getSuspectDetail: vi.fn(),
  lockTraceCode: vi.fn(),
  unlockTraceCode: vi.fn(),
}));

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      userId: 'admin-user-id',
      username: 'admin',
      fullName: 'Quản trị viên',
      roles: ['VT-01'],
    },
  }),
}));

vi.mock('@/components/help/HelpButton', () => ({
  HelpButton: ({ screenKey, customContent }: { screenKey: string; customContent?: any }) => (
    <div data-testid="help-button" data-screenkey={screenKey}>
      {customContent?.title || 'Hướng dẫn'}
    </div>
  ),
}));

const MOCK_DETAIL: SuspectTraceCodeDetailResponse = {
  id: '00000000-0000-0000-0000-000000060001',
  codeValue: 'NCL-TEST-SUS-01',
  shipmentName: 'Lô Sầu Riêng Xuất Khẩu',
  productCategoryName: 'Sầu riêng Ri6',
  status: 'SUSPECT',
  suspicionScore: 80,
  suspicionReason: 'Khoảng cách di chuyển bất khả thi >500km trong 15 phút; Tần suất cao',
  scanCount: 0,
  uniqueLocations: 0,
  firstScannedAt: null,
  lastScannedAt: null,
  lockedAt: null,
  lockedBy: null,
  lockedByName: null,
  lockReason: null,
  evaluatedAt: '2026-09-14T10:00:00',
  effectiveThreshold: {
    id: 'threshold-1',
    productCategoryId: 'cat-1',
    productCategoryName: 'Sầu riêng Ri6',
    maxScansPerHour: 4,
    maxScansPerDay: 8,
    maxDistanceKmPer30Min: 40,
    minTimeBetweenScansMinutes: 20,
    activationAgeDays: 3,
    isActive: true,
  },
  anomalyDetails: {
    totalScans: 0,
    uniqueLocations: 0,
    impossibleTravelCount: 1,
    scoreBreakdown: {
      highFrequency: 35,
      impossibleTravel: 45,
      multipleLocations: 0,
    },
  },
  scanLogs: [],
};

describe('SuspectTraceCodeDetailPage (NCL-08-CN-007 & NCL-08-CN-014)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(suspectApi.getSuspectDetail).mockResolvedValue(MOCK_DETAIL);
  });

  it('hiển thị đầy đủ thông tin mã tem, loại nông sản và nguồn ngưỡng áp dụng', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/suspect-trace-codes/00000000-0000-0000-0000-000000060001']}>
        <Routes>
          <Route path="/admin/suspect-trace-codes/:traceCodeId" element={<SuspectTraceCodeDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Chi tiết mã tem nghi vấn')).toBeInTheDocument();
      expect(screen.getAllByText('NCL-TEST-SUS-01').length).toBeGreaterThan(0);
      expect(screen.getByText('Sầu riêng Ri6')).toBeInTheDocument();
      expect(screen.getByText('Ghi đè theo danh mục (Sầu riêng Ri6)')).toBeInTheDocument();
    });
  });

  it('hiển thị nhãn ngưỡng động và snapshot điểm nghi vấn 80/100', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/suspect-trace-codes/00000000-0000-0000-0000-000000060001']}>
        <Routes>
          <Route path="/admin/suspect-trace-codes/:traceCodeId" element={<SuspectTraceCodeDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      // Điểm tổng
      expect(screen.getAllByText('80/100').length).toBeGreaterThan(0);
      // Điểm thành phần từ snapshot
      expect(screen.getByText('+35')).toBeInTheDocument();
      expect(screen.getByText('+45')).toBeInTheDocument();
      // Nhãn động hiển thị ngưỡng từ effectiveThreshold (≥ 8 lượt/24h hoặc ≥ 4 lượt/giờ, >40km trong ≤20 phút)
      expect(screen.getByText(/≥ 8 lượt\/24h hoặc ≥ 4 lượt\/giờ/)).toBeInTheDocument();
      expect(screen.getByText(/>40km trong ≤20 phút/)).toBeInTheDocument();
    });
  });

  it('hiển thị thông báo giải thích rõ ràng khi không có lượt quét nào trong 24h gần nhất', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/suspect-trace-codes/00000000-0000-0000-0000-000000060001']}>
        <Routes>
          <Route path="/admin/suspect-trace-codes/:traceCodeId" element={<SuspectTraceCodeDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Không có lượt quét nào trong 24 giờ gần nhất')).toBeInTheDocument();
      expect(screen.getByText(/Dữ liệu bằng chứng vi phạm lúc phát hiện/)).toBeInTheDocument();
    });
  });

  it('hiển thị hướng dẫn khóa mã tem khi trạng thái là SUSPECT', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/suspect-trace-codes/00000000-0000-0000-0000-000000060001']}>
        <Routes>
          <Route path="/admin/suspect-trace-codes/:traceCodeId" element={<SuspectTraceCodeDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      const helpBtn = screen.getByTestId('help-button');
      expect(helpBtn).toHaveAttribute('data-screenkey', 'admin-suspect-trace-code-suspect');
      expect(helpBtn).toHaveTextContent('Hướng dẫn khóa mã tem nghi vấn (Trạng thái Nghi vấn)');
    });
  });

  it('hiển thị hướng dẫn mở khóa mã tem khi trạng thái là LOCKED', async () => {
    vi.mocked(suspectApi.getSuspectDetail).mockResolvedValueOnce({
      ...MOCK_DETAIL,
      status: 'LOCKED',
      lockedAt: '2026-09-14T11:00:00',
      lockedByName: 'Admin',
      lockReason: 'Nghi vấn quét bất thường',
    });

    render(
      <MemoryRouter initialEntries={['/admin/suspect-trace-codes/00000000-0000-0000-0000-000000060001']}>
        <Routes>
          <Route path="/admin/suspect-trace-codes/:traceCodeId" element={<SuspectTraceCodeDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      const helpBtn = screen.getByTestId('help-button');
      expect(helpBtn).toHaveAttribute('data-screenkey', 'admin-suspect-trace-code-locked');
      expect(helpBtn).toHaveTextContent('Hướng dẫn mở khóa mã tem (Trạng thái Đã khóa)');
    });
  });
});
