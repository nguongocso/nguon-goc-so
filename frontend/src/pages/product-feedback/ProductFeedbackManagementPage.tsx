import { useEffect, useState, useCallback } from "react";
import { getProductFeedbacks } from "@/api/productFeedbackApi";
import type { ProductFeedback } from "@/types/productFeedback";
import type { PageResponse } from "@/types/common";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ChevronLeft, ChevronRight, RefreshCw, MessageSquare } from "lucide-react";
import { HelpButton } from "@/components/help/HelpButton";
import { toast } from "sonner";
import { format } from "date-fns";
import { vi } from "date-fns/locale";

// ─── Helpers ─────────────────────────────────────────────

function formatDate(dateStr?: string): string {
  if (!dateStr) return "—";
  try {
    return format(new Date(dateStr), "dd/MM/yyyy HH:mm", { locale: vi });
  } catch {
    return dateStr;
  }
}

function truncateContent(content: string, maxLen = 120): string {
  if (content.length <= maxLen) return content;
  return content.slice(0, maxLen) + "…";
}

// ─── Component ───────────────────────────────────────────

export default function ProductFeedbackManagementPage() {
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
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [selectedFeedback, setSelectedFeedback] = useState<ProductFeedback | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);

  const fetchFeedbacks = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getProductFeedbacks({
        page,
        size,
        sort: "createdAt,desc",
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
    } catch (error: any) {
      const msg = error.response?.data?.message || "Không thể tải danh sách phản ánh";
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }, [page, size]);

  useEffect(() => {
    fetchFeedbacks();
  }, [fetchFeedbacks]);

  const handleRefresh = () => {
    fetchFeedbacks();
  };

  const handleViewDetail = (feedback: ProductFeedback) => {
    setSelectedFeedback(feedback);
    setDetailOpen(true);
  };

  const handleCloseDetail = () => {
    setDetailOpen(false);
    setSelectedFeedback(null);
  };

  // ─── Render ─────────────────────────────────────────────

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">
            Nhận phản ánh
          </h1>
          <p className="text-sm text-muted-foreground">
            Xem và xử lý các phản ánh từ người dùng về sản phẩm.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="product-feedback" />
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefresh}
            disabled={loading}
            className="shrink-0"
          >
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Làm mới
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
          <p className="text-sm font-medium text-muted-foreground">Tổng phản ánh</p>
          <p className="mt-1 text-2xl font-bold text-emerald-700">
            {loading ? "..." : pageInfo.totalElements}
          </p>
        </div>
        <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
          <p className="text-sm font-medium text-muted-foreground">Trang hiện tại</p>
          <p className="mt-1 text-2xl font-bold text-emerald-700">
            {pageInfo.totalPages > 0 ? pageInfo.page + 1 : 0} / {pageInfo.totalPages}
          </p>
        </div>
        <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
          <p className="text-sm font-medium text-muted-foreground">Kích thước trang</p>
          <p className="mt-1 text-2xl font-bold text-emerald-700">{pageInfo.size}</p>
        </div>
      </div>

      {/* Table */}
      <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-left text-xs font-semibold uppercase text-muted-foreground">
              <tr>
                <th className="px-4 py-3">Lô sản xuất</th>
                <th className="px-4 py-3">Loại nông sản</th>
                <th className="px-4 py-3">Tổ chức</th>
                <th className="px-4 py-3">Nội dung phản ánh</th>
                <th className="px-4 py-3">Thời gian gửi</th>
                <th className="px-4 py-3 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-muted-foreground">
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : feedbacks.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-muted-foreground">
                    <MessageSquare className="mx-auto mb-2 h-8 w-8 text-gray-300" />
                    Chưa có phản ánh nào.
                  </td>
                </tr>
              ) : (
                feedbacks.map((fb) => (
                  <tr key={fb.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-medium text-foreground">
                      {fb.productionLotName}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {fb.productCategoryName || "—"}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {fb.organizationName || "—"}
                    </td>
                    <td className="px-4 py-3 max-w-xs">
                      <p className="truncate" title={fb.content}>
                        {truncateContent(fb.content)}
                      </p>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground whitespace-nowrap">
                      {formatDate(fb.createdAt)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleViewDetail(fb)}
                      >
                        Xem chi tiết
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {!loading && pageInfo.totalPages > 0 && (
          <div className="flex flex-col-reverse items-center justify-between gap-3 border-t border-gray-100 px-4 py-3 sm:flex-row">
            <p className="text-xs text-muted-foreground">
              Hiển thị {pageInfo.page * pageInfo.size + 1}–
              {Math.min((pageInfo.page + 1) * pageInfo.size, pageInfo.totalElements)} trên{" "}
              {pageInfo.totalElements} phản ánh
            </p>
            <div className="flex items-center gap-2">
              <Select
                value={String(size)}
                onValueChange={(val) => {
                  setSize(Number(val));
                  setPage(0);
                }}
              >
                <SelectTrigger className="h-8 w-[70px] text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="5">5</SelectItem>
                  <SelectItem value="10">10</SelectItem>
                  <SelectItem value="20">20</SelectItem>
                  <SelectItem value="50">50</SelectItem>
                </SelectContent>
              </Select>
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                disabled={pageInfo.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-xs text-muted-foreground min-w-[60px] text-center">
                {pageInfo.page + 1} / {pageInfo.totalPages}
              </span>
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                disabled={pageInfo.last}
                onClick={() => setPage((p) => p + 1)}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Detail Sheet */}
      {detailOpen && selectedFeedback && (
        <FeedbackDetailSheet
          feedback={selectedFeedback}
          onClose={handleCloseDetail}
        />
      )}
    </div>
  );
}

// ─── Detail Sheet (inline for simplicity) ─────────────────

function FeedbackDetailSheet({
  feedback,
  onClose,
}: {
  feedback: ProductFeedback;
  onClose: () => void;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-end">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/30 backdrop-blur-sm"
        onClick={onClose}
      />
      {/* Sheet */}
      <div className="relative z-10 flex h-full w-full max-w-lg flex-col bg-white shadow-xl animate-in slide-in-from-right">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-100 px-6 py-4">
          <h2 className="text-lg font-semibold text-foreground">
            Chi tiết phản ánh
          </h2>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-muted-foreground hover:bg-gray-100 hover:text-foreground transition-colors"
            aria-label="Đóng"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={2}
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-6 py-4 space-y-6">
          {/* Section: Thông tin phản ánh */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
              Thông tin phản ánh
            </h3>
            <div className="rounded-lg border border-gray-100 bg-gray-50 p-4 space-y-2">
              <div>
                <span className="text-xs text-muted-foreground">Mã phản ánh</span>
                <p className="text-sm font-mono text-foreground break-all">{feedback.id}</p>
              </div>
              <div>
                <span className="text-xs text-muted-foreground">Nội dung</span>
                <p className="mt-1 text-sm text-foreground whitespace-pre-wrap leading-relaxed">
                  {feedback.content}
                </p>
              </div>
              <div>
                <span className="text-xs text-muted-foreground">Thời gian gửi</span>
                <p className="text-sm text-foreground">{formatDate(feedback.createdAt)}</p>
              </div>
            </div>
          </section>

          {/* Section: Thông tin sản phẩm */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
              Thông tin sản phẩm
            </h3>
            <div className="rounded-lg border border-gray-100 bg-gray-50 p-4 space-y-2">
              <div>
                <span className="text-xs text-muted-foreground">Lô sản xuất</span>
                <p className="text-sm font-medium text-foreground">
                  {feedback.productionLotName}
                </p>
              </div>
              <div>
                <span className="text-xs text-muted-foreground">Mã lô sản xuất</span>
                <p className="text-sm font-mono text-foreground break-all">
                  {feedback.productionLotId}
                </p>
              </div>
              <div>
                <span className="text-xs text-muted-foreground">Loại nông sản</span>
                <p className="text-sm text-foreground">
                  {feedback.productCategoryName || "—"}
                </p>
              </div>
            </div>
          </section>

          {/* Section: Thông tin tổ chức */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
              Thông tin tổ chức
            </h3>
            <div className="rounded-lg border border-gray-100 bg-gray-50 p-4 space-y-2">
              <div>
                <span className="text-xs text-muted-foreground">Tổ chức</span>
                <p className="text-sm text-foreground">
                  {feedback.organizationName || "—"}
                </p>
              </div>
              <div>
                <span className="text-xs text-muted-foreground">Mã tổ chức</span>
                <p className="text-sm font-mono text-foreground break-all">
                  {feedback.organizationId || "—"}
                </p>
              </div>
            </div>
          </section>
        </div>

        {/* Footer */}
        <div className="border-t border-gray-100 px-6 py-4">
          <Button
            variant="outline"
            className="w-full"
            onClick={onClose}
          >
            Đóng
          </Button>
        </div>
      </div>
    </div>
  );
}