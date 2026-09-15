import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import AggregateAlertPage from '../AggregateAlertPage';
import * as aggregateAlertApi from '@/api/aggregateAlertApi';
import type { AggregateAlertPageResponse } from '@/types/aggregateAlert';

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      userId: 'user-01',
      username: 'manager_vt02',
      fullName: 'Nguyễn Văn Quản Lý',
      roleCode: 'VT-02',
      organizationId: 'org-01',
      organizationName: 'Hợp tác xã Nông nghiệp Xanh',
    },
  }),
}));

describe('NCL-08-CN-016: Trang cảnh báo tổng hợp', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('TC-01: Hiển thị đầy đủ 4 loại cảnh báo, thẻ tóm tắt và nút lối tắt xử lý trực tiếp (không dùng modal)', async () => {
    const mockData: AggregateAlertPageResponse = {
      items: [
        {
          id: 'alert-1',
          type: 'SCAN_ANOMALY',
          typeName: 'Tem quét bất thường',
          severity: 'HIGH',
          title: 'Phát hiện quét bất thường',
          message: 'Mã tem bị quét 20 lần tại 3 tỉnh thành',
          relatedEntityType: 'TRACE_CODE',
          relatedEntityId: 'code-1',
          relatedEntityName: 'Mã tem TEM-001',
          createdAt: '2026-09-14T08:00:00',
          actionUrl: '/alerts/scan-anomaly',
          organizationId: 'org-01',
          organizationName: 'Hợp tác xã Nông nghiệp Xanh',
          status: 'OPEN',
        },
        {
          id: 'alert-2',
          type: 'CERT_EXPIRING',
          typeName: 'Chứng nhận sắp hết hạn',
          severity: 'MEDIUM',
          title: 'Chứng nhận VietGAP sắp hết hạn',
          message: 'Còn 5 ngày nữa sẽ hết hiệu lực',
          relatedEntityType: 'CERTIFICATION',
          relatedEntityId: 'cert-1',
          relatedEntityName: 'VietGAP Trồng trọt',
          createdAt: '2026-09-14T07:30:00',
          actionUrl: '/certifications',
          organizationId: 'org-01',
          organizationName: 'Hợp tác xã Nông nghiệp Xanh',
          status: 'OPEN',
        },
        {
          id: 'alert-3',
          type: 'UNPROCESSED_FEEDBACK',
          typeName: 'Phản ánh chưa xử lý',
          severity: 'HIGH',
          title: 'Phản ánh của người tiêu dùng (Mới)',
          message: 'Tem rách không thể kích hoạt tra cứu',
          relatedEntityType: 'PRODUCT_FEEDBACK',
          relatedEntityId: 'fb-1',
          relatedEntityName: 'Lô Xoài Cát Chu',
          createdAt: '2026-09-13T16:00:00',
          actionUrl: '/product-feedbacks',
          organizationId: 'org-01',
          organizationName: 'Hợp tác xã Nông nghiệp Xanh',
          status: 'OPEN',
        },
        {
          id: 'alert-4',
          type: 'OVERDUE_MILESTONE',
          typeName: 'Mốc canh tác quá hạn',
          severity: 'HIGH',
          title: 'Quá hạn mốc: Bón phân đợt 1',
          message: 'Lô xoài đã quá hạn ghi mốc 8 ngày',
          relatedEntityType: 'MILESTONE_REMINDER',
          relatedEntityId: 'mr-1',
          relatedEntityName: 'Lô Xoài Cát Chu',
          createdAt: '2026-09-12T09:00:00',
          actionUrl: '/farm-logs/create?lotId=lot-1',
          organizationId: 'org-01',
          organizationName: 'Hợp tác xã Nông nghiệp Xanh',
          status: 'OPEN',
        },
      ],
      totalElements: 4,
      totalPages: 1,
      currentPage: 0,
      pageSize: 10,
      summaryCounts: {
        totalOpen: 4,
        highSeverityCount: 3,
        mediumSeverityCount: 1,
        byTypeCounts: {
          SCAN_ANOMALY: 1,
          CERT_EXPIRING: 1,
          UNPROCESSED_FEEDBACK: 1,
          OVERDUE_MILESTONE: 1,
        },
      },
    };

    vi.spyOn(aggregateAlertApi, 'getAggregateAlerts').mockResolvedValueOnce(mockData);

    render(
      <MemoryRouter>
        <AggregateAlertPage />
      </MemoryRouter>
    );

    // Tiêu đề trang
    await waitFor(() => {
      expect(screen.getByText('Tổng hợp cảnh báo')).toBeInTheDocument();
    });

    // Thẻ thống kê
    expect(screen.getByText('4')).toBeInTheDocument(); // Tổng cảnh báo mở
    expect(screen.getByText('3')).toBeInTheDocument(); // Mức cao
    expect(screen.getByText('1')).toBeInTheDocument(); // Trung bình

    // Danh sách các loại cảnh báo
    expect(screen.getAllByText('Tem quét bất thường').length).toBeGreaterThan(0);
    expect(screen.getByText('Chứng nhận sắp hết hạn')).toBeInTheDocument();
    expect(screen.getByText('Phản ánh chưa xử lý')).toBeInTheDocument();
    expect(screen.getByText('Mốc canh tác quá hạn')).toBeInTheDocument();

    // Nút lối tắt xử lý trực tiếp
    const actionButtons = screen.getAllByRole('button', { name: /Xử lý ngay/i });
    expect(actionButtons.length).toBe(4);

    // Click lối tắt chuyển trang trực tiếp (không mở modal)
    await userEvent.click(actionButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith('/alerts/scan-anomaly');

    // Kiểm tra không chứa các emoji icon text như "👉"
    const pageText = document.body.textContent || '';
    expect(pageText).not.toContain('👉');
  });

  it('TC-04: Hiển thị giao diện rỗng trang nhã khi không còn cảnh báo nào đang mở', async () => {
    const emptyData: AggregateAlertPageResponse = {
      items: [],
      totalElements: 0,
      totalPages: 0,
      currentPage: 0,
      pageSize: 10,
      summaryCounts: {
        totalOpen: 0,
        highSeverityCount: 0,
        mediumSeverityCount: 0,
        byTypeCounts: {},
      },
    };

    vi.spyOn(aggregateAlertApi, 'getAggregateAlerts').mockResolvedValueOnce(emptyData);

    render(
      <MemoryRouter>
        <AggregateAlertPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Không có cảnh báo nào đang mở')).toBeInTheDocument();
    });

    expect(screen.getByText(/Tất cả các nguồn cảnh báo của tổ chức hiện đang ở trạng thái an toàn/i)).toBeInTheDocument();

    // Kiểm tra không chứa các emoji icon text như "👉"
    const pageText = document.body.textContent || '';
    expect(pageText).not.toContain('👉');
  });
});
