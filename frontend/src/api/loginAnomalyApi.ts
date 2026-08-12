import apiClient from './axiosConfig';
import type { PageResponse } from '@/types/common';
import type {
  LoginAnomaly,
  LoginAnomalyParams,
  LockLoginAnomalyResponse,
} from '@/types/loginAnomaly';
import { getUser } from '@/utils/storage';

/**
 * ============================================================
 * BACKEND CHƯA CÓ API.
 *
 * Bật USE_MOCK = true để chạy với dữ liệu giả lập.
 * Khi backend hoàn thiện: đổi USE_MOCK = false
 * và chỉnh lại endpoint/type nếu cần.
 * ============================================================
 */
const USE_MOCK = true;

const delay = (milliseconds: number) =>
  new Promise((resolve) => setTimeout(resolve, milliseconds));

// ─── Mock data ─────────────────────────────────────────────

interface MockUserSeed {
  username: string;
  fullName: string;
  organizationId: string;
  organizationName: string;
}

const MOCK_USERS: MockUserSeed[] = [
  { username: 'nguyenvanan', fullName: 'Nguyễn Văn An', organizationId: 'org-a', organizationName: 'Hợp tác xã Nông sản Xanh' },
  { username: 'tranthibinh', fullName: 'Trần Thị Bình', organizationId: 'org-a', organizationName: 'Hợp tác xã Nông sản Xanh' },
  { username: 'levanhuy', fullName: 'Lê Văn Huy', organizationId: 'org-a', organizationName: 'Hợp tác xã Nông sản Xanh' },
  { username: 'phamthuha', fullName: 'Phạm Thu Hà', organizationId: 'org-a', organizationName: 'Hợp tác xã Nông sản Xanh' },
  { username: 'hoangvanminh', fullName: 'Hoàng Văn Minh', organizationId: 'org-b', organizationName: 'HTX Nông nghiệp sạch Sông Hồng' },
  { username: 'nguyenthilan', fullName: 'Nguyễn Thị Lan', organizationId: 'org-b', organizationName: 'HTX Nông nghiệp sạch Sông Hồng' },
];

interface MockRecordSeed {
  user: MockUserSeed;
  ipAddress: string;
  location: string;
  reason: string;
  severity: LoginAnomaly['severity'];
  minutesAgo: number;
  locked?: boolean;
}

const MOCK_RECORDS: MockRecordSeed[] = [
  { user: MOCK_USERS[0], ipAddress: '103.75.200.11', location: 'Hà Nội, Việt Nam', reason: 'Đăng nhập sai 5 lần liên tiếp trong 2 phút', severity: 'HIGH', minutesAgo: 8 },
  { user: MOCK_USERS[0], ipAddress: '103.75.200.12', location: 'Hà Nội, Việt Nam', reason: 'Đăng nhập vào khung giờ bất thường (02:17)', severity: 'LOW', minutesAgo: 45 },
  { user: MOCK_USERS[1], ipAddress: '45.119.80.233', location: 'TP. Hồ Chí Minh, Việt Nam', reason: 'Đăng nhập từ vị trí khác với thường lệ', severity: 'MEDIUM', minutesAgo: 25 },
  { user: MOCK_USERS[2], ipAddress: '185.220.101.42', location: 'Frankfurt, Đức', reason: 'Đăng nhập sai 5 lần liên tiếp trong 2 phút', severity: 'HIGH', minutesAgo: 70 },
  { user: MOCK_USERS[3], ipAddress: '171.224.188.5', location: 'Hà Nội, Việt Nam', reason: 'IP thay đổi thường xuyên trong thời gian ngắn', severity: 'MEDIUM', minutesAgo: 130 },
  { user: MOCK_USERS[4], ipAddress: '14.161.47.99', location: 'TP. Hồ Chí Minh, Việt Nam', reason: 'Đăng nhập vào khung giờ bất thường (03:40)', severity: 'LOW', minutesAgo: 300, locked: true },
  { user: MOCK_USERS[5], ipAddress: '1.52.240.171', location: 'Hà Nội, Việt Nam', reason: 'Đăng nhập sai 5 lần liên tiếp trong 2 phút', severity: 'HIGH', minutesAgo: 360, locked: true },
  { user: MOCK_USERS[5], ipAddress: '1.52.240.172', location: 'Hà Nội, Việt Nam', reason: 'Đăng nhập từ vị trí khác với thường lệ', severity: 'MEDIUM', minutesAgo: 375, locked: true },
  { user: MOCK_USERS[1], ipAddress: '42.112.70.8', location: 'Hải Phòng, Việt Nam', reason: 'Đăng nhập từ vị trí khác với thường lệ', severity: 'MEDIUM', minutesAgo: 1440 },
  { user: MOCK_USERS[2], ipAddress: '113.161.65.2', location: 'Hà Nội, Việt Nam', reason: 'Đăng nhập sai 5 lần liên tiếp trong 2 phút', severity: 'HIGH', minutesAgo: 2900 },
  { user: MOCK_USERS[3], ipAddress: '14.225.220.18', location: 'Hà Nội, Việt Nam', reason: 'Đăng nhập vào khung giờ bất thường (01:12)', severity: 'LOW', minutesAgo: 4300 },
  { user: MOCK_USERS[4], ipAddress: '113.185.10.44', location: 'Hà Nội, Việt Nam', reason: 'IP thay đổi thường xuyên trong thời gian ngắn', severity: 'MEDIUM', minutesAgo: 5700 },
];

