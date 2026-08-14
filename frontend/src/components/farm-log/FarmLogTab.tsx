import { Plus, ClipboardList } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion';
import { AttachmentManager } from './AttachmentManager';
import type { FarmLog } from '@/types/farmLog';

interface FarmLogTabProps {
  logs: FarmLog[];
  onCreateLog: () => void;
  onLogUpdated?: () => void;
}

export function FarmLogTab({ logs, onCreateLog, onLogUpdated }: FarmLogTabProps) {
  const openItems = logs.length > 0 ? [logs[0].id] : [];

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
                  <div className="font-medium">
                    {new Date(log.executedDate).toLocaleDateString('vi-VN')} — {log.activityType}
                  </div>
                  <div className="text-sm text-muted-foreground">
                    {log.attachments?.length || 0} chứng từ
                  </div>
                </div>
              </div>
            </AccordionTrigger>
            <AccordionContent>
              {log.notes && (
                <div className="mb-4 p-3 bg-muted rounded-md">
                  <p className="text-sm text-muted-foreground">{log.notes}</p>
                </div>
              )}
              <AttachmentManager logId={log.id} onUpdate={onLogUpdated} />
            </AccordionContent>
          </AccordionItem>
        ))}
      </Accordion>
    </div>
  );
}