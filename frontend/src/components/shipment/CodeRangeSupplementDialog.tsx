import { useCallback, useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { AlertTriangle, Hash, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { DataTableShell } from '@/components/common/DataTableShell';
import { StatusBadge } from '@/components/common/StatusBadge';
import { useAuth } from '@/hooks/useAuth';
import { getRemainingCodes } from '@/api/codeRangeApi';
import {
  createSupplementRequest,
  getEvidenceEvents,
  getMySupplementRequests,
} from '@/api/codeRangeSupplementApi';
import type { RemainingCodesResponse } from '@/types/codeRange';
import type {
  CodeRangeSupplementRequest,
  EvidenceEvent,
} from '@/types/codeRangeSupplement';
import { createSupplementSchema } from '@/utils/validators/codeRangeSupplementSchema';

/**
 * NCL-04-CN-007: Dialog gửi yêu cầu cấp bổ sung dải mã truy xuất (VT-02).
 *
 * Được mở từ:
 *  - Tab "Lô hàng & Mã QR" của trang chi tiết lô sản xuất (ShipmentList).
 *  - Cảnh báo hạn mức trên màn hình tạo lô hàng (CreateShipmentPage).
 *
 * Bằng chứng sản lượng thực: danh sách phẳng sự kiện thu hoạch/sơ chế của tổ
 * chức (GET /code-range-supplement-requests/evidence-events) — người dùng chỉ
 * cần tick chọn, không phải đi qua lô sản xuất → lô hàng.
 */

const EVIDENCE_TYPE_LABEL: Record<string, string> = {
  HARVEST: 'Thu hoạch',
  PREPROCESSING: 'Sơ chế',
};

const MY_STATUS_TONE = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
} as const;

export const CodeRangeSupplementDialog = ({
  open,
  onClose,
}: CodeRangeSupplementDialogProps) => {
  const { user } = useAuth();

  const [quota, setQuota] = useState<RemainingCodesResponse | null>(null);
  const [quotaLoading, setQuotaLoading] = useState(false);

  const [requestedQuantity, setRequestedQuantity] = useState('');
  const [reason, setReason] = useState('');

  const [events, setEvents] = useState<EvidenceEvent[]>([]);
  const [eventsLoading, setEventsLoading] = useState(false);
  const [selectedEventIds, setSelectedEventIds] = useState<string[]>([]);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [myRequests, setMyRequests] = useState<CodeRangeSupplementRequest[]>([]);
  const [myLoading, setMyLoading] = useState(false);

  const resetForm = useCallback(() => {
    setRequestedQuantity('');
    setReason('');
    setSelectedEventIds([]);
    setError(null);
  }, []);

  const loadMyRequests = useCallback(async () => {
    try {
      setMyLoading(true);
      const result = await getMySupplementRequests({ page: 0, size: 20 });
      setMyRequests(result.items);
    } catch {
      setMyRequests([]);
    } finally {
      setMyLoading(false);
    }
  }, []);

  // Tải dữ liệu mỗi lần mở dialog
  useEffect(() => {
    if (!open) return;

    resetForm();
    setEvents([]);

    if (user?.organizationId) {
      setQuotaLoading(true);
      getRemainingCodes(user.organizationId)
        .then(setQuota)
        .catch(() => setQuota(null))
        .finally(() => setQuotaLoading(false));
    }

    setEventsLoading(true);
    getEvidenceEvents()
      .then(setEvents)
      .catch(() => {
        setEvents([]);
        toast.error('Không thể tải danh sách sự kiện bằng chứng.');
      })
      .finally(() => setEventsLoading(false));

    void loadMyRequests();
  }, [open, user?.organizationId, resetForm, loadMyRequests]);

  const toggleEvent = (eventId: string) => {
    setSelectedEventIds((prev) =>
      prev.includes(eventId) ? prev.filter((id) => id !== eventId) : [...prev, eventId],
    );
  };

  const quotaRatio = useMemo(() => {
    if (!quota || !quota.hasCodeRange || quota.totalLimit <= 0) return null;
    return quota.remainingCount / quota.totalLimit;
  }, [quota]);

  const showWarning = quotaRatio !== null && quotaRatio < 0.2;

  const handleSubmit = async () => {
    setError(null);

    const parsed = createSupplementSchema.safeParse({
      requestedQuantity: requestedQuantity === '' ? NaN : Number(requestedQuantity),
      reason,
      evidenceEventIds: selectedEventIds,
    });
    if (!parsed.success) {
      const message = parsed.error.issues[0]?.message || 'Dữ liệu không hợp lệ.';
      setError(message);
      return;
    }

    try {
      setSubmitting(true);
      await createSupplementRequest({
        requestedQuantity: parsed.data.requestedQuantity,
        reason: parsed.data.reason,
        evidenceEventIds: parsed.data.evidenceEventIds,
      });
      toast.success('Yêu cầu cấp bổ sung mã đã được gửi. Vui lòng chờ quản trị viên duyệt.');
      resetForm();
      await loadMyRequests();
    } catch (err: any) {
      const message = err.response?.data?.message || 'Không thể tạo yêu cầu cấp bổ sung mã.';
      toast.error(message);
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="flex max-h-[85vh] flex-col overflow-hidden sm:max-w-2xl lg:max-w-3xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Hash className="size-4 text-emerald-600" />
            Yêu cầu cấp bổ sung mã truy xuất
          </DialogTitle>
          <DialogDescription>
            Gửi yêu cầu để quản trị viên nền tảng xét duyệt và tăng hạn mức dải mã của tổ chức.
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 space-y-4 overflow-y-auto pr-1">
          {/* Hạn mức dải mã hiện tại */}
          <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3.5 text-sm">
            <div className="flex items-center justify-between">
              <span className="font-medium text-slate-700">Hạn mức dải mã của tổ chức:</span>
              {quotaLoading ? (
                <span className="text-xs text-muted-foreground">Đang tải...</span>
              ) : !quota?.hasCodeRange ? (
                <span className="flex items-center gap-1 font-semibold text-amber-600">
                  <AlertTriangle className="h-4 w-4" />
                  Chưa được cấp dải mã
                </span>
              ) : (
                <span className="font-bold text-emerald-600">
                  Còn {quota.remainingCount.toLocaleString()} /{' '}
                  {quota.totalLimit.toLocaleString()} mã
                  <span className="ml-2 text-xs font-normal text-muted-foreground">
                    (đã dùng {quota.usedCount.toLocaleString()})
                  </span>
                </span>
              )}
            </div>
            {showWarning && (
              <p className="mt-1 text-xs text-amber-600">
                Hạn mức còn lại dưới 20%. Bạn nên gửi yêu cầu cấp bổ sung trước khi hết mã.
              </p>
            )}
          </div>

          {/* Số lượng đề nghị */}
          <div className="space-y-1.5">
            <Label htmlFor="supplement-quantity">
              Số lượng đề nghị <span className="text-red-600">*</span>
            </Label>
            <Input
              id="supplement-quantity"
              type="number"
              min={1}
              step={1}
              placeholder="VD: 500"
              value={requestedQuantity}
              onChange={(e) => setRequestedQuantity(e.target.value)}
            />
          </div>

          {/* Lý do */}
          <div className="space-y-1.5">
            <Label htmlFor="supplement-reason">
              Lý do đề nghị <span className="text-red-600">*</span>
            </Label>
            <Textarea
              id="supplement-reason"
              placeholder="VD: Vụ thu đông sản lượng cao, cần thêm tem truy xuất"
              value={reason}
              rows={3}
              onChange={(e) => {
                setReason(e.target.value);
                if (error) setError(null);
              }}
            />
          </div>

          {/* Bằng chứng sản lượng */}
          <div className="space-y-1.5">
            <Label>
              Bằng chứng sản lượng thực (sự kiện thu hoạch / sơ chế){' '}
              <span className="text-red-600">*</span>
            </Label>
            {eventsLoading ? (
              <p className="text-sm text-muted-foreground">Đang tải sự kiện...</p>
            ) : events.length === 0 ? (
              <p className="text-sm text-amber-600">
                Tổ chức chưa có sự kiện thu hoạch hoặc sơ chế nào để làm bằng chứng.
              </p>
            ) : (
              <div className="max-h-48 space-y-2 overflow-y-auto rounded-lg border p-3">
                {events.map((event) => (
                  <label
                    key={event.eventId}
                    className="flex cursor-pointer items-start gap-2 text-sm"
                  >
                    <input
                      type="checkbox"
                      className="mt-1"
                      checked={selectedEventIds.includes(event.eventId)}
                      onChange={() => toggleEvent(event.eventId)}
                    />
                    <span>
                      <span className="font-medium">
                        {EVIDENCE_TYPE_LABEL[event.eventType] || event.eventType}
                      </span>
                      <span className="text-muted-foreground">
                        {' '}
                        — {new Date(event.recordedAt).toLocaleString('vi-VN')} —{' '}
                        {event.recordedByName}
                      </span>
                      {event.productionLotName && (
                        <span className="block text-xs text-muted-foreground">
                          Lô: {event.productionLotName}
                        </span>
                      )}
                    </span>
                  </label>
                ))}
              </div>
            )}
            <p className="text-xs text-muted-foreground">
              Đã chọn {selectedEventIds.length} sự kiện.
            </p>
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <div className="flex items-center justify-end gap-2 border-t border-slate-100 pt-3">
            <Button variant="outline" onClick={onClose} disabled={submitting}>
              Hủy
            </Button>
            <Button onClick={() => void handleSubmit()} disabled={submitting}>
              {submitting && <Loader2 className="size-4 animate-spin" />}
              {submitting ? 'Đang gửi...' : 'Gửi yêu cầu'}
            </Button>
          </div>

          {/* Yêu cầu của tổ chức */}
          <div className="space-y-2 border-t border-slate-100 pt-3">
            <h3 className="text-sm font-semibold text-slate-900">Yêu cầu của tổ chức</h3>
            <DataTableShell
              colSpan={5}
              header={
                <>
                  <TableHead className="w-12 text-center">STT</TableHead>
                  <TableHead>SL đề nghị</TableHead>
                  <TableHead>SL thực cấp</TableHead>
                  <TableHead>Thời điểm</TableHead>
                  <TableHead>Trạng thái</TableHead>
                </>
              }
              body={myRequests.map((item, index) => (
                <TableRow key={item.id} className="hover:bg-muted/40 transition-colors">
                  <TableCell className="text-center font-medium text-muted-foreground">
                    {index + 1}
                  </TableCell>
                  <TableCell className="font-medium">
                    {item.requestedQuantity.toLocaleString()}
                  </TableCell>
                  <TableCell>
                    {item.approvedQuantity != null
                      ? item.approvedQuantity.toLocaleString()
                      : '—'}
                  </TableCell>
                  <TableCell>
                    {new Date(item.requestedAt).toLocaleString('vi-VN')}
                  </TableCell>
                  <TableCell>
                    <StatusBadge
                      label={MY_STATUS_LABEL[item.status]}
                      tone={MY_STATUS_TONE[item.status]}
                    />
                  </TableCell>
                </TableRow>
              ))}
              loading={myLoading}
              empty={!myLoading && myRequests.length === 0}
              loadingMessage="Đang tải yêu cầu của tổ chức..."
              emptyMessage="Chưa có yêu cầu cấp bổ sung nào."
            />
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};


const MY_STATUS_LABEL: Record<string, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
};

interface CodeRangeSupplementDialogProps {
  open: boolean;
  onClose: () => void;
}
