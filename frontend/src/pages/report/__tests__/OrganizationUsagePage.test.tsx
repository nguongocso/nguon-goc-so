import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import OrganizationUsagePage from '../OrganizationUsagePage';
import * as organizationUsageApi from '@/api/organizationUsageApi';
import { OrganizationUsageApiError } from '@/api/organizationUsageApi';
import type {
  OrganizationUsageDashboard,
  OrganizationUsageItem,
} from '@/types/organizationUsage';

// Mock sonner toast
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}));

// Mock useHelp để không gọi API hướng dẫn thật
vi.mock('@/hooks/useHelp', () => ({
  useHelp: () => ({ data: null, isLoading: false, error: null, refetch: vi.fn() }),
}));

function buildItem(overrides: Partial<OrganizationUsageItem> = {}): OrganizationUsageItem {
  return {
    organizationId: 'a9f8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4d',
    organizationCode: 'HTX001',
    organizationName: 'Hợp tác xã Chè Tân Cương',
    organizationType: 'COOPERATIVE',
    organizationStatus: 'ACTIVE',
    createdAt: '2026-01-10T08:00:00',
    hasData: true,
    lastActivityAt: '2026-09-28T15:30:00',
    needsSupport: false,
    productionLots: { current: 10, previous: 8, change: 2, changePercent: 25.0 },
    farmLogs: { current: 35, previous: 30, change: 5, changePercent: 16.67 },
    chainEvents: { current: 20, previous: 22, change: -2, changePercent: -9.09 },
    activatedLabels: { current: 500, previous: 0, change: 500, changePercent: null },
    publicLookups: { current: 120, previous: 100, change: 20, changePercent: 20.0 },
    activeUsers: { current: 5, previous: 4, change: 1, changePercent: 25.0 },
    ...overrides,
  };
}

function buildDashboard(items: OrganizationUsageItem[]): OrganizationUsageDashboard {
  return {
    startDate: '2026-09-01',
    endDate: '2026-09-30',
    previousStartDate: '2026-08-02',
    previousEndDate: '2026-08-31',
    totalOrganizations: items.length,
    items,
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <OrganizationUsagePage />
    </MemoryRouter>
  );
}

