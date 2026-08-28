import { useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Eye } from 'lucide-react';
import type { ActivityLog } from '@/types/activityLog';
import { ActivityLogDetailDialog } from './ActivityLogDetailDialog';
import {
  formatActionType,
  formatTargetType,
  getActionColor,
} from '@/utils/activityLogFormatter';

interface Props {
  logs: ActivityLog[];
  loading?: boolean;
}

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

export const ActivityLogTable = ({ logs, loading }: Props) => {
  const [selectedLog, setSelectedLog] = useState<ActivityLog | null>(null);

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  if (logs.length === 0) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        <p className="text-lg font-semibold">Chưa có hoạt động</p>
        <p className="text-sm">Chưa có thao tác nào được ghi nhận trong hệ thống.</p>
      </div>
    );
  }

  const getActionValue = (log: ActivityLog) => log.actionType || log.action;
  const getTargetValue = (log: ActivityLog) => log.targetType || log.entityType || '';
  const getTargetIdValue = (log: ActivityLog) => log.targetId || log.entityId || '';
  const getActorValue = (log: ActivityLog) => log.actorName || log.fullName || log.username;

  return (
    <>
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[170px]">Thời gian</TableHead>
              <TableHead className="w-[180px]">Người thực hiện</TableHead>
              <TableHead className="w-[160px]">Hành động</TableHead>
              <TableHead className="w-[150px]">Đối tượng</TableHead>
              <TableHead>Mô tả</TableHead>
              <TableHead className="w-[110px] text-center">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {logs.map((log) => {
              const actionVal = getActionValue(log);
              const targetVal = getTargetValue(log);
              const targetIdVal = getTargetIdValue(log);

              return (
                <TableRow key={log.id} className="hover:bg-slate-50/80 transition-colors">
                  <TableCell className="whitespace-nowrap text-sm text-slate-600 font-mono">
                    {formatDate(log.createdAt)}
                  </TableCell>
                  <TableCell>
                    <div>
                      <div className="font-medium text-slate-800">{getActorValue(log)}</div>
                      <div className="text-xs text-muted-foreground">@{log.username}</div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline" className={`font-medium ${getActionColor(actionVal)}`}>
                      {formatActionType(actionVal)}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {targetVal ? (
                      <div>
                        <span className="text-sm font-medium text-slate-700">
                          {formatTargetType(targetVal)}
                        </span>
                        {targetIdVal && (
                          <div className="text-xs font-mono text-muted-foreground truncate max-w-[120px]" title={targetIdVal}>
                            {targetIdVal}
                          </div>
                        )}
                      </div>
                    ) : (
                      <span className="text-sm text-muted-foreground">—</span>
                    )}
                  </TableCell>
                  <TableCell className="max-w-[320px]">
                    <span className="truncate block text-sm text-slate-700" title={log.description}>
                      {log.description || '—'}
                    </span>
                  </TableCell>
                  <TableCell className="text-center">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setSelectedLog(log)}
                      className="h-8 px-2 text-emerald-700 hover:text-emerald-800 hover:bg-emerald-50 gap-1"
                      title="Xem chi tiết thao tác"
                    >
                      <Eye className="h-4 w-4" />
                      <span className="text-xs font-medium">Chi tiết</span>
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>

      {/* Modal Chi tiết hoạt động */}
      <ActivityLogDetailDialog
        log={selectedLog}
        onClose={() => setSelectedLog(null)}
      />
    </>
  );
};
