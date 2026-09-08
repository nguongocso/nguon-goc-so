import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  AlertCircle,
  Camera,
  Download,
  History,
  RefreshCw,
  Search,
  X,
} from 'lucide-react';
import { toast } from 'sonner';

import { exportTraceCodes, getShipmentTraceCodes } from '@/api/traceCodeApi';
import { getShipmentById } from '@/api/shipmentApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { Card, CardContent } from '@/components/ui/card';
import { DataTableShell } from '@/components/common/DataTableShell';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import type { Shipment } from '@/types/shipment';
import type { TraceCodeSummary } from '@/types/traceCode';
import {
  TRACE_CODE_STATUS_LABELS,
  TraceCodeStatusBadge,
} from '@/components/shipment/TraceCodeStatusBadge';
import { TraceCodeHistoryDialog } from '@/components/shipment/TraceCodeHistoryDialog';
import { TraceCodeQrScanModal } from '@/components/shipment/TraceCodeQrScanModal';

const STATUS_FILTER_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'INACTIVE', label: TRACE_CODE_STATUS_LABELS.INACTIVE },
  { value: 'ACTIVE', label: TRACE_CODE_STATUS_LABELS.ACTIVE },
  { value: 'LOCKED', label: TRACE_CODE_STATUS_LABELS.LOCKED },
  { value: 'CANCELLED', label: TRACE_CODE_STATUS_LABELS.CANCELLED },
  { value: 'RECALLED', label: TRACE_CODE_STATUS_LABELS.RECALLED },
  { value: 'SUSPECT', label: TRACE_CODE_STATUS_LABELS.SUSPECT },
] as const;

const STATUS_FILTER_LABEL_MAP: Record<string, string> = {
  ALL: 'Tất cả trạng thái',
  ...TRACE_CODE_STATUS_LABELS,
};

const formatDateTime = (value?: string | null) => {
  if (!value) return '—';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString('vi-VN');
};

