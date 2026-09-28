import React from 'react';
import { AlertTriangle, Check, RefreshCw, X } from 'lucide-react';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import type { AreaDeviationErrorData } from '@/types/farmArea';

/** Thuộc tính của hộp thoại xác nhận chênh lệch diện tích. */
interface AreaDeviationConfirmDialogProps {
  open: boolean;
  data: AreaDeviationErrorData | null;
  onConfirm: () => void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export const AreaDeviationConfirmDialog: React.FC<AreaDeviationConfirmDialogProps> = ({
  open,
  data,
  onConfirm,
  onCancel,
  isSubmitting = false,
}) => {
  if (!data) return null;

  return (
    <AlertDialog open={open} onOpenChange={(nextOpen) => !nextOpen && onCancel()}>
      <AlertDialogContent className="max-w-lg border-amber-200 bg-white p-6 shadow-xl dark:border-amber-900/50 dark:bg-card">
        <AlertDialogHeader>
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-600 dark:bg-amber-950/60 dark:text-amber-400">
              <AlertTriangle className="size-5" aria-hidden="true" />
            </div>
            <div>
              <AlertDialogTitle className="text-lg font-semibold text-slate-900 dark:text-foreground">
                Xác nhận chênh lệch diện tích
              </AlertDialogTitle>
              <AlertDialogDescription className="text-xs text-muted-foreground">
                Quy tắc nghiệp vụ kiểm tra ranh giới WGS84 (TC-03)
              </AlertDialogDescription>
            </div>
          </div>
        </AlertDialogHeader>

        <div className="mt-4 space-y-4">
          <p className="text-sm text-slate-700 dark:text-slate-300">
            Diện tích tính từ đa giác ranh giới lệch hơn{' '}
            <strong className="text-amber-600 dark:text-amber-400">
              {data.thresholdPercentage}%
            </strong>{' '}
            so với diện tích khai báo ban đầu. Vui lòng đối chiếu số liệu trước khi lưu:
          </p>

          <div className="grid grid-cols-1 gap-2.5 rounded-xl border border-slate-200 bg-slate-50/70 p-3.5 text-sm sm:grid-cols-2 dark:border-border dark:bg-muted/30">
            <div>
              <span className="text-xs text-muted-foreground">Diện tích khai báo:</span>
              <p className="font-semibold text-slate-900 dark:text-foreground">
                {Number(data.declaredArea).toFixed(4)} ha
              </p>
            </div>

            <div>
              <span className="text-xs text-muted-foreground">Diện tích ranh giới:</span>
              <p className="font-semibold text-emerald-600 dark:text-emerald-400">
                {Number(data.calculatedArea).toFixed(4)} ha
              </p>
            </div>

            <div>
              <span className="text-xs text-muted-foreground">Tỷ lệ chênh lệch:</span>
              <p className="font-bold text-amber-600 dark:text-amber-400">
                {Number(data.deviationPercentage).toFixed(2)}%
              </p>
            </div>

            <div>
              <span className="text-xs text-muted-foreground">Ngưỡng cảnh báo:</span>
              <p className="font-medium text-slate-700 dark:text-slate-300">
                &gt; {data.thresholdPercentage}%
              </p>
            </div>
          </div>

          <div className="rounded-lg bg-amber-50/80 p-3 text-xs leading-relaxed text-amber-800 dark:bg-amber-950/40 dark:text-amber-300">
            <strong>Lưu ý:</strong> Sau khi xác nhận, ranh giới và diện tích tính toán mới sẽ được
            lưu chính thức vào hệ thống và ghi nhận phiên bản trong nhật ký hoạt động.
          </div>
        </div>

        <AlertDialogFooter className="mt-6 flex gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={isSubmitting}
            className="flex items-center gap-1.5"
          >
            <X className="size-4" aria-hidden="true" />
            Quay lại chỉnh sửa
          </Button>

          <Button
            type="button"
            variant="default"
            onClick={onConfirm}
            disabled={isSubmitting}
            className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white"
          >
            {isSubmitting ? (
              <>
                <RefreshCw className="size-4 animate-spin" aria-hidden="true" />
                Đang lưu...
              </>
            ) : (
              <>
                <Check className="size-4" aria-hidden="true" />
                Tôi hiểu và đồng ý lưu
              </>
            )}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
};
