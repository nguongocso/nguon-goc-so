import type { FarmLogEligibilityStatus } from './FarmLogEligibilityAlert';

interface EligibilityStatusStyle {
  container: string;
  icon: string;
  title: string;
}

/** Ánh xạ trạng thái kiểm tra sang lớp hiển thị tương ứng. */
export const FARM_LOG_ELIGIBILITY_STYLES: Record<
  FarmLogEligibilityStatus,
  EligibilityStatusStyle
> = {
  unselected: {
    container: 'border-slate-200 bg-slate-50',
    icon: 'bg-white text-slate-500 ring-slate-200',
    title: 'text-slate-900',
  },
  idle: {
    container: 'border-blue-200 bg-blue-50/70',
    icon: 'bg-white text-blue-700 ring-blue-200',
    title: 'text-blue-950',
  },
  checking: {
    container: 'border-blue-200 bg-blue-50/70',
    icon: 'bg-white text-blue-700 ring-blue-200',
    title: 'text-blue-950',
  },
  eligible: {
    container: 'border-emerald-200 bg-emerald-50/70',
    icon: 'bg-white text-emerald-700 ring-emerald-200',
    title: 'text-emerald-950',
  },
  ineligible: {
    container: 'border-amber-300 bg-amber-50',
    icon: 'bg-white text-amber-700 ring-amber-200',
    title: 'text-amber-950',
  },
  error: {
    container: 'border-red-200 bg-red-50/70',
    icon: 'bg-white text-red-700 ring-red-200',
    title: 'text-red-950',
  },
};
