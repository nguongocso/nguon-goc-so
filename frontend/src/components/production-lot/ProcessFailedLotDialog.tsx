import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { ProductionLot } from "@/types/productionLot";
import { AlertTriangle, Trash2, RotateCw } from "lucide-react";

interface ProcessFailedLotDialogProps {
  open: boolean;
  lot: ProductionLot | null;
  onClose: () => void;
  onSelectDispose: () => void;
  onSelectReInspection: () => void;
}

/**
 * Dialog xử lý lô không đạt kiểm nghiệm (NCL-11-CN-005).
 * Hiển thị hai lựa chọn: Loại bỏ lô / Kiểm nghiệm lại.
 */
export const ProcessFailedLotDialog = ({
  open,
  lot,
  onClose,
  onSelectDispose,
  onSelectReInspection,
}: ProcessFailedLotDialogProps) => {
  if (!lot) return null;

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-amber-500" />
            Xử lý lô không đạt
          </DialogTitle>
          <DialogDescription>
            Lô sản xuất{" "}
            <span className="font-semibold text-foreground">{lot.name}</span>{" "}
            có kết quả kiểm nghiệm <span className="font-semibold text-red-600">Không đạt</span>.
            Vui lòng chọn hướng xử lý.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3 py-4">
          {/* Cảnh báo */}
          <div className="rounded-lg border border-amber-200 bg-amber-50 p-3">
            <p className="text-sm text-amber-800">
              <strong>Lưu ý:</strong> Lô này chưa đủ điều kiện để tạo Lô hàng.
              Bạn cần xử lý theo một trong hai hướng bên dưới.
            </p>
          </div>

          {/* Lựa chọn 1: Loại bỏ lô */}
          <button
            type="button"
            onClick={onSelectDispose}
            className="w-full rounded-lg border border-red-200 bg-red-50 p-4 text-left transition-colors hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
          >
            <div className="flex items-start gap-3">
              <Trash2 className="h-5 w-5 text-red-600 mt-0.5 shrink-0" />
              <div>
                <p className="font-medium text-red-800">Loại bỏ lô</p>
                <p className="mt-1 text-sm text-red-700">
                  Lô sẽ chuyển sang trạng thái &quot;Đã loại bỏ&quot; và không thể
                  tạo Lô hàng. Cần ghi lý do và biện pháp xử lý.
                </p>
              </div>
            </div>
          </button>

          {/* Lựa chọn 2: Kiểm nghiệm lại */}
          <button
            type="button"
            onClick={onSelectReInspection}
            className="w-full rounded-lg border border-blue-200 bg-blue-50 p-4 text-left transition-colors hover:bg-blue-100 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            <div className="flex items-start gap-3">
              <RotateCw className="h-5 w-5 text-blue-600 mt-0.5 shrink-0" />
              <div>
                <p className="font-medium text-blue-800">Kiểm nghiệm lại</p>
                <p className="mt-1 text-sm text-blue-700">
                  Tạo yêu cầu kiểm nghiệm mới cho lô. Khi kiểm nghiệm lại đạt,
                  lô sẽ đủ điều kiện tạo Lô hàng.
                </p>
              </div>
            </div>
          </button>
        </div>

        <div className="flex justify-end pt-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-md border border-input bg-background px-4 py-2 text-sm font-medium ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          >
            Hủy
          </button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
