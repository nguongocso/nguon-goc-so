import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AreaDeviationConfirmDialog } from '../AreaDeviationConfirmDialog';
import type { AreaDeviationErrorData } from '@/types/farmArea';

describe('AreaDeviationConfirmDialog', () => {
  const mockData: AreaDeviationErrorData = {
    code: 'AREA_DEVIATION_CONFIRMATION_REQUIRED',
    declaredArea: 1.0,
    calculatedArea: 1.45,
    deviationPercentage: 45.0,
    thresholdPercentage: 30.0,
  };

  it('không hiển thị khi open = false hoặc data = null', () => {
    const { container } = render(
      <AreaDeviationConfirmDialog
        open={false}
        data={mockData}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />
    );
    expect(screen.queryByText('Xác nhận chênh lệch diện tích')).not.toBeInTheDocument();
  });

  it('hiển thị đầy đủ thông tin so sánh diện tích khi mở', () => {
    render(
      <AreaDeviationConfirmDialog
        open={true}
        data={mockData}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByText('Xác nhận chênh lệch diện tích')).toBeInTheDocument();
    expect(screen.getByText('1.0000 ha')).toBeInTheDocument();
    expect(screen.getByText('1.4500 ha')).toBeInTheDocument();
    expect(screen.getByText('45.00%')).toBeInTheDocument();
    expect(screen.getByText(/Tôi hiểu và đồng ý lưu/i)).toBeInTheDocument();
  });

  it('kích hoạt onConfirm khi người dùng bấm đồng ý lưu', async () => {
    const user = userEvent.setup();
    const handleConfirm = vi.fn();
    const handleCancel = vi.fn();

    render(
      <AreaDeviationConfirmDialog
        open={true}
        data={mockData}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    );

    const confirmBtn = screen.getByRole('button', { name: /Tôi hiểu và đồng ý lưu/i });
    await user.click(confirmBtn);

    expect(handleConfirm).toHaveBeenCalledTimes(1);
    expect(handleCancel).not.toHaveBeenCalled();
  });

  it('kích hoạt onCancel khi người dùng bấm quay lại chỉnh sửa', async () => {
    const user = userEvent.setup();
    const handleConfirm = vi.fn();
    const handleCancel = vi.fn();

    render(
      <AreaDeviationConfirmDialog
        open={true}
        data={mockData}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    );

    const cancelBtn = screen.getByRole('button', { name: /Quay lại chỉnh sửa/i });
    await user.click(cancelBtn);

    expect(handleCancel).toHaveBeenCalledTimes(1);
    expect(handleConfirm).not.toHaveBeenCalled();
  });
});
