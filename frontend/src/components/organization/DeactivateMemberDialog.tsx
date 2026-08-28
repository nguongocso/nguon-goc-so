import { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, LoaderCircle, RotateCcw, ShieldOff } from 'lucide-react';

import { getReplacementCandidates } from '@/api/memberApi';
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Textarea } from '@/components/ui/textarea';
import { getRoleLabel } from '@/config/roleAccess';
import type { DeactivateOutcome } from '@/hooks/useMemberStatusActions';
import type {
  MemberLotSummary,
  OrganizationMember,
  ReplacementCandidate,
} from '@/types/member';

const MAX_REASON_LENGTH = 500;

/** Giai đoạn của dialog: xác nhận lý do hoặc chọn người thay thế sau khi 409. */
type DeactivatePhase = 'confirm' | 'replacement';

// Ánh xạ trạng thái lô sản xuất sang tiếng Việt (chỉ các trạng thái
// "chưa hoàn thành" theo quyết định D-4 có thể xuất hiện ở đây).
const LOT_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Bản nháp',
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  HARVESTED: 'Đã thu hoạch',
  PREPROCESSED: 'Đã sơ chế',
  PACKAGED: 'Đã đóng gói',
};

const getLotStatusLabel = (status: string) => LOT_STATUS_LABELS[status] ?? status;

const getLotStatusBadgeClass = (status: string) => {
  const classNames: Record<string, string> = {
    DRAFT: 'bg-slate-100 text-slate-700',
    PENDING: 'bg-yellow-100 text-yellow-800',
    APPROVED: 'bg-emerald-100 text-emerald-800',
    HARVESTED: 'bg-lime-100 text-lime-800',
    PREPROCESSED: 'bg-teal-100 text-teal-800',
    PACKAGED: 'bg-sky-100 text-sky-800',
  };
  return classNames[status] ?? 'bg-slate-100 text-slate-700';
};

const formatLotId = (lotId: string) => `${lotId.slice(0, 8)}…`;

const formatDate = (value?: string | null) => {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime())
    ? '—'
    : parsed.toLocaleDateString('vi-VN');
};

interface DeactivateMemberDialogProps {
  member: OrganizationMember | null;
  /** Đúng khi hook đang gọi API vô hiệu hóa. */
  deactivating: boolean;
  onClose: () => void;
  onConfirm: (
    userId: string,
    reason: string,
    replacementUserId?: string,
  ) => Promise<DeactivateOutcome>;
}

/**
 * Dialog vô hiệu hóa thành viên (NCL-01-CN-009, QTN-32):
 *
 * - Giai đoạn "confirm": hiển thị cảnh báo mất quyền + chấm dứt phiên,
 *   bắt buộc nhập lý do.
 * - Backend trả 409 `requiresReplacement` (còn lô chưa hoàn thành) →
 *   chuyển sang giai đoạn "replacement": hiển thị danh sách lô cần
 *   chuyển giao và chọn MỘT người thay thế dùng chung cho toàn bộ
 *   phân công (theo API contract: deactivate nhận 1 replacementUserId).
 */