let mockStore: LoginAnomaly[] = MOCK_RECORDS.map((record, index) => ({
  id: `mock-anomaly-${index + 1}`,
  username: record.user.username,
  fullName: record.user.fullName,
  organizationId: record.user.organizationId,
  organizationName: record.user.organizationName,
  ipAddress: record.ipAddress,
  location: record.location,
  reason: record.reason,
  severity: record.severity,
  loginAt: new Date(Date.now() - record.minutesAgo * 60 * 1000).toISOString(),
  accountStatus: record.locked ? 'LOCKED' : 'ACTIVE',
}));

/** Giả lập phân quyền: VT-02 chỉ thấy dữ liệu của tổ chức mình (TC-03). */
function scopeByCurrentUser(): LoginAnomaly[] {
  const user = getUser();
  if (!user || user.roleCode !== 'VT-02') {
    return mockStore;
  }
  return mockStore.filter(
    (anomaly) => anomaly.organizationId === user.organizationId,
  );
}

// ─── Mock implementations ──────────────────────────────────

async function mockGetLoginAnomalies(
  params: LoginAnomalyParams,
): Promise<PageResponse<LoginAnomaly>> {
  await delay(600);

  const page = params.page ?? 0;
  const size = params.size ?? 20;

  let items = scopeByCurrentUser();

  if (params.severity && params.severity !== 'ALL') {
    items = items.filter((item) => item.severity === params.severity);
  }
  if (params.accountStatus && params.accountStatus !== 'ALL') {
    items = items.filter((item) => item.accountStatus === params.accountStatus);
  }
  if (params.keyword) {
    const keyword = params.keyword.trim().toLowerCase();
    items = items.filter(
      (item) =>
        item.username.toLowerCase().includes(keyword) ||
        item.fullName.toLowerCase().includes(keyword) ||
        item.ipAddress.includes(keyword),
    );
  }
  if (params.fromDate) {
    items = items.filter((item) => item.loginAt.slice(0, 10) >= params.fromDate!);
  }
  if (params.toDate) {
    items = items.filter((item) => item.loginAt.slice(0, 10) <= params.toDate!);
  }

  items = [...items].sort((a, b) => b.loginAt.localeCompare(a.loginAt));

  const totalElements = items.length;
  const totalPages = Math.ceil(totalElements / size);
  const start = page * size;

  return {
    items: items.slice(start, start + size),
    page,
    size,
    totalElements,
    totalPages,
    first: page === 0,
    last: page >= totalPages - 1,
  };
}

async function mockLockLoginAnomalyAccount(
  anomalyId: string,
): Promise<LockLoginAnomalyResponse> {
  await delay(900);

  const anomaly = mockStore.find((item) => item.id === anomalyId);
  if (!anomaly) {
    throw Object.assign(new Error('Bản ghi không tồn tại'), {
      response: { data: { message: 'Bản ghi không tồn tại' } },
    });
  }
  if (anomaly.accountStatus === 'LOCKED') {
    throw Object.assign(new Error('Tài khoản đã bị khóa'), {
      response: { data: { message: 'Tài khoản đã bị khóa' } },
    });
  }

  const actor = getUser();
  const lockedAt = new Date().toISOString();

  mockStore = mockStore.map((item) =>
    item.username === anomaly.username
      ? { ...item, accountStatus: 'LOCKED' }
      : item,
  );

  return {
    id: anomaly.id,
    username: anomaly.username,
    accountStatus: 'LOCKED',
    lockedAt,
    lockedBy: actor?.fullName ?? actor?.username ?? 'Administrator',
  };
}

// ─── Real implementations (chờ backend) ────────────────────

async function realGetLoginAnomalies(
  params: LoginAnomalyParams,
): Promise<PageResponse<LoginAnomaly>> {
  const response = await apiClient.get<{
    data: PageResponse<LoginAnomaly>;
  }>('/login-anomalies', {
    params: {
      severity: params.severity === 'ALL' ? undefined : params.severity,
      accountStatus:
        params.accountStatus === 'ALL' ? undefined : params.accountStatus,
      keyword: params.keyword || undefined,
      fromDate: params.fromDate || undefined,
      toDate: params.toDate || undefined,
      page: params.page,
      size: params.size,
      sort: 'loginAt,desc',
    },
  });
  return response.data.data;
}

async function realLockLoginAnomalyAccount(
  anomalyId: string,
): Promise<LockLoginAnomalyResponse> {
  const response = await apiClient.post<{
    data: LockLoginAnomalyResponse;
  }>(`/login-anomalies/${anomalyId}/lock`);
  return response.data.data;
}

// ─── Public API ────────────────────────────────────────────

export const getLoginAnomalies = (params: LoginAnomalyParams) =>
  USE_MOCK
    ? mockGetLoginAnomalies(params)
    : realGetLoginAnomalies(params);

export const lockLoginAnomalyAccount = (anomalyId: string) =>
  USE_MOCK
    ? mockLockLoginAnomalyAccount(anomalyId)
    : realLockLoginAnomalyAccount(anomalyId);
