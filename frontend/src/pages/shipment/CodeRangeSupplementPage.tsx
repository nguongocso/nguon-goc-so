import { useCallback, useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { AlertTriangle, Hash, Loader2, Send } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { DataTableShell } from '@/components/common/DataTableShell';
import { FilterSelect } from '@/components/common/FilterSelect';
import { ListCard } from '@/components/common/ListCard';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListToolbar } from '@/components/common/ListToolbar';
import { Pagination } from '@/components/common/Pagination';
import { RefreshButton } from '@/components/common/RefreshButton';
import { SearchInput } from '@/components/common/SearchInput';
import { StatusBadge } from '@/components/common/StatusBadge';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { HelpButton } from '@/components/help/HelpButton';
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

const EVIDENCE_TYPE_LABEL: Record<string, string> = {
  HARVEST: 'Thu hoạch',
  PREPROCESSING: 'Sơ chế',
};

const TYPE_FILTER_OPTIONS = [
  { value: 'ALL', label: 'Tất cả loại' },
  { value: 'HARVEST', label: 'Thu hoạch' },
  { value: 'PREPROCESSING', label: 'Sơ chế' },
];

const MY_STATUS_LABEL: Record<string, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
};

const MY_STATUS_TONE = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
} as const;

const EVIDENCE_PAGE_SIZE = 10;

/**
 * NCL-04-CN-007: Trang gửi yêu cầu cấp bổ sung dải mã truy xuất (VT-02).
 *
 * Thay thế `CodeRangeSupplementDialog`: mở từ tab "Lô hàng & Mã QR" của trang
 * chi tiết lô sản xuất và cảnh báo hạn mức ở màn hình tạo lô hàng.
 * Bằng chứng sản lượng thực là bảng có tìm kiếm + phân trang; không có nút
 * Quay lại — gửi xong ở lại trang xem danh sách yêu cầu của tổ chức.
 */
