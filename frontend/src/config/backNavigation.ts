import { matchPath, type Params } from 'react-router-dom';

type BackNavTarget =
  | string
  | ((params: Params<string>, search: URLSearchParams) => string);

interface BackNavRule {
  path: string;
  label: BackNavTarget;
  fallbackTo: BackNavTarget;
}

export interface ResolvedBackNav {
  label: string;
  fallbackTo: string;
}

const lotIdFromParams = (params: Params<string>) =>
  params.productionLotId ?? params.id ?? '';

const fallbackFromLotQuery =
  (suffix = '') =>
  (_params: Params<string>, search: URLSearchParams) => {
    const lotId = search.get('productionLotId');
    return lotId ? `/production-lots/${lotId}${suffix}` : '/production-lots';
  };

const BACK_NAV_RULES: BackNavRule[] = [
  {
    path: '/organizations/create',
    label: 'Quay lại danh sách tổ chức',
    fallbackTo: '/organizations',
  },
  {
    path: '/organizations/:id',
    label: 'Quay lại danh sách tổ chức',
    fallbackTo: '/organizations',
  },
  {
    path: '/members/create',
    label: 'Quay lại quản lý thành viên',
    fallbackTo: '/members',
  },
  {
    path: '/invitations/create',
    label: 'Quay lại quản lý thành viên',
    fallbackTo: '/members',
  },
  {
    path: '/farm-areas/create',
    label: 'Quay lại danh sách vùng trồng',
    fallbackTo: '/farm-areas',
  },
  {
    path: '/production-lots/create',
    label: 'Quay lại danh sách lô sản xuất',
    fallbackTo: '/production-lots',
  },
  {
    path: '/production-lots/import',
    label: 'Quay lại danh sách lô sản xuất',
    fallbackTo: '/production-lots',
  },
  {
    path: '/production-lots/:id/edit',
    label: 'Quay lại chi tiết lô',
    fallbackTo: (params) => `/production-lots/${lotIdFromParams(params)}`,
  },
  {
    path: '/production-lots/:productionLotId/farm-logs',
    label: 'Quay lại chi tiết lô',
    fallbackTo: (params) => `/production-lots/${lotIdFromParams(params)}`,
  },
  {
    path: '/production-lots/:id',
    label: 'Quay lại danh sách lô sản xuất',
    fallbackTo: '/production-lots',
  },
  {
    path: '/farm-logs/create',
    label: 'Quay lại lịch sử nhật ký canh tác',
    fallbackTo: fallbackFromLotQuery('/farm-logs'),
  },
  {
    path: '/preprocessing-events/create',
    label: 'Quay lại chi tiết lô',
    fallbackTo: fallbackFromLotQuery(),
  },
  {
    path: '/preprocessing-events/:id/correct',
    label: 'Quay lại chi tiết lô',
    fallbackTo: fallbackFromLotQuery(),
  },
  {
    path: '/packaging-events/create',
    label: 'Quay lại chi tiết lô',
    fallbackTo: fallbackFromLotQuery(),
  },
  {
    path: '/packaging-events/:id/correct',
    label: 'Quay lại chi tiết lô',
    fallbackTo: fallbackFromLotQuery(),
  },
  {
    path: '/transport-events/record',
    label: 'Quay lại chi tiết lô',
    fallbackTo: fallbackFromLotQuery(),
  },
  {
    path: '/admin/code-ranges/create',
    label: 'Quay lại danh sách dải mã',
    fallbackTo: '/admin/code-ranges',
  },
  {
    path: '/admin/standards/:standardId/criteria',
    label: 'Quay lại danh sách tiêu chuẩn',
    fallbackTo: '/admin/standards',
  },
  {
    path: '/admin/suspect-trace-codes/:traceCodeId',
    label: 'Quay lại danh sách tem nghi vấn',
    fallbackTo: '/admin/suspect-trace-codes',
  },
  {
    path: '/certifications/create',
    label: 'Quay lại danh sách chứng nhận',
    fallbackTo: '/certifications',
  },
  {
    path: '/recall-requests/create',
    label: 'Quay lại danh sách yêu cầu thu hồi',
    fallbackTo: '/recall-requests',
  },
  {
    path: '/recall-requests/:id',
    label: 'Quay lại danh sách yêu cầu thu hồi',
    fallbackTo: '/recall-requests',
  },
  {
    path: '/warehouse-receipt/:eventId',
    label: 'Quay lại danh sách phiếu nhập kho',
    fallbackTo: '/warehouse-receipt',
  },
];

export const resolveBackNav = (
  pathname: string,
  search = '',
): ResolvedBackNav | null => {
  const searchParams = new URLSearchParams(search);

  for (const rule of BACK_NAV_RULES) {
    const match = matchPath(rule.path, pathname);
    if (!match) continue;

    const resolveTarget = (target: BackNavTarget) =>
      typeof target === 'function' ? target(match.params, searchParams) : target;

    return {
      label: resolveTarget(rule.label),
      fallbackTo: resolveTarget(rule.fallbackTo),
    };
  }

  return null;
};
