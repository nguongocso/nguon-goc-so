import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

/**
 * Thanh phân trang dùng chung cho các bảng danh sách (0-based page).
 *
 * Dùng cho cả hai kiểu:
 * - Server-side: truyền page/pageSize/totalElements do API trả về.
 * - Client-side: truyền danh sách đã lọc, component không tự cắt mảng —
 *   bên gọi chịu trách nhiệm slice danh sách theo `page`, chỉ dùng
 *   `onPageChange` để cập nhật state.
 */
interface DataTablePaginationProps {
  /** Trang hiện tại, 0-based. */
  page: number;
  /** Số bản ghi mỗi trang. */
  pageSize: number;
  /** Tổng số bản ghi (đã lọc nếu dùng client-side). */
  totalElements: number;
  onPageChange: (page: number) => void;
  /** Nhãn hiển thị cạnh tổng số (vd: "vùng trồng", "lô sản xuất"). */
  itemLabel?: string;
}

export function DataTablePagination({
  page,
  pageSize,
  totalElements,
  onPageChange,
  itemLabel = "bản ghi",
}: DataTablePaginationProps) {
  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));
  const safePage = Math.min(Math.max(0, page), totalPages - 1);
  const from = totalElements === 0 ? 0 : safePage * pageSize + 1;
  const to = Math.min((safePage + 1) * pageSize, totalElements);

  if (totalElements === 0) return null;

  return (
    <div className="flex flex-col items-center justify-between gap-3 border-t border-slate-100 px-4 py-3 sm:flex-row">
      <p className="text-sm text-muted-foreground">
        Hiển thị {from}–{to} trong tổng số {totalElements} {itemLabel}
      </p>
      <div className="flex items-center gap-1">
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={safePage <= 0}
          onClick={() => onPageChange(safePage - 1)}
        >
          <ChevronLeft className="size-4" /> Trước
        </Button>
        <span className="min-w-[80px] text-center text-sm text-muted-foreground">
          Trang {safePage + 1} / {totalPages}
        </span>
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={safePage >= totalPages - 1}
          onClick={() => onPageChange(safePage + 1)}
        >
          Sau <ChevronRight className="size-4" />
        </Button>
      </div>
    </div>
  );
}