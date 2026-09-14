import { useEffect, useState } from 'react';
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
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import {
  AlertCircle,
  Calendar,
  CheckCircle2,
  FileDown,
  Info,
  Layers,
  Loader2,
  ShieldCheck,
  User,
} from 'lucide-react';
import type { ActivityLogExportFilterRequest } from '@/types/activityLog';
import {
  getActivityLogApiError,
  previewExportActivityLogs,
  requestActivityLogExport,
} from '@/api/activityLogApi';
import { formatActionType, formatTargetType } from '@/utils/activityLogFormatter';
import { toast } from 'sonner';

interface Props {
  open: boolean;
  onClose: () => void;
  filter: ActivityLogExportFilterRequest;
  onExportSuccess?: () => void;
}

/**
 * Hộp thoại xem trước số lượng và tải tệp CSV nhật ký hoạt động cho Quản lý tổ chức (VT-02).
 */
export const ActivityLogExportDialog = ({
  open,
  onClose,
  filter,
  onExportSuccess,
}: Props) => {
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [recordCount, setRecordCount] = useState<number | null>(null);
  const [exportMode, setExportMode] = useState<'DIRECT' | 'ASYNC'>('DIRECT');
  const [exporting, setExporting] = useState(false);
  const [previewError, setPreviewError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setRecordCount(null);
      setPreviewError(null);
      return;
    }

    let isMounted = true;
    const fetchPreview = async () => {
      setLoadingPreview(true);
      setPreviewError(null);
      try {
        const res = await previewExportActivityLogs(filter);
        if (isMounted) {
          setRecordCount(res.count);
          setExportMode(res.mode);
        }
      } catch (err: unknown) {
        if (isMounted) {
          const msg = await getActivityLogApiError(
            err, 'Không thể tính toán số lượng bản ghi xem trước.',
          );
          setPreviewError(msg);
        }
      } finally {
        if (isMounted) {
          setLoadingPreview(false);
        }
      }
    };

    fetchPreview();

    return () => {
      isMounted = false;
    };
  }, [open, filter.action, filter.actorName, filter.startDate, filter.endDate, filter.objectType]);

  const handleExport = async () => {
    setExporting(true);
    try {
      const result = await requestActivityLogExport(filter);
      toast.success(result.mode === 'DIRECT'
        ? 'Xuất nhật ký hoạt động thành công.'
        : 'Đã tạo yêu cầu xuất nền. Hệ thống sẽ thông báo khi tệp sẵn sàng.');
      onExportSuccess?.();
      onClose();
    } catch (err: unknown) {
      const msg = await getActivityLogApiError(err, 'Không thể tạo tệp nhật ký hoạt động.');
      toast.error(msg);
    } finally {
      setExporting(false);
    }
  };

  const hasNoData = recordCount === 0;
  const isAsync = exportMode === 'ASYNC';

  return (
    <Dialog open={open} onOpenChange={(v) => !v && !exporting && onClose()}>
      <DialogContent className="max-h-[calc(100vh-2rem)] overflow-hidden sm:max-w-xl">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary-light text-primary">
              <FileDown className="size-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-semibold text-foreground">
                Xuất nhật ký hoạt động
              </DialogTitle>
              <DialogDescription className="mt-1">
                Xem trước số lượng và tải tệp CSV snapshot phục vụ kiểm tra
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="max-h-[60vh] space-y-4 overflow-y-auto pr-1">
          {/* Card trạng thái số lượng bản ghi */}
          {loadingPreview ? (
            <div className="flex flex-col items-center justify-center gap-2 rounded-xl border border-border bg-muted/30 p-6 text-muted-foreground">
              <Loader2 className="size-6 animate-spin text-primary" />
              <span className="text-sm font-medium">Đang tính toán số lượng bản ghi...</span>
            </div>
          ) : previewError ? (
            <Alert variant="destructive" className="p-4">
              <AlertCircle />
              <AlertTitle>Lỗi xem trước số lượng</AlertTitle>
              <AlertDescription>{previewError}</AlertDescription>
            </Alert>
          ) : hasNoData ? (
            <Alert variant="warning" className="p-4">
              <AlertCircle />
              <AlertTitle>Không có bản ghi phù hợp</AlertTitle>
              <AlertDescription>
                  Không tìm thấy nhật ký hoạt động nào trong phạm vi bộ lọc đang chọn. Bạn cần điều
                  chỉnh lại điều kiện lọc trước khi xuất.
              </AlertDescription>
            </Alert>
          ) : isAsync ? (
            <Alert variant="warning" className="p-4">
              <AlertCircle />
              <AlertTitle>Sẽ xử lý trong nền</AlertTitle>
              <AlertDescription>
                <p>
                  Có <strong>{recordCount?.toLocaleString('vi-VN')}</strong> bản ghi.
                </p>
                <p>
                  Dữ liệu vượt ngưỡng xuất trực tiếp. Hệ thống sẽ tạo snapshot, xử lý trong nền và gửi thông báo khi tệp sẵn sàng.
                </p>
              </AlertDescription>
            </Alert>
          ) : (
            <Alert
              variant="success"
              className="flex flex-col items-start gap-3 p-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <CheckCircle2 className="size-4" />
              <div className="space-y-0.5">
                <AlertTitle className="text-xs font-semibold uppercase tracking-wider">
                  Snapshot dữ liệu
                </AlertTitle>
                <AlertDescription className="font-medium">
                  Tổng số bản ghi sẽ xuất ra tệp
                </AlertDescription>
              </div>
              <div className="flex items-center gap-2">
                <Badge
                  variant="outline"
                  className="border-success/30 bg-card px-3 py-1 text-base font-bold text-success"
                >
                  {recordCount} bản ghi
                </Badge>
                {exportMode === 'DIRECT' && (
                  <Badge variant="secondary" className="text-xs font-normal">
                    Trực tiếp
                  </Badge>
                )}
              </div>
            </Alert>
          )}

          {/* Phạm vi bộ lọc đang áp dụng */}
          <div className="space-y-3 rounded-xl border border-border bg-muted/30 p-4">
            <h4 className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              <Info className="size-3.5" />
              Bộ lọc đang áp dụng
            </h4>
            <div className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
              <div className="flex items-center gap-2 text-foreground">
                <Calendar className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-muted-foreground text-xs">Thời gian:</span>
                <span className="font-medium truncate">
                  {filter.startDate || filter.endDate
                    ? `${filter.startDate || '—'} đến ${filter.endDate || '—'}`
                    : 'Toàn bộ thời gian'}
                </span>
              </div>

              <div className="flex items-center gap-2 text-foreground">
                <Layers className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-muted-foreground text-xs">Thao tác:</span>
                <span className="font-medium truncate">
                  {filter.action ? formatActionType(filter.action) : 'Tất cả'}
                </span>
              </div>

              <div className="flex items-center gap-2 text-foreground">
                <User className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-muted-foreground text-xs">Người thực hiện:</span>
                <span className="font-medium truncate">{filter.actorName || 'Tất cả'}</span>
              </div>

              <div className="flex items-center gap-2 text-foreground">
                <ShieldCheck className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-muted-foreground text-xs">Loại đối tượng:</span>
                <span className="font-medium truncate">
                  {filter.objectType ? formatTargetType(filter.objectType) : 'Tất cả'}
                </span>
              </div>
            </div>
          </div>

          {/* Quy chuẩn an toàn dữ liệu */}
          <div className="space-y-1.5 rounded-xl border border-border bg-muted/30 p-3 text-xs text-muted-foreground">
            <div className="flex items-center gap-1.5 font-medium text-foreground">
              <CheckCircle2 className="size-4 shrink-0 text-success" />
              <span>Tiêu chuẩn xuất tệp an toàn (QTN-01, QTN-08)</span>
            </div>
            <ul className="list-inside list-disc space-y-0.5 pl-1">
              <li>Định dạng CSV UTF-8 có BOM tương thích Microsoft Excel.</li>
              <li>Chống tấn công chèn mã công thức (Formula Injection) cho các ô text.</li>
              <li>Tự động ghi sự kiện nhật ký thao tác xuất (<code className="font-mono text-primary">EXPORT_ACTIVITY_LOG</code>).</li>
            </ul>
          </div>
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={exporting}
          >
            Hủy
          </Button>
          <Button
            type="button"
            onClick={handleExport}
            disabled={loadingPreview || !!previewError || hasNoData || exporting}
          >
            {exporting ? (
              <>
                <Loader2 className="size-4 animate-spin" />
                {isAsync ? 'Đang tạo yêu cầu...' : 'Đang tạo tệp CSV...'}
              </>
            ) : (
              <>
                <FileDown className="size-4" />
                {isAsync ? 'Tạo yêu cầu xuất nền' : 'Xuất tệp CSV'}
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
