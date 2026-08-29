import { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, LoaderCircle, ShieldOff } from 'lucide-react';

import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { getRoleLabel } from '@/config/roleAccess';
import type { DeactivateOutcome } from '@/hooks/useMemberStatusActions';
import type { OrganizationMember } from '@/types/member';

const MAX_REASON_LENGTH = 500;

interface DeactivateMemberDialogProps {
  member: OrganizationMember | null;
  /** Đúng khi hook đang gọi API vô hiệu hóa. */
  deactivating: boolean;
  onClose: () => void;
  onConfirm: (userId: string, reason: string) => Promise<DeactivateOutcome>;
}

/**
 * Dialog vô hiệu hóa thành viên (NCL-01-CN-009, QTN-32).
 *
 * - Giai đoạn "confirm": hiển thị cảnh báo mất quyền + chấm dứt phiên, bắt
 *   buộc nhập lý do.
 * - Khi người dùng bấm **Tiếp tục**, mở thêm modal cảnh báo rủi ro trước
 *   khi gọi API vô hiệu hóa (không còn logic chuyển giao lô — hệ thống chưa
 *   có phân quyền ghi sự kiện theo lô, D-4).
 */
export const DeactivateMemberDialog = ({
  member,
  deactivating,
  onClose,
  onConfirm,
}: DeactivateMemberDialogProps) => {
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState<string | null>(null);
  /** true -> hiện modal cảnh báo rủi ro trước khi gọi API deactivate. */
  const [showWarning, setShowWarning] = useState(false);

  const resetState = useCallback(() => {
    setReason('');
    setReasonError(null);
    setShowWarning(false);
  }, []);

  // Reset toàn bộ state mỗi khi mở dialog với thành viên mới.
  useEffect(() => {
    resetState();
  }, [member, resetState]);

  const handleClose = () => {
    if (deactivating) return;
    resetState();
    onClose();
  };

  const validateReason = () => {
    const trimmed = reason.trim();
    if (!trimmed) {
      setReasonError('Lý do vô hiệu hóa không được để trống.');
      return null;
    }
    if (trimmed.length > MAX_REASON_LENGTH) {
      setReasonError(`Lý do không được vượt quá ${MAX_REASON_LENGTH} ký tự.`);
      return null;
    }
    setReasonError(null);
    return trimmed;
  };

  /** Gọi API vô hiệu hóa thực sự + xử lý outcome trả về. */
  const commitDeactivation = async (trimmedReason: string) => {
    if (!member || deactivating) return;

    const outcome = await onConfirm(member.userId, trimmedReason);

    if (outcome.ok || outcome.fatal) {
      // Thành công, hoặc lỗi không thể xử lý tiếp tại chỗ (403/404/409):
      // toast đã hiển thị → đóng dialog, danh sách sẽ được refresh từ backend.
      handleClose();
    }
  };

  const handleConfirm = () => {
    const trimmed = validateReason();
    if (!trimmed) return;
    setShowWarning(true);
  };

  if (!member) return null;

  return (
    <>
      <AlertDialog
        open={member !== null}
        onOpenChange={(open) => {
          if (!open) handleClose();
        }}
      >
        <AlertDialogPopup>
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2">
              <ShieldOff className="size-5 text-red-600" />
              Vô hiệu hóa thành viên
            </AlertDialogTitle>
                        <AlertDialogDescription>
              <span className="mb-2 block">
                Thao tác này sẽ thu hồi quyền truy cập và chấm dứt phiên làm
                việc của thành viên ngay lập tức.
              </span>
              <span className="block">
                Thành viên có thể được kích hoạt lại sau nếu quay lại làm việc.
                Dữ liệu đã ghi trước đó vẫn được giữ nguyên với tên người ghi cũ.
              </span>
            </AlertDialogDescription>
          </AlertDialogHeader>

          <dl className="mt-4 space-y-0.5">
            <div className="flex items-start justify-between gap-4 py-3">
              <dt className="text-sm text-muted-foreground">Thành viên</dt>
              <dd className="text-right text-sm font-semibold">
                {member.fullName ?? '—'}
              </dd>
            </div>
            <div className="flex items-start justify-between gap-4 py-3">
              <dt className="text-sm text-muted-foreground">Tên đăng nhập</dt>
              <dd className="text-right text-sm font-semibold">
                @{member.username ?? '—'}
              </dd>
            </div>
            <div className="flex items-start justify-between gap-4 py-3">
              <dt className="text-sm text-muted-foreground">Vai trò hiện tại</dt>
              <dd className="text-right text-sm font-semibold">
                {getRoleLabel(member.roleCode ?? undefined)}
              </dd>
            </div>
          </dl>

          <div className="mt-4 space-y-1.5">
            <label
              htmlFor="deactivate-reason"
              className="text-sm font-medium leading-none"
            >
              Lý do vô hiệu hóa <span className="text-red-600">*</span>
            </label>
            <Textarea
              id="deactivate-reason"
              placeholder="VD: Thành viên nghỉ việc từ 01/09/2026"
              value={reason}
              maxLength={MAX_REASON_LENGTH}
              disabled={deactivating}
              onChange={(event) => {
                setReason(event.target.value);
                if (reasonError) setReasonError(null);
              }}
              rows={3}
            />
            {reasonError && (
              <p className="text-sm text-red-600">{reasonError}</p>
            )}
          </div>

          <p className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            Thành viên có thể được kích hoạt lại sau nếu quay lại làm việc.
            Dữ liệu đã ghi trước đó vẫn được giữ nguyên với tên người ghi cũ.
          </p>

          <AlertDialogFooter>
            <AlertDialogCancel disabled={deactivating} onClick={handleClose}>
              Hủy
            </AlertDialogCancel>
            <Button
              type="button"
              variant="delete"
              disabled={deactivating}
              onClick={() => {
                void handleConfirm();
              }}
            >
              {deactivating && (
                <LoaderCircle className="size-4 animate-spin" />
              )}
              {deactivating ? 'Đang xử lý…' : 'Tiếp tục'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogPopup>
      </AlertDialog>

      {/* Modal cảnh báo rủi ro trước khi thực hiện vô hiệu hóa. */}
      <Dialog open={showWarning} onOpenChange={setShowWarning}>
        <DialogPortal>
          <DialogOverlay className="fixed inset-0 bg-black/60" />
        </DialogPortal>
        <DialogContent className="sm:max-w-lg">
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="size-5 text-amber-600" />
            Xác nhận vô hiệu hóa
          </DialogTitle>
          <DialogDescription>
            Khi thực hiện vô hiệu hóa thành viên, thành viên sẽ không còn
            quyền thao tác đến tất cả dữ liệu ghi sự kiện của lô sản xuất. Hãy
            thực hiện rà soát, kiểm tra lại các công việc chưa hoàn thành và
            thay thế người để công việc không bị ảnh hưởng.
          </DialogDescription>
          <div className="flex items-center justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              disabled={deactivating}
              onClick={() => setShowWarning(false)}
            >
              Hủy bỏ
            </Button>
            <Button
              type="button"
              variant="delete"
              disabled={deactivating}
              onClick={() => {
                setShowWarning(false);
                void commitDeactivation(reason.trim());
              }}
            >
              {deactivating && (
                <LoaderCircle className="size-4 animate-spin" />
              )}
              Xác nhận
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
};
