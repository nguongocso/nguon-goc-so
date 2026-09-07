import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { CheckCircle2, LoaderCircle, XCircle } from 'lucide-react';
import { HelpButton } from '@/components/help/HelpButton';
import {
  approveSupplementRequest,
  getSupplementRequest,
  rejectSupplementRequest,
} from '@/api/codeRangeSupplementApi';
import type { CodeRangeSupplementRequest } from '@/types/codeRangeSupplement';

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  PENDING: { label: 'Chờ duyệt', className: 'bg-yellow-100 text-yellow-800' },
  APPROVED: { label: 'Đã duyệt', className: 'bg-emerald-100 text-emerald-800' },
  REJECTED: { label: 'Đã từ chối', className: 'bg-red-100 text-red-700' },
};

export const CodeRangeSupplementDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [request, setRequest] = useState<CodeRangeSupplementRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [approvedQuantity, setApprovedQuantity] = useState('');
  const [remarks, setRemarks] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const load = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const data = await getSupplementRequest(id);
      setRequest(data);
      setApprovedQuantity(String(data.requestedQuantity));
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể tải yêu cầu cấp bổ sung mã');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleApprove = async () => {
    if (!id || !request) return;
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
      const partial = result.approvedQuantity != null && result.approvedQuantity < result.requestedQuantity;
      toast.success(
        partial
          ? `Đã duyệt một phần: thực cấp ${result.approvedQuantity} trên ${result.requestedQuantity} mã đề nghị.`
          : `Đã duyệt toàn bộ ${result.requestedQuantity} mã cho tổ chức "${result.organizationName}".`,
      );
      setApproveDialogOpen(false);
      navigate('/admin/code-range-supplements');
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
      navigate('/admin/code-range-supplements');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể từ chối yêu cầu');
    } finally {
      setActionLoading(false);
    }
  };

  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20 text-muted-foreground">Đang tải...</div>
    );
  }

  if (!request) {
    return (
      <div className="text-center py-20 text-muted-foreground">
        Không tìm thấy yêu cầu cấp bổ sung mã
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Yêu cầu bổ sung {request.requestedQuantity.toLocaleString()} mã: {request.organizationName}
            </h1>
            <span
              className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                STATUS_MAP[request.status]?.className || 'bg-gray-100 text-gray-700'
              }`}
            >
              {STATUS_MAP[request.status]?.label || request.status}
            </span>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Chi tiết yêu cầu cấp bổ sung dải mã và nhật ký xét duyệt.
          </p>
        </div>
        <HelpButton screenKey="code-range-supplement-detail" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardContent className="space-y-4 pt-6">
          <dl className="divide-y rounded-lg border bg-slate-50 px-4">
            <div className="flex items-start justify-between gap-4 py-3">
              <dt className="text-sm text-muted-foreground">Tổ chức</dt>
              <dd className="text-right text-sm font-semibold">{request.organizationName}</dd>
            </div>
            <div className="flex items-start justify-between gap-4 py-3">
              <dt className="text-sm text-muted-foreground">Số lượng đề nghị</dt>
              <dd className="text-right text-sm font-semibold">
                {request.requestedQuantity.toLocaleString()} mã
              </dd>
            </div>
            <div className="flex items-start justify-between gap-4 py-3">
              <dt className="text-sm text-muted-foreground">Người yêu cầu</dt>
              <dd className="text-right text-sm font-semibold">
                {request.requestedBy?.fullName || '—'}
              </dd>
            </div>
            <div className="flex items-start justify-between gap-4 py-3">
              <dt className="text-sm text-muted-foreground">Thời điểm yêu cầu</dt>
              <dd className="text-right text-sm">{formatDate(request.requestedAt)}</dd>
            </div>
            <div className="flex items-start justify-between gap-4 py-3">
              <dt className="text-sm text-muted-foreground">Lý do</dt>
              <dd className="text-right text-sm whitespace-pre-line max-w-[60%]">{request.reason}</dd>
            </div>
            <div className="flex items-start justify-between gap-4 py-3">
              <dt className="text-sm text-muted-foreground">Bằng chứng (ID sự kiện)</dt>
              <dd className="text-right text-sm font-mono max-w-[60%] break-all">
                {request.evidenceEventIds.length > 0 ? request.evidenceEventIds.join(', ') : '—'}
              </dd>
            </div>
            {request.status === 'APPROVED' && (
              <>
                <div className="flex items-start justify-between gap-4 py-3">
                  <dt className="text-sm text-muted-foreground">Số lượng thực cấp</dt>
                  <dd className="text-right text-sm font-semibold">
                    {request.approvedQuantity != null
                      ? `${request.approvedQuantity.toLocaleString()} mã`
                      : '—'}
                  </dd>
                </div>
                <div className="flex items-start justify-between gap-4 py-3">
                  <dt className="text-sm text-muted-foreground">Người duyệt</dt>
                  <dd className="text-right text-sm font-semibold">
                    {request.approvedBy?.fullName || '—'}
                  </dd>
                </div>
                <div className="flex items-start justify-between gap-4 py-3">
                  <dt className="text-sm text-muted-foreground">Thời điểm duyệt</dt>
                  <dd className="text-right text-sm">{formatDate(request.approvedAt)}</dd>
                </div>
                {request.approvalRemarks && (
                  <div className="flex items-start justify-between gap-4 py-3">
                    <dt className="text-sm text-muted-foreground">Ghi chú duyệt</dt>
                    <dd className="text-right text-sm whitespace-pre-line max-w-[60%]">
                      {request.approvalRemarks}
                    </dd>
                  </div>
                )}
              </>
            )}
            {request.status === 'REJECTED' && (
              <>
                <div className="flex items-start justify-between gap-4 py-3">
                  <dt className="text-sm text-muted-foreground">Người từ chối</dt>
                  <dd className="text-right text-sm font-semibold">
                    {request.rejectedBy?.fullName || '—'}
                  </dd>
                </div>
                <div className="flex items-start justify-between gap-4 py-3">
                  <dt className="text-sm text-muted-foreground">Thời điểm từ chối</dt>
                  <dd className="text-right text-sm">{formatDate(request.rejectedAt)}</dd>
                </div>
                <div className="flex items-start justify-between gap-4 py-3">
                  <dt className="text-sm text-muted-foreground">Lý do từ chối</dt>
                  <dd className="text-right text-sm whitespace-pre-line max-w-[60%]">
                    {request.rejectionReason}
                  </dd>
                </div>
              </>
            )}
          </dl>

          {request.status === 'PENDING' && (
            <div className="flex items-center justify-end gap-2">
              <Button variant="outline" onClick={() => setRejectDialogOpen(true)}>
                <XCircle className="size-4 mr-1" /> Từ chối
              </Button>
              <Button
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
                Số lượng thực cấp (đề nghị {request.requestedQuantity.toLocaleString()} mã){' '}
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
            <Button variant="outline" disabled={actionLoading} onClick={() => void handleReject()}>
              {actionLoading && <LoaderCircle className="size-4 animate-spin" />}
              Xác nhận từ chối
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
};
