import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import {
  ShipmentStatusBadge,
  SHIPMENT_STATUS_LABELS,
  getShipmentStatusLabel,
} from '../ShipmentStatusBadge';
import type { ShipmentStatus } from '../ShipmentStatusBadge';

describe('ShipmentStatusBadge', () => {
  const statuses: ShipmentStatus[] = [
    'DRAFT',
    'CODE_PRINTED',
    'ACTIVATED',
    'RECALLING',
    'RECALLED',
  ];

  it.each(statuses)('Hiển thị đúng nhãn tiếng Việt cho trạng thái %s', (status) => {
    render(<ShipmentStatusBadge status={status} />);
    const label = SHIPMENT_STATUS_LABELS[status];
    expect(screen.getByText(label)).toBeDefined();
  });

  it('Hiển thị chính xác nhãn và class cho trạng thái RECALLING (Đang thu hồi)', () => {
    render(<ShipmentStatusBadge status="RECALLING" />);
    const badge = screen.getByText('Đang thu hồi');
    expect(badge).toBeDefined();
    expect(getShipmentStatusLabel('RECALLING')).toBe('Đang thu hồi');
  });

  it('Hiển thị chính xác nhãn cho trạng thái RECALLED (Đã thu hồi)', () => {
    render(<ShipmentStatusBadge status="RECALLED" />);
    const badge = screen.getByText('Đã thu hồi');
    expect(badge).toBeDefined();
    expect(getShipmentStatusLabel('RECALLED')).toBe('Đã thu hồi');
  });
});
