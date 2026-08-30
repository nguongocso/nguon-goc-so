import { useState } from 'react';
import { unlockTraceCode } from '@/api/suspectTraceCodeApi';
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
import { AlertTriangle, CheckCircle2, Clock, Unlock, User } from 'lucide-react';
import { toast } from 'sonner';

interface UnlockTraceCodeDialogProps {
  traceCode: SuspectTraceCodeResponse | null;
  currentUserId?: string;
  onClose: () => void;
  onSuccess: () => void;
}

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return '—';
  return new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export function UnlockTraceCodeDialog({
  traceCode,
  currentUserId,
  onClose,
  onSuccess,
}: UnlockTraceCodeDialogProps) {
  const [conclusion, setConclusion] = useState('');
  const [evidence, setEvidence] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const isSameAdmin = Boolean(
    currentUserId && traceCode?.lockedBy && currentUserId === traceCode.lockedBy
  );

  const minChars = isSameAdmin ? 20 : 10;
  const trimmedConclusion = conclusion.trim();
  const isEmpty = !trimmedConclusion;
  const isTooShort = trimmedConclusion.length > 0 && trimmedConclusion.length < minChars;
  const isTooLong = trimmedConclusion.length > 500;
  const isEvidenceTooLong = evidence.trim().length > 500;

  const isValid = !isEmpty && !isTooShort && !isTooLong && !isEvidenceTooLong;

  const handleSubmit = async () => {
    if (!traceCode || !isValid) return;

    try {
      setSubmitting(true);
      await unlockTraceCode(traceCode.id, {
        conclusion: trimmedConclusion,
        evidence: evidence.trim() ? evidence.trim() : undefined,
      });
      toast.success(`Đã mở khóa mã tem ${traceCode.codeValue}`);
      setConclusion('');
      setEvidence('');
      onSuccess();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || 'Không thể mở khóa mã tem',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={!!traceCode} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-emerald-700">
            <Unlock className="h-5 w-5" />
            Mở khóa mã tem sau xác minh
          </DialogTitle>
          <DialogDescription>
            Khôi phục trạng thái hoạt động của mã tem, gỡ cảnh báo trên trang tra cứu công khai và gửi thông báo đến Hợp tác xã sở hữu.
          </DialogDescription>
        </DialogHeader>

        {traceCode && (
          <div className="space-y-4">
            <div className="rounded-lg border bg-muted/30 p-3 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Mã tem:</span>
                <span className="font-mono font-bold text-slate-800">{traceCode.codeValue}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Lô hàng:</span>
                <span className="font-medium text-slate-800">{traceCode.shipmentName || '—'}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground flex items-center gap-1">
                  <User className="h-3.5 w-3.5" /> Người khóa:
                </span>
                <span className="font-medium">{traceCode.lockedByName || traceCode.lockedBy || 'Quản trị viên'}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground flex items-center gap-1">
                  <Clock className="h-3.5 w-3.5" /> Thời điểm khóa:
                </span>
                <span>{formatDateTime(traceCode.lockedAt)}</span>
              </div>
              {traceCode.lockReason && (
                <div className="rounded border border-red-200 bg-red-50/70 p-2 text-xs text-red-700">
                  <span className="font-semibold">Lý do khóa: </span>
                  <span>{traceCode.lockReason}</span>
                </div>
              )}
            </div>

            {isSameAdmin && (
              <div className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 p-2.5 text-xs text-amber-800">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
                <span>
                  Bạn là người đã khóa mã tem này. Vui lòng nhập kết luận xác minh chi tiết (tối thiểu 20 ký tự).
                </span>
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="unlockConclusion" className="required font-medium text-slate-800">
                Kết luận xác minh *
              </Label>
              <Textarea
                id="unlockConclusion"
                placeholder={`Nhập kết luận xác minh (tối thiểu ${minChars} ký tự)`}
                value={conclusion}
                onChange={(e) => setConclusion(e.target.value)}
                rows={3}
                maxLength={500}
              />
              <div className="flex justify-between text-xs">
                <span>
                  {isEmpty && <span className="text-destructive">Vui lòng nhập kết luận xác minh</span>}
                  {isTooShort && (
                    <span className="text-destructive">
                      Tối thiểu {minChars} ký tự (hiện có {trimmedConclusion.length})
                    </span>
                  )}
                  {isTooLong && <span className="text-destructive">Tối đa 500 ký tự</span>}
                  {isValid && (
                    <span className="text-emerald-600 flex items-center gap-1">
                      <CheckCircle2 className="h-3.5 w-3.5" /> Hợp lệ
                    </span>
                  )}
                </span>
                <span className="text-muted-foreground">{trimmedConclusion.length}/500</span>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="unlockEvidence" className="font-medium text-slate-800">
                Bằng chứng xác minh (tùy chọn)
              </Label>
              <Textarea
                id="unlockEvidence"
                placeholder="Nhập thông tin chứng từ, biên bản xác minh, ghi chú..."
                value={evidence}
                onChange={(e) => setEvidence(e.target.value)}
                rows={2}
                maxLength={500}
              />
              <div className="flex justify-end text-xs text-muted-foreground">
                {evidence.trim().length}/500
              </div>
            </div>
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={submitting}>
            Hủy
          </Button>
          <Button
            className="bg-emerald-600 hover:bg-emerald-700 text-white"
            onClick={handleSubmit}
            disabled={!isValid || submitting}
          >
            <Unlock className="mr-2 h-4 w-4" />
            {submitting ? 'Đang xử lý...' : 'Xác nhận mở khóa'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
