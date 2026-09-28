import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom';
import { HandoverListPage } from '../HandoverListPage';
import * as handoverApi from '@/api/handoverApi';
import type { HandoverSummary } from '@/types/handover';

vi.mock('@/api/handoverApi', () => ({
  getHandovers: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

const mockHandovers: HandoverSummary[] = [
  {
    id: '3dd95ecb-978f-42f7-8b09-cf1a966872d0',
    shipmentId: '00000000-0000-0000-0000-001000000001',
    shipmentName: 'Lô hàng Nho đỏ Ninh Thuận',
    fromOrganizationName: 'HTX Nông Nghiệp Ba Mọi',
    toOrganizationName: 'Công ty Thực Phẩm Sạch VinEco',
    quantity: 500,
    unit: 'kg',
    status: 'PENDING',
    createdAt: '2026-09-10T08:00:00Z',
  },
  {
    id: '4ee95ecb-978f-42f7-8b09-cf1a966872d1',
    shipmentId: '00000000-0000-0000-0000-001000000002',
    shipmentName: 'Lô Xoài Cát Chu Đồng Tháp',
    fromOrganizationName: 'HTX Xoài Mỹ Xương',
    toOrganizationName: 'Công ty Thực Phẩm Sạch VinEco',
    quantity: 1200,
    unit: 'kg',
    status: 'ACCEPTED',
    createdAt: '2026-09-09T08:00:00Z',
  },
];

function DetailStub() {
  const { id } = useParams<{ id: string }>();
  return <div>Chi tiết phiếu: {id}</div>;
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/handover']}>
      <Routes>
        <Route path="/handover" element={<HandoverListPage />} />
        <Route path="/handover/:id" element={<DetailStub />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('HandoverListPage - Danh sách phiếu bàn giao nhận cho VT-04', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('hiển thị danh sách phiếu bàn giao nhận với các cột chuẩn và không để lộ UUID', async () => {
    vi.mocked(handoverApi.getHandovers).mockResolvedValue({
      items: mockHandovers,
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
      empty: false,
    });

    renderPage();

    // Tiêu đề trang
    expect(await screen.findByText('Phiếu bàn giao nhận')).toBeInTheDocument();

    // Header bảng
    expect(screen.getByText('STT')).toBeInTheDocument();
    expect(screen.getByText('Tên lô hàng')).toBeInTheDocument();
    expect(screen.getByText('Tổ chức giao')).toBeInTheDocument();
    expect(screen.getByText('Số lượng')).toBeInTheDocument();
    expect(screen.getByText('Trạng thái')).toBeInTheDocument();
    expect(screen.getByText('Ngày tạo')).toBeInTheDocument();
    expect(screen.getByText('Thao tác')).toBeInTheDocument();

    // Dữ liệu hàng
    expect(screen.getByText('Lô hàng Nho đỏ Ninh Thuận')).toBeInTheDocument();
    expect(screen.getByText('HTX Nông Nghiệp Ba Mọi')).toBeInTheDocument();
    expect(screen.getByText('500 kg')).toBeInTheDocument();
    expect(screen.getByText('Chờ xác nhận')).toBeInTheDocument();

    expect(screen.getByText('Lô Xoài Cát Chu Đồng Tháp')).toBeInTheDocument();
    expect(screen.getByText('HTX Xoài Mỹ Xương')).toBeInTheDocument();
    expect(screen.getByText('1.200 kg')).toBeInTheDocument();
    expect(screen.getByText('Đã xác nhận')).toBeInTheDocument();

    // Không để lộ bất kỳ UUID nào trong UI
    expect(screen.queryByText('3dd95ecb-978f-42f7-8b09-cf1a966872d0')).not.toBeInTheDocument();
    expect(screen.queryByText('00000000-0000-0000-0000-001000000001')).not.toBeInTheDocument();
  });

  it('click nút "Xem chi tiết" điều hướng tới trang chi tiết /handover/:id', async () => {
    vi.mocked(handoverApi.getHandovers).mockResolvedValue({
      items: [mockHandovers[0]],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      empty: false,
    });

    renderPage();

    const detailBtn = await screen.findByRole('button', { name: /Xem chi tiết/i });
    await userEvent.click(detailBtn);

    expect(await screen.findByText('Chi tiết phiếu: 3dd95ecb-978f-42f7-8b09-cf1a966872d0')).toBeInTheDocument();
  });

  it('click vào dòng bảng điều hướng tới /handover/:id', async () => {
    vi.mocked(handoverApi.getHandovers).mockResolvedValue({
      items: [mockHandovers[1]],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      empty: false,
    });

    renderPage();

    const rowCell = await screen.findByText('Lô Xoài Cát Chu Đồng Tháp');
    await userEvent.click(rowCell);

    expect(await screen.findByText('Chi tiết phiếu: 4ee95ecb-978f-42f7-8b09-cf1a966872d1')).toBeInTheDocument();
  });
});
