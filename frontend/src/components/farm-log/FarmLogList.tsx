import { useState, useEffect, useMemo } from "react";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ChevronLeft,
  ChevronRight,
  Search,
  FileText,
  Plus,
  Paperclip,
  Pencil,
  MoreHorizontal,
  ClipboardList,
} from "lucide-react";
import { getFarmLogs } from "@/api/farmLogApi";
import type { FarmLog } from "@/types/farmLog";
import { useNavigate } from "react-router-dom";
import type { PageResponse } from "@/types/common";
import { useAuth } from "@/hooks/useAuth";
import { hasAnyRole, ROLE_ACCESS } from "@/config/roleAccess";
import {
  ACTIVITY_TYPE_ICONS,
  ACTIVITY_TYPE_LABELS,
  buildFarmLogGroups,
  formatDateTime,
  getActivityLabel,
  getLatestEffective,
} from "@/utils/farmLogCorrection";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

// 👇 Định nghĩa interface
interface FarmLogListProps {
  productionLotId: string;
  productionLotName?: string;
  /** Có quyền tạo nhật ký canh tác mới hay không (mặc định true để không phá các nơi gọi cũ). */
  canCreate?: boolean;
  /** NCL-03-CN-006: bật UI đính chính (cột "Hành động" + chuyển hướng). Mặc định false. */
  enableCorrection?: boolean;
}

const ACTIVITY_TYPE_OPTIONS = Object.entries(ACTIVITY_TYPE_LABELS).map(
  ([value, label]) => ({ value, label }),
);

