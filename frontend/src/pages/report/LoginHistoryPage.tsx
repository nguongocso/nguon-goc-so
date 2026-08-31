import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ShieldCheck } from "lucide-react";
import { toast } from "sonner";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ListCard } from "@/components/common/ListCard";
import { ListPageHeader } from "@/components/common/ListPageHeader";
import { Pagination } from "@/components/common/Pagination";
import { RefreshButton } from "@/components/common/RefreshButton";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { LoginHistoryFilter } from "@/components/login-history/LoginHistoryFilter";
import { LoginHistoryTable } from "@/components/login-history/LoginHistoryTable";
import { getLoginHistory } from "@/api/loginHistoryApi";
import { useAuth } from "@/hooks/useAuth";
import { HelpButton } from "@/components/help/HelpButton";
import type { LoginHistoryItem, LoginHistoryParams } from "@/types/loginHistory";
import type { PageResponse } from "@/types/common";

export default function LoginHistoryPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  useSetBreadcrumb([
    { label: "Dashboard", href: "/dashboard" },
    { label: "Lịch sử đăng nhập" },
  ]);
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

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={ShieldCheck}
        iconBoxClassName="bg-emerald-500/10"
        title="Lịch sử đăng nhập"
        description={`Xem lịch sử đăng nhập của tài khoản ${user?.fullName}`}
        actions={
          <>
            <HelpButton screenKey="report-login-history" />
            <RefreshButton
              onClick={() => fetchLoginHistory({ page, size })}
              loading={loading}
            />
          </>
        }
      />

      <LoginHistoryFilter
        onFilter={handleFilter}
        onReset={handleReset}
        loading={loading}
      />

      <ListCard>
        <div className="flex items-center justify-end gap-2">
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

        <LoginHistoryTable
          records={records}
          loading={loading}
          startIndex={page * size}
        />

        <Pagination
          currentPage={pageInfo.page}
          totalPages={pageInfo.totalPages}
          totalElements={pageInfo.totalElements}
          pageSize={size}
          loading={loading}
          itemLabel="bản ghi"
          onPageChange={setPage}
        />
      </ListCard>
    </div>
  );
}
