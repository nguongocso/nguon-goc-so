import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ChevronLeft, ChevronRight, RefreshCw } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { LoginHistoryFilter } from "@/components/login-history/LoginHistoryFilter";
import { LoginHistoryTable } from "@/components/login-history/LoginHistoryTable";
import { getLoginHistory } from "@/api/loginHistoryApi";
import { useAuth } from "@/hooks/useAuth";
import type { LoginHistoryItem, LoginHistoryParams } from "@/types/loginHistory";
import type { PageResponse } from "@/types/common";

export default function LoginHistoryPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [records, setRecords] = useState<LoginHistoryItem[]>([]);
  const [pageInfo, setPageInfo] = useState<Omit<PageResponse<LoginHistoryItem>, "items">>({
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

  const fetchLoginHistory = async (params: LoginHistoryParams) => {
    try {
      setLoading(true);
      const data = await getLoginHistory({
        page: params.page ?? 0,
        size: params.size ?? 10,
        userId: user?.userId,
        result: params.result || undefined,
        startDate: params.startDate || undefined,
        endDate: params.endDate || undefined,
      });

      setRecords(data.items);
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
        error.response?.data?.message || "Không thể tải lịch sử đăng nhập";
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const params: LoginHistoryParams = {
      page,
      size,
    };

    if (searchParams.get("result")) params.result = searchParams.get("result")!;
    if (searchParams.get("startDate")) params.startDate = searchParams.get("startDate")!;
    if (searchParams.get("endDate")) params.endDate = searchParams.get("endDate")!;

    fetchLoginHistory(params);
  }, [page, size, searchParams]);

  const handleFilter = (filters: { result: string; startDate: string; endDate: string }) => {
    const params = new URLSearchParams();
    if (filters.result) params.set("result", filters.result);
    if (filters.startDate) params.set("startDate", filters.startDate);
    if (filters.endDate) params.set("endDate", filters.endDate);

    setPage(0);
    setSearchParams(params);
  };

  const handleReset = () => {
    setPage(0);
    setSearchParams({});
  };

  const goToPage = (newPage: number) => {
    if (newPage >= 0 && newPage < pageInfo.totalPages) {
      setPage(newPage);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Lịch sử đăng nhập</h1>
          <p className="text-sm text-muted-foreground">
            Xem lịch sử đăng nhập của tài khoản {user?.fullName}
          </p>
        </div>
        <Button
          variant="outline"
          onClick={() => fetchLoginHistory({ page, size })}
          disabled={loading}
        >
          <RefreshCw className={`h-4 w-4 mr-1 ${loading ? "animate-spin" : ""}`} />
          Làm mới
        </Button>
      </div>

      <LoginHistoryFilter
        onFilter={handleFilter}
        onReset={handleReset}
        loading={loading}
      />

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
          <LoginHistoryTable records={records} loading={loading} />
        </div>

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