export function FarmLogList({
  productionLotId,
  productionLotName = "",
  canCreate = true,
  enableCorrection = false,
}: FarmLogListProps) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [logs, setLogs] = useState<FarmLog[]>([]);
  const [pageInfo, setPageInfo] = useState<
    Omit<PageResponse<FarmLog>, "items">
  >({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  // Bộ lọc client
  const [searchTerm, setSearchTerm] = useState("");
  const [activityFilter, setActivityFilter] = useState<string>("ALL");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("desc");

  /**
   * NCL-03-CN-006: xác định người dùng hiện tại có được đính chính 1 nhật ký hay không.
   * VT-02 được đính chính mọi nhật ký; VT-03 chỉ đính chính nhật ký do mình ghi.
   */
  const canCorrectLog = (log: FarmLog): boolean => {
    if (!user) return false;
    if (!hasAnyRole(user.roleCode, ROLE_ACCESS.farmLogCorrect)) return false;
    if (log.isCorrected) return false; // đã bị đính chính rồi
    if (user.roleCode === "VT-02") return true;
    // VT-03: chỉ nhật ký của chính mình
    return log.createdById === user.userId;
  };

  const goToCreateLog = () => {
    navigate(`/farm-logs/create?productionLotId=${productionLotId}`);
  };

  const loadLogs = async () => {
    if (!productionLotId) return;
    setIsLoading(true);
    try {
      const response = await getFarmLogs({
        productionLotId,
        page,
        size,
      });
      setLogs(response.items);
      setPageInfo({
        page: response.page,
        size: response.size,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        first: response.first,
        last: response.last,
      });
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        "Không thể tải lịch sử nhật ký canh tác.";
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, [productionLotId, page, size]);

  // Lọc client
  const filteredLogs = useMemo(() => {
    let result = [...logs];

    if (searchTerm.trim()) {
      const keyword = searchTerm.trim().toLowerCase();
      result = result.filter(
        (log) =>
          log.material?.toLowerCase().includes(keyword) ||
          log.notes?.toLowerCase().includes(keyword) ||
          log.createdByName.toLowerCase().includes(keyword) ||
          ACTIVITY_TYPE_LABELS[log.activityType]
            ?.toLowerCase()
            .includes(keyword),
      );
    }

    if (activityFilter !== "ALL") {
      result = result.filter((log) => log.activityType === activityFilter);
    }

    if (dateFrom) {
      result = result.filter((log) => log.executedDate >= dateFrom);
    }
    if (dateTo) {
      result = result.filter((log) => log.executedDate <= dateTo);
    }

    result.sort((a, b) => {
      const dateA = new Date(a.executedDate).getTime();
      const dateB = new Date(b.executedDate).getTime();
      return sortOrder === "asc" ? dateA - dateB : dateB - dateA;
    });

    return result;
  }, [logs, searchTerm, activityFilter, dateFrom, dateTo, sortOrder]);

  // NCL-03-CN-006 (GAP): nhóm bản gốc + các bản đính chính thành một nhóm.
  const groups = useMemo(() => buildFarmLogGroups(filteredLogs), [filteredLogs]);

  const goToPage = (newPage: number) => {
    if (newPage >= 0 && newPage < pageInfo.totalPages) {
      setPage(newPage);
    }
  };

  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <CardHeader className="border-b border-slate-100">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100">
              <ClipboardList className="h-5 w-5 text-emerald-700" />
            </div>
            <div>
              <CardTitle className="text-xl font-bold text-slate-900">
                Lịch sử nhật ký canh tác
              </CardTitle>
              {productionLotName ? (
                <p className="text-sm text-muted-foreground">
                  Lô sản xuất: <span className="font-medium text-slate-800">{productionLotName}</span>
                </p>
              ) : (
                <p className="text-sm text-muted-foreground">
                  Tổng số: <span className="font-medium text-slate-800">{filteredLogs.length}</span> bản ghi
                </p>
              )}
            </div>
          </div>
          <div className="flex gap-2">
            {canCreate && (
              <Button type="button" variant="create" onClick={goToCreateLog}>
                <Plus className="mr-1.5 h-4 w-4" />
                Thêm nhật ký
              </Button>
            )}
          </div>
        </div>
      </CardHeader>
      <CardContent className="p-4 space-y-4">
        {/* Bộ lọc */}
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              className="pl-9"
              placeholder="Tìm theo vật tư, ghi chú, người ghi..."
              aria-label="Tìm kiếm nhật ký canh tác"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <Select
            value={activityFilter}
            onValueChange={(value: string | null) => {
              if (value) setActivityFilter(value);
            }}
          >
            <SelectTrigger aria-label="Lọc theo loại hoạt động">
              <SelectValue placeholder="Loại hoạt động">
                {activityFilter === "ALL"
                  ? "Tất cả loại"
                  : getActivityLabel(activityFilter)}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Tất cả loại</SelectItem>
              {ACTIVITY_TYPE_OPTIONS.map((opt) => (
                <SelectItem key={opt.value} value={opt.value}>
                  {opt.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <div className="flex items-center gap-2">
            <Input
              type="date"
              placeholder="Từ ngày"
              aria-label="Từ ngày"
              value={dateFrom}
              onChange={(e) => setDateFrom(e.target.value)}
              className="w-full"
            />
            <span className="text-muted-foreground">→</span>
            <Input
              type="date"
              placeholder="Đến ngày"
              aria-label="Đến ngày"
              value={dateTo}
              onChange={(e) => setDateTo(e.target.value)}
              className="w-full"
            />
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant={sortOrder === "desc" ? "default" : "outline"}
              size="sm"
              onClick={() => setSortOrder("desc")}
              className="flex-1"
            >
              Mới nhất
            </Button>
            <Button
              variant={sortOrder === "asc" ? "default" : "outline"}
              size="sm"
              onClick={() => setSortOrder("asc")}
              className="flex-1"
            >
              Cũ nhất
            </Button>
          </div>
        </div>

        {/* Bảng danh sách */}
        {isLoading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          </div>
        ) : filteredLogs.length === 0 ? (
          <div className="grid place-items-center px-4 py-16 text-center">
            <FileText className="mb-3 size-10 text-slate-300" />
            <p className="font-semibold text-slate-900">Chưa có nhật ký canh tác</p>
            <p className="mt-1 text-sm text-muted-foreground">Hãy kiểm tra lại lô sản xuất hoặc bộ lọc.</p>
          </div>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-slate-200">
            <Table>
              <TableHeader>
                <TableRow className="bg-slate-50/80">
                  <TableHead className="font-semibold text-slate-700 w-25">Ngày</TableHead>
                  <TableHead className="font-semibold text-slate-700 w-30">Người ghi</TableHead>
                  <TableHead className="font-semibold text-slate-700 w-35">Hoạt động</TableHead>
                  <TableHead className="font-semibold text-slate-700 w-30">Trạng thái</TableHead>
                  <TableHead className="font-semibold text-slate-700">Vật tư & Số lượng</TableHead>
                  <TableHead className="font-semibold text-slate-700">Ghi chú</TableHead>
                  <TableHead className="font-semibold text-slate-700 w-30 text-center">Chứng từ</TableHead>
                  {enableCorrection && (
                    <TableHead className="font-semibold text-slate-700 w-30 text-center">
                      Hành động
                    </TableHead>
                  )}
                </TableRow>
              </TableHeader>
                <TableBody>
                  {groups.map((group) => {
                    const effective = getLatestEffective(group);
                    const hasCorrections = group.corrections.length > 0;
                    const Icon = ACTIVITY_TYPE_ICONS[effective.activityType] ?? ClipboardList;
                    const canCorrect = enableCorrection && canCorrectLog(effective);

                    const materialQuantity =
                      effective.material && effective.quantity != null
                        ? `${effective.material} – ${effective.quantity} ${effective.unit ?? ''}`
                        : effective.material
                          ? effective.material
                          : effective.quantity != null
                            ? `${effective.quantity} ${effective.unit ?? ''}`
                            : null;

                    return (
                        <TableRow key={group.original.id} className="hover:bg-slate-50/60 transition-colors">
                          <TableCell
                            title={
                              effective.createdAt
                                ? `Tạo lúc: ${formatDateTime(effective.createdAt)}`
                                : undefined
                            }
                          >
                            {effective.executedDate}
                          </TableCell>
                          <TableCell className="font-medium">
                            {effective.createdByName}
                          </TableCell>
                          <TableCell>
                            <div className="flex flex-wrap items-center gap-1">
                              <Icon className="mr-1 inline-block h-4 w-4 text-emerald-700" />
                              <span className="inline-flex items-center gap-1 rounded-full bg-info-bg px-2 py-0.5 text-xs font-medium text-info">
                                {getActivityLabel(effective.activityType)}
                              </span>
                            </div>
                          </TableCell>
                          <TableCell>
                            {hasCorrections ? (
                              <Badge className="bg-amber-100 text-amber-800 hover:bg-amber-100 border border-amber-300">
                                Đã đính chính
                              </Badge>
                            ) : (
                              <Badge variant="secondary" className="bg-slate-100 text-slate-600 border border-slate-200">
                                Chưa đính chính
                              </Badge>
                            )}
                          </TableCell>
                          <TableCell>{materialQuantity || "—"}</TableCell>
                          <TableCell
                            className="max-w-40 truncate"
                            title={effective.notes ?? undefined}
                          >
                            {effective.notes
                              ? effective.notes.length > 30
                                ? `${effective.notes.slice(0, 30)}…`
                                : effective.notes
                              : "—"}
                          </TableCell>
                          <TableCell className="text-center">
                            <Button
                              variant="view"
                              size="sm"
                              onClick={() => navigate(`/farm-logs/${effective.id}`)}
                              className="flex items-center gap-1"
                              aria-label="Xem chi tiết nhật ký"
                              title="Xem chi tiết & quản lý chứng từ"
                            >
                              <Paperclip className="h-4 w-4" />
                              <span>{effective.attachmentCount ?? 0}</span>
                            </Button>
                          </TableCell>
                          {enableCorrection && (
                            <TableCell className="text-center">
                              <DropdownMenu>
                                <DropdownMenuTrigger
                                  className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-input bg-white text-muted-foreground hover:bg-accent hover:text-foreground"
                                  aria-label="Hành động"
                                >
                                  <MoreHorizontal className="h-4 w-4" />
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                  {canCorrect && (
                                    <DropdownMenuItem
                                      onClick={() =>
                                        navigate(
                                          `/farm-logs/${effective.id}/correct`,
                                        )
                                      }
                                    >
                                      <Pencil className="h-4 w-4" />
                                      Đính chính
                                    </DropdownMenuItem>
                                  )}
                                  <DropdownMenuItem
                                    onClick={() =>
                                      navigate(`/farm-logs/${effective.id}`)
                                    }
                                  >
                                    <FileText className="h-4 w-4" />
                                    Xem chi tiết
                                  </DropdownMenuItem>
                                </DropdownMenuContent>
                              </DropdownMenu>
                            </TableCell>
                          )}
                        </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}

          {/* Phân trang */}
          {!isLoading && pageInfo.totalPages > 1 && (
            <div className="flex items-center justify-between border-t px-4 py-3">
              <div className="text-sm text-muted-foreground">
                Trang {pageInfo.page + 1} / {pageInfo.totalPages}
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => goToPage(page - 1)}
                  disabled={pageInfo.first}
                  aria-label="Trang trước"
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <span className="text-sm">
                  {pageInfo.page + 1} / {pageInfo.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => goToPage(page + 1)}
                  disabled={pageInfo.last}
                  aria-label="Trang sau"
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <span>Hiển thị</span>
                <Select
                  value={String(size)}
                  onValueChange={(value: string | null) => {
                    if (value) {
                      setSize(Number(value));
                      setPage(0);
                    }
                  }}
                >
                  <SelectTrigger className="w-17.5 h-8">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[5, 10, 20, 50].map((s) => (
                      <SelectItem key={s} value={String(s)}>
                        {s}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <span>bản ghi</span>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
  );
}