export default function ShipmentTraceCodesPage() {
  const { shipmentId, lotId } = useParams<{
    shipmentId: string;
    lotId?: string;
  }>();

  const backUrl = lotId
    ? `/production-lots/${lotId}/shipments/${shipmentId}`
    : `/shipments/${shipmentId}`;

  // ── Data state ─────────────────────────────────────────────────────────────
  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [codes, setCodes] = useState<TraceCodeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // ── Filters & Pagination ───────────────────────────────────────────────────
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchInput, setSearchInput] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // ── Actions & Modals ───────────────────────────────────────────────────────
  const [exporting, setExporting] = useState(false);
  const [historyCode, setHistoryCode] = useState<string | null>(null);
  const [showQrScanModal, setShowQrScanModal] = useState(false);

  // ── Đồng bộ Breadcrumb ─────────────────────────────────────────────────────
  useSetBreadcrumb(
    shipment
      ? [
          { label: 'Tổng quan', href: '/dashboard' },
          { label: 'Lô sản xuất', href: '/production-lots' },
          ...(lotId
            ? [
                {
                  label: shipment.productionLotName || 'Chi tiết lô sản xuất',
                  href: `/production-lots/${lotId}`,
                },
              ]
            : []),
          {
            label: shipment.name || 'Chi tiết lô hàng',
            href: backUrl,
          },
          { label: 'Mã tem truy xuất' },
        ]
      : null,
  );

  // ── Load Shipment info ─────────────────────────────────────────────────────
  useEffect(() => {
    if (!shipmentId) return;
    getShipmentById(shipmentId)
      .then(setShipment)
      .catch((err: any) => {
        console.error('Không thể tải thông tin lô hàng:', err);
      });
  }, [shipmentId]);

  // ── Load Trace Codes ───────────────────────────────────────────────────────
  const loadTraceCodes = useCallback(() => {
    if (!shipmentId) return;

    setLoading(true);
    setError(null);

    const params = {
      status: statusFilter === 'ALL' ? undefined : statusFilter,
      search: searchQuery ? searchQuery.trim() : undefined,
      page,
      size: pageSize,
    };

    getShipmentTraceCodes(shipmentId, params)
      .then((res) => {
        setCodes(res.items || []);
        setTotalPages(res.totalPages || 0);
        setTotalElements(res.totalElements || 0);
      })
      .catch((err: any) => {
        const msg =
          err?.response?.data?.message ||
          'Không thể tải danh sách mã tem. Vui lòng kiểm tra quyền truy cập.';
        setError(msg);
        toast.error(msg);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [shipmentId, statusFilter, searchQuery, page, pageSize]);

  useEffect(() => {
    loadTraceCodes();
  }, [loadTraceCodes]);

  // ── Search & Filter Handlers ───────────────────────────────────────────────
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setSearchQuery(searchInput);
  };

  const handleClearSearch = () => {
    setSearchInput('');
    setSearchQuery('');
    setPage(0);
  };

  const handleStatusChange = (val: string | null) => {
    setStatusFilter(val || 'ALL');
    setPage(0);
  };

  // ── QR Scan Success Handler (TC-02) ────────────────────────────────────────
  const handleScanSuccess = (scannedCode: string) => {
    toast.success(`Đã nhận diện mã: ${scannedCode}`);
    setSearchInput(scannedCode);
    setSearchQuery(scannedCode);
    setPage(0);
    // Tự động mở lịch sử theo yêu cầu TC-02
    setHistoryCode(scannedCode);
  };

  // ── Export CSV Handler ─────────────────────────────────────────────────────
  const handleExport = async () => {
    if (!shipmentId) return;

    try {
      setExporting(true);
      const blob = await exportTraceCodes(shipmentId, {
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        search: searchQuery ? searchQuery.trim() : undefined,
      });

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `Danh_sach_ma_tem_${shipmentId.slice(0, 8)}_${Date.now()}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      toast.success('Xuất file CSV danh sách mã tem thành công!');
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message || 'Lỗi khi xuất file CSV danh sách mã tem.',
      );
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* ── Header ── */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-3">
            Trạng thái & Lịch sử mã tem
            {shipment && (
              <span className="text-sm font-normal text-muted-foreground bg-muted px-2.5 py-1 rounded-md">
                {shipment.name}
              </span>
            )}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Theo dõi trạng thái, thời điểm in, kích hoạt và lịch sử quét của từng mã truy xuất trong lô hàng (NCL-04-CN-008).
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            className="border-emerald-200 text-emerald-700 hover:bg-emerald-50"
            onClick={() => setShowQrScanModal(true)}
          >
            <Camera className="mr-2 h-4 w-4 text-emerald-600" />
            Quét mã QR
          </Button>

          <Button
            onClick={handleExport}
            disabled={exporting || loading || totalElements === 0}
            className="bg-emerald-700 hover:bg-emerald-800 text-white"
          >
            {exporting ? (
              <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <Download className="mr-2 h-4 w-4" />
            )}
            Xuất file CSV
          </Button>
        </div>
      </div>

      {/* ── Error banner if any ── */}
      {error && (
        <div className="flex items-center gap-3 p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-sm">
          <AlertCircle className="h-5 w-5 text-rose-600 shrink-0" />
          <div className="flex-1">
            <p className="font-semibold">Lỗi tải dữ liệu</p>
            <p className="text-xs text-rose-700">{error}</p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={loadTraceCodes}
            className="border-rose-300 text-rose-700 hover:bg-rose-100 shrink-0"
          >
            Thử lại
          </Button>
        </div>
      )}

      {/* ── Filter toolbar ── */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-muted/30 p-4 rounded-xl border">
        <div className="flex flex-wrap items-center gap-3 flex-1">
          {/* Status Filter */}
          <div className="w-56 min-w-[200px]">
            <Select value={statusFilter} onValueChange={handleStatusChange}>
              <SelectTrigger className="w-full min-w-[200px] bg-white">
                <SelectValue placeholder="Trạng thái mã tem">
                  {STATUS_FILTER_LABEL_MAP[statusFilter] || 'Tất cả trạng thái'}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                {STATUS_FILTER_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Search form */}
          <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 flex-1 min-w-[240px] max-w-md">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Tìm theo mã tem (VD: HTX01-...)"
                className="pl-9 pr-8 bg-white"
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={handleClearSearch}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>
            <Button type="submit" variant="secondary" size="default">
              Tìm kiếm
            </Button>
          </form>
        </div>

        {/* Stats */}
        <div className="text-xs text-muted-foreground font-medium">
          Tổng số: <span className="text-foreground font-semibold">{totalElements}</span> mã tem
        </div>
      </div>

      {/* ── Table & Pagination Card ── */}
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardContent className="p-4 space-y-4">
          <DataTableShell
            loading={loading}
            empty={!loading && codes.length === 0}
            colSpan={7}
            emptyMessage="Chưa có mã tem nào cho lô hàng này."
            header={
              <>
                <TableHead className="w-14 text-center">STT</TableHead>
                <TableHead className="min-w-[200px]">Mã tem truy xuất</TableHead>
                <TableHead className="w-36">Trạng thái</TableHead>
                <TableHead className="w-44">Ngày in tem</TableHead>
                <TableHead className="w-44">Ngày kích hoạt</TableHead>
                <TableHead className="w-28 text-center">Lượt quét</TableHead>
                <TableHead className="w-28 text-right pr-4">Hành động</TableHead>
              </>
            }
            body={
              codes.map((code, index) => (
                <TableRow key={code.id} className="hover:bg-muted/40 transition-colors">
                  <TableCell className="text-center text-xs text-muted-foreground font-mono">
                    {page * pageSize + index + 1}
                  </TableCell>
                  <TableCell>
                    <span className="font-mono font-semibold text-emerald-900 bg-emerald-50/80 px-2 py-0.5 rounded border border-emerald-200">
                      {code.codeValue}
                    </span>
                  </TableCell>
                  <TableCell>
                    <TraceCodeStatusBadge status={code.status} />
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {formatDateTime(code.printedAt)}
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {formatDateTime(code.activatedAt)}
                  </TableCell>
                  <TableCell className="text-center font-semibold text-sm text-foreground">
                    {code.scanCount}
                  </TableCell>
                  <TableCell className="text-right pr-4">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setHistoryCode(code.codeValue)}
                      className="h-8 gap-1.5 text-xs border-slate-300 hover:bg-slate-100"
                    >
                      <History className="h-3.5 w-3.5 text-blue-600" />
                      Lịch sử
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            }
          />

          {/* ── Pagination ── */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
              <p className="text-xs text-muted-foreground">
                Trang <span className="font-medium text-foreground">{page + 1}</span> / {totalPages}
              </p>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0 || loading}
                >
                  Trang trước
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1 || loading}
                >
                  Trang sau
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* ── Modals ── */}
      <TraceCodeHistoryDialog
        open={Boolean(historyCode)}
        onOpenChange={(open) => {
          if (!open) setHistoryCode(null);
        }}
        codeValue={historyCode}
      />

      <TraceCodeQrScanModal
        open={showQrScanModal}
        onOpenChange={setShowQrScanModal}
        onScanSuccess={handleScanSuccess}
      />
    </div>
  );
}
