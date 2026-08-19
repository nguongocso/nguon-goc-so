import { useState } from 'react';
import { lockTraceCode } from '@/api/suspectTraceCodeApi';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import type { SuspectTraceCodeResponse } from '@/types/suspectTraceCode';
import { AlertTriangle, Lock } from 'lucide-react';
import { toast } from 'sonner';

interface LockTraceCodeDialogProps {
  traceCode: SuspectTraceCodeResponse | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function LockTraceCodeDialog({
  traceCode,
  onClose,
  onSuccess,
}: LockTraceCodeDialogProps) {
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const isEmpty = !reason.trim();
  const isTooShort = reason.trim().length > 0 && reason.trim().length < 10;
  const isTooLong = reason.trim().length > 500;

  const isValid = !isEmpty && !isTooShort && !isTooLong;

  const handleSubmit = async () => {
    if (!traceCode || !isValid) return;

    try {
      setSubmitting(true);
      await lockTraceCode(traceCode.id, { reason: reason.trim() });
      toast.success(`Đã khóa mã tem ${traceCode.codeValue}`);
      setReason('');
      onSuccess();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || 'Không thể khóa mã tem',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={!!traceCode} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-destructive">
            <Lock className="h-5 w-5" />
            Khóa mã tem nghi vấn
          </DialogTitle>
          <DialogDescription>
            Hành động này sẽ chặn mã tem, người tiêu dùng quét mã sẽ thấy cảnh báo
            thay vì hành trình bình thường.
          </DialogDescription>
        </DialogHeader>

        {traceCode && (
          <div className="space-y-4">
            <div className="rounded-lg border bg-muted/30 p-3 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Mã tem:</span>
                <span className="font-mono font-medium">{traceCode.codeValue}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Lô hàng:</span>
                <span className="font-medium">{traceCode.shipmentName}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Điểm nghi vấn:</span>
                <span className="font-bold text-red-600">{traceCode.suspicionScore}</span>
              </div>
              {traceCode.suspicionReason && (
                <div className="flex items-start gap-2 text-sm text-amber-700">
                  <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
                  <span>{traceCode.suspicionReason}</span>
                </div>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="lockReason" className="required">
                Lý do khóa
              </Label>
              <Textarea
                id="lockReason"
                placeholder="Nhập lý do khóa mã tem (tối thiểu 10 ký tự)"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={4}
                maxLength={500}
              />
              <div className="flex justify-between text-xs">
                <span>
                  {isEmpty && (
                    <span className="text-destructive">Vui lòng nhập lý do khóa</span>
                  )}
                  {isTooShort && (
                    <span className="text-destructive">
                      Tối thiểu 10 ký tự (hiện có {reason.trim().length})
                    </span>
                  )}
                  {isTooLong && (
                    <span className="text-destructive">Tối đa 500 ký tự</span>
                  )}
                  {isValid && (
                    <span className="text-emerald-600">Hợp lệ</span>
                  )}
                </span>
                <span className="text-muted-foreground">
                  {reason.trim().length}/500
                </span>
              </div>
            </div>
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={submitting}>
            Hủy
          </Button>
          <Button
            variant="destructive"
            onClick={handleSubmit}
            disabled={!isValid || submitting}
          >
            <Lock className="mr-2 h-4 w-4" />
            {submitting ? 'Đang xử lý...' : 'Xác nhận khóa'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}