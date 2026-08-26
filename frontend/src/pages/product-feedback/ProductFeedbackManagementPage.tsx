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
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { DetailSection } from "@/components/common/detail/DetailSection";
import { DetailField } from "@/components/common/detail/DetailField";
import { ChevronLeft, ChevronRight, RefreshCw, MessageSquare } from "lucide-react";
import { HelpButton } from "@/components/help/HelpButton";
import { toast } from "sonner";
import { maskId } from "@/lib/utils";
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
    <div className="space-y-6">
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
      <FeedbackDetailSheet
        open={detailOpen}
        feedback={selectedFeedback}
        onClose={handleCloseDetail}
      />
    </div>
  );
}

// ─── Detail Sheet (Sheet primitive) ───────────────────────

function FeedbackDetailSheet({
  feedback,
  open,
  onClose,
}: {
  feedback: ProductFeedback | null;
  open: boolean;
  onClose: () => void;
}) {
  return (
    <Sheet open={open} onOpenChange={(nextOpen) => !nextOpen && onClose()}>
      <SheetContent side="right" className="w-full gap-0 p-0 sm:max-w-lg">
        {feedback && (
          <>
            <SheetHeader className="border-b px-6 py-4">
              <SheetTitle className="text-lg font-semibold text-foreground">
                Chi tiết phản ánh
              </SheetTitle>
              <SheetDescription className="text-sm text-muted-foreground">
                Nội dung phản ánh từ người dùng về sản phẩm.
              </SheetDescription>
            </SheetHeader>

            {/* Content */}
            <div className="min-h-0 flex-1 space-y-6 overflow-y-auto px-6 py-4">
              {/* Section: Thông tin phản ánh */}
              <DetailSection title="Thông tin phản ánh" contentClassName="space-y-3">
                <DetailField label="Mã phản ánh" mono value={maskId(feedback.id)} />
                <DetailField
                  label="Nội dung"
                  value={
                    <span className="block whitespace-pre-wrap font-normal leading-relaxed">
                      {feedback.content}
                    </span>
                  }
                />
                <DetailField label="Thời gian gửi" value={formatDate(feedback.createdAt)} />
              </DetailSection>

              {/* Section: Thông tin sản phẩm */}
              <DetailSection title="Thông tin sản phẩm" contentClassName="space-y-3">
                <DetailField label="Lô sản xuất" value={feedback.productionLotName} />
                <DetailField label="Mã lô sản xuất" mono value={maskId(feedback.productionLotId)} />
                <DetailField label="Loại nông sản" value={feedback.productCategoryName || undefined} />
              </DetailSection>

              {/* Section: Thông tin tổ chức */}
              <DetailSection title="Thông tin tổ chức" contentClassName="space-y-3">
                <DetailField label="Tổ chức" value={feedback.organizationName || undefined} />
                <DetailField label="Mã tổ chức" mono value={maskId(feedback.organizationId) || undefined} />
              </DetailSection>
            </div>

            {/* Footer */}
            <SheetFooter className="mx-0 mb-0 border-t px-6 py-4 sm:flex-row">
              <Button variant="outline" className="w-full" onClick={onClose}>
                Đóng
              </Button>
            </SheetFooter>
          </>
        )}
      </SheetContent>
    </Sheet>
  );
}