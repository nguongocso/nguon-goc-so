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

/**
 * GET /admin/users?role=&keyword=&page=&size= — contract NCL-742 §2.
 *
 * Mapping PageResponse → UI: backend trả `ApiResult<PageResponse<UserOption>>`
 * với PageResponse `{ items, page, size, totalElements, totalPages, first, last }`
 * (khớp `src/types/common.ts`). Chữ ký hàm cũ trả thẳng `UserOption[]` nên ở đây
 * chỉ lấy `items`; tổng số bản ghi nằm ở `totalElements` nếu cần phân trang sau này.
 */
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

/** GET /admin/users/{userId}/areas — contract NCL-742 §3. */
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

/** POST /admin/users/{userId}/areas (batch all-or-nothing) — contract NCL-742 §4. */
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

/** DELETE /admin/users/{userId}/areas/{unitId} — contract NCL-742 §5. */
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

/** GET /me/areas — cán bộ VT-05 tự xem địa bàn của mình — contract NCL-742 §6. */
export async function getMyAreas(): Promise<AssignedArea[]> {
  try {
    const response = await apiClient.get<ApiResult<AssignedArea[]>>('/me/areas');
    return response.data.data ?? [];
  } catch (err) {
    throw toApiError(err);
  }
}
