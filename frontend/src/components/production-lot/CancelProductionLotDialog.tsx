import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Ban, LoaderCircle } from 'lucide-react';
import type {
  CancelProductionLotRequest,
  ProductionLot,
} from '@/types/productionLot';

/**
 * Các trạng thái được phép hủy — mọi lô chưa sinh mã truy xuất
 * và chưa ở trạng thái cuối (NCL-02-CN-006).
 *
 * Theo tài liệu gốc ("Bản sao của Bản sao của Nguồn Gốc Số.xlsx", ROW 77,
 * cột mô tả I77 / precondition J77): điều kiện DUY NHẤT để hủy là "chưa sinh
 * mã truy xuất", không giới hạn trạng thái — lô đã đóng gói (PACKAGED) nhưng
 * chưa tạo lô hàng vẫn được hủy. Chỉ chặn trạng thái cuối CANCELLED /
 * CLOSED / RECALLED (thực tế đều đã sinh mã, backend tự chặn qua gate mã).
 */
export const CANCELLABLE_PRODUCTION_LOT_STATUSES: ProductionLot['status'][] = [
  'DRAFT',
  'PENDING',
  'REJECTED',
  'APPROVED',
  'HARVESTED',
  'PREPROCESSED',
  'PACKAGED',
];

const CANCEL_REASONS = [
  'Mất mùa do thời tiết',
  'Sâu bệnh',
  'Khai báo nhầm',
  'Lý do khác',
] as const;

interface CancelProductionLotDialogProps {
  open: boolean;
  lot: ProductionLot | null;
  onClose: () => void;
  /** Gọi API hủy lô; ném lỗi để giữ dialog mở và hiển thị toast lỗi. */
  onCancel: (id: string, payload: CancelProductionLotRequest) => Promise<void>;
}

export const CancelProductionLotDialog = ({
  open,
  lot,
  onClose,
  onCancel,
}: CancelProductionLotDialogProps) => {
  const [reason, setReason] = useState('');
  const [note, setNote] = useState('');
  const [reasonError, setReasonError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open) {
      setReason('');
      setNote('');
      setReasonError(null);
      setSubmitting(false);
    }
  }, [open, lot?.id]);

  if (!lot) return null;

  const handleConfirm = async () => {
    const trimmedReason = reason.trim();
    const trimmedNote = note.trim();

    // TC-03: chỉ LÝ DO HỦY là bắt buộc; "Tại sao?" (diễn giải) không bắt buộc
    // (quyết định người dùng 2026-09-04).
    if (!trimmedReason) {
      setReasonError('Vui lòng chọn lý do hủy');
      return;
    }
    setReasonError(null);

    setSubmitting(true);
    try {
      await onCancel(lot.id, { reason: trimmedReason, note: trimmedNote });
      onClose();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => !next && !submitting && onClose()}
    >
      <DialogContent className="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>Hủy lô sản xuất</DialogTitle>
          <DialogDescription>
            Lô sẽ chuyển sang trạng thái{' '}
            <span className="font-semibold">Đã hủy</span> và không thể ghi thêm
            sự kiện hay nhật ký canh tác. Nhật ký cũ vẫn được xem ở chế độ chỉ
            đọc.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <dl className="grid grid-cols-2 gap-x-4 gap-y-2 rounded-lg border p-3 text-sm">
            <div>
              <dt className="text-xs text-muted-foreground">Tên lô</dt>
              <dd className="font-medium">{lot.name}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Nông sản</dt>
              <dd>{lot.productCategoryName || '—'}</dd>
            </div>
          </dl>

          <div className="space-y-2">
            <Label htmlFor="cancel-reason">Lý do hủy *</Label>
            <Select
              value={reason}
              onValueChange={(value) => {
                setReason(value ?? '');
                setReasonError(null);
              }}
              disabled={submitting}
            >
              <SelectTrigger id="cancel-reason" className="w-full">
                <SelectValue placeholder="Chọn lý do hủy" />
              </SelectTrigger>
              <SelectContent>
                {CANCEL_REASONS.map((item) => (
                  <SelectItem key={item} value={item}>
                    {item}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {reasonError && <p className="text-sm text-red-500">{reasonError}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="cancel-note">Tại sao?</Label>
            <Textarea
              id="cancel-note"
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="Mô tả chi tiết lý do hủy lô (không bắt buộc)..."
              rows={4}
              maxLength={1000}
              disabled={submitting}
            />
          </div>
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={submitting}
          >
            Đóng
          </Button>
          <Button
            type="button"
            variant="delete"
            onClick={handleConfirm}
            disabled={submitting}
          >
            {submitting ? (
              <LoaderCircle className="size-4 animate-spin" />
            ) : (
              <Ban className="size-4" />
            )}
            Xác nhận hủy lô
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};