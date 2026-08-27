import type { AssignedArea, UserOption } from '@/types/areaAssignment';
import { MOCK_ADMIN_UNITS } from '@/mocks/administrativeUnits';

// ─── Seed users (VT-05) ──────────────────────────────────

export const MOCK_CURRENT_USER_ID = 'b1000000-0000-0000-0000-00000000000a';

const MOCK_USERS: Array<UserOption & { roleCode: string }> = [
  {
    userId: 'b1000000-0000-0000-0000-00000000000b',
    username: 'tran_thi_b',
    fullName: 'Trần Thị B',
    email: null,
    phone: null,
    organizationName: 'Sở Nông nghiệp và Môi trường',
    roleCode: 'VT-05',
  },
  {
    userId: MOCK_CURRENT_USER_ID,
    username: 'nguyen_van_a',
    fullName: 'Nguyễn Văn A',
    email: 'nguyen.van.a@nguongocso.vn',
    phone: '0901000001',
    organizationName: 'Cục Quản lý Nông nghiệp',
    roleCode: 'VT-05',
  },
  {
    userId: 'b1000000-0000-0000-0000-00000000000c',
    username: 'le_van_c',
    fullName: 'Lê Văn C',
    email: 'le.van.c@nguongocso.vn',
    phone: '0903000003',
    organizationName: 'Chi cục Trồng trọt Phú Thọ',
    roleCode: 'VT-05',
  },
];

// ─── Messages khớp contract NCL-742 ──────────────────────

export const MSG_USER_NOT_FOUND = 'Tài khoản không tồn tại.';
export const MSG_NOT_REGULATOR = 'Tài khoản không có vai trò Cán bộ quản lý ngành.';
export const MSG_UNIT_UNKNOWN = 'Địa bàn không nằm trong danh mục hành chính.';
export const MSG_DUPLICATED = 'Địa bàn đã được gán cho tài khoản này.';
export const MSG_ASSIGNMENT_NOT_FOUND = 'Tài khoản chưa được gán địa bàn này.';

// ─── In-memory store ─────────────────────────────────────
// Map userId → AssignedArea[]; khởi tạo 1 user có sẵn 1 địa bàn, còn lại rỗng.

const HANOI_ID = 'a1000000-0000-0000-0000-000000000001';
const HOAN_KIEM_ID = 'a1000000-0000-0000-0000-000000010001';

type Store = Map<string, AssignedArea[]>;

let store: Store = createStore();

function createStore(): Store {
  return new Map<string, AssignedArea[]>([
    [
      // Nguyễn Văn A (user tự xem /me/areas) đã có sẵn 1 địa bàn.
      MOCK_CURRENT_USER_ID,
      [
        {
          assignmentId: 'c1000000-0000-0000-0000-000000000001',
          unitId: HOAN_KIEM_ID,
          unitCode: '00001',
          unitName: 'Phường Hoàn Kiếm',
          unitLevel: 'COMMUNE',
          provinceId: HANOI_ID,
          provinceName: 'Hà Nội',
          assignedAt: '2026-08-01T08:00:00Z',
        },
      ],
    ],
    [MOCK_USERS[0].userId, []],
    [MOCK_USERS[2].userId, []],
  ]);
}

/** Xoá store về trạng thái seed ban đầu (dùng trong test). */
export function resetMockAreaAssignments(): void {
  store = createStore();
}

// ─── Helpers ─────────────────────────────────────────────

const delay = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

function findUser(userId: string) {
  return MOCK_USERS.find((user) => user.userId === userId);
}

function requireUser(userId: string) {
  const user = findUser(userId);
  if (!user) throw new Error(MSG_USER_NOT_FOUND);
  if (user.roleCode !== 'VT-05') throw new Error(MSG_NOT_REGULATOR);
  return user;
}

let assignmentIdSeq = 100;

