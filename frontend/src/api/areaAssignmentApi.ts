import apiClient from '@/api/axiosConfig';
import { toApiError } from '@/api/apiError';
import type { ApiResult } from '@/types/member';
import type { PageResponse } from '@/types/common';
import type {
  AssignAreasRequest,
  AssignAreasResult,
  AssignedArea,
  UserOption,
} from '@/types/areaAssignment';

export interface GetAssignableUsersParams {
  role?: string;
  keyword?: string;
  page?: number;
  size?: number;
}

/** Lấy danh sách người dùng có thể gán địa bàn. */
export async function getAssignableUsers(
  params: GetAssignableUsersParams = {},
): Promise<UserOption[]> {
  try {
    const response = await apiClient.get<ApiResult<PageResponse<UserOption>>>(
      '/admin/users',
      {
        params: {
          role: params.role ?? 'VT-05',
          keyword: params.keyword ?? '',
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      },
    );
    return response.data.data?.items ?? [];
  } catch (err) {
    throw toApiError(err);
  }
}

/** Lấy danh sách địa bàn được gán cho người dùng. */
export async function getUserAreas(userId: string): Promise<AssignedArea[]> {
  try {
    const response = await apiClient.get<ApiResult<AssignedArea[]>>(
      `/admin/users/${userId}/areas`,
    );
    return response.data.data ?? [];
  } catch (err) {
    throw toApiError(err);
  }
}

/** Gán danh sách địa bàn cho người dùng. */
export async function assignAreas(
  userId: string,
  request: AssignAreasRequest,
): Promise<AssignAreasResult> {
  try {
    const response = await apiClient.post<ApiResult<AssignAreasResult>>(
      `/admin/users/${userId}/areas`,
      request,
    );
    return response.data.data;
  } catch (err) {
    throw toApiError(err);
  }
}

/** Gỡ gán địa bàn của người dùng. */
export async function unassignArea(
  userId: string,
  unitId: string,
): Promise<{ message: string }> {
  try {
    const response = await apiClient.delete<ApiResult<{ message: string }>>(
      `/admin/users/${userId}/areas/${unitId}`,
    );
    return response.data.data;
  } catch (err) {
    throw toApiError(err);
  }
}

/** Cán bộ tự xem danh sách địa bàn của mình. */
export async function getMyAreas(): Promise<AssignedArea[]> {
  try {
    const response = await apiClient.get<ApiResult<AssignedArea[]>>('/me/areas');
    return response.data.data ?? [];
  } catch (err) {
    throw toApiError(err);
  }
}
