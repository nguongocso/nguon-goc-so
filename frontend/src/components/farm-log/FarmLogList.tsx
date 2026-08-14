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
} from "lucide-react";
import { getFarmLogs } from "@/api/farmLogApi";
import type { FarmLog } from "@/types/farmLog";
import { useNavigate } from "react-router-dom";
import { AttachmentManager } from "./AttachmentManager";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { PageResponse } from "@/types/common";

// 👇 Định nghĩa interface
interface FarmLogListProps {
  productionLotId: string;
  productionLotName?: string;
  onBack?: () => void;
  /** Có quyền tạo nhật ký canh tác mới hay không (mặc định true để không phá các nơi gọi cũ). */
  canCreate?: boolean;
}

const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  PLANTING: "Gieo trồng",
  WATERING: "Tưới nước",
  FERTILIZING: "Bón phân",
  PESTICIDE: "Phun thuốc",
  WEEDING: "Làm cỏ",
  HARVESTING: "Thu hoạch",
  OTHER: "Khác",
};

const ACTIVITY_TYPE_OPTIONS = Object.entries(ACTIVITY_TYPE_LABELS).map(
  ([value, label]) => ({ value, label }),
);

const formatDate = (dateStr: string) => {
  try {
    const d = new Date(dateStr);
    const day = String(d.getDate()).padStart(2, "0");
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const year = d.getFullYear();
    return `${day}/${month}/${year}`;
  } catch {
    return dateStr;
  }
};

const formatDateTime = (dateStr: string) => {
  try {
    const d = new Date(dateStr);
    const day = String(d.getDate()).padStart(2, "0");
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const year = d.getFullYear();
    const hours = String(d.getHours()).padStart(2, "0");
    const minutes = String(d.getMinutes()).padStart(2, "0");
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  } catch {
    return dateStr;
  }
};

const getActivityLabel = (value: string): string => {
  if (value === "ALL") return "Tất cả loại";
  return ACTIVITY_TYPE_LABELS[value] || value;
};

export function FarmLogList({
  productionLotId,
  productionLotName = "",
  onBack,
  canCreate = true,
}: FarmLogListProps) {
  const navigate = useNavigate();
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

  // State cho modal đính kèm
  const [attachmentModalOpen, setAttachmentModalOpen] = useState(false);
  const [selectedLogId, setSelectedLogId] = useState<string | null>(null);

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

  const goToPage = (newPage: number) => {
    if (newPage >= 0 && newPage < pageInfo.totalPages) {
      setPage(newPage);
    }
  };

  const openAttachmentModal = (logId: string) => {
    setSelectedLogId(logId);
    setAttachmentModalOpen(true);
  };

  const handleAttachmentUpdated = () => {
    // Reload lại danh sách để cập nhật dữ liệu attachments mới
    loadLogs();
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Lịch sử nhật ký canh tác</h1>
          {productionLotName && (
            <p className="text-sm text-muted-foreground">
              Lô sản xuất:{" "}
              <span className="font-medium">{productionLotName}</span>
            </p>
          )}
        </div>
        <div className="flex gap-2">
          {onBack && (
            <Button variant="outline" onClick={onBack}>
              <ChevronLeft className="h-4 w-4 mr-1" />
              Quay lại
            </Button>
          )}
          {canCreate && (
            <Button variant="create" onClick={goToCreateLog}>
              <Plus className="h-4 w-4 mr-1" />
              Tạo nhật ký
            </Button>
          )}
        </div>
      </div>

      {/* Bộ lọc */}
      <Card>
        <CardContent className="p-4">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                className="pl-9"
                placeholder="Tìm theo vật tư, ghi chú, người ghi..."
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
              <SelectTrigger>
                <SelectValue placeholder="Loại hoạt động">
                  {getActivityLabel(activityFilter)}
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
                value={dateFrom}
                onChange={(e) => setDateFrom(e.target.value)}
                className="w-full"
              />
              <span className="text-muted-foreground">→</span>
              <Input
                type="date"
                placeholder="Đến ngày"
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
        </CardContent>
      </Card>

      {/* Bảng danh sách */}
      <Card>
        <CardHeader>
          <CardTitle>Danh sách nhật ký</CardTitle>
          <p className="text-sm text-muted-foreground">
            Tổng số: {filteredLogs.length} bản ghi
          </p>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
          ) : filteredLogs.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              <FileText className="mx-auto h-12 w-12 text-muted-foreground/50" />
              <p className="mt-2 font-medium">Chưa có nhật ký canh tác</p>
              <p className="text-sm">
                Hãy kiểm tra lại lô sản xuất hoặc bộ lọc.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-25">Ngày</TableHead>
                    <TableHead className="w-30">Người ghi</TableHead>
                    <TableHead className="w-35">Hoạt động</TableHead>
                    <TableHead>Vật tư</TableHead>
                    <TableHead className="w-20">Số lượng</TableHead>
                    <TableHead>Ghi chú</TableHead>
                    <TableHead className="w-35">Thời gian tạo</TableHead>
                    <TableHead className="w-30 text-center">Chứng từ</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredLogs.map((log) => (
                    <TableRow key={log.id}>
                      <TableCell>{formatDate(log.executedDate)}</TableCell>
                      <TableCell className="font-medium">
                        {log.createdByName}
                      </TableCell>
                      <TableCell>
                        <span className="inline-flex rounded-full bg-info-bg px-2.5 py-0.5 text-xs font-medium text-info">
                          {ACTIVITY_TYPE_LABELS[log.activityType] ||
                            log.activityType}
                        </span>
                      </TableCell>
                      <TableCell>{log.material || "—"}</TableCell>
                      <TableCell>
                        {log.quantity !== null && log.unit
                          ? `${log.quantity} ${log.unit}`
                          : "—"}
                      </TableCell>
                      <TableCell className="max-w-50 truncate">
                        {log.notes || "—"}
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground">
                        {formatDateTime(log.createdAt)}
                      </TableCell>
                      <TableCell className="text-center">
                        <Button
                          variant="view"
                          size="sm"
                          onClick={() => openAttachmentModal(log.id)}
                          className="flex items-center gap-1"
                        >
                          <Paperclip className="h-4 w-4" />
                          <span>{log.attachmentCount ?? 0}</span>
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
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

      {/* Modal đính kèm */}
      <Dialog open={attachmentModalOpen} onOpenChange={setAttachmentModalOpen}>
        <DialogContent className="max-w-xl md:max-w-3xl lg:max-w-4xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Quản lý chứng từ</DialogTitle>
          </DialogHeader>
          {selectedLogId && (
            <AttachmentManager
              logId={selectedLogId}
              onUpdate={handleAttachmentUpdated}
            />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
