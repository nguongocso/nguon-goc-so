import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { CheckCircle2, LoaderCircle, XCircle } from 'lucide-react';
import type { ProductionLot } from '@/types/productionLot';

interface ApproveProductionLotDialogProps {
  open: boolean;
  lot: ProductionLot | null;
  onClose: () => void;
  onDecide: (id: string, approved: boolean, reason?: string) => Promise<void>;
}

const formatDate = (value: string | null) => {
  if (!value) return '—';
  return new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`));
};

export const ApproveProductionLotDialog = ({
  open,
  lot,
  onClose,
  onDecide,
}: ApproveProductionLotDialogProps) => {
  const [mode, setMode] = useState<'review' | 'reject'>('review');
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState<'approve' | 'reject' | null>(null);

  useEffect(() => {
    if (open) {
      setMode('review');
      setReason('');
      setReasonError(null);
      setSubmitting(null);
    }
  }, [open, lot?.id]);

  if (!lot) return null;

  // Kiểm tra sơ bộ các trường bắt buộc trước khi cho duyệt (khớp lỗi 400 của API:
  // "Thiếu thông tin bắt buộc: vùng trồng, sản lượng").
  const missingFields: string[] = [];
  if (!lot.farmAreaId) missingFields.push('Vùng trồng');
  if (!lot.expectedQuantity || lot.expectedQuantity <= 0) missingFields.push('Sản lượng dự kiến');
  if (!lot.productCategoryId) missingFields.push('Loại nông sản');
  const hasMissingFields = missingFields.length > 0;

  const handleApprove = async () => {
    setSubmitting('approve');
    try {
      await onDecide(lot.id, true);
      onClose();
    } finally {
      setSubmitting(null);
    }
  };

  const handleReject = async () => {
    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      setReasonError('Vui lòng nhập lý do từ chối');
      return;
    }
    setSubmitting('reject');
    try {
      await onDecide(lot.id, false, trimmedReason);
      onClose();
    } finally {
      setSubmitting(null);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(next) => !next && !submitting && onClose()}>
      <DialogContent className="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>Duyệt lô sản xuất</DialogTitle>
          <DialogDescription>
            Kiểm tra thông tin lô trước khi duyệt hoặc trả lại.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <dl className="grid grid-cols-2 gap-x-4 gap-y-2 rounded-lg border p-3 text-sm">
            <div>
              <dt className="text-xs text-muted-foreground">Tên lô</dt>
              <dd className="font-medium">{lot.name}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Nông sản</dt>
              <dd>{lot.productCategoryName || '—'}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Vùng trồng</dt>
              <dd>{lot.farmAreaName || '—'}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Sản lượng dự kiến</dt>
              <dd>
                {lot.expectedQuantity} {lot.expectedQuantityUnit}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Ngày xuống giống</dt>
              <dd>{formatDate(lot.plantingDate)}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Người tạo</dt>
              <dd>{lot.createdByName || '—'}</dd>
            </div>
          </dl>

          {hasMissingFields && (
            <Alert className="border-amber-200 bg-amber-50">
              <AlertDescription className="text-sm text-amber-700">
                Lô đang thiếu: {missingFields.join(', ')}. Không thể duyệt cho đến khi hợp tác xã
                bổ sung đầy đủ thông tin.
              </AlertDescription>
            </Alert>
          )}

          {mode === 'reject' && (
            <div className="space-y-2">
              <Label htmlFor="reason">Lý do từ chối *</Label>
              <Textarea
                id="reason"
                value={reason}
                onChange={(event) => {
                  setReason(event.target.value);
                  if (event.target.value.trim()) setReasonError(null);
                }}
                placeholder="Ví dụ: Vui lòng bổ sung nhật ký canh tác"
                rows={3}
              />
              {reasonError && <p className="text-sm text-red-500">{reasonError}</p>}
            </div>
          )}
        </div>

        <DialogFooter>
          {mode === 'review' ? (
            <>
              <Button
                type="button"
                variant="delete"
                onClick={() => setMode('reject')}
                disabled={!!submitting}
              >
                <XCircle className="size-4" />
                Trả lại lô
              </Button>
              <Button
                type="button"
                onClick={handleApprove}
                disabled={!!submitting || hasMissingFields}
                variant="search"
              >
                {submitting === 'approve' ? (
                  <LoaderCircle className="size-4 animate-spin" />
                ) : (
                  <CheckCircle2 className="size-4" />
                )}
                Duyệt lô
              </Button>
            </>
          ) : (
            <>
              <Button
                type="button"
                variant="outline"
                onClick={() => setMode('review')}
                disabled={!!submitting}
              >
                Quay lại
              </Button>
              <Button
                type="button"
                variant="delete"
                onClick={handleReject}
                disabled={!!submitting}
              >
                {submitting === 'reject' && <LoaderCircle className="size-4 animate-spin" />}
                Xác nhận trả lại
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};