export const CodeRangeSupplementPage = () => {
  const { user } = useAuth();

  const [quota, setQuota] = useState<RemainingCodesResponse | null>(null);
  const [quotaLoading, setQuotaLoading] = useState(false);

  const [requestedQuantity, setRequestedQuantity] = useState('');
  const [reason, setReason] = useState('');

  const [events, setEvents] = useState<EvidenceEvent[]>([]);
  const [eventsLoading, setEventsLoading] = useState(false);
  const [selectedEventIds, setSelectedEventIds] = useState<string[]>([]);
  const [evidenceSearch, setEvidenceSearch] = useState('');
  const [evidenceType, setEvidenceType] = useState('ALL');
  const [evidencePage, setEvidencePage] = useState(0);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [myRequests, setMyRequests] = useState<CodeRangeSupplementRequest[]>([]);
  const [myLoading, setMyLoading] = useState(false);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Yêu cầu cấp bổ sung mã truy xuất' },
  ]);

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

  const loadEvents = useCallback(async () => {
    try {
      setEventsLoading(true);
      const data = await getEvidenceEvents();
      setEvents(data);
    } catch {
      setEvents([]);
      toast.error('Không thể tải danh sách sự kiện bằng chứng.');
    } finally {
      setEventsLoading(false);
    }
  }, []);

  useEffect(() => {
    resetForm();
    setEvents([]);

    if (user?.organizationId) {
      setQuotaLoading(true);
      getRemainingCodes(user.organizationId)
        .then(setQuota)
        .catch(() => setQuota(null))
        .finally(() => setQuotaLoading(false));
    }

    void loadEvents();
    void loadMyRequests();
  }, [user?.organizationId, resetForm, loadEvents, loadMyRequests]);

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

  const filteredEvents = useMemo(() => {
    const q = evidenceSearch.trim().toLowerCase();
    return events.filter((event) => {
      const matchType = evidenceType === 'ALL' || event.eventType === evidenceType;
      const matchKeyword =
        !q ||
        (event.productionLotName ?? '').toLowerCase().includes(q) ||
        (EVIDENCE_TYPE_LABEL[event.eventType] ?? event.eventType).toLowerCase().includes(q) ||
        (event.recordedByName ?? '').toLowerCase().includes(q);
      return matchType && matchKeyword;
    });
  }, [events, evidenceSearch, evidenceType]);

  const evidenceTotalPages = Math.max(1, Math.ceil(filteredEvents.length / EVIDENCE_PAGE_SIZE));
  const evidenceSafePage = Math.min(evidencePage, evidenceTotalPages - 1);
  const pagedEvents = filteredEvents.slice(
    evidenceSafePage * EVIDENCE_PAGE_SIZE,
    evidenceSafePage * EVIDENCE_PAGE_SIZE + EVIDENCE_PAGE_SIZE,
  );

  const pagedSelectedCount = pagedEvents.filter((e) => selectedEventIds.includes(e.eventId)).length;
  const allPagedSelected = pagedEvents.length > 0 && pagedSelectedCount === pagedEvents.length;

  const togglePage = () => {
    if (allPagedSelected) {
      const pageIds = new Set(pagedEvents.map((e) => e.eventId));
      setSelectedEventIds((prev) => prev.filter((id) => !pageIds.has(id)));
    } else {
      const current = new Set(selectedEventIds);
      pagedEvents.forEach((e) => current.add(e.eventId));
      setSelectedEventIds([...current]);
    }
  };

  const handleSubmit = async () => {
    setError(null);

    if (requestedQuantity.trim() === '') {
      setError('Số lượng đề nghị không được để trống');
      return;
    }

    const parsed = createSupplementSchema.safeParse({
      requestedQuantity: Number(requestedQuantity),
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
      toast.success('Yêu cầu cấp bổ sung mã đã được gửi. Vui lòng chờ quản trị viên phê duyệt.');
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

  const evidenceHeader = (
    <>
      <TableHead className="w-10 text-center">
        <input
          type="checkbox"
          className="align-middle"
          checked={allPagedSelected}
          onChange={togglePage}
          aria-label="Chọn tất cả sự kiện trong trang"
        />
      </TableHead>
      <TableHead>Loại sự kiện</TableHead>
      <TableHead>Lô sản xuất</TableHead>
      <TableHead>Thời điểm ghi</TableHead>
      <TableHead>Người ghi</TableHead>
      <TableHead className="text-right">Sản lượng thực</TableHead>
    </>
  );

  const evidenceBody = pagedEvents.map((event) => {
    const checked = selectedEventIds.includes(event.eventId);
    return (
      <TableRow
        key={event.eventId}
        className="hover:bg-muted/40 transition-colors cursor-pointer"
        onClick={() => toggleEvent(event.eventId)}
      >
        <TableCell className="text-center" onClick={(e) => e.stopPropagation()}>
          <input
            type="checkbox"
            className="align-middle"
            checked={checked}
            onChange={() => toggleEvent(event.eventId)}
            aria-label={`Chọn sự kiện ${event.eventId}`}
          />
        </TableCell>
        <TableCell className="font-medium">
          {EVIDENCE_TYPE_LABEL[event.eventType] || event.eventType}
        </TableCell>
        <TableCell>{event.productionLotName || '—'}</TableCell>
        <TableCell>{new Date(event.recordedAt).toLocaleString('vi-VN')}</TableCell>
        <TableCell>{event.recordedByName || '—'}</TableCell>
        <TableCell className="text-right font-medium text-emerald-700 tabular-nums">
          {event.quantity != null ? event.quantity.toLocaleString('vi-VN') : '—'}
        </TableCell>
      </TableRow>
    );
  });

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={Hash}
        title="Yêu cầu cấp bổ sung mã truy xuất"
        description="Gửi yêu cầu để quản trị viên nền tảng xét duyệt và tăng hạn mức dải mã của tổ chức."
        actions={<HelpButton screenKey="code-range-supplement-create" />}
      />

      <ListCard>
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
                Còn {quota.remainingCount.toLocaleString()} / {quota.totalLimit.toLocaleString()} mã
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

        <div className="border-t border-slate-100 pt-4">
          <h3 className="text-sm font-semibold text-slate-900">
            Bằng chứng sản lượng thực (sự kiện thu hoạch / sơ chế){' '}
            <span className="text-red-600">*</span>
          </h3>
        </div>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo tên lô, loại sự kiện, người ghi..."
                value={evidenceSearch}
                onChange={(e) => {
                  setEvidenceSearch(e.target.value);
                  setEvidencePage(0);
                }}
              />
              <FilterSelect
                value={evidenceType}
                onValueChange={(val) => {
                  setEvidenceType(val || 'ALL');
                  setEvidencePage(0);
                }}
                options={TYPE_FILTER_OPTIONS}
              />
            </>
          }
          right={
            <RefreshButton onClick={() => void loadEvents()} loading={eventsLoading} />
          }
        />

        <DataTableShell
          colSpan={6}
          header={evidenceHeader}
          body={evidenceBody}
          loading={eventsLoading}
          empty={!eventsLoading && filteredEvents.length === 0}
          loadingMessage="Đang tải sự kiện..."
          emptyMessage={
            evidenceSearch || evidenceType !== 'ALL'
              ? 'Không tìm thấy sự kiện nào phù hợp với bộ lọc.'
              : 'Tổ chức chưa có sự kiện thu hoạch hoặc sơ chế nào để làm bằng chứng.'
          }
        />

        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs text-muted-foreground">
            Đã chọn {selectedEventIds.length} / {events.length} sự kiện.
          </p>
          <div className="flex gap-1">
            <Button variant="outline" size="xs" onClick={togglePage} disabled={pagedEvents.length === 0}>
              {allPagedSelected ? 'Bỏ chọn trang này' : 'Chọn trang này'}
            </Button>
            <Button
              variant="outline"
              size="xs"
              onClick={() => setSelectedEventIds([])}
              disabled={selectedEventIds.length === 0}
            >
              Bỏ chọn tất cả
            </Button>
          </div>
        </div>

        <Pagination
          currentPage={evidenceSafePage}
          totalPages={evidenceTotalPages}
          totalElements={filteredEvents.length}
          pageSize={EVIDENCE_PAGE_SIZE}
          loading={eventsLoading}
          itemLabel="sự kiện"
          onPageChange={setEvidencePage}
        />

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex items-center justify-end gap-2 border-t border-slate-100 pt-3">
          <Button variant="create" onClick={() => void handleSubmit()} disabled={submitting}>
            {submitting ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              <Send className="size-4" />
            )}
            {submitting ? 'Đang gửi...' : 'Gửi yêu cầu'}
          </Button>
        </div>
      </ListCard>

      {/* Yêu cầu của tổ chức (card riêng) */}
      <ListCard>
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
                {item.approvedQuantity != null ? item.approvedQuantity.toLocaleString() : '—'}
              </TableCell>
              <TableCell>{new Date(item.requestedAt).toLocaleString('vi-VN')}</TableCell>
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
      </ListCard>
    </div>
  );
};

export default CodeRangeSupplementPage;
