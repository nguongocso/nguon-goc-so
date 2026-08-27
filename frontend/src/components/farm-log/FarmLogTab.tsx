import { Plus, ClipboardList, PencilLine, Pencil } from 'lucide-react';
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
  ACTIVITY_TYPE_LABELS,
  buildCorrectionMap,
  formatDateTime,
  groupLogsWithCorrections,
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

  const groupedLogs = useMemo(() => groupLogsWithCorrections(logs), [logs]);
  const correctionMap = useMemo(() => buildCorrectionMap(logs), [logs]);
  const openItems = logs.length > 0 ? [logs[0].id] : [];

  /**
   * NCL-03-CN-006: quyền hiển thị nút Đính chính theo vai trò.
   */
  const canCorrectLog = (log: FarmLog): boolean => {
    if (!hasAnyRole(user?.roleCode, ROLE_ACCESS.farmLogCorrect)) {
      return false;
    }
    if (log.isCorrected) {
      return false;
    }

    const isManager = user?.roleCode === 'VT-02';

    if (!isManager) {
      return !log.createdById || log.createdById === user?.userId;
    }

    return true;
  };

  if (logs.length === 0) {
    return (
      <div className="text-center py-12">
        <ClipboardList className="mx-auto h-12 w-12 text-muted-foreground" />
        <h3 className="mt-4 text-lg font-semibold">Chưa có nhật ký canh tác</h3>
        <p className="text-sm text-muted-foreground">Nhấn "Thêm nhật ký" để ghi lại hoạt động</p>
        <Button onClick={onCreateLog} variant="create" className="mt-4">
          <Plus className="mr-2 h-4 w-4" /> Thêm nhật ký
        </Button>
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold">Nhật ký canh tác</h2>
        <Button variant="create" onClick={onCreateLog} size="sm">
          <Plus className="mr-2 h-4 w-4" /> Thêm nhật ký
        </Button>
      </div>

      <Accordion defaultValue={openItems} className="space-y-2">
        {groupedLogs.map((log) => {
          const isOriginalCorrected = log.isCorrected === true;
          const isCorrectionEntry = log.isCorrection === true;
          const correction = isOriginalCorrected
            ? correctionMap.get(log.id)
            : undefined;
          // Trường nào bị thay đổi so với bản đính chính mới nhất
          const isChanged = (current: unknown, other: unknown) =>
            isOriginalCorrected &&
            Boolean(correction) &&
            String(current ?? '') !== String(other ?? '');

          const activityLabel =
            ACTIVITY_TYPE_LABELS[log.activityType] || log.activityType;
          const originalActivity =
            correction?.activityType
              ? ACTIVITY_TYPE_LABELS[correction.activityType] ||
                correction.activityType
              : undefined;

          return (
            <AccordionItem
              key={log.id}
              value={log.id}
              className={cn(
                'border rounded-lg px-4 transition-colors',
                isOriginalCorrected && 'bg-slate-50/60 text-muted-foreground',
                isCorrectionEntry &&
                  'bg-amber-50/70 border-l-4 border-amber-400',
                isCorrectionEntry && 'ml-6',
              )}
            >
              <AccordionTrigger className="hover:no-underline">
                <div className="flex items-center gap-3 text-left">
                  <div
                    className={cn(
                      'w-8 h-8 rounded flex items-center justify-center',
                      isCorrectionEntry
                        ? 'bg-amber-100 text-amber-700'
                        : 'bg-primary/10 text-primary',
                    )}
                  >
                    {isCorrectionEntry ? (
                      <Pencil className="h-4 w-4" />
                    ) : (
                      <ClipboardList className="h-4 w-4" />
                    )}
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2 font-medium">
                      {isCorrectionEntry && (
                        <Badge className="bg-amber-100 text-amber-800 hover:bg-amber-100">
                          Bản đính chính
                        </Badge>
                      )}
                      {isOriginalCorrected && (
                        <Badge
                          className="bg-slate-200 text-slate-600 hover:bg-slate-200"
                        >
                          Gốc (Đã đính chính)
                        </Badge>
                      )}
                      <span
                        className={cn(
                          isChanged(activityLabel, originalActivity) &&
                            'line-through decoration-red-400',
                        )}
                      >
                        {new Date(log.executedDate).toLocaleDateString('vi-VN')}{' '}
                        — {activityLabel}
                      </span>
                    </div>
                    <div className="text-sm text-muted-foreground">
                      {log.attachments?.length || 0} chứng từ
                    </div>
                  </div>
                </div>
              </AccordionTrigger>
              <AccordionContent>
                {isCorrectionEntry && (
                  <div className="mb-4 p-3 border-l-4 border-amber-400 bg-amber-50 rounded-md">
                    <p className="text-sm text-amber-900">
                      <span className="font-semibold">Lý do đính chính:</span>{' '}
                      {log.correctionReason}
                    </p>
                    {(log.correctedByName || log.createdAt) && (
                      <p className="mt-1 text-xs text-amber-700">
                        ✏️ Người sửa: {log.correctedByName ?? '—'} · Thời gian:{' '}
                        {log.createdAt ? formatDateTime(log.createdAt) : '—'}
                        {log.originalFarmLogId && ' · Liên kết tới bản gốc'}
                      </p>
                    )}
                  </div>
                )}
                {log.notes && (
                  <div className="mb-4 p-3 bg-muted rounded-md">
                    <p
                      className={cn(
                        'text-sm text-muted-foreground',
                        isOriginalCorrected &&
                          correction?.notes !== log.notes &&
                          'line-through decoration-red-400',
                      )}
                    >
                      {log.notes}
                    </p>
                  </div>
                )}
                {canCorrectLog(log) && (
                  <div className="mb-3 flex justify-end">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => navigate(`/farm-logs/${log.id}/correct`)}
                    >
                      <PencilLine className="mr-2 h-4 w-4" /> Đính chính
                    </Button>
                  </div>
                )}
                <AttachmentManager logId={log.id} onUpdate={onLogUpdated} />
              </AccordionContent>
            </AccordionItem>
          );
        })}
      </Accordion>
    </div>
  );
}