import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import type { TestingUnit } from "@/types/certification";

interface DeactivateTestingUnitDialogProps {
  /** Đơn vị đang chờ xác nhận ngừng hoạt động (null nghĩa là đóng hộp thoại). */
  target: TestingUnit | null;
  /** Đang gửi yêu cầu ngừng hoạt động hay không. */
  submitting: boolean;
  /** Gọi khi người dùng huỷ hoặc đóng hộp thoại. */
  onClose: () => void;
  /** Gọi khi người dùng xác nhận ngừng hoạt động. */
  onConfirm: () => void;
}

/**
 * Hộp thoại xác nhận ngừng hoạt động đơn vị kiểm nghiệm.
 * Nhắc người dùng hệ quả: đơn vị sẽ không còn xuất hiện khi tạo yêu cầu kiểm nghiệm.
 */
export const DeactivateTestingUnitDialog = ({
  target,
  submitting,
  onClose,
  onConfirm,
}: DeactivateTestingUnitDialogProps) => {
  return (
    <AlertDialog open={!!target} onOpenChange={(open) => !open && onClose()}>
      <AlertDialogPopup>
        <AlertDialogHeader>
          <AlertDialogTitle>
            Ngừng hoạt động đơn vị kiểm nghiệm
          </AlertDialogTitle>
          <AlertDialogDescription>
            Bạn có chắc muốn ngừng hoạt động đơn vị <strong>{target?.name}</strong>{" "}
            không? Đơn vị sẽ không còn xuất hiện trong danh sách lựa chọn khi tạo
            yêu cầu kiểm nghiệm.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={onClose} disabled={submitting}>
            Hủy
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={onConfirm}
            disabled={submitting}
            className="bg-red-600 hover:bg-red-700"
          >
            {submitting ? "Đang xử lý..." : "Ngừng hoạt động"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};