export const DeactivateMemberDialog = ({
  member,
  deactivating,
  onClose,
  onConfirm,
}: DeactivateMemberDialogProps) => {
  const [phase, setPhase] = useState<DeactivatePhase>('confirm');
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState<string | null>(null);
  const [pendingLots, setPendingLots] = useState<MemberLotSummary[]>([]);
  const [replacementUserId, setReplacementUserId] = useState('');
  const [replacementError, setReplacementError] = useState<string | null>(null);
  const [candidates, setCandidates] = useState<ReplacementCandidate[]>([]);
  const [candidatesLoading, setCandidatesLoading] = useState(false);
  const [candidatesError, setCandidatesError] = useState<string | null>(null);

  const isReplacementPhase = phase === 'replacement';

  const resetState = useCallback(() => {
    setPhase('confirm');
    setReason('');
    setReasonError(null);
    setPendingLots([]);
    setReplacementUserId('');
    setReplacementError(null);
    setCandidates([]);
    setCandidatesLoading(false);
    setCandidatesError(null);
  }, []);

  // Reset toàn bộ state mỗi khi mở dialog với thành viên mới.
  useEffect(() => {
    resetState();
  }, [member, resetState]);

  const loadCandidates = useCallback(async (userId: string) => {
    setCandidatesLoading(true);
    setCandidatesError(null);
    try {
      const data = await getReplacementCandidates(userId);
      setCandidates(data);
    } catch {
      setCandidates([]);
      setCandidatesError(
        'Không thể tải danh sách người thay thế. Vui lòng thử lại.',
      );
    } finally {
      setCandidatesLoading(false);
    }
  }, []);

  // Khi backend trả 409 còn lô chưa hoàn thành → nạp danh sách ứng viên.
  useEffect(() => {
    if (member && isReplacementPhase) {
      void loadCandidates(member.userId);
    }
  }, [member, isReplacementPhase, loadCandidates]);

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

  const handleConfirm = async () => {
    if (!member || deactivating) return;

    const trimmedReason = validateReason();
    if (!trimmedReason) return;

    if (isReplacementPhase && !replacementUserId) {
      setReplacementError(
        'Vui lòng chọn người thay thế cho các lô chưa hoàn thành.',
      );
      return;
    }
    setReplacementError(null);

    const outcome = await onConfirm(
      member.userId,
      trimmedReason,
      isReplacementPhase ? replacementUserId : undefined,
    );

    if (outcome.ok) {
      handleClose();
      return;
    }

    if (outcome.requiresReplacement) {
      // Còn lô chưa hoàn thành → hiển thị danh sách lô + form chọn
      // người thay thế (cập nhật lại nếu assignment thay đổi giữa chừng).
      setPendingLots(outcome.pendingLots ?? []);
      setPhase('replacement');
      return;
    }

    if (outcome.fatal) {
      // 403/404/409 đã ngừng hoạt động: toast đã hiển thị, đóng dialog
      // và để danh sách refresh về trạng thái đúng từ backend.
      handleClose();
    }
  };

  const getReplacementTriggerLabel = () => {
    if (candidatesLoading) return 'Đang tải danh sách người thay thế...';
    const selected = candidates.find(
      (candidate) => candidate.userId === replacementUserId,
    );
    if (!selected) return 'Chọn người thay thế';
    return `${selected.fullName} (@${selected.username})`;
  };

  return (
    <AlertDialog
      open={member !== null}
      onOpenChange={(open) => {
        if (!open) handleClose();
      }}
    >
      <AlertDialogPopup
        className={isReplacementPhase ? 'max-w-xl' : 'max-w-lg'}
      >
        <AlertDialogHeader>
          <div className="mb-2 flex size-11 items-center justify-center rounded-full bg-red-100 text-red-700">
            <ShieldOff className="size-6" />
          </div>
          <AlertDialogTitle>Vô hiệu hóa thành viên</AlertDialogTitle>
          <AlertDialogDescription>
            {isReplacementPhase
              ? 'Chọn người thay thế và nhập lý do để hoàn tất việc vô hiệu hóa.'
              : 'Sau khi vô hiệu hóa, thành viên sẽ mất toàn bộ quyền trong tổ chức ngay lập tức và mọi phiên đăng nhập đang mở sẽ bị chấm dứt. Dữ liệu đã ghi trước đó vẫn được giữ nguyên.'}
          </AlertDialogDescription>
        </AlertDialogHeader>

        {isReplacementPhase && (
          <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-4">
            <p className="flex items-center gap-2 text-sm font-semibold text-red-800">
              <AlertTriangle className="size-4 shrink-0" />
              Không thể vô hiệu hóa thành viên
            </p>
            <p className="mt-1.5 text-sm text-red-700">
              Lý do: Thành viên vẫn đang được phân công vào{' '}
              {pendingLots.length} lô chưa hoàn thành. Vui lòng chọn người
              thay thế để tiếp nhận các lô này.
            </p>
          </div>
        )}

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
            <dt className="text-sm text-muted-foreground">Vai trò hiện tại</dt>
            <dd className="text-right text-sm font-semibold">
              {getRoleLabel(member?.roleCode ?? undefined)}
            </dd>
          </div>
        </dl>

        {isReplacementPhase && (
          <div className="mt-4">
            <p className="mb-2 text-sm font-semibold text-slate-900">
              Lô được phân công cần chuyển giao
            </p>
            <div className="overflow-hidden rounded-lg border border-slate-200">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50/80">
                    <TableHead className="w-12 font-semibold text-slate-700">
                      STT
                    </TableHead>
                    <TableHead className="font-semibold text-slate-700">
                      Lô sản xuất
                    </TableHead>
                    <TableHead className="font-semibold text-slate-700">
                      Mã lô
                    </TableHead>
                    <TableHead className="font-semibold text-slate-700">
                      Trạng thái
                    </TableHead>
                    <TableHead className="font-semibold text-slate-700">
                      Ngày thu hoạch
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {pendingLots.map((lot, index) => (
                    <TableRow key={lot.lotId}>
                      <TableCell className="text-muted-foreground">
                        {index + 1}
                      </TableCell>
                      <TableCell
                        className="max-w-52 truncate font-medium text-slate-900"
                        title={lot.lotName}
                      >
                        {lot.lotName}
                      </TableCell>
                      <TableCell
                        className="font-mono text-xs text-slate-500"
                        title={lot.lotId}
                      >
                        {formatLotId(lot.lotId)}
                      </TableCell>
                      <TableCell>
                        <span
                          className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${getLotStatusBadgeClass(lot.lotStatus)}`}
                        >
                          {getLotStatusLabel(lot.lotStatus)}
                        </span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {formatDate(lot.harvestDate)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            <div className="mt-4">
              <Label htmlFor="replacement-user">
                Người thay thế <span className="text-red-600">*</span>
              </Label>
              {candidatesLoading ? (
                <div className="mt-1.5 flex h-11 w-full items-center gap-2 rounded-lg border border-border bg-muted px-4 text-sm text-muted-foreground">
                  <LoaderCircle className="size-4 animate-spin" />
                  Đang tải danh sách người thay thế...
                </div>
              ) : candidatesError ? (
                <div className="mt-1.5 rounded-lg border border-amber-200 bg-amber-50 p-3">
                  <p className="text-sm text-amber-800">{candidatesError}</p>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="mt-2"
                    onClick={() => member && void loadCandidates(member.userId)}
                  >
                    <RotateCcw className="size-3.5" />
                    Thử lại
                  </Button>
                </div>
              ) : candidates.length === 0 ? (
                <div className="mt-1.5 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
                  Không có ứng viên thay thế phù hợp trong tổ chức. Vui lòng
                  hoàn thành hoặc phân công lại các lô trước khi vô hiệu hóa
                  thành viên này.
                </div>
              ) : (
                <Select
                  value={replacementUserId || null}
                  onValueChange={(value) => {
                    setReplacementUserId(value ?? '');
                    if (replacementError) setReplacementError(null);
                  }}
                >
                  <SelectTrigger className="mt-1.5 w-full">
                    {getReplacementTriggerLabel()}
                  </SelectTrigger>
                  <SelectContent>
                    {candidates.map((candidate) => (
                      <SelectItem
                        key={candidate.userId}
                        value={candidate.userId}
                      >
                        <span className="flex flex-col">
                          <span className="font-medium">
                            {candidate.fullName}
                          </span>
                          <span className="text-xs text-muted-foreground">
                            @{candidate.username}
                            {candidate.roleName
                              ? ` · ${candidate.roleName}`
                              : ''}
                          </span>
                        </span>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
              {!candidatesLoading &&
                !candidatesError &&
                candidates.length > 0 &&
                replacementError && (
                  <p className="mt-1.5 text-sm text-red-600">
                    {replacementError}
                  </p>
                )}
              <p className="mt-2 text-xs leading-5 text-muted-foreground">
                Chỉ hiển thị thành viên đang hoạt động trong tổ chức và có
                quyền ghi sự kiện. Người được chọn sẽ tiếp nhận toàn bộ phân
                công lô còn hiệu lực của thành viên bị vô hiệu hóa.
              </p>
            </div>
          </div>
        )}

        <div className="mt-4 space-y-1.5">
          <Label htmlFor="deactivate-reason">
            Lý do vô hiệu hóa <span className="text-red-600">*</span>
          </Label>
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
          {reasonError && <p className="text-sm text-red-600">{reasonError}</p>}
        </div>

        <p className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {isReplacementPhase
            ? 'Các lô chưa hoàn thành sẽ được chuyển giao cho người thay thế trong cùng một thao tác. Dữ liệu lịch sử đã ghi không bị thay đổi.'
            : 'Thành viên có thể được kích hoạt lại sau nếu quay lại làm việc. Dữ liệu đã ghi trước đó vẫn được giữ nguyên với tên người ghi cũ.'}
        </p>

        <AlertDialogFooter>
          <AlertDialogCancel disabled={deactivating} onClick={handleClose}>
            Hủy
          </AlertDialogCancel>
          <Button
            type="button"
            variant="delete"
            disabled={
              deactivating ||
              (isReplacementPhase &&
                !candidatesLoading &&
                !candidatesError &&
                candidates.length === 0)
            }
            onClick={() => {
              void handleConfirm();
            }}
          >
            {deactivating && <LoaderCircle className="size-4 animate-spin" />}
            {deactivating
              ? 'Đang xử lý...'
              : isReplacementPhase
                ? 'Xác nhận & chuyển giao lô'
                : 'Xác nhận vô hiệu hóa'}
          </Button>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};
