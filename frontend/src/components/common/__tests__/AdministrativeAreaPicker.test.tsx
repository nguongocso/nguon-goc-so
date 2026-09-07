import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdministrativeAreaPicker } from '@/components/common/AdministrativeAreaPicker';
import { MOCK_ADMIN_UNITS } from '@/mocks/administrativeUnits';

function PickerHarness({ onChangeSpy }: { onChangeSpy?: (ids: string[]) => void }) {
  const [value, setValue] = useState<string[]>([]);
  return (
    <AdministrativeAreaPicker
      units={MOCK_ADMIN_UNITS}
      value={value}
      onChange={(ids) => {
        setValue(ids);
        onChangeSpy?.(ids);
      }}
    />
  );
}

describe('AdministrativeAreaPicker', () => {
  it('chọn tỉnh từ dropdown → hiển thị danh sách xã của tỉnh đó', async () => {
    const user = userEvent.setup();
    render(<PickerHarness />);

    // Mở dropdown chọn tỉnh
    const trigger = screen.getByTestId('province-select-trigger');
    await user.click(trigger);

    // Chọn Phú Thọ
    const option = await screen.findByTestId('province-option-Phú Thọ');
    await user.click(option);

    // Danh sách xã của Phú Thọ hiển thị
    expect(await screen.findByText(/Chọn Xã \/ Phường thuộc/)).toBeInTheDocument();
    expect(screen.getAllByText('Phú Thọ').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Xã Thanh Sơn')).toBeInTheDocument();
    expect(screen.getByText('Phường Việt Trì')).toBeInTheDocument();
  });

  it('tiện ích Chọn tất cả và Bỏ chọn trong tỉnh', async () => {
    const user = userEvent.setup();
    render(<PickerHarness />);

    // Mở chọn Phú Thọ
    await user.click(screen.getByTestId('province-select-trigger'));
    await user.click(await screen.findByTestId('province-option-Phú Thọ'));

    // Bấm Chọn tất cả
    const selectAllBtn = screen.getByRole('button', { name: 'Chọn tất cả' });
    await user.click(selectAllBtn);

    // Nút Thêm vào danh sách hiển thị số lượng
    expect(screen.getByRole('button', { name: /Thêm vào danh sách \(\d+\)/ })).toBeInTheDocument();

    // Bấm Bỏ chọn
    const deselectBtn = screen.getByRole('button', { name: 'Bỏ chọn' });
    await user.click(deselectBtn);

    // Nút Thêm vào danh sách disabled (0)
    expect(screen.getByRole('button', { name: /Thêm vào danh sách \(0\)/ })).toBeDisabled();
  });

  it('thêm xã vào danh sách đã chọn → hiển thị phân nhóm theo tỉnh', async () => {
    const onChangeSpy = vi.fn();
    const user = userEvent.setup();
    render(<PickerHarness onChangeSpy={onChangeSpy} />);

    // Chọn Phú Thọ -> tick Xã Thanh Sơn -> Thêm
    await user.click(screen.getByTestId('province-select-trigger'));
    await user.click(await screen.findByTestId('province-option-Phú Thọ'));
    await user.click(screen.getByRole('checkbox', { name: 'Xã Thanh Sơn' }));
    await user.click(screen.getByRole('button', { name: /Thêm vào danh sách/ }));

    // Khu vực danh sách đã chọn xuất hiện nhóm Phú Thọ
    const pendingList = await screen.findByTestId('pending-area-list');
    expect(within(pendingList).getByText('Phú Thọ')).toBeInTheDocument();
    expect(within(pendingList).getByText('Xã Thanh Sơn')).toBeInTheDocument();

    // Chọn tiếp tỉnh Hà Nội -> tick Phường Hoàn Kiếm -> Thêm
    await user.click(screen.getByTestId('province-select-trigger'));
    await user.click(await screen.findByTestId('province-option-Hà Nội'));
    await user.click(screen.getByRole('checkbox', { name: 'Phường Hoàn Kiếm' }));
    await user.click(screen.getByRole('button', { name: /Thêm vào danh sách/ }));

    // Khu vực danh sách đã chọn có cả 2 nhóm Phú Thọ và Hà Nội
    expect(within(pendingList).getByText('Hà Nội')).toBeInTheDocument();
    expect(within(pendingList).getByText('Phường Hoàn Kiếm')).toBeInTheDocument();

    // Huy hiệu tổng số đã chọn
    expect(screen.getByText('Đã chọn: 2 xã/phường')).toBeInTheDocument();
  });

  it('xóa 1 xã khỏi danh sách đã chọn bằng nút ✕', async () => {
    const user = userEvent.setup();
    render(<PickerHarness />);

    // Thêm Xã Thanh Sơn
    await user.click(screen.getByTestId('province-select-trigger'));
    await user.click(await screen.findByTestId('province-option-Phú Thọ'));
    await user.click(screen.getByRole('checkbox', { name: 'Xã Thanh Sơn' }));
    await user.click(screen.getByRole('button', { name: /Thêm vào danh sách/ }));

    // Xóa Xã Thanh Sơn
    const removeBtn = screen.getByRole('button', { name: 'Xóa Xã Thanh Sơn' });
    await user.click(removeBtn);

    // Danh sách trở về rỗng
    expect(screen.getByText(/Chưa có địa bàn nào được chọn/)).toBeInTheDocument();
  });

  it('nút Xóa tất cả reset toàn bộ danh sách địa bàn đã chọn', async () => {
    const user = userEvent.setup();
    render(<PickerHarness />);

    // Thêm 2 xã
    await user.click(screen.getByTestId('province-select-trigger'));
    await user.click(await screen.findByTestId('province-option-Phú Thọ'));
    await user.click(screen.getByRole('button', { name: 'Chọn tất cả' }));
    await user.click(screen.getByRole('button', { name: /Thêm vào danh sách/ }));

    expect(screen.getByTestId('pending-area-list')).toBeInTheDocument();

    // Bấm Xóa tất cả
    const clearAllBtn = screen.getByRole('button', { name: /Xóa tất cả/ });
    await user.click(clearAllBtn);

    // Danh sách trống
    expect(screen.getByText(/Chưa có địa bàn nào được chọn/)).toBeInTheDocument();
  });
});
