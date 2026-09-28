import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ProcurementDashboard } from '../ProcurementDashboard';

vi.mock('@/components/shipment/ProcurementShipmentList', () => ({
  ProcurementShipmentList: () => <div data-testid="procurement-shipment-list" />,
}));

vi.mock('@/components/procurement/RecordProcurementDialog', () => ({
  RecordProcurementDialog: () => <div data-testid="record-procurement-dialog" />,
}));

vi.mock('@/components/help/HelpButton', () => ({
  HelpButton: ({ screenKey }: { screenKey: string }) => (
    <button data-testid="help-button">Hướng dẫn ({screenKey})</button>
  ),
}));

describe('ProcurementDashboard (NCL-12-CN-004)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders header, help button, and data portal docs button', () => {
    render(<ProcurementDashboard />);

    expect(screen.getByRole('heading', { name: /Thu mua nông sản/i })).toBeInTheDocument();
    expect(screen.getByTestId('help-button')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Tài liệu cổng dữ liệu/i })
    ).toBeInTheDocument();
  });

  it('opens /portal in a new tab when clicking "Tài liệu cổng dữ liệu"', () => {
    const windowOpenMock = vi.spyOn(window, 'open').mockImplementation(() => null);

    render(<ProcurementDashboard />);

    const docsBtn = screen.getByRole('button', { name: /Tài liệu cổng dữ liệu/i });
    fireEvent.click(docsBtn);

    expect(windowOpenMock).toHaveBeenCalledWith('/portal', '_blank');
  });
});
