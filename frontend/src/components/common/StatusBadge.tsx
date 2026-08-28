import React from 'react';
import type { LucideIcon } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

export type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

const TONE_CLASSES: Record<StatusTone, { badge: string; icon: string }> = {
  success: {
    badge: 'bg-emerald-500/15 text-emerald-700 border-emerald-200 dark:text-emerald-400 dark:border-emerald-800',
    icon: 'text-emerald-600 dark:text-emerald-400',
  },
  warning: {
    badge: 'bg-amber-500/15 text-amber-700 border-amber-200 dark:text-amber-400 dark:border-amber-800',
    icon: 'text-amber-600 dark:text-amber-400',
  },
  danger: {
    badge: 'bg-rose-500/15 text-rose-700 border-rose-200 dark:text-rose-400 dark:border-rose-800',
    icon: 'text-rose-600 dark:text-rose-400',
  },
  info: {
    badge: 'bg-blue-500/15 text-blue-700 border-blue-200 dark:text-blue-400 dark:border-blue-800',
    icon: 'text-blue-600 dark:text-blue-400',
  },
  neutral: {
    badge: 'bg-slate-500/15 text-slate-700 border-slate-200 dark:text-slate-300 dark:border-slate-700',
    icon: 'text-slate-600 dark:text-slate-300',
  },
};

interface StatusBadgeProps {
  /** Chuỗi hiển thị trong badge. */
  label: string;
  /** Tone màu (success/warning/danger/info/neutral). */
  tone: StatusTone;
  /** Icon tùy chọn hiển thị cạnh label. */
  icon?: LucideIcon;
}

/**
 * Badge trạng thái dùng chung toàn dự án (mẫu `ApiKeyStatusBadge` của bảng API key).
 * Màu theo tone, hỗ trợ dark mode bằng token.
 */
export const StatusBadge: React.FC<StatusBadgeProps> = ({ label, tone, icon: Icon }) => {
  const toneClass = TONE_CLASSES[tone];
  return (
    <Badge
      variant="outline"
      className={cn(
        'flex items-center gap-1 w-fit',
        toneClass.badge
      )}
    >
      {Icon && <Icon className={cn('w-3 h-3', toneClass.icon)} />}
      <span>{label}</span>
    </Badge>
  );
};