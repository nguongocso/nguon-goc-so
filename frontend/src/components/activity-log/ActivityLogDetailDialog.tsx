import { useMemo } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { DetailSection } from '@/components/common/detail/DetailSection';
import { DetailField } from '@/components/common/detail/DetailField';
import {
  Activity,
  Braces,
  Calendar,
  ChevronDown,
  FileText,
  Info,
  Layers,
  MapPin,
  User,
} from 'lucide-react';
import type { ActivityLog } from '@/types/activityLog';
import {
  formatActionType,
  formatTargetType,
  getActionColor,
} from '@/utils/activityLogFormatter';

interface Props {
  log: ActivityLog | null;
  onClose: () => void;
}

/**
 * Field accessors kept identical to ActivityLogTable so backend field
 * fallbacks (actionType/action, targetId/entityId, …) never diverge.
 */
const getActionValue = (log: ActivityLog) => log.actionType || log.action;
const getTargetValue = (log: ActivityLog) => log.targetType || log.entityType || '';
const getTargetIdValue = (log: ActivityLog) => log.targetId || log.entityId || '';
const getActorValue = (log: ActivityLog) => log.actorName || log.fullName || log.username;

const formatDate = (iso: string) => {
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return iso;
  }
};

/**
 * Pretty-print the payload when it is valid JSON; otherwise return the raw
 * string untouched. Never discards or truncates data.
 */
const formatPayload = (raw: string): string => {
  try {
    const parsed: unknown = JSON.parse(raw);
    return typeof parsed === 'string' ? parsed : JSON.stringify(parsed, null, 2);
  } catch {
    return raw;
  }
};

/**
 * Activity Log detail dialog (extracted from ActivityLogTable).
 *
 * Reading order follows audit priority:
 * actor/action/time → target → human-readable description → raw payload last,
 * collapsed by default as clearly secondary technical information.
 */
export function ActivityLogDetailDialog({ log, onClose }: Props) {
  const formattedPayload = useMemo(() => {
    if (!log?.details) return null;
    return formatPayload(log.details);
  }, [log]);

  if (!log) return null;

  const hasPayload = Boolean(log.details && log.details !== log.description);

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-2 text-emerald-700">
            <Info className="h-5 w-5" />
            <DialogTitle>Chi tiết nhật ký hoạt động</DialogTitle>
          </div>
          <DialogDescription>
            Thông tin chi tiết thao tác được ghi nhận bởi hệ thống kiểm toán
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {/* Actor / Action / Time / IP */}
          <DetailSection
            title="Người thực hiện & Thời gian"
            contentClassName="grid gap-3 sm:grid-cols-2"
          >
            <DetailField
              label="Người thực hiện"
              icon={<User className="h-3.5 w-3.5 text-muted-foreground" />}
              value={
                <>
                  {getActorValue(log)}{' '}
                  <span className="text-xs font-normal text-muted-foreground">
                    (@{log.username})
                  </span>
                </>
              }
            />

            <DetailField
              label="Hành động"
              icon={<Activity className="h-3.5 w-3.5 text-muted-foreground" />}
              value={
                <Badge
                  variant="outline"
                  className={`font-medium ${getActionColor(getActionValue(log))}`}
                >
                  {formatActionType(getActionValue(log))}
                </Badge>
              }
            />

            <DetailField
              label="Thời gian"
              icon={<Calendar className="h-3.5 w-3.5 text-muted-foreground" />}
              mono
              value={formatDate(log.createdAt)}
            />

            <DetailField
              label="Địa chỉ IP"
              icon={<MapPin className="h-3.5 w-3.5 text-muted-foreground" />}
              mono
              value={log.ipAddress || 'Không xác định'}
            />
          </DetailSection>

          {/* Target */}
          <DetailSection
            title="Đối tượng tác động"
            icon={<Layers className="h-4 w-4 text-emerald-600" />}
          >
            <div className="grid gap-3">
              <DetailField
                label="Loại đối tượng"
                value={formatTargetType(getTargetValue(log))}
              />
              {getTargetIdValue(log) && (
                <DetailField
                  label="Mã định danh (ID)"
                  mono
                  value={getTargetIdValue(log)}
                />
              )}
            </div>
          </DetailSection>

          {/* Description */}
          <DetailSection
            title="Nội dung thao tác"
            icon={<FileText className="h-4 w-4 text-emerald-600" />}
          >
            <p className="text-sm leading-relaxed">
              {log.description || 'Không có mô tả chi tiết'}
            </p>
          </DetailSection>

          {/* Raw payload — technical, secondary, collapsed by default */}
          {hasPayload && formattedPayload !== null && (
            <DetailSection
              title="Dữ liệu kỹ thuật"
              description="Dữ liệu chi tiết (Payload) — JSON thô được ghi bởi hệ thống."
              icon={<Braces className="h-4 w-4 text-muted-foreground" />}
              contentClassName="overflow-hidden p-0"
            >
              <details className="group">
                <summary className="flex cursor-pointer select-none items-center justify-between gap-2 bg-muted/40 px-4 py-2.5 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground [&::-webkit-details-marker]:hidden">
                  Xem dữ liệu payload
                  <ChevronDown className="h-3.5 w-3.5 transition-transform group-open:rotate-180" />
                </summary>
                <pre className="max-h-60 overflow-auto whitespace-pre-wrap break-all bg-slate-900 p-3 font-mono text-xs leading-relaxed text-slate-100">
                  {formattedPayload}
                </pre>
              </details>
            </DetailSection>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Đóng
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

