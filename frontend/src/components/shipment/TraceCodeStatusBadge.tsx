import type { TraceCodeStatus } from '@/types/traceCode';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

export const TRACE_CODE_STATUS_LABELS: Record<TraceCodeStatus, string> = {
  INACTIVE: 'Chưa kích hoạt',
  ACTIVE: 'Đã kích hoạt',
  LOCKED: 'Đang bị khóa',
  CANCELLED: 'Đã hủy',
  RECALLED: 'Đã thu hồi',
  SUSPECT: 'Nghi vấn',
};

const TRACE_CODE_STATUS_CLASSES: Record<TraceCodeStatus, string> = {
  INACTIVE: 'bg-slate-100 text-slate-700 border-slate-300',
  ACTIVE: 'bg-emerald-50 text-emerald-700 border-emerald-300',
  LOCKED: 'bg-rose-50 text-rose-700 border-rose-300',
  CANCELLED: 'bg-gray-100 text-gray-500 border-gray-300',
  RECALLED: 'bg-amber-50 text-amber-700 border-amber-300',
  SUSPECT: 'bg-yellow-50 text-yellow-800 border-yellow-300',
};

interface TraceCodeStatusBadgeProps {
  status: TraceCodeStatus;
  className?: string;
}

export function TraceCodeStatusBadge({ status, className }: TraceCodeStatusBadgeProps) {
  return (
    <Badge
      variant="outline"
      className={cn('font-medium', TRACE_CODE_STATUS_CLASSES[status] || 'bg-slate-100 text-slate-700', className)}
    >
      {TRACE_CODE_STATUS_LABELS[status] ?? status}
    </Badge>
  );
}
