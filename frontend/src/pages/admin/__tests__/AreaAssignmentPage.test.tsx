import { describe, expect, it, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Toaster } from 'sonner';
import { AreaAssignmentPage } from '@/pages/admin/AreaAssignmentPage';
import { resetMockAreaAssignments } from '@/mocks/areaAssignments';

function renderPage() {
  return render(
    <>
      <Toaster position="top-center" />
      <AreaAssignmentPage />
    </>,
  );
}

async function selectUser(
  user: ReturnType<typeof userEvent.setup>,
  fullName: string,
) {
  await user.click(await screen.findByText(fullName));
}

beforeEach(() => {
  resetMockAreaAssignments();
});

describe('AreaAssignmentPage', () => {
  it('TC-A: hiển thị empty-state đúng chuỗi khi cán bộ đầu tiên chưa được gán địa bàn', async () => {
    const user = userEvent.setup();
    renderPage();

    // Cán bộ đầu tiên trong mock (Trần Thị B) chưa có địa bàn.
    await selectUser(user, 'Trần Thị B');

    const emptyState = await screen.findByTestId('empty-assigned-areas');
    expect(
      within(emptyState).getByText('Chưa được phân công địa bàn nào.'),
    ).toBeInTheDocument();
    expect(
      within(emptyState).getByText(
        'Chọn địa bàn ở khung bên phải rồi bấm Gán địa bàn.',
      ),
    ).toBeInTheDocument();
  });

  it('TC-B: chọn cán bộ đã gán sẵn → hiển thị chip địa bàn đúng tên', async () => {
    const user = userEvent.setup();
    renderPage();

    await selectUser(user, 'Nguyễn Văn A');

    const list = await screen.findByTestId('assigned-area-list');
    expect(within(list).getByText(/Phường Hoàn Kiếm/)).toBeInTheDocument();
    expect(within(list).getByText(/Hà Nội/)).toBeInTheDocument();
  });

  it('TC-C: tick 1 xã rồi bấm Gán địa bàn → chip mới xuất hiện', async () => {
    const user = userEvent.setup();
    renderPage();

    await selectUser(user, 'Trần Thị B');
    await user.click(await screen.findByRole('button', { name: 'Mở Phú Thọ' }));
    await user.click(await screen.findByRole('checkbox', { name: 'Xã Thanh Sơn' }));

    await user.click(screen.getByRole('button', { name: /Gán địa bàn/ }));

    const list = await screen.findByTestId('assigned-area-list');
    await waitFor(() => {
      expect(within(list).getByText(/Xã Thanh Sơn/)).toBeInTheDocument();
    });
    // Lựa chọn tạm ở khung phải được xoá sau khi gán thành công.
    expect(within(list).getByText(/Phú Thọ/)).toBeInTheDocument();
  });

  it('TC-D: bấm X trên chip → chip biến mất (unassign)', async () => {
    const user = userEvent.setup();
    renderPage();

    await selectUser(user, 'Nguyễn Văn A');
    const list = await screen.findByTestId('assigned-area-list');

    await user.click(
      within(list).getByRole('button', { name: 'Gỡ địa bàn Phường Hoàn Kiếm' }),
    );

    await screen.findByTestId('empty-assigned-areas');
    expect(screen.queryByTestId('assigned-area-list')).not.toBeInTheDocument();
  });

  it('TC-E1: gán trùng địa bàn đã có → toast lỗi đúng chuỗi contract', async () => {
    const user = userEvent.setup();
    renderPage();

    await selectUser(user, 'Nguyễn Văn A');
    await screen.findByTestId('assigned-area-list');

    await user.click(await screen.findByRole('button', { name: 'Mở Hà Nội' }));
    await user.click(
      await screen.findByRole('checkbox', { name: 'Phường Hoàn Kiếm' }),
    );
    await user.click(screen.getByRole('button', { name: /Gán địa bàn/ }));

    expect(
      await screen.findByText('Địa bàn đã được gán cho tài khoản này.'),
    ).toBeInTheDocument();
  });

  it('TC-E2: unassign qua store cập nhật thật (gán lại không còn bị chặn trùng)', async () => {
    const user = userEvent.setup();
    renderPage();

    await selectUser(user, 'Lê Văn C');
    await user.click(await screen.findByRole('button', { name: 'Mở Phú Thọ' }));
    await user.click(await screen.findByRole('checkbox', { name: 'Xã Thanh Sơn' }));
    await user.click(screen.getByRole('button', { name: /Gán địa bàn/ }));

    const list = await screen.findByTestId('assigned-area-list');
    await waitFor(() => {
      expect(within(list).getByText(/Xã Thanh Sơn/)).toBeInTheDocument();
    });

    // Gỡ vừa gán → empty-state trở lại.
    await user.click(
      within(list).getByRole('button', { name: 'Gỡ địa bàn Xã Thanh Sơn' }),
    );
    await screen.findByTestId('empty-assigned-areas');

    // Gán lại cùng unitId vẫn thành công vì store đã cập nhật.
    await user.click(screen.getByRole('checkbox', { name: `Xã Thanh Sơn` }));
    await user.click(screen.getByRole('button', { name: /Gán địa bàn/ }));
    const listAgain = await screen.findByTestId('assigned-area-list');
    expect(within(listAgain).getByText(/Xã Thanh Sơn/)).toBeInTheDocument();
  });

  it('lọc danh sách cán bộ theo từ khoá', async () => {
    renderPage();
    const input = await screen.findByRole('textbox', { name: 'Tìm kiếm cán bộ' });
    const user = userEvent.setup();
    await user.type(input, 'trần thị');

    expect(await screen.findByText('Trần Thị B')).toBeInTheDocument();
    expect(screen.queryByText('Nguyễn Văn A')).not.toBeInTheDocument();
  });

  it('nút Gán địa bàn disabled khi chưa chọn user hoặc chưa chọn địa bàn', async () => {
    renderPage();
    const assignButton = await screen.findByRole('button', {
      name: /Gán địa bàn/,
    });
    expect(assignButton).toBeDisabled();

    const user = userEvent.setup();
    await selectUser(user, 'Trần Thị B');
    await waitFor(() => {
      expect(assignButton).toBeDisabled(); // đã chọn user nhưng chưa chọn xã
    });
  });
});
