import {
  Plus,
  ClipboardList,
  PencilLine,
  FileText,
  Clock,
} from 'lucide-react';
import { useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion';
import { AttachmentManager } from './AttachmentManager';
import { useAuth } from '@/hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import { ROLE_ACCESS, hasAnyRole } from '@/config/roleAccess';
import { cn } from '@/lib/utils';
import {
  ACTIVITY_TYPE_ICONS,
  buildFarmLogGroups,
  formatDateTime,
  getActivityLabel,
  getLatestEffective,
  isFieldChanged,
  type FarmLogGroup,
} from '@/utils/farmLogCorrection';
import type { FarmLog } from '@/types/farmLog';

interface FarmLogTabProps {
  logs: FarmLog[];
  onCreateLog: () => void;
  onLogUpdated?: () => void;
}

export function FarmLogTab({ logs, onCreateLog, onLogUpdated }: FarmLogTabProps) {
  const { user } = useAuth();
  const navigate = useNavigate();

  const groups = useMemo(() => buildFarmLogGroups(logs), [logs]);
  const openItems = groups.length > 0 ? [groups[0].original.id] : [];

  const canCorrectLog = (log: FarmLog): boolean => {
    if (!hasAnyRole(user?.roleCode, ROLE_ACCESS.farmLogCorrect)) return false;
    if (log.isCorrected) return false;
    const isManager = user?.roleCode === 'VT-02';
    if (!isManager) return !log.createdById || log.createdById === user?.userId;
    return true;
  };

  if (logs.length === 0) {
    return (
      <div className="text-center py-12">
        <ClipboardList className="mx-auto h-12 w-12 text-muted-foreground" />
        <h3 className="mt-4 text-lg font-semibold">Chưa có nhật ký canh tác</h3>
        <p className="text-sm text-muted-foreground">
          Nhấn "Thêm nhật ký" để ghi lại hoạt động
        </p>
        <Button onClick={onCreateLog} variant="create" className="mt-4">
          <Plus className="mr-2 h-4 w-4" /> Thêm nhật ký
        </Button>
      </div>
    );
  }

  const renderCorrectionHistory = (group: FarmLogGroup) => {
    if (group.corrections.length === 0) return null;
    return (
      <div className="mb-4 space-y-2">
        <p className="text-xs font-semibold uppercase tracking-wide text-amber-700">
          Lịch sử đính chính
        </p>
        {group.corrections.map((c) => (
          <div
            key={c.id}
            className="rounded-md border-l-4 border-amber-400 bg-amber-50 p-3"
          >
            <p className="text-sm text-amber-900">
              <span className="font-semibold">Lý do đính chính:</span>{' '}
              {c.correctionReason ?? '—'}
            </p>
            <p className="mt-1 text-xs text-amber-700">
              ✏️ Người sửa: {c.correctedByName ?? '—'} · Thời gian:{' '}
              {c.createdAt ? formatDateTime(c.createdAt) : '—'}
            </p>
          </div>
        ))}
      </div>
    );
  };

  const renderOriginal = (group: FarmLogGroup) => {
    const latest = getLatestEffective(group);
    if (group.corrections.length === 0) return null;
    return (
      <div className="mb-4 rounded-md border border-slate-200 bg-slate-50 p-3">
        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Giá trị gốc (đã đính chính)
        </p>
        <dl className="mt-2 grid grid-cols-1 gap-x-6 gap-y-1 text-sm sm:grid-cols-2">
          <div className="flex justify-between gap-4">
            <dt className="text-muted-foreground">Hoạt động</dt>
            <dd
              className={cn(
                'font-medium',
                isFieldChanged(group.original, latest, 'activityType') &&
                  'line-through decoration-red-400',
              )}
            >
              {getActivityLabel(group.original.activityType)}
            </dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-muted-foreground">Ngày thực hiện</dt>
            <dd
              className={cn(
                'font-medium',
                isFieldChanged(group.original, latest, 'executedDate') &&
                  'line-through decoration-red-400',
              )}
            >
              {group.original.executedDate}
            </dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-muted-foreground">Vật tư</dt>
            <dd
              className={cn(
                'font-medium',
                isFieldChanged(group.original, latest, 'material') &&
                  'line-through decoration-red-400',
              )}
            >
              {group.original.material ?? '—'}
            </dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-muted-foreground">Số lượng</dt>
            <dd
              className={cn(
                'font-medium',
                isFieldChanged(group.original, latest, 'quantity') &&
                  'line-through decoration-red-400',
              )}
            >
              {group.original.quantity != null
                ? `${group.original.quantity} ${group.original.unit ?? ''}`
                : '—'}
            </dd>
          </div>
          {group.original.notes && (
            <div className="sm:col-span-2">
              <dt className="text-muted-foreground">Ghi chú gốc</dt>
              <dd
                className={cn(
                  'whitespace-pre-wrap font-medium',
                  isFieldChanged(group.original, latest, 'notes') &&
                    'line-through decoration-red-400',
                )}
              >
                {group.original.notes}
              </dd>
            </div>
          )}
        </dl>
      </div>
    );
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold">Nhật ký canh tác</h2>
        <Button variant="create" onClick={onCreateLog} size="sm">
          <Plus className="mr-2 h-4 w-4" /> Thêm nhật ký
        </Button>
      </div>

      <Accordion defaultValue={openItems} className="space-y-2">
        {groups.map((group) => {
          const effective = getLatestEffective(group);
          const hasCorrections = group.corrections.length > 0;
          const Icon = ACTIVITY_TYPE_ICONS[effective.activityType] ?? ClipboardList;
          const canCorrect = canCorrectLog(effective);

          return (
            <AccordionItem
              key={group.original.id}
              value={group.original.id}
              className={cn(
                'border rounded-lg px-4 transition-colors',
                hasCorrections && 'bg-amber-50/40 border-l-4 border-amber-300',
              )}
            >
              <AccordionTrigger className="hover:no-underline">
                <div className="flex items-center gap-3 text-left">
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-emerald-100 text-emerald-700">
                    <Icon className="h-4 w-4" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2 font-medium">
                      {hasCorrections ? (
                        <Badge className="bg-amber-100 text-amber-800 hover:bg-amber-100">
                          Đã đính chính
                        </Badge>
                      ) : (
                        <Badge variant="secondary">Bản gốc</Badge>
                      )}
                      <span className="truncate">
                        {effective.executedDate} —{' '}
                        {getActivityLabel(effective.activityType)}
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center gap-x-3 text-xs text-muted-foreground">
                      <span className="inline-flex items-center gap-1">
                        <FileText className="h-3.5 w-3.5" />
                        {effective.attachmentCount ?? 0} chứng từ
                      </span>
                      {effective.material && (
                        <span>
                          {effective.material}
                          {effective.quantity != null
                            ? ` – ${effective.quantity} ${effective.unit ?? ''}`
                            : ''}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </AccordionTrigger>
              <AccordionContent>
                {renderOriginal(group)}
                {renderCorrectionHistory(group)}
                {effective.notes && (
                  <div className="mb-4 p-3 bg-muted rounded-md">
                    <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                      {effective.notes}
                    </p>
                  </div>
                )}
                {canCorrect && (
                  <div className="mb-3 flex items-center justify-end gap-3">
                    <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                      <Clock className="h-3.5 w-3.5" />
                      Tạo lúc {formatDateTime(effective.createdAt)}
                    </span>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        navigate(`/farm-logs/${effective.id}/correct`)
                      }
                    >
                      <PencilLine className="mr-2 h-4 w-4" /> Đính chính
                    </Button>
                  </div>
                )}
                <AttachmentManager
                  logId={effective.id}
                  onUpdate={onLogUpdated}
                />
              </AccordionContent>
            </AccordionItem>
          );
        })}
      </Accordion>
    </div>
  );
}
