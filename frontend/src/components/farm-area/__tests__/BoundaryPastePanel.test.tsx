import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BoundaryPastePanel } from '../BoundaryPastePanel';

describe('BoundaryPastePanel', () => {
  it('thu gọn theo mặc định và mở rộng khi nhấn nút', async () => {
    const user = userEvent.setup();
    render(<BoundaryPastePanel onApplyPoints={vi.fn()} />);

    expect(screen.getByText('Dán danh sách tọa độ')).toBeInTheDocument();
    expect(screen.queryByPlaceholderText(/21.0285/)).not.toBeInTheDocument();

    const expandBtn = screen.getByRole('button', { name: /Mở rộng/i });
    await user.click(expandBtn);

    expect(screen.getByPlaceholderText(/21.0285/)).toBeInTheDocument();
  });

  it('gọi onApplyPoints khi dán tọa độ hợp lệ', async () => {
    const user = userEvent.setup();
    const handleApply = vi.fn();

    render(<BoundaryPastePanel onApplyPoints={handleApply} />);

    await user.click(screen.getByRole('button', { name: /Mở rộng/i }));

    const textarea = screen.getByPlaceholderText(/21.0285/);
    await user.type(textarea, '21.01, 105.01{enter}21.02, 105.02{enter}21.03, 105.03');

    const applyBtn = screen.getByRole('button', { name: /Áp dụng danh sách/i });
    await user.click(applyBtn);

    expect(handleApply).toHaveBeenCalledTimes(1);
    expect(handleApply).toHaveBeenCalledWith([
      { latitude: 21.01, longitude: 105.01 },
      { latitude: 21.02, longitude: 105.02 },
      { latitude: 21.03, longitude: 105.03 },
    ]);
  });

  it('hiển thị thông báo lỗi khi dữ liệu sai định dạng', async () => {
    const user = userEvent.setup();
    const handleApply = vi.fn();

    render(<BoundaryPastePanel onApplyPoints={handleApply} />);

    await user.click(screen.getByRole('button', { name: /Mở rộng/i }));

    const textarea = screen.getByPlaceholderText(/21.0285/);
    await user.type(textarea, 'toa do sai{enter}123');

    const applyBtn = screen.getByRole('button', { name: /Áp dụng danh sách/i });
    await user.click(applyBtn);

    expect(handleApply).not.toHaveBeenCalled();
    expect(screen.getByText('Tọa độ chưa hợp lệ')).toBeInTheDocument();
  });
});
