import { useCallback, useEffect, useState } from 'react';
import { LoaderCircle, RotateCcw } from 'lucide-react';

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
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { getRoleLabel } from '@/config/roleAccess';
import type { ReactivateOutcome } from '@/hooks/useMemberStatusActions';
import type { OrganizationMember } from '@/types/member';

const MAX_REASON_LENGTH = 500;

interface ReactivateMemberDialogProps {
  member: OrganizationMember | null;
  /** Đúng khi hook đang gọi API kích hoạt lại. */
  reactivating: boolean;
  onClose: () => void;
  onConfirm: (userId: string, reason: string) => Promise<ReactivateOutcome>;
}

/**
 * Dialog kích hoạt lại thành viên đã ngừng hoạt động (NCL-01-CN-009,
 * QTN-32 mục 9): bắt buộc nhập lý do. Vai trò cũ được giữ nguyên theo
 * backend — UI không tự suy đoán quyền mới.
 */
export const ReactivateMemberDialog = ({
  member,
  reactivating,
  onClose,
  onConfirm,
}: ReactivateMemberDialogProps) => {
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState<string | null>(null);

  const resetState = useCallback(() => {
    setReason('');
    setReasonError(null);
  }, []);

  useEffect(() => {
    resetState();
  }, [member, resetState]);

  const handleClose = () => {
    if (reactivating) return;
    resetState();
    onClose();
  };

  const handleConfirm = async () => {
    if (!member || reactivating) return;

    const trimmed = reason.trim();
    if (!trimmed) {
      setReasonError('Lý do kích hoạt lại không được để trống.');
      return;
    }
    if (trimmed.length > MAX_REASON_LENGTH) {
      setReasonError(`Lý do không được vượt quá ${MAX_REASON_LENGTH} ký tự.`);
      return;
    }
    setReasonError(null);

    const outcome = await onConfirm(member.userId, trimmed);

    if (outcome.ok || outcome.fatal) {
      // Thành công hoặc lỗi không thể xử lý tại chỗ (403/404/409):
      // toast đã hiển thị, đóng dialog và refresh danh sách từ backend.
      handleClose();
    }
  };

  return (
    <AlertDialog
      open={member !== null}
      onOpenChange={(open) => {
        if (!open) handleClose();
      }}
    >
      <AlertDialogPopup className="max-w-lg">
        <AlertDialogHeader>
          <div className="mb-2 flex size-11 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
            <RotateCcw className="size-6" />
          </div>
          <AlertDialogTitle>Kích hoạt lại thành viên</AlertDialogTitle>
          <AlertDialogDescription>
            Thành viên sẽ được khôi phục quyền trong tổ chức với vai trò cũ và
            có thể tiếp tục đăng nhập, ghi dữ liệu như trước.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <dl className="mt-5 divide-y rounded-lg border bg-slate-50 px-4">
          <div className="flex items-start justify-between gap-4 py-3">
            <dt className="text-sm text-muted-foreground">Thành viên</dt>
            <dd className="text-right text-sm font-semibold">
              {member?.fullName ?? '—'}
            </dd>
          </div>
          <div className="flex items-start justify-between gap-4 py-3">
            <dt className="text-sm text-muted-foreground">Tên đăng nhập</dt>
            <dd className="text-right text-sm font-semibold">
              @{member?.username ?? '—'}
            </dd>
          </div>
          <div className="flex items-start justify-between gap-4 py-3">
            <dt className="text-sm text-muted-foreground">
              Vai trò (được giữ lại)
            </dt>
            <dd className="text-right text-sm font-semibold">
              {getRoleLabel(member?.roleCode ?? undefined)}
            </dd>
          </div>
        </dl>

        <div className="mt-4 space-y-1.5">
          <Label htmlFor="reactivate-reason">
            Lý do kích hoạt lại <span className="text-red-600">*</span>
          </Label>
          <Textarea
            id="reactivate-reason"
            placeholder="VD: Thành viên quay lại làm việc từ 01/10/2026"
            value={reason}
            maxLength={MAX_REASON_LENGTH}
            disabled={reactivating}
            onChange={(event) => {
              setReason(event.target.value);
              if (reasonError) setReasonError(null);
            }}
            rows={3}
          />
          {reasonError && <p className="text-sm text-red-600">{reasonError}</p>}
        </div>

        <p className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Các phân công lô cũ không được khôi phục tự động. Nếu cần giao lại
          lô, hãy thực hiện phân công riêng sau khi kích hoạt.
        </p>

        <AlertDialogFooter>
          <AlertDialogCancel disabled={reactivating} onClick={handleClose}>
            Hủy
          </AlertDialogCancel>
          <Button
            type="button"
            variant="create"
            disabled={reactivating}
            onClick={() => {
              void handleConfirm();
            }}
          >
            {reactivating && <LoaderCircle className="size-4 animate-spin" />}
            {reactivating ? 'Đang xử lý...' : 'Xác nhận kích hoạt lại'}
          </Button>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};
