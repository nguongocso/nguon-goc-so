import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  downloadActivityLogExportJob,
  getActivityLogApiError,
  getActivityLogExportJob,
  getActivityLogs,
} from "@/api/activityLogApi";
import type { ActivityLog, ActivityLogParams } from "@/types/activityLog";
import { ActivityLogFilter } from "@/components/activity-log/ActivityLogFilter";
import { ActivityLogTable } from "@/components/activity-log/ActivityLogTable";
import { ListCard } from "@/components/common/ListCard";
import { Pagination } from "@/components/common/Pagination";
import { ListPageHeader } from "@/components/common/ListPageHeader";
import { ListToolbar } from "@/components/common/ListToolbar";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { FileDown, History, RefreshCw } from "lucide-react";
import { HelpButton } from "@/components/help/HelpButton";
import { toast } from "sonner";
import type { PageResponse } from "@/types/common";
import { ActivityLogExportDialog } from "@/components/activity-log/ActivityLogExportDialog";
import type { ActivityLogExportFilterRequest } from "@/types/activityLog";

export default function ActivityLogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [logs, setLogs] = useState<ActivityLog[]>([]);
  const [exportDialogOpen, setExportDialogOpen] = useState(false);

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
        objectType: params.objectType || undefined,
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
    if (searchParams.get("objectType"))
      params.objectType = searchParams.get("objectType")!;
    fetchLogs(params);
  }, [page, size, searchParams]);

  const handleFilter = (filters: ActivityLogExportFilterRequest) => {
    const params = new URLSearchParams();
    if (filters.action) params.set("action", filters.action);
    if (filters.actorName) params.set("actorName", filters.actorName);
    if (filters.startDate) params.set("startDate", filters.startDate);
    if (filters.endDate) params.set("endDate", filters.endDate);
    if (filters.objectType) params.set("objectType", filters.objectType);
    setPage(0);
    setSearchParams(params);
  };

  const handleReset = () => {
    setSearchParams({});
    setPage(0);
  };

  const currentExportFilter = useMemo<ActivityLogExportFilterRequest>(() => ({
    action: searchParams.get("action") || undefined,
    actorName: searchParams.get("actorName") || undefined,
    startDate: searchParams.get("startDate") || undefined,
    endDate: searchParams.get("endDate") || undefined,
    objectType: searchParams.get("objectType") || undefined,
  }), [searchParams]);

  const refreshCurrentLogs = () => fetchLogs({ page, size, ...currentExportFilter });

  useEffect(() => {
    const exportJobId = searchParams.get("exportJobId");
    if (!exportJobId) return;
    let active = true;
    const downloadCompletedJob = async () => {
      try {
        const job = await getActivityLogExportJob(exportJobId);
        if (job.status === "SUCCESS") {
          await downloadActivityLogExportJob(exportJobId);
          if (active) toast.success("Đã tải tệp nhật ký hoạt động.");
        } else if (active) {
          toast.info(job.status === "IN_PROGRESS"
            ? "Tệp nhật ký vẫn đang được xử lý."
            : "Yêu cầu xuất nhật ký đã thất bại.");
        }
      } catch (error: unknown) {
        if (active) {
          toast.error(await getActivityLogApiError(error, "Không thể tải tệp nhật ký hoạt động."));
        }
      } finally {
        if (active) {
          const next = new URLSearchParams(searchParams);
          next.delete("exportJobId");
          setSearchParams(next, { replace: true });
        }
      }
    };
    void downloadCompletedJob();
    return () => { active = false; };
  }, [searchParams, setSearchParams]);

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={History}
        title="Lịch sử hoạt động"
        description="Theo dõi và kiểm tra toàn bộ nhật ký thao tác trong tổ chức"
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <HelpButton screenKey="report-activity-log" />
            <Button
              variant="outline"
              onClick={refreshCurrentLogs}
              disabled={loading}
            >
              <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />
              Làm mới
            </Button>
            <Button onClick={() => setExportDialogOpen(true)}>
              <FileDown className="size-4" />
              Xuất nhật ký
            </Button>
          </div>
        }
      />


      {/* Bộ lọc */}
      <ActivityLogFilter
        onFilter={handleFilter}
        onReset={handleReset}
        loading={loading}
        initialValues={currentExportFilter}
      />

      {/* Bảng danh sách */}
      <ListCard>
        <ListToolbar
          left={
            <span className="text-sm text-muted-foreground">
              Tổng số: {pageInfo.totalElements} bản ghi
            </span>
          }
          right={
            <div className="flex items-center gap-2">
              <span className="text-sm text-muted-foreground">Hiển thị</span>
              <Select
                value={String(size)}
                onValueChange={(value) => {
                  setSize(Number(value));
                  setPage(0);
                }}
              >
                <SelectTrigger className="w-[120px]" aria-label="Số bản ghi mỗi trang">
                  <SelectValue placeholder="Chọn số lượng" />
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
          }
        />

        <ActivityLogTable logs={logs} loading={loading} />

        {/* Phân trang chuẩn hệ thống */}
        <Pagination
          currentPage={page}
          totalPages={pageInfo.totalPages}
          totalElements={pageInfo.totalElements}
          pageSize={size}
          loading={loading}
          itemLabel="bản ghi"
          alwaysShow
          onPageChange={setPage}
        />
      </ListCard>

      <ActivityLogExportDialog
        open={exportDialogOpen}
        onClose={() => setExportDialogOpen(false)}
        filter={currentExportFilter}
        onExportSuccess={refreshCurrentLogs}
      />
    </div>
  );
}

