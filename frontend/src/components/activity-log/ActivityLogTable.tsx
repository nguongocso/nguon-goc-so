import { useState } from 'react';
import {
  TableCell,
  TableHead,
  TableRow,
} from '@/components/ui/table';
import { DataTableShell } from '@/components/common/DataTableShell';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Eye } from 'lucide-react';
import type { ActivityLog } from '@/types/activityLog';
import { ActivityLogDetailDialog } from './ActivityLogDetailDialog';
import {
  formatActionType,
  formatActivityLogDescription,
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

  const getActionValue = (log: ActivityLog) => log.actionType || log.action;
  const getTargetValue = (log: ActivityLog) => log.targetType || log.entityType || '';
  const getTargetIdValue = (log: ActivityLog) => log.targetId || log.entityId || '';
  const getActorValue = (log: ActivityLog) => log.actorName || log.fullName || log.username;

  return (
    <>
      <DataTableShell
        loading={loading}
        empty={!loading && logs.length === 0}
        colSpan={6}
        loadingMessage="Đang tải lịch sử hoạt động..."
        emptyMessage="Chưa có thao tác nào được ghi nhận trong hệ thống."
        header={
          <>
            <TableHead className="w-[170px]">Thời gian</TableHead>
            <TableHead className="w-[180px]">Người thực hiện</TableHead>
            <TableHead className="w-[160px]">Hành động</TableHead>
            <TableHead className="w-[150px]">Đối tượng</TableHead>
            <TableHead>Mô tả</TableHead>
            <TableHead className="w-[110px] text-center">Thao tác</TableHead>
          </>
        }
        body={
          <>
            {logs.map((log) => {
              const actionVal = getActionValue(log);
              const targetVal = getTargetValue(log);
              const targetIdVal = getTargetIdValue(log);
              const description = formatActivityLogDescription(log.description, actionVal);

              return (
                <TableRow key={log.id} className="transition-colors hover:bg-table-hover">
                  <TableCell className="whitespace-nowrap font-mono text-sm text-muted-foreground">
                    {formatDate(log.createdAt)}
                  </TableCell>
                  <TableCell>
                    <div>
                      <div className="font-medium text-foreground">{getActorValue(log)}</div>
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
                        <span className="text-sm font-medium text-foreground">
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
                    <span className="block truncate text-sm text-foreground" title={description}>
                      {description}
                    </span>
                  </TableCell>
                  <TableCell className="text-center">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => setSelectedLog(log)}
                      aria-label="Xem chi tiết thao tác"
                      title="Xem chi tiết thao tác"
                    >
                      <Eye className="size-4" />
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </>
        }
      />

      {/* Modal Chi tiết hoạt động */}
      <ActivityLogDetailDialog
        log={selectedLog}
        onClose={() => setSelectedLog(null)}
      />
    </>
  );
};
