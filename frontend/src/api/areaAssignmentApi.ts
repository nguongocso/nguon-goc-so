import type {
  AssignAreasRequest,
  AssignAreasResult,
  AssignedArea,
  UserOption,
} from '@/types/areaAssignment';
import {
  mockAssignAreas,
  mockGetAssignableUsers,
  mockGetMyAreas,
  mockGetUserAreas,
  mockUnassignArea,
} from '@/mocks/areaAssignments';

// Chữ ký hàm giữ nguyên cho giai đoạn 3 — khi đó chỉ thay thân hàm bằng
// apiClient + unwrap ApiResult (docs/NCL-742-api-contract.md).

export interface GetAssignableUsersParams {
  role: string;
  keyword?: string;
}

export async function getAssignableUsers(
  params: GetAssignableUsersParams,
): Promise<UserOption[]> {
  // TODO(NCL-742-giai-doan-3): apiClient.get('/admin/users', { params })
  return mockGetAssignableUsers(params);
}

export async function getUserAreas(userId: string): Promise<AssignedArea[]> {
  // TODO(NCL-742-giai-doan-3): apiClient.get(`/admin/users/${userId}/areas`)
  return mockGetUserAreas(userId);
}

export async function assignAreas(
  userId: string,
  request: AssignAreasRequest,
): Promise<AssignAreasResult> {
  // TODO(NCL-742-giai-doan-3): apiClient.post(`/admin/users/${userId}/areas`, request)
  const { assignedCount, assigned } = await mockAssignAreas(userId, request.unitIds);
  return { assignedCount, assigned };
}

export async function unassignArea(
  userId: string,
  unitId: string,
): Promise<{ message: string }> {
  // TODO(NCL-742-giai-doan-3): apiClient.delete(`/admin/users/${userId}/areas/${unitId}`)
  return mockUnassignArea(userId, unitId);
}

export async function getMyAreas(): Promise<AssignedArea[]> {
  // TODO(NCL-742-giai-doan-3): apiClient.get('/me/areas')
  return mockGetMyAreas();
}
