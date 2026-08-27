import { Plus, ClipboardList, PencilLine } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion';
import { CorrectFarmLogDialog } from './CorrectFarmLogDialog';
import { AttachmentManager } from './AttachmentManager';
import { useAuth } from '@/hooks/useAuth';
import { ROLE_ACCESS, hasAnyRole } from '@/config/roleAccess';
import type { FarmLog } from '@/types/farmLog';

interface FarmLogTabProps {
  logs: FarmLog[];
  onCreateLog: () => void;
  onLogUpdated?: () => void;
}

export function FarmLogTab({ logs, onCreateLog, onLogUpdated }: FarmLogTabProps) {
  const { user } = useAuth();
  const [correctingLog, setCorrectingLog] = useState<FarmLog | null>(null);
  const openItems = logs.length > 0 ? [logs[0].id] : [];

  /**
   * NCL-03-CN-006: quyền hiển thị nút Đính chính theo vai trò.
   * VT-02 được đính chính mọi nhật ký của tổ chức; VT-03 chỉ được
   * đính chính nhật ký do chính mình ghi (bản ghi đã bị thay thế
   * bởi bản đính chính khác sẽ không còn nút đính chính).
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
        {logs.map((log) => (
          <AccordionItem key={log.id} value={log.id} className="border rounded-lg px-4">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-3 text-left">
                <div className="w-8 h-8 rounded bg-primary/10 flex items-center justify-center">
                  <ClipboardList className="h-4 w-4 text-primary" />
                </div>
                <div>
                  <div className="flex flex-wrap items-center gap-2 font-medium">
                    {new Date(log.executedDate).toLocaleDateString('vi-VN')} — {log.activityType}
                    {log.isCorrection && (
                      <Badge className="bg-amber-100 text-amber-800 hover:bg-amber-100">
                        Đính chính
                      </Badge>
                    )}
                    {log.isCorrected && (
                      <Badge variant="secondary">Đã đính chính</Badge>
                    )}
                  </div>
                  <div className="text-sm text-muted-foreground">
                    {log.attachments?.length || 0} chứng từ
                  </div>
                </div>
              </div>
            </AccordionTrigger>
            <AccordionContent>
              {log.isCorrection && log.correctionReason && (
                <div className="mb-4 p-3 border-l-4 border-amber-400 bg-amber-50 rounded-md">
                  <p className="text-sm text-amber-900">
                    <span className="font-semibold">Lý do đính chính:</span>{' '}
                    {log.correctionReason}
                  </p>
                  {log.correctedByName && (
                    <p className="mt-1 text-xs text-amber-700">
                      Người đính chính: {log.correctedByName}
                      {log.originalFarmLogId && ' · Liên kết tới bản gốc'}
                    </p>
                  )}
                </div>
              )}
              {log.notes && (
                <div className="mb-4 p-3 bg-muted rounded-md">
                  <p className="text-sm text-muted-foreground">{log.notes}</p>
                </div>
              )}
              {canCorrectLog(log) && (
                <div className="mb-3 flex justify-end">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setCorrectingLog(log)}
                  >
                    <PencilLine className="mr-2 h-4 w-4" /> Đính chính
                  </Button>
                </div>
              )}
              <AttachmentManager logId={log.id} onUpdate={onLogUpdated} />
            </AccordionContent>
          </AccordionItem>
        ))}
      </Accordion>

      {correctingLog && (
        <CorrectFarmLogDialog
          log={correctingLog}
          open
          onOpenChange={(open) => {
            if (!open) setCorrectingLog(null);
          }}
          onSuccess={() => onLogUpdated?.()}
        />
      )}
    </div>
  );
}