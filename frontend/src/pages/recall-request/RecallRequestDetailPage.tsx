import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { AlertDialog, AlertDialogCancel, AlertDialogContent, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { CheckCircle2, LoaderCircle, XCircle } from 'lucide-react';
import { HelpButton } from '@/components/help/HelpButton';
import { approveRecallRequest, getRecallRequest, rejectRecallRequest } from '@/api/recallApi';
import { useAuth } from '@/hooks/useAuth';
import type { RecallRequest } from '@/types/recallRequest';

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  PENDING: { label: 'Chờ duyệt', className: 'bg-yellow-100 text-yellow-800' },
  APPROVED: { label: 'Đã duyệt', className: 'bg-emerald-100 text-emerald-800' },
  REJECTED: { label: 'Đã từ chối', className: 'bg-red-100 text-red-700' },
};

export const RecallRequestDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [request, setRequest] = useState<RecallRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [remarks, setRemarks] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const load = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const data = await getRecallRequest(id);
      setRequest(data);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể tải yêu cầu thu hồi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [id]);

  const isOwnRequest = user?.userId === request?.requestedBy?.userId;

  const handleApprove = async () => {
    if (!id) return;
    try {
      setActionLoading(true);
      const result = await approveRecallRequest(id, {
        remarks: remarks.trim() || undefined,
      });
      toast.success(
        `Đã duyệt yêu cầu thu hồi. Lô "${result.lotName}" đã chuyển sang trạng thái thu hồi.${
          result.notifiedBuyerCount ? ` Đã thông báo ${result.notifiedBuyerCount} người mua.` : ''
        }`,
      );
      setApproveDialogOpen(false);
      navigate('/recall-requests');
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
      await rejectRecallRequest(id, { rejectionReason: rejectionReason.trim() });
      toast.success('Đã từ chối yêu cầu thu hồi');
      setRejectDialogOpen(false);
      navigate('/recall-requests');
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
      <div className="flex justify-center items-center py-20 text-muted-foreground">
        Đang tải...
      </div>
    );
  }

  if (!request) {
    return <div className="text-center py-20 text-muted-foreground">Không tìm thấy yêu cầu thu hồi</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Yêu cầu thu hồi: {request.lotName}
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
            Chi tiết yêu cầu thu hồi lô sản xuất và nhật ký xét duyệt.
          </p>
        </div>
        <HelpButton screenKey="recall-request-detail" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardContent className="space-y-4 pt-6">
          <dl className="divide-y rounded-lg border bg-slate-50 px-4">
            <div className="flex items-start justify-between gap-4 py-3">
              <dt className="text-sm text-muted-foreground">Lô sản xuất</dt>
              <dd className="text-right text-sm font-semibold">{request.lotName}</dd>
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
              <dd className="text-right text-sm whitespace-pre-line max-w-[60%]">
                {request.reason}
              </dd>
            </div>
            {request.evidence && (
              <div className="flex items-start justify-between gap-4 py-3">
                <dt className="text-sm text-muted-foreground">Bằng chứng</dt>
                <dd className="text-right text-sm whitespace-pre-line max-w-[60%]">
                  {request.evidence}
                </dd>
              </div>
            )}
            {request.status === 'APPROVED' && (
              <>
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
            <div>
              {isOwnRequest ? (
                <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                  Bạn là người tạo yêu cầu này và không thể tự duyệt (QTN-22).
                </div>
              ) : (
                <div className="flex items-center justify-end gap-2">
                  <Button variant="outline" onClick={() => setRejectDialogOpen(true)}>
                    <XCircle className="size-4 mr-1" /> Từ chối
                  </Button>
                  <Button variant="delete" onClick={() => setApproveDialogOpen(true)}>
                    <CheckCircle2 className="size-4 mr-1" /> Duyệt & thu hồi
                  </Button>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <AlertDialog open={approveDialogOpen} onOpenChange={setApproveDialogOpen}>
        <AlertDialogContent className="max-w-lg">
          <AlertDialogHeader>
            <AlertDialogTitle>Duyệt yêu cầu thu hồi</AlertDialogTitle>
          </AlertDialogHeader>
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
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>Hủy</AlertDialogCancel>
            <Button
              variant="delete"
              disabled={actionLoading}
              onClick={() => void handleApprove()}
            >
              {actionLoading && <LoaderCircle className="size-4 animate-spin" />}
              Xác nhận duyệt
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
        <AlertDialogContent className="max-w-lg">
          <AlertDialogHeader>
            <AlertDialogTitle>Từ chối yêu cầu thu hồi</AlertDialogTitle>
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
              variant="outline"
              disabled={actionLoading}
              onClick={() => void handleReject()}
            >
              {actionLoading && <LoaderCircle className="size-4 animate-spin" />}
              Xác nhận từ chối
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
};