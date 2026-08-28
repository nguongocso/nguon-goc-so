import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import { DeactivateMemberDialog } from '@/components/organization/DeactivateMemberDialog';
import type { OrganizationMember } from '@/types/member';

const makeMember = (): OrganizationMember => ({
  id: 'ou-1',
  organizationId: 'org-1',
  userId: 'user-1',
  username: 'nguoighi01',
  fullName: 'Trần Người Ghi',
  roleId: 3,
  roleCode: 'VT-03',
  roleName: 'Người ghi sự kiện',
  status: 'ACTIVE',
  membershipStatus: 'ACTIVE',
  joinedAt: '2026-01-10T08:00:00',
});

describe('DeactivateMemberDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('hiển thị dialog khi có member được chọn', () => {
    render(
      <DeactivateMemberDialog
        member={makeMember()}
        deactivating={false}
        onClose={vi.fn()}
        onConfirm={vi.fn().mockResolvedValue({ ok: true })}
      />,
    );

    expect(screen.getByText('Vô hiệu hóa thành viên')).toBeInTheDocument();
    expect(screen.getByLabelText(/Lý do vô hiệu hóa/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tiếp tục' })).toBeInTheDocument();
  });

  it('báo lỗi khi lý do để trống và KHÔNG mở modal cảnh báo', async () => {
    const user = userEvent.setup();
    render(
      <DeactivateMemberDialog
        member={makeMember()}
        deactivating={false}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Tiếp tục' }));

    expect(
      screen.getByText('Lý do vô hiệu hóa không được để trống.'),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(/thay thế người để công việc không bị ảnh hưởng/),
    ).not.toBeInTheDocument();
  });

  it('nhập lý do → bấm Tiếp tục → hiện modal cảnh báo → Xác nhận gọi onConfirm', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn().mockResolvedValue({ ok: true });
    const onClose = vi.fn();

    render(
      <DeactivateMemberDialog
        member={makeMember()}
        deactivating={false}
        onClose={onClose}
        onConfirm={onConfirm}
      />,
    );

    await user.type(
      screen.getByLabelText(/Lý do vô hiệu hóa/),
      'Thành viên nghỉ việc',
    );
    await user.click(screen.getByRole('button', { name: 'Tiếp tục' }));

    // Modal cảnh báo rủi ro xuất hiện.
    expect(
      await screen.findByText(/thay thế người để công việc không bị ảnh hưởng/),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Xác nhận' }));

    await waitFor(() => {
      expect(onConfirm).toHaveBeenCalledWith('user-1', 'Thành viên nghỉ việc');
    });
    await waitFor(() => {
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('nút Hủy bỏ ở modal cảnh báo đóng modal nhưng giữ dialog', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const onClose = vi.fn();

    render(
      <DeactivateMemberDialog
        member={makeMember()}
        deactivating={false}
        onClose={onClose}
        onConfirm={onConfirm}
      />,
    );

    await user.type(
      screen.getByLabelText(/Lý do vô hiệu hóa/),
      'lý do bất kỳ',
    );
    await user.click(screen.getByRole('button', { name: 'Tiếp tục' }));
    await user.click(await screen.findByRole('button', { name: 'Hủy bỏ' }));

    expect(
      screen.queryByText(/thay thế người để công việc không bị ảnh hưởng/),
    ).not.toBeInTheDocument();
    // Dialog chính vẫn mở, chưa gọi API.
    expect(screen.getByText('Vô hiệu hóa thành viên')).toBeInTheDocument();
    expect(onConfirm).not.toHaveBeenCalled();
    expect(onClose).not.toHaveBeenCalled();
  });
});
