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
import {
  AlertCircle,
  Calendar,
  CheckCircle2,
  Download,
  FileSpreadsheet,
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
      <DialogContent className="max-w-lg p-0 overflow-hidden sm:max-w-xl">
        <DialogHeader className="p-6 pb-4 border-b border-border bg-card">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
              <FileSpreadsheet className="w-5 h-5" />
            </div>
            <div>
              <DialogTitle className="text-xl font-bold text-foreground">
                Xuất nhật ký hoạt động
              </DialogTitle>
              <DialogDescription className="text-sm text-muted-foreground mt-0.5">
                Xem trước số lượng và tải tệp CSV snapshot phục vụ kiểm tra
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
          {/* Card trạng thái số lượng bản ghi */}
          {loadingPreview ? (
            <div className="flex flex-col items-center justify-center p-6 border border-slate-200 rounded-xl bg-slate-50 text-muted-foreground gap-2">
              <Loader2 className="w-6 h-6 animate-spin text-emerald-600" />
              <span className="text-sm font-medium">Đang tính toán số lượng bản ghi...</span>
            </div>
          ) : previewError ? (
            <div className="flex items-start gap-3 p-4 border border-rose-200 rounded-xl bg-rose-50 text-rose-800">
              <AlertCircle className="w-5 h-5 mt-0.5 shrink-0 text-rose-600" />
              <div className="text-sm">
                <p className="font-semibold">Lỗi xem trước số lượng</p>
                <p className="mt-0.5 text-rose-700">{previewError}</p>
              </div>
            </div>
          ) : hasNoData ? (
            <div className="flex items-start gap-3 p-4 border border-amber-200 rounded-xl bg-amber-50 text-amber-900">
              <AlertCircle className="w-5 h-5 mt-0.5 shrink-0 text-amber-600" />
              <div className="text-sm">
                <p className="font-semibold">Không có bản ghi phù hợp</p>
                <p className="mt-0.5 text-amber-700">
                  Không tìm thấy nhật ký hoạt động nào trong phạm vi bộ lọc đang chọn. Bạn cần điều
                  chỉnh lại điều kiện lọc trước khi xuất.
                </p>
              </div>
            </div>
          ) : isAsync ? (
            <div className="flex items-start gap-3 p-4 border border-amber-200 rounded-xl bg-amber-50 text-amber-900">
              <AlertCircle className="w-5 h-5 mt-0.5 shrink-0 text-amber-600" />
              <div className="text-sm">
                <p className="font-semibold">Sẽ xử lý trong nền</p>
                <p className="mt-1 text-amber-800">
                  Có <strong>{recordCount?.toLocaleString('vi-VN')}</strong> bản ghi.
                </p>
                <p className="mt-0.5 text-amber-800">
                  Dữ liệu vượt ngưỡng xuất trực tiếp. Hệ thống sẽ tạo snapshot, xử lý trong nền và gửi thông báo khi tệp sẵn sàng.
                </p>
              </div>
            </div>
          ) : (
            <div className="flex items-center justify-between p-4 border border-emerald-200 rounded-xl bg-emerald-50/70 text-emerald-950">
              <div className="space-y-0.5">
                <span className="text-xs font-semibold uppercase tracking-wider text-emerald-700">
                  Snapshot dữ liệu
                </span>
                <p className="text-sm font-medium text-emerald-900">
                  Tổng số bản ghi sẽ xuất ra tệp
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Badge
                  variant="outline"
                  className="bg-white text-emerald-800 border-emerald-300 text-base font-bold px-3.5 py-1 shadow-sm"
                >
                  {recordCount} bản ghi
                </Badge>
                {exportMode === 'DIRECT' && (
                  <Badge variant="secondary" className="text-xs text-slate-600 font-normal">
                    Trực tiếp
                  </Badge>
                )}
              </div>
            </div>
          )}

          {/* Phạm vi bộ lọc đang áp dụng */}
          <div className="border border-border rounded-xl p-4 bg-slate-50/50 space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
              <Info className="w-3.5 h-3.5" />
              Bộ lọc đang áp dụng
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-sm">
              <div className="flex items-center gap-2 text-slate-700">
                <Calendar className="w-4 h-4 text-slate-400 shrink-0" />
                <span className="text-muted-foreground text-xs">Thời gian:</span>
                <span className="font-medium truncate">
                  {filter.startDate || filter.endDate
                    ? `${filter.startDate || '—'} đến ${filter.endDate || '—'}`
                    : 'Toàn bộ thời gian'}
                </span>
              </div>

              <div className="flex items-center gap-2 text-slate-700">
                <Layers className="w-4 h-4 text-slate-400 shrink-0" />
                <span className="text-muted-foreground text-xs">Thao tác:</span>
                <span className="font-medium truncate">
                  {filter.action ? formatActionType(filter.action) : 'Tất cả'}
                </span>
              </div>

              <div className="flex items-center gap-2 text-slate-700">
                <User className="w-4 h-4 text-slate-400 shrink-0" />
                <span className="text-muted-foreground text-xs">Người thực hiện:</span>
                <span className="font-medium truncate">{filter.actorName || 'Tất cả'}</span>
              </div>

              <div className="flex items-center gap-2 text-slate-700">
                <ShieldCheck className="w-4 h-4 text-slate-400 shrink-0" />
                <span className="text-muted-foreground text-xs">Loại đối tượng:</span>
                <span className="font-medium truncate">
                  {filter.objectType ? formatTargetType(filter.objectType) : 'Tất cả'}
                </span>
              </div>
            </div>
          </div>

          {/* Quy chuẩn an toàn dữ liệu */}
          <div className="p-3.5 rounded-xl border border-slate-200 bg-white text-xs text-muted-foreground space-y-1.5">
            <div className="flex items-center gap-1.5 font-medium text-slate-800">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>Tiêu chuẩn xuất tệp an toàn (QTN-01, QTN-08)</span>
            </div>
            <ul className="list-disc list-inside space-y-0.5 text-slate-600 pl-1">
              <li>Định dạng CSV UTF-8 có BOM tương thích Microsoft Excel.</li>
              <li>Chống tấn công chèn mã công thức (Formula Injection) cho các ô text.</li>
              <li>Tự động ghi sự kiện nhật ký thao tác xuất (<code className="text-emerald-700 font-mono">EXPORT_ACTIVITY_LOG</code>).</li>
            </ul>
          </div>
        </div>

        <DialogFooter className="p-4 border-t border-border bg-slate-50 flex items-center justify-end gap-2">
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
            className="bg-emerald-700 hover:bg-emerald-800 text-white gap-2 font-medium"
            onClick={handleExport}
            disabled={loadingPreview || !!previewError || hasNoData || exporting}
          >
            {exporting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                {isAsync ? 'Đang tạo yêu cầu...' : 'Đang tạo tệp CSV...'}
              </>
            ) : (
              <>
                <Download className="w-4 h-4" />
                {isAsync ? 'Tạo yêu cầu xuất nền' : 'Tải tệp CSV'}
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
