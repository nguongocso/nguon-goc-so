import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import {
  ArrowLeft,
  CheckCircle2,
  Copy,
  Eye,
  Hash,
  LoaderCircle,
  XCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from '@/components/ui/select';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Card, CardContent } from '@/components/ui/card';
import { StatusBadge } from '@/components/common/StatusBadge';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { HelpButton } from '@/components/help/HelpButton';
import {
  approveSupplementRequest,
  getSupplementRequest,
  getSupplementRequests,
  rejectSupplementRequest,
} from '@/api/codeRangeSupplementApi';
import type {
  CodeRangeSupplementRequest,
  EvidenceEvent,
} from '@/types/codeRangeSupplement';

const STATUS_LABEL: Record<string, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
};

const STATUS_TONE = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
} as const;

const EVIDENCE_TYPE_LABEL: Record<string, string> = {
  HARVEST: 'Thu hoạch',
  PREPROCESSING: 'Sơ chế',
};

/** Rút gọn UUID dạng `c5351614...bc3f7f18` để tránh tràn dòng. */
const shortenId = (id: string): string =>
  id.length > 16 ? `${id.slice(0, 8)}...${id.slice(-8)}` : id;

/**
 * NCL-04-CN-007: Trang chi tiết + xét duyệt yêu cầu cấp bổ sung dải mã (VT-01).
 *
 * Thay thế `SupplementReviewDialog` (dialog inline cũ trên trang Quản lý dải
 * mã): nút "Duyệt bổ sung" điều hướng tới đây, duyệt/từ chối xong quay về
 * `/admin/code-ranges`.
 */
