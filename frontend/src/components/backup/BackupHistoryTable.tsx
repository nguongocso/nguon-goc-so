import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Download, Trash2, RotateCcw } from 'lucide-react';
import type { BackupHistoryItem } from '@/types/backup';

const statusConfig = {
  IN_PROGRESS: { label: 'Đang xử lý', className: 'bg-warning-bg text-warning' },
  SUCCESS: { label: 'Thành công', className: 'bg-success-bg text-success' },
  FAILED: { label: 'Thất bại', className: 'bg-error-bg text-destructive' },
};

const operationLabels = {
  BACKUP: 'Sao lưu',
  RESTORE: 'Phục hồi',
};

interface Props {
  history: BackupHistoryItem[];
  loading: boolean;
  onDownload: (id: number) => void;
  onDelete: (id: number) => void;
  onRestore: (id: number) => void;
  disabled: boolean;
}

export const BackupHistoryTable = ({
  history,
  loading,
  onDownload,
  onDelete,
  onRestore,
  disabled,
}: Props) => {
  if (loading) {
    return <div className="flex justify-center py-12">Đang tải...</div>;
  }

  if (history.length === 0) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        <p className="text-lg font-semibold">Chưa có hoạt động</p>
        <p className="text-sm">Chưa có bản sao lưu hoặc phục hồi nào được thực hiện.</p>
      </div>
    );
  }

  const formatFileSize = (bytes: number | null) => {
    if (!bytes) return '—';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  };

  const formatDate = (iso: string) => {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>ID</TableHead>
            <TableHead>Thời gian</TableHead>
            <TableHead>Loại</TableHead>
            <TableHead>File</TableHead>
            <TableHead>Kích thước</TableHead>
            <TableHead>Trạng thái & Lỗi</TableHead>
            <TableHead className="text-right">Thao tác</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {history.map((item) => (
            <TableRow key={item.id}>
              <TableCell className="font-mono text-sm">{item.id}</TableCell>
              <TableCell>{formatDate(item.createdAt)}</TableCell>
              <TableCell>
                <Badge variant={item.operationType === 'BACKUP' ? 'default' : 'secondary'}>
                  {operationLabels[item.operationType]}
                </Badge>
              </TableCell>
              <TableCell className="font-mono text-sm">
                {item.fileName || '—'}
              </TableCell>
              <TableCell>{formatFileSize(item.fileSize)}</TableCell>
              <TableCell className="max-w-[340px]">
                <Badge className={statusConfig[item.status].className}>
                  {statusConfig[item.status].label}
                </Badge>
                {item.errorMessage && (
                  <div
                    className="text-[11px] font-mono text-rose-700 dark:text-rose-300 mt-1.5 p-2 rounded bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/50 whitespace-pre-wrap break-words leading-relaxed shadow-xs"
                    title={item.errorMessage}
                  >
                    {item.errorMessage}
                  </div>
                )}
              </TableCell>
              <TableCell className="text-right">
                <div className="flex justify-end gap-1">
                  {item.operationType === 'BACKUP' && item.status === 'SUCCESS' && (
                    <>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => onDownload(item.id)}
                        disabled={disabled}
                        title="Tải xuống"
                        className="hover:bg-muted"
                      >
                        <Download className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => onDelete(item.id)}
                        disabled={disabled}
                        title="Xóa bản sao lưu"
                        className="text-destructive hover:text-destructive hover:bg-muted"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => onRestore(item.id)}
                        disabled={disabled}
                        title="Phục hồi dữ liệu"
                        className="text-warning hover:text-warning hover:bg-muted"
                      >
                        <RotateCcw className="h-4 w-4" />
                      </Button>
                    </>
                  )}
                  {item.operationType === 'RESTORE' && (
                    <span className="text-xs text-muted-foreground">—</span>
                  )}
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};