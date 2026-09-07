import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { AlertTriangle, LoaderCircle, Trash2 } from "lucide-react";
import type {
  DisposeProductionLotRequest,
  ProductionLot,
} from "@/types/productionLot";

interface DisposeLotDialogProps {
  open: boolean;
  lot: ProductionLot | null;
  onClose: () => void;
  /** Gọi API loại bỏ lô; ném lỗi để giữ dialog mở và hiển thị toast lỗi. */
  onDispose: (id: string, payload: DisposeProductionLotRequest) => Promise<void>;
}

/**
 * Dialog loại bỏ lô không đạt kiểm nghiệm (NCL-11-CN-005).
 * Yêu cầu lý do loại bỏ và biện pháp xử lý (bắt buộc).
 */
export const DisposeLotDialog = ({
  open,
  lot,
  onClose,
  onDispose,
}: DisposeLotDialogProps) => {
  const [reason, setReason] = useState("");
  const [handlingMeasure, setHandlingMeasure] = useState("");
  const [note, setNote] = useState("");
  const [reasonError, setReasonError] = useState<string | null>(null);
  const [handlingMeasureError, setHandlingMeasureError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open) {
      setReason("");
      setHandlingMeasure("");
      setNote("");
      setReasonError(null);
      setHandlingMeasureError(null);
      setSubmitting(false);
    }
  }, [open, lot?.id]);

  if (!lot) return null;

  const handleConfirm = async () => {
    const trimmedReason = reason.trim();
    const trimmedHandlingMeasure = handlingMeasure.trim();
    let hasError = false;

    // TC-03: Validation — không được để trống lý do và biện pháp xử lý
    if (!trimmedReason) {
      setReasonError("Vui lòng nhập lý do loại bỏ");
      hasError = true;
    } else {
      setReasonError(null);
    }

    if (!trimmedHandlingMeasure) {
      setHandlingMeasureError("Vui lòng nhập biện pháp xử lý");
      hasError = true;
    } else {
      setHandlingMeasureError(null);
    }

    if (hasError) return;

    setSubmitting(true);
    try {
      await onDispose(lot.id, {
        reason: trimmedReason,
        handlingMeasure: trimmedHandlingMeasure,
        note: note.trim() || undefined,
      });
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
          <DialogTitle className="flex items-center gap-2">
            <Trash2 className="h-5 w-5 text-red-500" />
            Loại bỏ lô sản xuất
          </DialogTitle>
          <DialogDescription>
            Lô sản xuất{" "}
            <span className="font-semibold text-foreground">{lot.name}</span>{" "}
            sẽ chuyển sang trạng thái &quot;Đã loại bỏ&quot; và không thể tạo Lô hàng.
          </DialogDescription>
        </DialogHeader>

        {/* Cảnh báo */}
        <div className="flex items-start gap-2.5 rounded-lg border border-amber-200 bg-amber-50 p-3">
          <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
          <p className="text-xs text-amber-800">
            Thao tác này không thể hoàn tác. Lô đã loại bỏ sẽ không thể tạo Lô hàng
            và không tính vào sản lượng dự kiến.
          </p>
        </div>

        <div className="space-y-4">
          {/* Thông tin lô */}
          <dl className="grid grid-cols-2 gap-x-4 gap-y-2 rounded-lg border p-3 text-sm">
            <div>
              <dt className="text-xs text-muted-foreground">Tên lô</dt>
              <dd className="font-medium">{lot.name}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Nông sản</dt>
              <dd>{lot.productCategoryName || "—"}</dd>
            </div>
          </dl>

          {/* Lý do loại bỏ */}
          <div className="space-y-2">
            <Label htmlFor="dispose-reason">
              Lý do loại bỏ <span className="text-red-500">*</span>
            </Label>
            <Textarea
              id="dispose-reason"
              value={reason}
              onChange={(event) => {
                setReason(event.target.value);
                setReasonError(null);
              }}
              placeholder="Ví dụ: Nông sản bị nhiễm khuẩn, không đạt tiêu chuẩn an toàn thực phẩm..."
              rows={3}
              maxLength={500}
              disabled={submitting}
            />
            {reasonError && (
              <p className="text-sm text-red-500">{reasonError}</p>
            )}
          </div>

          {/* Biện pháp xử lý */}
          <div className="space-y-2">
            <Label htmlFor="handling-measure">
              Biện pháp xử lý <span className="text-red-500">*</span>
            </Label>
            <Textarea
              id="handling-measure"
              value={handlingMeasure}
              onChange={(event) => {
                setHandlingMeasure(event.target.value);
                setHandlingMeasureError(null);
              }}
              placeholder="Ví dụ: Tiêu hủy toàn bộ lô theo quy định, đẩy ra khu vực cách ly..."
              rows={3}
              maxLength={500}
              disabled={submitting}
            />
            {handlingMeasureError && (
              <p className="text-sm text-red-500">{handlingMeasureError}</p>
            )}
          </div>

          {/* Ghi chú bổ sung */}
          <div className="space-y-2">
            <Label htmlFor="dispose-note">Ghi chú bổ sung</Label>
            <Textarea
              id="dispose-note"
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="Thông tin bổ sung (không bắt buộc)..."
              rows={2}
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
            Hủy
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
              <Trash2 className="size-4" />
            )}
            Xác nhận loại bỏ
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