export const CodeRangeSupplementDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [request, setRequest] = useState<CodeRangeSupplementRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [sameOrgPending, setSameOrgPending] = useState<CodeRangeSupplementRequest[]>([]);

  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [approvedQuantity, setApprovedQuantity] = useState('');
  const [remarks, setRemarks] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [viewingEvent, setViewingEvent] = useState<EvidenceEvent | null>(null);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Quản lý dải mã truy xuất', href: '/admin/code-ranges' },
    { label: 'Chi tiết yêu cầu cấp bổ sung' },
  ]);

  const load = useCallback(async () => {
    if (!id) return;
    try {
      setLoading(true);
      const data = await getSupplementRequest(id);
      setRequest(data);
      setApprovedQuantity(String(data.requestedQuantity));
      setRemarks('');
      setRejectionReason('');
      if (data.status === 'PENDING') {
        try {
          const pending = await getSupplementRequests({ status: 'PENDING', page: 0, size: 1000 });
          setSameOrgPending(
            pending.items.filter(
              (item) => item.organizationId === data.organizationId && item.id !== data.id,
            ),
          );
        } catch {
          setSameOrgPending([]);
        }
      } else {
        setSameOrgPending([]);
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể tải yêu cầu cấp bổ sung mã');
      setRequest(null);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  /** Map chi tiết bằng chứng theo ID để hiển thị rõ nghĩa thay vì UUID thô. */
  const evidenceById = useMemo<Record<string, EvidenceEvent>>(
    () => Object.fromEntries((request?.evidenceEvents ?? []).map((event) => [event.eventId, event])),
    [request],
  );

  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  /** Mô tả một sự kiện bằng chứng rõ nghĩa: loại — tên lô — thời điểm — người ghi — SL. */
  const describeEvidence = (event: EvidenceEvent): string => {
    const typeLabel = EVIDENCE_TYPE_LABEL[event.eventType] ?? event.eventType;
    const parts = [
      typeLabel,
      event.productionLotName || 'lô chưa rõ',
      formatDate(event.recordedAt),
    ];
    if (event.recordedByName) parts.push(event.recordedByName);
    if (event.quantity != null) parts.push(`SL: ${event.quantity.toLocaleString('vi-VN')}`);
    return parts.join(' — ');
  };

  const copyText = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      toast.success('Đã sao chép ID sự kiện');
    } catch {
      toast.error('Không thể sao chép ID sự kiện');
    }
  };

  const handleApprove = async () => {
    if (!id || !request) return;
    if (approvedQuantity.trim() === '') {
      toast.error('Số lượng thực cấp không được để trống');
      return;
    }
    const qty = Number(approvedQuantity);
    if (!Number.isInteger(qty) || qty <= 0) {
      toast.error('Số lượng thực cấp phải là số nguyên lớn hơn 0');
      return;
    }
    if (qty > request.requestedQuantity) {
      toast.error('Số lượng thực cấp không được vượt quá số lượng đề nghị');
      return;
    }
    try {
      setActionLoading(true);
      const result = await approveSupplementRequest(id, {
        approvedQuantity: qty,
        remarks: remarks.trim() || undefined,
      });
      const partial =
        result.approvedQuantity != null && result.approvedQuantity < result.requestedQuantity;
      toast.success(
        partial
          ? `Đã duyệt một phần: thực cấp ${result.approvedQuantity} trên ${result.requestedQuantity} mã đề nghị.`
          : `Đã duyệt toàn bộ ${result.requestedQuantity} mã cho tổ chức "${result.organizationName}".`,
      );
      setApproveDialogOpen(false);
      navigate('/admin/code-ranges');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể duyệt yêu cầu');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!id) return;
    if (!rejectionReason.trim()) {
      toast.error('Lý do từ chối không được để trống');
      return;
    }
    try {
      setActionLoading(true);
      await rejectSupplementRequest(id, { rejectionReason: rejectionReason.trim() });
      toast.success('Đã từ chối yêu cầu cấp bổ sung mã');
      setRejectDialogOpen(false);
      navigate('/admin/code-ranges');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể từ chối yêu cầu');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20 text-muted-foreground">
        <LoaderCircle className="size-5 animate-spin mr-2" /> Đang tải...
      </div>
    );
  }

  if (!request) {
    return (
      <div className="space-y-6">
        <Button variant="outline" size="sm" onClick={() => navigate('/admin/code-ranges')}>
          <ArrowLeft className="size-4 mr-1" /> Quay lại
        </Button>
        <div className="text-center py-20 text-muted-foreground">
          Không tìm thấy yêu cầu cấp bổ sung mã
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={Hash}
        title={`Yêu cầu bổ sung ${request.requestedQuantity.toLocaleString('vi-VN')} mã: ${request.organizationName}`}
        description="Chi tiết yêu cầu cấp bổ sung dải mã và xét duyệt."
        actions={
          <>
            <HelpButton screenKey="code-range-supplement-detail" />
            <Button variant="outline" size="sm" onClick={() => navigate('/admin/code-ranges')}>
              <ArrowLeft className="size-4 mr-1" /> Quay lại
            </Button>
          </>
        }
      />

      {sameOrgPending.length > 0 && (
        <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
          <CardContent className="pt-6">
            <div className="space-y-1.5">
              <Label>Tổ chức còn {sameOrgPending.length} yêu cầu khác đang chờ — chuyển xem</Label>
              <Select value={request.id} onValueChange={(v) => v && navigate(`/admin/code-range-supplements/${v}`)}>
                <SelectTrigger className="w-full">
                  {request.requestedQuantity.toLocaleString('vi-VN')} mã —{' '}
                  {request.requestedBy?.fullName || '—'} — {formatDate(request.requestedAt)}
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={request.id}>
                    {request.requestedQuantity.toLocaleString('vi-VN')} mã —{' '}
                    {request.requestedBy?.fullName || '—'} — {formatDate(request.requestedAt)} (đang xem)
                  </SelectItem>
                  {sameOrgPending.map((item) => (
                    <SelectItem key={item.id} value={item.id}>
                      {item.requestedQuantity.toLocaleString('vi-VN')} mã —{' '}
                      {item.requestedBy?.fullName || '—'} — {formatDate(item.requestedAt)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>
      )}

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardContent className="space-y-4 pt-6">
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">Trạng thái:</span>
            <StatusBadge
              label={STATUS_LABEL[request.status] || request.status}
              tone={STATUS_TONE[request.status as keyof typeof STATUS_TONE] ?? 'warning'}
            />
          </div>

          <dl className="divide-y rounded-lg border bg-slate-50 px-4">
            <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
              <dt className="text-sm text-muted-foreground">Tổ chức</dt>
              <dd className="text-sm font-semibold text-left">{request.organizationName}</dd>
            </div>
            <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
              <dt className="text-sm text-muted-foreground">Số lượng đề nghị</dt>
              <dd className="text-sm font-semibold text-left">
                {request.requestedQuantity.toLocaleString('vi-VN')} mã
              </dd>
            </div>
            <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
              <dt className="text-sm text-muted-foreground">Người yêu cầu</dt>
              <dd className="text-sm font-semibold text-left">
                {request.requestedBy?.fullName || '—'}
              </dd>
            </div>
            <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
              <dt className="text-sm text-muted-foreground">Thời điểm yêu cầu</dt>
              <dd className="text-sm text-left">{formatDate(request.requestedAt)}</dd>
            </div>
            <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
              <dt className="text-sm text-muted-foreground">Lý do</dt>
              <dd className="text-sm text-left whitespace-pre-line">{request.reason}</dd>
            </div>
            <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
              <dt className="text-sm text-muted-foreground">Bằng chứng (sự kiện sản lượng)</dt>
              <dd className="min-w-0 text-left">
                {request.evidenceEventIds.length > 0 ? (
                  <div className="flex flex-col gap-1.5">
                    {request.evidenceEventIds.map((eventId) => {
                      const detail = evidenceById[eventId];
                      return (
                        <div key={eventId} className="flex items-center gap-2">
                          {detail ? (
                            <span className="text-sm" title={eventId}>
                              {describeEvidence(detail)}
                            </span>
                          ) : (
                            <code className="truncate font-mono text-xs" title={eventId}>
                              {shortenId(eventId)}
                            </code>
                          )}
                          <Button
                            variant="ghost"
                            size="icon-xs"
                            className="shrink-0 text-muted-foreground"
                            onClick={() => detail ? setViewingEvent(detail) : toast.info('Không có chi tiết sự kiện')}
                            title="Xem chi tiết sự kiện"
                            aria-label="Xem chi tiết sự kiện"
                          >
                            <Eye className="size-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon-xs"
                            className="shrink-0 text-muted-foreground"
                            onClick={() => void copyText(eventId)}
                            title="Sao chép toàn bộ ID sự kiện"
                            aria-label="Sao chép ID sự kiện"
                          >
                            <Copy className="size-3.5" />
                          </Button>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  '—'
                )}
              </dd>
            </div>
            {request.status === 'APPROVED' && (
              <>
                <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                  <dt className="text-sm text-muted-foreground">Số lượng thực cấp</dt>
                  <dd className="text-sm font-semibold text-left">
                    {request.approvedQuantity != null
                      ? `${request.approvedQuantity.toLocaleString('vi-VN')} mã`
                      : '—'}
                  </dd>
                </div>
                <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                  <dt className="text-sm text-muted-foreground">Người duyệt</dt>
                  <dd className="text-sm font-semibold text-left">
                    {request.approvedBy?.fullName || '—'}
                  </dd>
                </div>
                <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                  <dt className="text-sm text-muted-foreground">Thời điểm duyệt</dt>
                  <dd className="text-sm text-left">{formatDate(request.approvedAt)}</dd>
                </div>
                {request.approvalRemarks && (
                  <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                    <dt className="text-sm text-muted-foreground">Ghi chú duyệt</dt>
                    <dd className="text-sm text-left whitespace-pre-line">
                      {request.approvalRemarks}
                    </dd>
                  </div>
                )}
              </>
            )}
            {request.status === 'REJECTED' && (
              <>
                <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                  <dt className="text-sm text-muted-foreground">Người từ chối</dt>
                  <dd className="text-sm font-semibold text-left">
                    {request.rejectedBy?.fullName || '—'}
                  </dd>
                </div>
                <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                  <dt className="text-sm text-muted-foreground">Thời điểm từ chối</dt>
                  <dd className="text-sm text-left">{formatDate(request.rejectedAt)}</dd>
                </div>
                <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                  <dt className="text-sm text-muted-foreground">Lý do từ chối</dt>
                  <dd className="text-sm text-left whitespace-pre-line">
                    {request.rejectionReason}
                  </dd>
                </div>
              </>
            )}
          </dl>

          {request.status === 'PENDING' && (
            <div className="flex items-center justify-end gap-2">
              <Button variant="destructive" onClick={() => setRejectDialogOpen(true)}>
                <XCircle className="size-4 mr-1" /> Từ chối
              </Button>
              <Button
                className="bg-blue-600 hover:bg-blue-700 text-white"
                onClick={() => {
                  setApprovedQuantity(String(request.requestedQuantity));
                  setApproveDialogOpen(true);
                }}
              >
                <CheckCircle2 className="size-4 mr-1" /> Duyệt
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <AlertDialog open={approveDialogOpen} onOpenChange={setApproveDialogOpen}>
        <AlertDialogContent className="max-w-lg">
          <AlertDialogHeader>
            <AlertDialogTitle>Duyệt yêu cầu cấp bổ sung mã</AlertDialogTitle>
          </AlertDialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="approvedQuantity">
                Số lượng thực cấp (đề nghị {request.requestedQuantity.toLocaleString('vi-VN')} mã){' '}
                <span className="text-red-600">*</span>
              </Label>
              <Input
                id="approvedQuantity"
                type="number"
                min={1}
                max={request.requestedQuantity}
                step={1}
                value={approvedQuantity}
                onChange={(e) => setApprovedQuantity(e.target.value)}
              />
              <p className="text-xs text-muted-foreground">
                Nhập bằng số lượng đề nghị để duyệt toàn bộ, nhập ít hơn để duyệt một phần.
              </p>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="remarks">Ghi chú (tùy chọn)</Label>
              <Textarea
                id="remarks"
                value={remarks}
                rows={3}
                placeholder="Ghi chú khi duyệt"
                onChange={(e) => setRemarks(e.target.value)}
              />
            </div>
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>Hủy</AlertDialogCancel>
            <Button disabled={actionLoading} onClick={() => void handleApprove()}>
              {actionLoading && <LoaderCircle className="size-4 animate-spin" />}
              Xác nhận duyệt
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
        <AlertDialogContent className="max-w-lg">
          <AlertDialogHeader>
            <AlertDialogTitle>Từ chối yêu cầu cấp bổ sung mã</AlertDialogTitle>
          </AlertDialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="rejectionReason">
              Lý do từ chối <span className="text-red-600">*</span>
            </Label>
            <Textarea
              id="rejectionReason"
              value={rejectionReason}
              rows={3}
              placeholder="Bắt buộc nhập lý do từ chối"
              onChange={(e) => setRejectionReason(e.target.value)}
            />
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>Hủy</AlertDialogCancel>
            <Button
              variant="destructive"
              disabled={actionLoading}
              onClick={() => void handleReject()}
            >
              {actionLoading && <LoaderCircle className="size-4 animate-spin" />}
              Xác nhận từ chối
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <Dialog open={!!viewingEvent} onOpenChange={(open) => !open && setViewingEvent(null)}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Chi tiết sự kiện bằng chứng</DialogTitle>
            <DialogDescription>
              Thông tin chi tiết của sự kiện được chọn làm bằng chứng sản lượng.
            </DialogDescription>
          </DialogHeader>
          {viewingEvent && (
            <dl className="divide-y rounded-lg border bg-slate-50 px-4">
              <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                <dt className="text-sm text-muted-foreground">Loại sự kiện</dt>
                <dd className="text-sm font-medium text-left">
                  {EVIDENCE_TYPE_LABEL[viewingEvent.eventType] || viewingEvent.eventType}
                </dd>
              </div>
              <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                <dt className="text-sm text-muted-foreground">Lô sản xuất</dt>
                <dd className="text-sm text-left">
                  {viewingEvent.productionLotName || '—'}
                </dd>
              </div>
              <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                <dt className="text-sm text-muted-foreground">Lô hàng (Shipment)</dt>
                <dd className="text-sm text-left">
                  {viewingEvent.shipmentId || '—'}
                </dd>
              </div>
              <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                <dt className="text-sm text-muted-foreground">Sản lượng thực</dt>
                <dd className="text-sm font-medium text-left">
                  {viewingEvent.quantity != null
                    ? viewingEvent.quantity.toLocaleString('vi-VN')
                    : '—'}
                </dd>
              </div>
              <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                <dt className="text-sm text-muted-foreground">Thời điểm ghi</dt>
                <dd className="text-sm text-left">{formatDate(viewingEvent.recordedAt)}</dd>
              </div>
              <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                <dt className="text-sm text-muted-foreground">Người ghi</dt>
                <dd className="text-sm text-left">{viewingEvent.recordedByName || '—'}</dd>
              </div>
              <div className="grid grid-cols-[35%_1fr] items-start gap-x-3 py-3">
                <dt className="text-sm text-muted-foreground">ID sự kiện</dt>
                <dd className="text-sm font-mono text-left break-all">
                  {viewingEvent.eventId}
                </dd>
              </div>
            </dl>
          )}
          <div className="flex justify-end">
            <Button onClick={() => setViewingEvent(null)}>Đóng</Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
};
