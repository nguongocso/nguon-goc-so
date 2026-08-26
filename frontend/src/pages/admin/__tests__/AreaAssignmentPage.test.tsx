import { describe, expect, it, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Toaster, toast } from 'sonner';
import { AreaAssignmentPage } from '@/pages/admin/AreaAssignmentPage';
import { MOCK_ADMIN_UNITS } from '@/mocks/administrativeUnits';
import type { AssignedArea, UserOption } from '@/types/areaAssignment';

// ─── Mock tầng API thật (NCL-670 giai đoạn tích hợp) ──────
// Test không còn phụ thuộc mock store; mọi kịch bản điều khiển qua
// fixture trả về từ vi.mock('@/api/areaAssignmentApi').

const adminUnitApi = vi.hoisted(() => ({
  getAdministrativeUnitTree: vi.fn(),
}));

const areaApi = vi.hoisted(() => ({
  getAssignableUsers: vi.fn(),
  getUserAreas: vi.fn(),
  assignAreas: vi.fn(),
  unassignArea: vi.fn(),
  getMyAreas: vi.fn(),
}));

vi.mock('@/api/administrativeUnitApi', () => adminUnitApi);
vi.mock('@/api/areaAssignmentApi', () => areaApi);

// ─── Fixtures theo contract NCL-742 ──────────────────────

const USERS: UserOption[] = [
  {
    userId: 'b1000000-0000-0000-0000-00000000000b',
    username: 'tran_thi_b',
    fullName: 'Trần Thị B',
    email: null,
    phone: null,
    organizationName: 'Sở Nông nghiệp và Môi trường',
  },
  {
    userId: 'b1000000-0000-0000-0000-00000000000a',
    username: 'nguyen_van_a',
    fullName: 'Nguyễn Văn A',
    email: 'nguyen.van.a@nguongocso.vn',
    phone: '0901000001',
    organizationName: 'Cục Quản lý Nông nghiệp',
  },
  {
    userId: 'b1000000-0000-0000-0000-00000000000c',
    username: 'le_van_c',
    fullName: 'Lê Văn C',
    email: 'le.van.c@nguongocso.vn',
    phone: '0903000003',
    organizationName: 'Chi cục Trồng trọt Phú Thọ',
  },
];

const USER_B = USERS[0];
const USER_A = USERS[1];
const USER_C = USERS[2];

const HOAN_KIEM_ID = 'a1000000-0000-0000-0000-000000010001';
const THANH_SON_ID = 'a2000000-0000-0000-0000-000000020002';

function makeAssignedArea(
  assignmentId: string,
  unitId: string,
  unitCode: string,
  unitName: string,
  provinceId: string,
  provinceName: string,
): AssignedArea {
  return {
    assignmentId,
    unitId,
    unitCode,
    unitName,
    unitLevel: 'COMMUNE',
    provinceId,
    provinceName,
    assignedAt: '2026-08-01T08:00:00Z',
  };
}

const HOAN_KIEM_AREA = makeAssignedArea(
  'c1000000-0000-0000-0000-000000000001',
  HOAN_KIEM_ID,
  '00001',
  'Phường Hoàn Kiếm',
  'a1000000-0000-0000-0000-000000000001',
  'Hà Nội',
);

/** Store gán địa bàn trong test — thay cho mock store cũ. */
let assignedStore: Map<string, AssignedArea[]>;

