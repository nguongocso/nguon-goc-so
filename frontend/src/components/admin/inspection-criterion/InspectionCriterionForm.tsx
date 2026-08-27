import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { InspectionCriterionFormContent } from "./InspectionCriterionFormContent";
import type { InspectionCriterion } from "@/types/inspectionCriterion";

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  criterion?: InspectionCriterion | null;
}

/**
 * Modal chỉnh sửa chỉ tiêu kiểm nghiệm.
 * Form logic được chia sẻ với trang tạo mới qua InspectionCriterionFormContent.
 * Modal chỉ dùng cho chức năng SỬA (tạo mới đã chuyển sang trang riêng).
 */
export const InspectionCriterionForm = ({
  open,
  onClose,
  onSuccess,
  criterion,
}: Props) => {
  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {criterion
              ? "Cập nhật chỉ tiêu kiểm nghiệm"
              : "Thêm mới chỉ tiêu kiểm nghiệm"}
          </DialogTitle>
        </DialogHeader>
        <InspectionCriterionFormContent
          open={open}
          criterion={criterion}
          onSuccess={() => {
            onSuccess();
            onClose();
          }}
          onCancel={onClose}
        />
      </DialogContent>
    </Dialog>
  );
};
