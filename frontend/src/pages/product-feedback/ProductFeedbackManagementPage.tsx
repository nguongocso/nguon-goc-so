import { useCallback, useEffect, useState } from "react";
import { ChevronLeft, ChevronRight, RefreshCw, Search } from "lucide-react";
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
  const [size, setSize] = useState(10);
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

      <div className="rounded-xl border bg-white p-4 shadow-sm">
        <div className="grid gap-3 lg:grid-cols-[1fr_220px_220px_auto_auto]">
          <Input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onKeyDown={(event) => event.key === "Enter" && applyFilters()}
            placeholder="Tìm theo nội dung, lô sản xuất hoặc mã tem..."
          />
          <Select value={status} onValueChange={(value) => { setStatus(value as typeof status); setPage(0); }}>
            <SelectTrigger>
              <SelectValue>
                {status === "ALL" ? "Tất cả trạng thái" : PRODUCT_FEEDBACK_STATUS_LABELS[status]}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Tất cả trạng thái</SelectItem>
              {Object.entries(PRODUCT_FEEDBACK_STATUS_LABELS).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
            </SelectContent>
          </Select>
          <Select value={severity} onValueChange={(value) => { setSeverity(value as typeof severity); setPage(0); }}>
            <SelectTrigger>
              <SelectValue>
                {severity === "ALL" ? "Tất cả mức độ" : PRODUCT_FEEDBACK_SEVERITY_LABELS[severity]}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Tất cả mức độ</SelectItem>
              {Object.entries(PRODUCT_FEEDBACK_SEVERITY_LABELS).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
            </SelectContent>
          </Select>
          <Button onClick={applyFilters}><Search className="mr-2 h-4 w-4" />Tìm kiếm</Button>
          <Button variant="outline" onClick={clearFilters}>Xóa lọc</Button>
        </div>
      </div>

      <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-left text-xs font-semibold uppercase text-muted-foreground">
              <tr>
                <th className="px-4 py-3">Lô sản xuất</th>
                <th className="px-4 py-3">Nội dung phản ánh</th>
                <th className="px-4 py-3">Trạng thái</th>
                <th className="px-4 py-3">Mức độ</th>
                <th className="px-4 py-3">Người xử lý</th>
                <th className="px-4 py-3">Thời gian gửi</th>
                <th className="px-4 py-3 text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <MessageRow colSpan={7} message="Đang tải dữ liệu..." />
              ) : loadError ? (
                <MessageRow colSpan={7} message={loadError} error />
              ) : feedbacks.length === 0 ? (
                <MessageRow colSpan={7} message="Không tìm thấy phản ánh phù hợp." />
              ) : feedbacks.map((feedback) => (
                <tr key={feedback.id} className="transition-colors hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium">{feedback.productionLotName}</td>
                  <td className="max-w-xs px-4 py-3"><p className="truncate" title={feedback.content}>{feedback.content}</p></td>
                  <td className="px-4 py-3"><ProductFeedbackStatusPill status={feedback.status} /></td>
                  <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">{PRODUCT_FEEDBACK_SEVERITY_LABELS[feedback.severity]}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">{feedback.assignedToName || "Chưa gán"}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">{formatProductFeedbackDate(feedback.createdAt)}</td>
                  <td className="px-4 py-3 text-center"><Button variant="outline" size="sm" onClick={() => navigate(`/product-feedbacks/${feedback.id}`)}>Xem chi tiết</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {!loading && !loadError && pageInfo.totalPages > 0 && (
          <div className="flex flex-col-reverse items-center justify-between gap-3 border-t px-4 py-3 sm:flex-row">
            <p className="text-xs text-muted-foreground">Hiển thị {pageInfo.page * pageInfo.size + 1}–{Math.min((pageInfo.page + 1) * pageInfo.size, pageInfo.totalElements)} trên {pageInfo.totalElements} phản ánh</p>
            <div className="flex items-center gap-2">
              <Select value={String(size)} onValueChange={(value) => { setSize(Number(value)); setPage(0); }}>
                <SelectTrigger className="h-8 w-[90px] text-xs">
                  <SelectValue>{size} dòng</SelectValue>
                </SelectTrigger>
                <SelectContent>{[5, 10, 20, 50].map((value) => <SelectItem key={value} value={String(value)}>{value} dòng</SelectItem>)}</SelectContent>
              </Select>
              <Button variant="outline" size="icon" className="h-8 w-8" disabled={pageInfo.first} onClick={() => setPage((value) => Math.max(0, value - 1))}><ChevronLeft className="h-4 w-4" /></Button>
              <span className="min-w-[60px] text-center text-xs text-muted-foreground">{pageInfo.page + 1} / {pageInfo.totalPages}</span>
              <Button variant="outline" size="icon" className="h-8 w-8" disabled={pageInfo.last} onClick={() => setPage((value) => value + 1)}><ChevronRight className="h-4 w-4" /></Button>
            </div>
          </div>
        )}
      </div>

    </div>
  );
}

function Summary({ title, value }: { title: string; value: string | number }) {
  return <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm"><p className="text-sm font-medium text-muted-foreground">{title}</p><p className="mt-1 text-2xl font-bold text-emerald-700">{value}</p></div>;
}

function MessageRow({ colSpan, message, error = false }: { colSpan: number; message: string; error?: boolean }) {
  return <tr><td colSpan={colSpan} className={`px-4 py-12 text-center ${error ? "text-destructive" : "text-muted-foreground"}`}>{message}</td></tr>;
}
