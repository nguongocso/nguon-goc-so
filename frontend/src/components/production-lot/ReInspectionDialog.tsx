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
import { AlertTriangle, LoaderCircle, RotateCw } from "lucide-react";
import type { ProductionLot } from "@/types/productionLot";

interface ReInspectionDialogProps {
  open: boolean;
  lot: ProductionLot | null;
  onClose: () => void;
  /** Chuyển hướng đến trang tạo yêu cầu kiểm nghiệm lại. */
  onNavigateToCreateInspection: (lotId: string) => void;
}

/**
 * Dialog kiểm nghiệm lại lô không đạt (NCL-11-CN-005).
 * Hiển thị form nhập biện pháp khắc phục trước khi tạo yêu cầu kiểm nghiệm lại.
 */
export const ReInspectionDialog = ({
  open,
  lot,
  onClose,
  onNavigateToCreateInspection,
}: ReInspectionDialogProps) => {
  const [correctiveAction, setCorrectiveAction] = useState("");
  const [correctiveActionError, setCorrectiveActionError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open) {
      setCorrectiveAction("");
      setCorrectiveActionError(null);
      setSubmitting(false);
    }
  }, [open, lot?.id]);

  if (!lot) return null;

  const handleConfirm = async () => {
    const trimmedAction = correctiveAction.trim();

    // Validation — không được để trống biện pháp khắc phục
    if (!trimmedAction) {
      setCorrectiveActionError("Vui lòng nhập biện pháp khắc phục");
      return;
    }
    setCorrectiveActionError(null);

    setSubmitting(true);
    try {
      // Chuyển hướng đến trang tạo yêu cầu kiểm nghiệm lại
      // Biện pháp khắc phục sẽ đưa vào note của yêu cầu kiểm nghiệm
      onNavigateToCreateInspection(lot.id);
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
            <RotateCw className="h-5 w-5 text-blue-500" />
            Kiểm nghiệm lại
          </DialogTitle>
          <DialogDescription>
            Tạo yêu cầu kiểm nghiệm mới cho lô{" "}
            <span className="font-semibold text-foreground">{lot.name}</span>.
            Khi kiểm nghiệm lại đạt, lô sẽ đủ điều kiện tạo Lô hàng.
          </DialogDescription>
        </DialogHeader>

        {/* Thông tin */}
        <div className="flex items-start gap-2.5 rounded-lg border border-blue-200 bg-blue-50 p-3">
          <AlertTriangle className="h-4 w-4 text-blue-600 shrink-0 mt-0.5" />
          <p className="text-xs text-blue-800">
            Lô sẽ chuyển sang trạng thái &quot;Đang kiểm nghiệm lại&quot;.
            Trong thời gian chờ, không thể tạo Lô hàng.
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

          {/* Biện pháp khắc phục */}
          <div className="space-y-2">
            <Label htmlFor="corrective-action">
              Biện pháp khắc phục <span className="text-red-500">*</span>
            </Label>
            <Textarea
              id="corrective-action"
              value={correctiveAction}
              onChange={(event) => {
                setCorrectiveAction(event.target.value);
                setCorrectiveActionError(null);
              }}
              placeholder="Ví dụ: Đã thay đổi phân bón, điều chỉnh quy trình tưới nước, kiểm soát sâu bệnh tốt hơn..."
              rows={4}
              maxLength={1000}
              disabled={submitting}
            />
            {correctiveActionError && (
              <p className="text-sm text-red-500">{correctiveActionError}</p>
            )}
            <p className="text-xs text-muted-foreground">
              Biện pháp khắc phục sẽ được lưu kèm yêu cầu kiểm nghiệm lại.
            </p>
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
            variant="create"
            onClick={handleConfirm}
            disabled={submitting}
          >
            {submitting ? (
              <LoaderCircle className="size-4 animate-spin" />
            ) : (
              <RotateCw className="size-4" />
            )}
            Tạo yêu cầu kiểm nghiệm lại
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