function seedStore() {
  assignedStore = new Map<string, AssignedArea[]>([
    [USER_A.userId, [HOAN_KIEM_AREA]],
    [USER_B.userId, []],
    [USER_C.userId, []],
  ]);
}

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
  vi.clearAllMocks();
  seedStore();

  adminUnitApi.getAdministrativeUnitTree.mockResolvedValue(MOCK_ADMIN_UNITS);
  areaApi.getAssignableUsers.mockResolvedValue(USERS);
  areaApi.getUserAreas.mockImplementation(async (userId: string) => [
    ...(assignedStore.get(userId) ?? []),
  ]);
  areaApi.assignAreas.mockImplementation(
    async (userId: string, request: { unitIds: string[] }) => {
      // Mô phỏng validate V5 của contract: trùng địa bàn → lỗi tiếng Việt.
      const existing = assignedStore.get(userId) ?? [];
      const duplicated = request.unitIds.some((unitId) =>
        existing.some((area) => area.unitId === unitId),
      );
      if (duplicated) {
        throw new Error('Địa bàn đã được gán cho tài khoản này.');
      }
      const created = request.unitIds.map((unitId) =>
        unitId === HOAN_KIEM_ID
          ? HOAN_KIEM_AREA
          : makeAssignedArea(
              `c1000000-0000-0000-0000-${unitId.slice(-12)}`,
              unitId,
              unitId === THANH_SON_ID ? '00205' : '00000',
              unitId === THANH_SON_ID ? 'Xã Thanh Sơn' : 'Địa bàn',
              'a2000000-0000-0000-0000-000000000002',
              'Phú Thọ',
            ),
      );
      assignedStore.set(userId, [...existing, ...created]);
      return { assignedCount: created.length, assigned: created };
    },
  );
  areaApi.unassignArea.mockImplementation(async (userId: string, unitId: string) => {
    const current = assignedStore.get(userId) ?? [];
    const target = current.find((area) => area.unitId === unitId);
    if (!target) throw new Error('Tài khoản chưa được gán địa bàn này.');
    assignedStore.set(
      userId,
      current.filter((area) => area.unitId !== unitId),
    );
    return { message: `Đã gỡ địa bàn ${target.unitName} khỏi tài khoản.` };
  });
});

describe('AreaAssignmentPage', () => {
  it('TC-A: hiển thị empty-state đúng chuỗi khi cán bộ đầu tiên chưa được gán địa bàn', async () => {
    const user = userEvent.setup();
    renderPage();

    // Cán bộ đầu tiên trong danh sách (Trần Thị B) chưa có địa bàn.
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
    // Đã gọi API thật với payload đúng contract.
    expect(areaApi.assignAreas).toHaveBeenCalledWith(USER_B.userId, {
      unitIds: [THANH_SON_ID],
    });
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
    expect(areaApi.unassignArea).toHaveBeenCalledWith(
      USER_A.userId,
      HOAN_KIEM_ID,
    );
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

  it('TC-E2: unassign cập nhật store → gán lại cùng unitId thành công', async () => {
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

  it('TC-E3: assignAreas reject message contract → toast.error hiển thị đúng chuỗi', async () => {
    const errorSpy = vi.spyOn(toast, 'error').mockImplementation(() => '');
    areaApi.assignAreas.mockRejectedValue(
      new Error('Địa bàn đã được gán cho tài khoản này.'),
    );

    try {
      const user = userEvent.setup();
      renderPage();

      await selectUser(user, 'Nguyễn Văn A');
      await screen.findByTestId('assigned-area-list');

      await user.click(await screen.findByRole('button', { name: 'Mở Hà Nội' }));
      await user.click(
        await screen.findByRole('checkbox', { name: 'Phường Hoàn Kiếm' }),
      );
      await user.click(screen.getByRole('button', { name: /Gán địa bàn/ }));

      await waitFor(() => {
        expect(errorSpy).toHaveBeenCalledWith(
          'Địa bàn đã được gán cho tài khoản này.',
        );
      });
    } finally {
      errorSpy.mockRestore();
    }
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

  it('lỗi tải danh sách cán bộ → toast message từ tầng API', async () => {
    const errorSpy = vi.spyOn(toast, 'error').mockImplementation(() => '');
    areaApi.getAssignableUsers.mockRejectedValue(
      new Error('Bạn không có quyền thực hiện thao tác này.'),
    );

    try {
      renderPage();
      await waitFor(() => {
        expect(errorSpy).toHaveBeenCalledWith(
          'Bạn không có quyền thực hiện thao tác này.',
        );
      });
    } finally {
      errorSpy.mockRestore();
    }
  });
});
