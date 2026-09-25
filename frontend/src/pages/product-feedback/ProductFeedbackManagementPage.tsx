import { useCallback, useEffect, useState } from "react";
import { RefreshCw, Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

import { getProductFeedbacks } from "@/api/productFeedbackApi";
import { HelpButton } from "@/components/help/HelpButton";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import { ListCard } from "@/components/common/ListCard";
import { DataTableShell } from "@/components/common/DataTableShell";
import { Pagination } from "@/components/common/Pagination";
import type { PageResponse } from "@/types/common";
import type {
  ProductFeedback,
  ProductFeedbackSeverity,
  ProductFeedbackStatus,
} from "@/types/productFeedback";
import {
  formatProductFeedbackDate,
  getProductFeedbackErrorMessage,
  PRODUCT_FEEDBACK_SEVERITY_LABELS,
  PRODUCT_FEEDBACK_STATUS_LABELS,
  ProductFeedbackStatusPill,
} from "./productFeedbackPresentation";

export default function ProductFeedbackManagementPage() {
  const navigate = useNavigate();
  const [feedbacks, setFeedbacks] = useState<ProductFeedback[]>([]);
  const [pageInfo, setPageInfo] = useState<Omit<PageResponse<ProductFeedback>, "items">>({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [status, setStatus] = useState<"ALL" | ProductFeedbackStatus>("ALL");
  const [severity, setSeverity] = useState<"ALL" | ProductFeedbackSeverity>("ALL");

  const fetchFeedbacks = useCallback(async () => {
    try {
      setLoading(true);
      setLoadError(null);
      const data = await getProductFeedbacks({
        page,
        size,
        sort: "createdAt,desc",
        keyword: appliedKeyword || undefined,
        status: status === "ALL" ? undefined : status,
        severity: severity === "ALL" ? undefined : severity,
      });
      setFeedbacks(data.items);
      setPageInfo({
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        first: data.first,
        last: data.last,
      });
    } catch (error: unknown) {
      const message = getProductFeedbackErrorMessage(error, "Không thể tải danh sách phản ánh");
      setLoadError(message);
      setFeedbacks([]);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  }, [appliedKeyword, page, severity, size, status]);

  useEffect(() => {
    void fetchFeedbacks();
  }, [fetchFeedbacks]);

  const applyFilters = () => {
    setPage(0);
    setAppliedKeyword(keyword.trim());
  };

  const clearFilters = () => {
    setKeyword("");
    setAppliedKeyword("");
    setStatus("ALL");
    setSeverity("ALL");
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Nhận phản ánh</h1>
          <p className="text-sm text-muted-foreground">Xem và xử lý các phản ánh từ người tiêu dùng về sản phẩm.</p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="product-feedback" />
          <Button variant="outline" size="sm" onClick={() => void fetchFeedbacks()} disabled={loading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Làm mới
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Summary title="Tổng phản ánh" value={loading ? "..." : pageInfo.totalElements} />
        <Summary title="Trang hiện tại" value={`${pageInfo.totalPages ? pageInfo.page + 1 : 0} / ${pageInfo.totalPages}`} />
        <Summary title="Kích thước trang" value={pageInfo.size} />
      </div>

      {/* Toàn bộ Bộ lọc và Bảng danh sách nằm trọn vẹn trong khung trắng ListCard chuẩn của hệ thống */}
      <ListCard>
        <div className="grid gap-3 lg:grid-cols-[1fr_220px_220px_auto_auto] pb-3 border-b border-border/40">
          <Input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onKeyDown={(event) => event.key === "Enter" && applyFilters()}
            placeholder="Tìm theo nội dung, lô sản xuất hoặc mã tem..."
          />
          <Select
            value={status}
            onValueChange={(value) => {
              setStatus(value as typeof status);
              setPage(0);
            }}
          >
            <SelectTrigger>
              <SelectValue>
                {status === "ALL" ? "Tất cả trạng thái" : PRODUCT_FEEDBACK_STATUS_LABELS[status]}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Tất cả trạng thái</SelectItem>
              {Object.entries(PRODUCT_FEEDBACK_STATUS_LABELS).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={severity}
            onValueChange={(value) => {
              setSeverity(value as typeof severity);
              setPage(0);
            }}
          >
            <SelectTrigger>
              <SelectValue>
                {severity === "ALL" ? "Tất cả mức độ" : PRODUCT_FEEDBACK_SEVERITY_LABELS[severity]}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Tất cả mức độ</SelectItem>
              {Object.entries(PRODUCT_FEEDBACK_SEVERITY_LABELS).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button onClick={applyFilters}>
            <Search className="mr-2 h-4 w-4" />
            Tìm kiếm
          </Button>
          <Button variant="outline" onClick={clearFilters}>
            Xóa lọc
          </Button>
        </div>

        <DataTableShell
          colSpan={8}
          loading={loading}
          empty={!loading && (Boolean(loadError) || feedbacks.length === 0)}
          loadingMessage="Đang tải danh sách phản ánh..."
          emptyMessage={
            loadError ||
            (appliedKeyword || status !== "ALL" || severity !== "ALL"
              ? "Không tìm thấy phản ánh nào phù hợp với bộ lọc."
              : "Không có phản ánh nào từ người tiêu dùng.")
          }
          header={
            <>
              <TableHead className="w-14 text-center">STT</TableHead>
              <TableHead>Lô sản xuất</TableHead>
              <TableHead>Nội dung phản ánh</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Mức độ</TableHead>
              <TableHead>Người xử lý</TableHead>
              <TableHead>Thời gian gửi</TableHead>
              <TableHead className="text-center">Thao tác</TableHead>
            </>
          }
          body={feedbacks.map((feedback, index) => (
            <TableRow key={feedback.id} className="transition-colors hover:bg-muted/40">
              <TableCell className="text-center font-medium text-muted-foreground">
                {page * size + index + 1}
              </TableCell>
              <TableCell className="font-semibold text-foreground">
                {feedback.productionLotName}
              </TableCell>
              <TableCell className="max-w-xs">
                <p className="truncate" title={feedback.content}>
                  {feedback.content}
                </p>
              </TableCell>
              <TableCell>
                <ProductFeedbackStatusPill status={feedback.status} />
              </TableCell>
              <TableCell className="whitespace-nowrap text-muted-foreground">
                {PRODUCT_FEEDBACK_SEVERITY_LABELS[feedback.severity]}
              </TableCell>
              <TableCell className="whitespace-nowrap text-muted-foreground">
                {feedback.assignedToName || "Chưa gán"}
              </TableCell>
              <TableCell className="whitespace-nowrap text-muted-foreground">
                {formatProductFeedbackDate(feedback.createdAt)}
              </TableCell>
              <TableCell className="text-center">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => navigate(`/product-feedbacks/${feedback.id}`)}
                >
                  Chi tiết
                </Button>
              </TableCell>
            </TableRow>
          ))}
        />

        <Pagination
          currentPage={page}
          totalPages={pageInfo.totalPages}
          totalElements={pageInfo.totalElements}
          pageSize={size}
          loading={loading}
          itemLabel="phản ánh"
          alwaysShow
          onPageChange={setPage}
        />
      </ListCard>
    </div>
  );
}

function Summary({ title, value }: { title: string; value: string | number }) {
  return (
    <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
      <p className="text-sm font-medium text-muted-foreground">{title}</p>
      <p className="mt-1 text-2xl font-bold text-emerald-700">{value}</p>
    </div>
  );
}
