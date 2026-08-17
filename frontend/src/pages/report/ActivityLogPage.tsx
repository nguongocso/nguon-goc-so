import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { getActivityLogs } from "@/api/activityLogApi";
import type { ActivityLog, ActivityLogParams } from "@/types/activityLog";
import { ActivityLogFilter } from "@/components/activity-log/ActivityLogFilter";
import { ActivityLogTable } from "@/components/activity-log/ActivityLogTable";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ChevronLeft, ChevronRight, RefreshCw } from "lucide-react";
import { HelpButton } from "@/components/help/HelpButton";
import { toast } from "sonner";
import type { PageResponse } from "@/types/common";

export default function ActivityLogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [logs, setLogs] = useState<ActivityLog[]>([]);
  const [pageInfo, setPageInfo] = useState<
    Omit<PageResponse<ActivityLog>, "items">
  >({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const fetchLogs = async (params: ActivityLogParams) => {
    try {
      setLoading(true);
      const data = await getActivityLogs({
        page: params.page ?? 0,
        size: params.size ?? 10,
        action: params.action || undefined,
        actorName: params.actorName || undefined,
        startDate: params.startDate || undefined,
        endDate: params.endDate || undefined,
      });
      setLogs(data.items);
      setPageInfo({
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        first: data.first,
        last: data.last,
      });
    } catch (error: any) {
      const msg =
        error.response?.data?.message || "Không thể tải lịch sử hoạt động";
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const params: ActivityLogParams = {
      page,
      size,
    };
    // Đọc từ searchParams nếu có
    if (searchParams.get("action")) params.action = searchParams.get("action")!;
    if (searchParams.get("actorName"))
      params.actorName = searchParams.get("actorName")!;
    if (searchParams.get("startDate"))
      params.startDate = searchParams.get("startDate")!;
    if (searchParams.get("endDate"))
      params.endDate = searchParams.get("endDate")!;
    fetchLogs(params);
  }, [page, size, searchParams]);

  const handleFilter = (filters: any) => {
    const params = new URLSearchParams();
    if (filters.action) params.set("action", filters.action);
    if (filters.actorName) params.set("actorName", filters.actorName);
    if (filters.startDate) params.set("startDate", filters.startDate);
    if (filters.endDate) params.set("endDate", filters.endDate);
    setPage(0);
    setSearchParams(params);
  };

  const handleReset = () => {
    setSearchParams({});
    setPage(0);
  };

  const goToPage = (newPage: number) => {
    if (newPage >= 0 && newPage < pageInfo.totalPages) {
      setPage(newPage);
    }
  };

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Lịch sử hoạt động</h1>
          <p className="text-sm text-muted-foreground">
            Theo dõi các thao tác đã thực hiện trong tổ chức
          </p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="report-activity-log" />
          <Button
            variant="outline"
            onClick={() => fetchLogs({ page, size })}
            disabled={loading}
          >
            <RefreshCw
              className={`h-4 w-4 mr-1 ${loading ? "animate-spin" : ""}`}
            />
            Làm mới
          </Button>
        </div>
      </div>

      {/* Bộ lọc */}
      <ActivityLogFilter
        onFilter={handleFilter}
        onReset={handleReset}
        loading={loading}
      />

      {/* Bảng danh sách */}
      <div className="bg-white rounded-lg border shadow-sm">
        <div className="p-4 border-b flex justify-between items-center">
          <span className="text-sm text-muted-foreground">
            Tổng số: {pageInfo.totalElements} bản ghi
          </span>
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">Hiển thị</span>
            <Select
              value={String(size)}
              onValueChange={(value) => {
                setSize(Number(value));
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[120px]">
                <SelectValue placeholder="Chọn size" />
              </SelectTrigger>
              <SelectContent>
                {[5, 10, 20, 50].map((s) => (
                  <SelectItem key={s} value={String(s)}>
                    {s}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <span className="text-sm text-muted-foreground">bản ghi</span>
          </div>
        </div>

        <div className="p-4">
          <ActivityLogTable logs={logs} loading={loading} />
        </div>

        {/* Phân trang */}
        {!loading && pageInfo.totalPages > 1 && (
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
          </div>
        )}
      </div>
    </div>
  );
}