function nextAssignmentId(): string {
  assignmentIdSeq += 1;
  return `c1000000-0000-0000-0000-${String(assignmentIdSeq).padStart(12, '0')}`;
}

// ─── Mutations dùng bởi api module ───────────────────────

export async function mockGetAssignableUsers(params: {
  role?: string;
  keyword?: string;
}): Promise<UserOption[]> {
  await delay(150);
  const role = params.role ?? 'VT-05';
  const keyword = (params.keyword ?? '').trim().toLowerCase();
  return MOCK_USERS.filter((user) => user.roleCode === role)
    .filter(
      (user) =>
        !keyword ||
        user.fullName.toLowerCase().includes(keyword) ||
        user.username.toLowerCase().includes(keyword),
    )
    .map(({ roleCode: _roleCode, ...user }) => user);
}

export async function mockGetUserAreas(userId: string): Promise<AssignedArea[]> {
  await delay(150);
  requireUser(userId);
  return [...(store.get(userId) ?? [])];
}

export async function mockGetMyAreas(): Promise<AssignedArea[]> {
  await delay(150);
  return [...(store.get(MOCK_CURRENT_USER_ID) ?? [])];
}

export async function mockAssignAreas(
  userId: string,
  unitIds: string[],
): Promise<{ assignedCount: number; assigned: AssignedArea[] }> {
  await delay(150);
  requireUser(userId);

  if (!unitIds || unitIds.length === 0) {
    throw new Error(MSG_UNIT_UNKNOWN);
  }

  // All-or-nothing: validate toàn bộ trước khi lưu.
  for (const unitId of unitIds) {
    if (!findUnit(unitId)) throw new Error(MSG_UNIT_UNKNOWN);
  }
  const existing = new Set((store.get(userId) ?? []).map((area) => area.unitId));
  for (const unitId of unitIds) {
    if (existing.has(unitId)) throw new Error(MSG_DUPLICATED);
  }

  const current = store.get(userId) ?? [];
  const assigned: AssignedArea[] = unitIds.map((unitId) =>
    toAssignedArea(nextAssignmentId(), findUnit(unitId)!),
  );
  store.set(userId, [...current, ...assigned]);
  return { assignedCount: assigned.length, assigned };
}

export async function mockUnassignArea(
  userId: string,
  unitId: string,
): Promise<{ message: string }> {
  await delay(150);
  requireUser(userId);

  const current = store.get(userId) ?? [];
  const target = current.find((area) => area.unitId === unitId);
  if (!target) throw new Error(MSG_ASSIGNMENT_NOT_FOUND);

  store.set(
    userId,
    current.filter((area) => area.unitId !== unitId),
  );
  return { message: `Đã gỡ địa bàn ${target.unitName} khỏi tài khoản.` };
}

// ─── Internal ────────────────────────────────────────────

function findUnit(unitId: string) {
  for (const province of MOCK_ADMIN_UNITS) {
    if (province.id === unitId) return province;
    const commune = province.children.find((child) => child.id === unitId);
    if (commune) return commune;
  }
  return undefined;
}

function toAssignedArea(assignmentId: string, unit: NonNullable<ReturnType<typeof findUnit>>): AssignedArea {
  if (unit.level === 'PROVINCE') {
    return {
      assignmentId,
      unitId: unit.id,
      unitCode: unit.code,
      unitName: unit.name,
      unitLevel: 'PROVINCE',
      provinceId: unit.id,
      provinceName: unit.name,
      assignedAt: new Date().toISOString(),
    };
  }
  const province = MOCK_ADMIN_UNITS.find((item) =>
    item.children.some((child) => child.id === unit.id),
  );
  return {
    assignmentId,
    unitId: unit.id,
    unitCode: unit.code,
    unitName: unit.name,
    unitLevel: 'COMMUNE',
    provinceId: province?.id ?? unit.id,
    provinceName: province?.name ?? '',
    assignedAt: new Date().toISOString(),
  };
}
