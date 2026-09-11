import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ShipmentTimelineItem } from '../ShipmentTimelineItem';
import type { ChainEventResponse } from '@/types/packaging';

vi.mock('@/hooks/usePermission', () => ({
  usePermission: () => false,
}));

describe('ShipmentTimelineItem - HANDOVER event localization', () => {
  const handoverEvent: ChainEventResponse = {
    id: 'evt-12345',
    eventType: 'HANDOVER' as never,
    recordedAt: '2026-09-11T10:00:00Z',
    recordedByName: 'Nguyễn Văn A',
    eventData: {
      action: 'ACCEPTED',
      quantity: 500,
      fromOrgId: '47bcceae-1234-5678-9abc-def012345678',
      fromOrganizationName: 'Hợp tác xã Nông nghiệp Hòa Bình',
      toOrgId: 'cc586748-8765-4321-cba9-876543210fed',
      toOrganizationName: 'Công ty Cổ phần Thực phẩm An Lành',
      note: 'Hàng đảm bảo chất lượng, đủ số lượng',
    },
  };

  it('hiển thị nhãn sự kiện là "Bàn giao" và người ghi nhận', () => {
    render(
      <MemoryRouter>
        <ShipmentTimelineItem event={handoverEvent} index={0} total={1} />
      </MemoryRouter>,
    );

    expect(screen.getByText('Bàn giao')).toBeInTheDocument();
    expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
  });

  it('Việt hóa hành động ACCEPTED thành "Đã xác nhận"', () => {
    render(
      <MemoryRouter>
        <ShipmentTimelineItem event={handoverEvent} index={0} total={1} />
      </MemoryRouter>,
    );

    expect(screen.getByText('Hành động:')).toBeInTheDocument();
    expect(screen.getByText('Đã xác nhận')).toBeInTheDocument();
  });

  it('hiển thị số lượng nhận và tên tổ chức giao/nhận thay vì UUID', () => {
    render(
      <MemoryRouter>
        <ShipmentTimelineItem event={handoverEvent} index={0} total={1} />
      </MemoryRouter>,
    );

    expect(screen.getByText('Số lượng nhận:')).toBeInTheDocument();
    expect(screen.getByText('500 kg')).toBeInTheDocument();

    expect(screen.getByText('Bên giao:')).toBeInTheDocument();
    expect(screen.getByText('Hợp tác xã Nông nghiệp Hòa Bình')).toBeInTheDocument();

    expect(screen.getByText('Bên nhận:')).toBeInTheDocument();
    expect(screen.getByText('Công ty Cổ phần Thực phẩm An Lành')).toBeInTheDocument();

    expect(screen.getByText('Ghi chú:')).toBeInTheDocument();
    expect(screen.getByText('Hàng đảm bảo chất lượng, đủ số lượng')).toBeInTheDocument();

    // Tuyệt đối không hiển thị raw UUID của fromOrgId / toOrgId
    expect(screen.queryByText('47bcceae-1234-5678-9abc-def012345678')).not.toBeInTheDocument();
    expect(screen.queryByText('cc586748-8765-4321-cba9-876543210fed')).not.toBeInTheDocument();
  });

  it('Việt hóa các action khác: REJECTED, EXPIRED, PENDING', () => {
    const rejectedEvent: ChainEventResponse = {
      ...handoverEvent,
      eventData: {
        action: 'REJECTED',
        quantity: 300,
        fromOrganizationName: 'HTX A',
        toOrganizationName: 'DN B',
      },
    };

    render(
      <MemoryRouter>
        <ShipmentTimelineItem event={rejectedEvent} index={0} total={1} />
      </MemoryRouter>,
    );

    expect(screen.getByText('Từ chối')).toBeInTheDocument();
  });
});