describe('NCL-07-CN-008: Bảng điều khiển mức độ sử dụng nền tảng theo tổ chức', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('TC-01: Hiển thị đủ 3 tổ chức với 6 chỉ số kèm so sánh kỳ trước', async () => {
    const items = [
      buildItem(),
      buildItem({
        organizationId: 'b2c8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4e',
        organizationCode: 'HTX002',
        organizationName: 'Hợp tác xã Rau Sạch',
        lastActivityAt: '2026-07-01T08:00:00',
        needsSupport: true,
        productionLots: { current: 1, previous: 2, change: -1, changePercent: -50.0 },
      }),
      buildItem({
        organizationId: 'c3c8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4f',
        organizationCode: 'HTX099',
        organizationName: 'Hợp tác xã mới thành lập',
        hasData: false,
        lastActivityAt: null,
        needsSupport: true,
        productionLots: { current: 0, previous: 0, change: 0, changePercent: null },
        farmLogs: { current: 0, previous: 0, change: 0, changePercent: null },
        chainEvents: { current: 0, previous: 0, change: 0, changePercent: null },
        activatedLabels: { current: 0, previous: 0, change: 0, changePercent: null },
        publicLookups: { current: 0, previous: 0, change: 0, changePercent: null },
        activeUsers: { current: 0, previous: 0, change: 0, changePercent: null },
      }),
    ];
    vi.spyOn(organizationUsageApi, 'getOrganizationUsage').mockResolvedValue(
      buildDashboard(items)
    );

    renderPage();

    // 3 tổ chức đều hiển thị
    expect(await screen.findByText('Hợp tác xã Chè Tân Cương')).toBeInTheDocument();
    expect(screen.getByText('Hợp tác xã Rau Sạch')).toBeInTheDocument();
    expect(screen.getByText('Hợp tác xã mới thành lập')).toBeInTheDocument();

    // Ô so sánh chỉ hiển thị % (kèm mũi tên), không Infinity/NaN
    expect(screen.getAllByText(/^[+-]?\d+\.\d%$/).length).toBeGreaterThanOrEqual(1);
    // previous = 0, current > 0 quy ước hiển thị "+100.0%"
    expect(screen.getAllByText('+100.0%').length).toBeGreaterThanOrEqual(1);
    expect(screen.queryByText('Mới phát sinh')).not.toBeInTheDocument();
    expect(screen.queryByText(/Infinity|NaN/)).not.toBeInTheDocument();

    // Tổ chức inactive hiển thị cảnh báo
    expect(screen.getAllByText('Cần liên hệ hỗ trợ').length).toBeGreaterThanOrEqual(1);

    // Tổ chức mới hiển thị "Chưa có dữ liệu"
    expect(screen.getAllByText('Chưa có dữ liệu').length).toBeGreaterThanOrEqual(1);
  });

  it('TC-01b: Tổ chức ngừng hoạt động (hasData=false, có lastActivityAt) hiển thị "Cần liên hệ hỗ trợ", không nhầm với "Chưa có dữ liệu"', async () => {
    const items = [
      buildItem({
        organizationCode: 'HTXB',
        organizationName: 'Hợp tác xã Rau Sạch',
        hasData: false,
        lastActivityAt: '2026-06-01T08:00:00',
        needsSupport: true,
        productionLots: { current: 0, previous: 0, change: 0, changePercent: null },
        farmLogs: { current: 0, previous: 0, change: 0, changePercent: null },
        chainEvents: { current: 0, previous: 0, change: 0, changePercent: null },
        activatedLabels: { current: 0, previous: 0, change: 0, changePercent: null },
        publicLookups: { current: 0, previous: 0, change: 0, changePercent: null },
        activeUsers: { current: 0, previous: 0, change: 0, changePercent: null },
      }),
      buildItem({
        organizationCode: 'HTXC',
        organizationName: 'Hợp tác xã Mới Thành Lập',
        hasData: false,
        lastActivityAt: null,
        needsSupport: true,
        productionLots: { current: 0, previous: 0, change: 0, changePercent: null },
        farmLogs: { current: 0, previous: 0, change: 0, changePercent: null },
        chainEvents: { current: 0, previous: 0, change: 0, changePercent: null },
        activatedLabels: { current: 0, previous: 0, change: 0, changePercent: null },
        publicLookups: { current: 0, previous: 0, change: 0, changePercent: null },
        activeUsers: { current: 0, previous: 0, change: 0, changePercent: null },
      }),
    ];
    vi.spyOn(organizationUsageApi, 'getOrganizationUsage').mockResolvedValue(
      buildDashboard(items)
    );

    renderPage();
    await screen.findByText('Hợp tác xã Rau Sạch');

    // HTXB: đã từng hoạt động nhưng ngừng >= 30 ngày → "Cần liên hệ hỗ trợ"
    expect(screen.getAllByText('Cần liên hệ hỗ trợ').length).toBeGreaterThanOrEqual(1);
    // HTXC: chưa từng hoạt động → "Chưa có dữ liệu"
    expect(screen.getAllByText('Chưa có dữ liệu').length).toBeGreaterThanOrEqual(1);
  });

  it('TC-06: Thứ tự mặc định ưu tiên Cần liên hệ → Hoạt động → Chưa có dữ liệu', async () => {
    const zeroMetrics = {
      productionLots: { current: 0, previous: 0, change: 0, changePercent: null },
      farmLogs: { current: 0, previous: 0, change: 0, changePercent: null },
      chainEvents: { current: 0, previous: 0, change: 0, changePercent: null },
      activatedLabels: { current: 0, previous: 0, change: 0, changePercent: null },
      publicLookups: { current: 0, previous: 0, change: 0, changePercent: null },
      activeUsers: { current: 0, previous: 0, change: 0, changePercent: null },
    };
    const items = [
      buildItem({
        organizationId: 'c3c8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4f',
        organizationCode: 'HTX099',
        organizationName: 'Hợp tác xã mới thành lập',
        hasData: false,
        lastActivityAt: null,
        needsSupport: false,
        ...zeroMetrics,
      }),
      buildItem(),
      buildItem({
        organizationId: 'b2c8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4e',
        organizationCode: 'HTX002',
        organizationName: 'Hợp tác xã Rau Sạch',
        lastActivityAt: '2026-07-01T08:00:00',
        needsSupport: true,
      }),
    ];
    vi.spyOn(organizationUsageApi, 'getOrganizationUsage').mockResolvedValue(
      buildDashboard(items)
    );

    renderPage();
    await screen.findByText('Hợp tác xã Chè Tân Cương');

    // Cột STT đứng đầu bảng, đánh số theo thứ tự hiển thị
    expect(screen.getByRole('columnheader', { name: 'STT' })).toBeInTheDocument();

    // Mô tả kỳ báo cáo phía trên bảng: kỳ hiện tại và kỳ trước
    const currentPeriod = screen.getByText('Kỳ hiện tại:').parentElement as HTMLElement;
    expect(currentPeriod.textContent).toContain('từ 01/09/2026 đến 30/09/2026');
    const previousPeriod = screen.getByText('Kỳ trước:').parentElement as HTMLElement;
    expect(previousPeriod.textContent).toContain('từ 02/08/2026 đến 31/08/2026');

    const rows = screen.getAllByRole('row');
    expect(within(rows[1]).getAllByRole('cell')[0]).toHaveTextContent('1');
    expect(within(rows[1]).getAllByRole('cell')[1]).toHaveTextContent(
      'Hợp tác xã Rau Sạch'
    );
    expect(within(rows[2]).getAllByRole('cell')[1]).toHaveTextContent(
      'Hợp tác xã Chè Tân Cương'
    );
    expect(within(rows[3]).getAllByRole('cell')[1]).toHaveTextContent(
      'Hợp tác xã mới thành lập'
    );
  });

  it('TC-02: Sắp xếp theo chỉ số lô sản xuất thay đổi thứ tự hàng', async () => {
    const items = [
      buildItem({
        productionLots: { current: 1, previous: 0, change: 1, changePercent: null },
      }),
      buildItem({
        organizationId: 'b2c8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4e',
        organizationCode: 'HTX002',
        organizationName: 'Hợp tác xã Rau Sạch',
        productionLots: { current: 30, previous: 10, change: 20, changePercent: 200.0 },
      }),
    ];
    vi.spyOn(organizationUsageApi, 'getOrganizationUsage').mockResolvedValue(
      buildDashboard(items)
    );

    const user = userEvent.setup();
    renderPage();
    await screen.findByText('Hợp tác xã Chè Tân Cương');

    // Nhấn tiêu đề cột "Lô sản xuất" để sắp xếp giảm dần (lần 1 desc vì khác cột mặc định)
    await user.click(screen.getByRole('button', { name: /Lô sản xuất/ }));

    const rows = screen.getAllByRole('row');
    const firstDataRow = within(rows[1]).getAllByRole('cell')[1];
    expect(firstDataRow).toHaveTextContent('Hợp tác xã Rau Sạch');
  });

  it('TC-03: Lọc trạng thái "Cần liên hệ hỗ trợ" chỉ giữ tổ chức inactive', async () => {
    const items = [
      buildItem(),
      buildItem({
        organizationId: 'b2c8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4e',
        organizationCode: 'HTX002',
        organizationName: 'Hợp tác xã Rau Sạch',
        lastActivityAt: '2026-07-01T08:00:00',
        needsSupport: true,
      }),
    ];
    vi.spyOn(organizationUsageApi, 'getOrganizationUsage').mockResolvedValue(
      buildDashboard(items)
    );

    const user = userEvent.setup();
    renderPage();
    await screen.findByText('Hợp tác xã Chè Tân Cương');

    // Mở select trạng thái và chọn "Cần liên hệ hỗ trợ"
    await user.click(screen.getByRole('combobox'));
    await user.click(screen.getByRole('option', { name: 'Cần liên hệ hỗ trợ' }));

    expect(screen.queryByText('Hợp tác xã Chè Tân Cương')).not.toBeInTheDocument();
    expect(screen.getByText('Hợp tác xã Rau Sạch')).toBeInTheDocument();
  });

  it('TC-04: Backend trả 403 thì hiển thị trạng thái không có quyền', async () => {
    vi.spyOn(organizationUsageApi, 'getOrganizationUsage').mockRejectedValue(
      new OrganizationUsageApiError('Bạn không có quyền thực hiện chức năng này', 403)
    );

    renderPage();

    expect(await screen.findByText('Bạn không có quyền truy cập')).toBeInTheDocument();
    expect(
      screen.getByText(/chỉ dành cho Quản trị viên hệ thống \(VT-01\)/)
    ).toBeInTheDocument();
    // Không render bảng số liệu
    expect(screen.queryByText('Hợp tác xã Chè Tân Cương')).not.toBeInTheDocument();
  });

  it('TC-05: Đổi kỳ tự động gọi API với kỳ mới, không còn nút Áp dụng/Đặt lại', async () => {
    const spy = vi
      .spyOn(organizationUsageApi, 'getOrganizationUsage')
      .mockResolvedValue(buildDashboard([]));

    const user = userEvent.setup();
    renderPage();
    await waitFor(() => expect(spy).toHaveBeenCalledTimes(1));

    await user.clear(screen.getByLabelText('Từ ngày'));
    await user.type(screen.getByLabelText('Từ ngày'), '2026-08-10');
    await user.clear(screen.getByLabelText('Đến ngày'));
    await user.type(screen.getByLabelText('Đến ngày'), '2026-08-19');

    await waitFor(() =>
      expect(spy).toHaveBeenLastCalledWith({
        startDate: '2026-08-10',
        endDate: '2026-08-19',
      })
    );
    expect(
      screen.queryByRole('button', { name: 'Áp dụng' })
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Đặt lại' })).not.toBeInTheDocument();
  });

  it('Export: chọn "Xuất CSV" gọi API xuất với định dạng csv', async () => {
    vi.spyOn(organizationUsageApi, 'getOrganizationUsage').mockResolvedValue(
      buildDashboard([])
    );
    const exportSpy = vi
      .spyOn(organizationUsageApi, 'exportOrganizationUsage')
      .mockResolvedValue({
        blob: new Blob(['csv'], { type: 'text/csv' }),
        fileName: 'Bao_cao_muc_do_su_dung.csv',
      });

    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: /Xuất báo cáo/i }));
    await user.click(await screen.findByText(/Xuất CSV/));

    await waitFor(() =>
      expect(exportSpy).toHaveBeenCalledWith(expect.anything(), 'csv')
    );
  });

  it('Export: chọn "Xuất PDF" gọi API xuất với định dạng pdf', async () => {
    vi.spyOn(organizationUsageApi, 'getOrganizationUsage').mockResolvedValue(
      buildDashboard([])
    );
    const exportSpy = vi
      .spyOn(organizationUsageApi, 'exportOrganizationUsage')
      .mockResolvedValue({
        blob: new Blob(['pdf'], { type: 'application/pdf' }),
        fileName: 'Bao_cao_muc_do_su_dung.pdf',
      });

    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: /Xuất báo cáo/i }));
    await user.click(await screen.findByText(/Xuất PDF/));

    await waitFor(() =>
      expect(exportSpy).toHaveBeenCalledWith(expect.anything(), 'pdf')
    );
  });
});
