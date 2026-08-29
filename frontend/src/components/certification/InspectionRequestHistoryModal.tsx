import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Eye,
  RefreshCw,
  Search,
  X,
} from "lucide-react";
import { getInspectionRequests } from "@/api/certificationApi";
import type {
  InspectionRequestListItem,
  InspectionRequestStatusDisplay,
  InspectionRequestStatusQuery,
} from "@/types/certification";
import type { PageResponse } from "@/types/common";

// Ánh xạ trạng thái yêu cầu kiểm nghiệm sang tiếng Việt + màu sắc
const INSPECTION_STATUS_MAP: Record<
  InspectionRequestStatusDisplay,
  { label: string; className: string }
> = {
  PENDING: {
    label: "Chờ kết quả",
    className: "bg-yellow-100 text-yellow-800 border-yellow-300",
  },
  PASSED: {
    label: "Đạt",
    className: "bg-emerald-100 text-emerald-800 border-emerald-300",
  },
  FAILED: {
    label: "Không đạt",
    className: "bg-red-100 text-red-800 border-red-300",
  },
  CANCELLED: {
    label: "Đã hủy",
    className: "bg-gray-100 text-gray-700 border-gray-300",
  },
};

const STATUS_FILTER_OPTIONS: Array<{
  value: InspectionRequestStatusQuery | "ALL";
  label: string;
}> = [
  { value: "ALL", label: "Tất cả trạng thái" },
  { value: "PENDING_RESULT", label: "Chờ kết quả" },
  { value: "PASSED", label: "Đạt" },
  { value: "FAILED", label: "Không đạt" },
  { value: "CANCELLED", label: "Đã hủy" },
];

// Số dòng hiển thị mỗi trang trong modal (nhiều dòng hơn nhờ layout mở rộng)
const PAGE_SIZE = 15;

const getInspectionStatusBadge = (status: InspectionRequestStatusDisplay) => {
  const config = INSPECTION_STATUS_MAP[status];
  if (!config) {
    return <Badge variant="outline">{status}</Badge>;
  }
  return (
    <Badge
      variant="outline"
      className={`${config.className} border px-2.5 py-0.5 text-xs font-semibold`}
    >
      {config.label}
    </Badge>
  );
};

// Định dạng ngày kiểu "YYYY-MM-DD" sang tiếng Việt
const formatDateOnly = (dateStr: string) => {
  if (!dateStr) return "—";
  const date = new Date(`${dateStr}T00:00:00`);
  if (Number.isNaN(date.getTime())) return dateStr;
  return date.toLocaleDateString("vi-VN");
};

interface InspectionRequestHistoryModalProps {
  open: boolean;
  onClose: () => void;
  lotId: string;
  canInspect: boolean;
}

