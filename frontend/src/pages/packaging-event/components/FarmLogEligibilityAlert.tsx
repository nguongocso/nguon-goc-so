import {
  CheckCircle2,
  ClipboardCheck,
  LoaderCircle,
  RefreshCw,
  TriangleAlert,
  XCircle,
} from 'lucide-react';

import { Button } from '@/components/ui/button';

import { cn } from '@/lib/utils';

import { FARM_LOG_ELIGIBILITY_STYLES } from './farmLogEligibilityStyles';

/** Trạng thái kiểm tra mốc canh tác trước khi đóng gói. */
export type FarmLogEligibilityStatus =
  | 'unselected'
  | 'idle'
  | 'checking'
  | 'eligible'
  | 'ineligible'
  | 'error';

/** Thuộc tính cấu hình hiển thị cảnh báo mốc canh tác. */
export interface FarmLogEligibilityAlertProps {
  status: FarmLogEligibilityStatus;
  productionLotName?: string;
  /** Tên các mốc canh tác bắt buộc còn thiếu (từ backend). */
  missingMilestones?: string[];
  message?: string;
  actionLabel?: string;
  onAction?: () => void;
  onRetry?: () => void;
}

/** Thành phần hiển thị thông báo trạng thái kiểm tra mốc canh tác cho lô sản xuất. */
export function FarmLogEligibilityAlert({
  status,
  productionLotName,
  missingMilestones = [],
  message,
  actionLabel,
  onAction,
  onRetry,
}: FarmLogEligibilityAlertProps) {
  const styles = FARM_LOG_ELIGIBILITY_STYLES[status];

  const content = {
    unselected: {
      icon: ClipboardCheck,
      title: 'Chọn lô để kiểm tra mốc canh tác',
      description:
        'Hệ thống sẽ kiểm tra mốc canh tác bắt buộc trước khi cho phép ghi sự kiện đóng gói.',
    },
    idle: {
      icon: ClipboardCheck,
      title: 'Sẵn sàng kiểm tra mốc canh tác',
      description: productionLotName
        ? `Mốc canh tác của lô “${productionLotName}” sẽ được kiểm tra khi bạn ghi sự kiện đóng gói.`
        : 'Mốc canh tác của lô sẽ được kiểm tra khi bạn ghi sự kiện đóng gói.',
    },
    checking: {
      icon: LoaderCircle,
      title: 'Đang kiểm tra mốc canh tác',
      description:
        'Vui lòng chờ trong khi hệ thống đối chiếu các mốc canh tác bắt buộc.',
    },
    eligible: {
      icon: CheckCircle2,
      title: 'Đủ điều kiện đóng gói',
      description:
        message ?? 'Lô sản xuất đã đáp ứng đủ mốc canh tác bắt buộc.',
    },
    ineligible: {
      icon: TriangleAlert,
      title: 'Chưa đủ điều kiện đóng gói',
      description:
        message ??
        'Lô sản xuất còn thiếu mốc canh tác bắt buộc. Vui lòng bổ sung trước khi tiếp tục.',
    },
    error: {
      icon: XCircle,
      title: 'Không thể kiểm tra mốc canh tác',
      description:
        message ?? 'Đã xảy ra lỗi khi kiểm tra. Vui lòng thử lại.',
    },
  }[status];

  const Icon = content.icon;
  const isAssertive = status === 'ineligible' || status === 'error';

  return (
    <section
      className={cn(
        'rounded-xl border p-4 sm:p-5',
        styles.container,
      )}
      aria-live={isAssertive ? 'assertive' : 'polite'}
      role={isAssertive ? 'alert' : 'status'}
    >
      <div className="flex items-start gap-3">
        <span
          className={cn(
            'grid size-9 shrink-0 place-items-center rounded-full ring-1',
            styles.icon,
          )}
        >
          <Icon
            className={cn(
              'size-5',
              status === 'checking' && 'animate-spin',
            )}
            aria-hidden="true"
          />
        </span>

        <div className="min-w-0 flex-1">
          <h3 className={cn('text-sm font-bold', styles.title)}>
            {content.title}
          </h3>
          <p className="mt-1 text-sm leading-6 text-slate-600">
            {content.description}
          </p>

          {status === 'ineligible' && missingMilestones.length > 0 && (
            <div className="mt-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-amber-800">
                Mốc canh tác còn thiếu
              </p>
              <ul className="mt-2 flex flex-wrap gap-2">
                {missingMilestones.map((milestoneName) => (
                  <li
                    className={cn(
                      'rounded-full border border-amber-200 bg-white px-3 py-1',
                      'text-xs font-semibold text-amber-800',
                    )}
                    key={milestoneName}
                  >
                    {milestoneName}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {(onAction || onRetry) && (
            <div className="mt-4 flex flex-wrap gap-2">
              {onAction && actionLabel && (
                <Button
                  type="button"
                  size="sm"
                  onClick={onAction}
                >
                  {actionLabel}
                </Button>
              )}
              {onRetry && (
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={onRetry}
                >
                  <RefreshCw className="size-4" />
                  Kiểm tra lại
                </Button>
              )}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
