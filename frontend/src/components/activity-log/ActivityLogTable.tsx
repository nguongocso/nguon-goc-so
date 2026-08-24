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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Eye, Info, User, Calendar, MapPin, Activity, Layers, FileText } from 'lucide-react';
import type { ActivityLog } from '@/types/activityLog';
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
              <TableHead className="w-[110px] text-right">Thao tác</TableHead>
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
                  <TableCell className="text-right">
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
      {selectedLog && (
        <Dialog open={!!selectedLog} onOpenChange={(open) => !open && setSelectedLog(null)}>
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
              <div className="grid grid-cols-2 gap-3 bg-slate-50 p-3.5 rounded-lg border border-slate-100 text-sm">
                <div className="space-y-1">
                  <span className="text-xs font-medium text-slate-500 flex items-center gap-1">
                    <Calendar className="h-3.5 w-3.5 text-slate-400" /> Thời gian
                  </span>
                  <div className="font-mono text-slate-800 text-xs">
                    {formatDate(selectedLog.createdAt)}
                  </div>
                </div>

                <div className="space-y-1">
                  <span className="text-xs font-medium text-slate-500 flex items-center gap-1">
                    <User className="h-3.5 w-3.5 text-slate-400" /> Người thực hiện
                  </span>
                  <div className="font-medium text-slate-800">
                    {getActorValue(selectedLog)}{' '}
                    <span className="text-xs text-muted-foreground font-normal">
                      (@{selectedLog.username})
                    </span>
                  </div>
                </div>

                <div className="space-y-1">
                  <span className="text-xs font-medium text-slate-500 flex items-center gap-1">
                    <Activity className="h-3.5 w-3.5 text-slate-400" /> Hành động
                  </span>
                  <div>
                    <Badge variant="outline" className={`font-medium ${getActionColor(getActionValue(selectedLog))}`}>
                      {formatActionType(getActionValue(selectedLog))}
                    </Badge>
                  </div>
                </div>

                <div className="space-y-1">
                  <span className="text-xs font-medium text-slate-500 flex items-center gap-1">
                    <MapPin className="h-3.5 w-3.5 text-slate-400" /> Địa chỉ IP
                  </span>
                  <div className="font-mono text-slate-800 text-xs">
                    {selectedLog.ipAddress || 'Không xác định'}
                  </div>
                </div>
              </div>

              {/* Đối tượng tác động */}
              <div className="space-y-1.5">
                <span className="text-xs font-semibold text-slate-600 flex items-center gap-1">
                  <Layers className="h-4 w-4 text-emerald-600" /> Đối tượng tác động
                </span>
                <div className="p-3 bg-white border border-slate-200 rounded-lg space-y-1 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-500 text-xs">Loại đối tượng:</span>
                    <span className="font-medium text-slate-800">
                      {formatTargetType(getTargetValue(selectedLog))}
                    </span>
                  </div>
                  {getTargetIdValue(selectedLog) && (
                    <div className="flex justify-between">
                      <span className="text-slate-500 text-xs">Mã định danh (ID):</span>
                      <span className="font-mono text-xs text-slate-700 select-all">
                        {getTargetIdValue(selectedLog)}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Mô tả chi tiết */}
              <div className="space-y-1.5">
                <span className="text-xs font-semibold text-slate-600 flex items-center gap-1">
                  <FileText className="h-4 w-4 text-emerald-600" /> Nội dung thao tác
                </span>
                <div className="p-3 bg-emerald-50/50 border border-emerald-100 rounded-lg text-sm text-slate-800 leading-relaxed">
                  {selectedLog.description || 'Không có mô tả chi tiết'}
                </div>
              </div>

              {/* Dữ liệu payload chi tiết nếu có */}
              {selectedLog.details && selectedLog.details !== selectedLog.description && (
                <div className="space-y-1.5">
                  <span className="text-xs font-semibold text-slate-600">Dữ liệu chi tiết (Payload)</span>
                  <pre className="p-3 bg-slate-900 text-slate-100 rounded-lg text-xs font-mono overflow-x-auto max-h-40">
                    {selectedLog.details}
                  </pre>
                </div>
              )}
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => setSelectedLog(null)}>
                Đóng
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </>
  );
};