export const InspectionRequestHistoryModal = ({
  open,
  onClose,
  lotId,
  canInspect,
}: InspectionRequestHistoryModalProps) => {
  const navigate = useNavigate();
  const panelRef = useRef<HTMLDivElement>(null);

  const [items, setItems] = useState<InspectionRequestListItem[]>([]);
  const [pageData, setPageData] =
    useState<PageResponse<InspectionRequestListItem> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<InspectionRequestStatusQuery | "ALL">(
    "ALL",
  );
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");

  const load = useCallback(
    async (
      nextStatus: InspectionRequestStatusQuery | "ALL",
      nextPage: number,
    ) => {
      try {
        setLoading(true);
        setError(null);
        const data = await getInspectionRequests({
          lotId,
          status: nextStatus === "ALL" ? undefined : nextStatus,
          page: nextPage,
          size: PAGE_SIZE,
        });
        setItems(data.items);
        setPageData(data);
      } catch {
        setItems([]);
        setPageData(null);
        setError("Không thể tải danh sách yêu cầu kiểm nghiệm.");
      } finally {
        setLoading(false);
      }
    },
    [lotId],
  );

  // Khi mở modal: reset bộ lọc/trang và nạp lại dữ liệu
  useEffect(() => {
    if (open) {
      setSearch("");
      setStatus("ALL");
      setPage(0);
      void load("ALL", 0);
    }
  }, [open, load]);

  // ── Đóng modal: tự quản lý toàn bộ, KHÔNG phụ thuộc cơ chế của Base UI ──
  // Panel này không phải DialogPrimitive.Popup nên các primitive (Close,
  // Backdrop, Root.dismiss) sẽ không hoạt động. Do đó cả 3 đường đóng đều
  // được wire trực tiếp tới callback onClose() của trang cha:
  //   1. Nút X            → onClick={onClose}
  //   2. Click lớp nền    → onClick={onClose}
  //   3. Phím Esc         → global keydown listener (hoạt động cả khi focus
  //                          vẫn còn ở nút ">>>" phía sau overlay)
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.stopPropagation();
        onClose();
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open, onClose]);

  // Khoá cuộn nền phía sau modal khi đang mở
  useEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [open]);

  // Tự động focus panel khi mở để thao tác bàn phím mượt hơn
  useEffect(() => {
    if (open) {
      panelRef.current?.focus({ preventScroll: true });
    }
  }, [open]);

  const handleStatusChange = (value: string | null) => {
    const next = (value ?? "ALL") as InspectionRequestStatusQuery | "ALL";
    setStatus(next);
    setPage(0);
    void load(next, 0);
  };

  const handlePageChange = (nextPage: number) => {
    if (nextPage < 0 || !pageData) return;
    setPage(nextPage);
    void load(status, nextPage);
  };

  // Tìm kiếm phía client trên trang hiện tại (theo mã/lô/đơn vị/ngày)
  const filteredItems = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return items;
    return items.filter(
      (request) =>
        request.testRequestId.toLowerCase().includes(keyword) ||
        request.lotCode?.toLowerCase().includes(keyword) ||
        request.testingUnit?.toLowerCase().includes(keyword) ||
        formatDateOnly(request.sampleSentDate).includes(keyword),
    );
  }, [items, search]);

  const totalRows = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;
  const rangeStart = totalRows === 0 ? 0 : page * PAGE_SIZE + 1;
  const rangeEnd = Math.min((page + 1) * PAGE_SIZE, totalRows);

  if (!open) return null;

  return createPortal(
    <div
      data-slot="inspection-history-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="inspection-history-title"
      aria-describedby="inspection-history-description"
      className="fixed inset-0 z-50 flex items-center justify-center p-2 outline-none sm:p-4"
    >
      {/* Lớp nền tối — click để đóng */}
      <div
        aria-hidden="true"
        onClick={onClose}
        className="absolute inset-0 animate-in fade-in-0 bg-black/40 backdrop-blur-sm duration-100"
      />

      {/* Panel mở rộng gần toàn màn hình */}
      <div
        ref={panelRef}
        tabIndex={-1}
        data-slot="inspection-history-panel"
        className="relative flex h-[94vh] w-full max-w-[1700px] animate-in flex-col overflow-hidden rounded-xl border border-emerald-100 bg-background text-foreground shadow-2xl outline-none duration-100 fade-in-0 zoom-in-95"
      >
        {/* ── Header ─────────────────────────────────────────────────── */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-emerald-100 bg-gradient-to-r from-emerald-50 via-white to-white px-5 py-4 sm:px-6">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-emerald-600 text-white">
              <ClipboardList className="h-5 w-5" />
            </div>
            <div>
              <h2
                id="inspection-history-title"
                className="text-xl font-semibold text-emerald-800"
              >
                Lịch sử yêu cầu kiểm nghiệm
              </h2>
              <p
                id="inspection-history-description"
                className="text-sm text-muted-foreground"
              >
                Xem trực quan toàn bộ lịch sử yêu cầu kiểm nghiệm của lô sản
                xuất — kèm bộ lọc, tìm kiếm, phân trang và thao tác xem / ghi
                nhận kết quả.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            {/* Tóm tắt nhanh */}
            <div className="hidden items-center gap-2 rounded-lg border border-emerald-100 bg-white px-4 py-2 text-sm md:flex">
              <span className="text-muted-foreground">Tổng số:</span>
              <span className="font-semibold tabular-nums text-emerald-700">
                {loading ? "..." : totalRows}
              </span>
              <span className="text-muted-foreground">yêu cầu</span>
            </div>
            <Button
              variant="ghost"
              size="icon-sm"
              className="h-9 w-9 rounded-lg hover:bg-emerald-50 hover:text-emerald-800"
              aria-label="Đóng"
              onClick={onClose}
            >
              <X className="h-5 w-5" />
            </Button>
          </div>
        </div>

        {/* ── Thanh tìm kiếm + làm mới + bộ lọc trạng thái ───────────── */}
        <div className="flex flex-col gap-3 border-b border-gray-100 bg-muted/30 px-4 py-3 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <div className="relative w-full sm:max-w-md">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              className="pl-9"
              placeholder="Tìm theo mã, lô, đơn vị kiểm nghiệm..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <div className="flex items-center gap-3">
            <div className="w-full sm:w-48">
              <Select
                items={STATUS_FILTER_OPTIONS}
                value={status}
                onValueChange={handleStatusChange}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Lọc trạng thái" />
                </SelectTrigger>
                <SelectContent>
                  {STATUS_FILTER_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <Button
                variant="outline"
                size="sm"
                disabled={loading}
                onClick={() => void load(status, page)}
            >
              <RefreshCw
                  className={`mr-1 size-4 ${loading ? "animate-spin" : ""}`}
              />
              Làm mới
            </Button>
          </div>
        </div>

        {/* ── Nội dung: bảng nằm gọn trong khung, có khoảng đệm bao quanh ── */}
        <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-4 py-4 sm:px-6 sm:py-5">
          <div className="overflow-x-auto rounded-md border bg-card">
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/50">
                  <TableHead className="w-12 text-center">STT</TableHead>
                  <TableHead className="min-w-[130px]">Mã yêu cầu</TableHead>
                  <TableHead className="min-w-[150px]">Lô sản xuất</TableHead>
                  <TableHead className="min-w-[220px]">
                    Đơn vị kiểm nghiệm
                  </TableHead>
                  <TableHead className="min-w-[130px]">Ngày gửi mẫu</TableHead>
                  <TableHead className="min-w-[120px]">Số chỉ tiêu</TableHead>
                  <TableHead className="min-w-[110px]">Trạng thái</TableHead>
                  {canInspect && (
                    <TableHead className="w-[190px] text-center">
                      Thao tác
                    </TableHead>
                  )}
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell
                      colSpan={canInspect ? 8 : 7}
                      className="h-32 text-center text-muted-foreground"
                    >
                      <div className="flex flex-col items-center justify-center gap-2">
                        <RefreshCw className="h-6 w-6 animate-spin text-emerald-600" />
                        <span>Đang tải danh sách yêu cầu kiểm nghiệm...</span>
                      </div>
                    </TableCell>
                  </TableRow>
                ) : error ? (
                  <TableRow>
                    <TableCell
                      colSpan={canInspect ? 8 : 7}
                      className="h-32 text-center text-red-600"
                    >
                      <div className="flex flex-col items-center justify-center gap-3">
                        <span>{error}</span>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => void load(status, page)}
                        >
                          Thử lại
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ) : filteredItems.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={canInspect ? 8 : 7}
                      className="h-32 text-center text-muted-foreground"
                    >
                      Không có yêu cầu kiểm nghiệm phù hợp.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredItems.map((request, index) => (
                    <TableRow
                      key={request.testRequestId}
                      className="align-middle transition-colors hover:bg-muted/40"
                    >
                      <TableCell className="text-center font-medium text-muted-foreground">
                        {page * PAGE_SIZE + index + 1}
                      </TableCell>
                      <TableCell>
                        <span
                          className="font-mono text-sm font-semibold"
                          title={request.testRequestId}
                        >
                          #{request.testRequestId.slice(0, 8)}
                        </span>
                      </TableCell>
                      <TableCell className="font-medium">
                        {request.lotCode}
                      </TableCell>
                      <TableCell>{request.testingUnit}</TableCell>
                      <TableCell>
                        {formatDateOnly(request.sampleSentDate)}
                      </TableCell>
                      <TableCell>
                        {request.criteriaCount}
                        {request.failedCriteriaCount > 0 && (
                          <span className="text-red-600">
                            {" "}
                            · {request.failedCriteriaCount} không đạt
                          </span>
                        )}
                      </TableCell>
                      <TableCell>
                        {getInspectionStatusBadge(request.status)}
                      </TableCell>
                      {canInspect && (
                        <TableCell className="text-center">
                          <div className="flex items-center justify-center gap-1.5">
                            {request.status === "PASSED" ||
                            request.status === "FAILED" ? (
                              <Button
                                size="sm"
                                variant="outline"
                                className="h-8 w-8 p-0 text-slate-600 hover:bg-emerald-50 hover:text-emerald-800"
                                title="Xem kết quả kiểm nghiệm"
                                onClick={() =>
                                  navigate(
                                    `/production-lots/${lotId}/inspection-requests/${request.testRequestId}/results`,
                                  )
                                }
                              >
                                <Eye className="h-4 w-4" />
                              </Button>
                            ) : (
                              <Button
                                size="sm"
                                variant="outline"
                                className="text-xs font-semibold"
                                onClick={() =>
                                  navigate(
                                    `/production-lots/${lotId}/inspection-requests/${request.testRequestId}/results`,
                                  )
                                }
                              >
                                {request.status === "PENDING"
                                  ? "Ghi nhận kết quả"
                                  : "Xem chi tiết"}
                              </Button>
                            )}
                          </div>
                        </TableCell>
                      )}
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          {/* ── Phân trang (kiểu PartnerApiKeyListPage) ────────────────── */}
          <div className="flex flex-wrap items-center justify-between gap-3 pb-1 text-xs text-muted-foreground sm:text-sm">
            <div>
              Hiển thị {rangeStart} – {rangeEnd} trên tổng số {totalRows} yêu
              cầu
            </div>
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="sm"
                disabled={(pageData?.first ?? true) || loading}
                onClick={() => handlePageChange(Math.max(0, page - 1))}
              >
                <ChevronLeft className="mr-1 h-4 w-4" />
                Trang trước
              </Button>
              <span className="px-2 font-medium tabular-nums">
                {totalPages > 0 ? page + 1 : 0}/{totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={(pageData?.last ?? true) || loading}
                onClick={() => handlePageChange(page + 1)}
              >
                Trang sau
                <ChevronRight className="ml-1 h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>,
    document.body,
  );
};
