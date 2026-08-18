import apiClient from './axiosConfig';
import type { PageResponse } from '@/types/common';
import type {
  LoginAnomalyItem,
  LoginAnomalyFilters,
  LoginAnomalyStatus,
  SuspiciousCaseFilters,
  SuspiciousCaseItem,
} from '@/types/loginAnomaly';

export const getLoginAnomalies = async (
  params: LoginAnomalyFilters = {},
): Promise<PageResponse<LoginAnomalyItem>> => {
  const response = await apiClient.get<{ data: PageResponse<LoginAnomalyItem> }>(
    '/auth/security/login-anomalies',
    {
      params: {
        ...params,
        username: params.username?.trim() || undefined,
      },
    },
  );

  return response.data.data;
};

export const getSuspiciousCases = async (
  params: SuspiciousCaseFilters = {},
): Promise<PageResponse<SuspiciousCaseItem>> => {
  const response = await apiClient.get<{ data: PageResponse<SuspiciousCaseItem> }>(
    '/auth/security/suspicious-cases',
    {
      params: {
        status: params.status && params.status !== 'ALL' ? params.status : undefined,
        username: params.username?.trim() || undefined,
        page: params.page,
        size: params.size,
      },
    },
  );

  return response.data.data;
};

export const lockAccount = async (
  accountId: string,
  anomalyId?: string,
  reason = 'khóa do phát hiện đăng nhập bất thường',
  options: { days?: number; hours?: number; minutes?: number; permanent?: boolean } = {},
) => {
  const response = await apiClient.patch<{ data: { accountId: string; status: string; reason: string } }>(
    `/auth/security/accounts/${accountId}/lock`,
    {
      anomalyId,
      reason,
      days: options.days ?? 0,
      hours: options.hours ?? 0,
      minutes: options.minutes ?? 0,
      permanent: !!options.permanent,
    },
  );

  return response.data.data;
};

export const unlockAccount = async (accountId: string) => {
  const response = await apiClient.patch<{ data: { accountId: string; status: string; reason: string } }>(
    `/auth/security/accounts/${accountId}/unlock`,
  );

  return response.data.data;
};

export const getStatusLabel = (status: LoginAnomalyStatus) => {
  const map: Record<LoginAnomalyStatus, string> = {
    OPEN: 'Chưa giải quyết',
    DISMISSED: 'Đã giải quyết',
  };

  return map[status];
};

export const resolveUserAnomalies = async (accountId: string) => {
  const response = await apiClient.patch<{ data: null }>(`/auth/security/accounts/${accountId}/resolve-anomalies`);
  return response.data.data;
};
