import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  /** Chỉ số trang hiện tại (0-based) */
  currentPage: number;
  /** Tổng số trang */
  totalPages: number;
  /** Tổng số phần tử */
  totalElements: number;
  /** Số phần tử trên một trang */
  pageSize: number;
  /** Đang tải dữ liệu */
  loading?: boolean;
  /** Nhãn đơn vị tính (ví dụ: "khóa", "tổ chức", "chỉ tiêu") */
  itemLabel?: string;
  /** Callback khi chuyển trang */
  onPageChange: (page: number) => void;
}

/**
 * Thanh phân trang chuẩn toàn dự án (giống PartnerApiKeyListPage).
 * - Nút "Trang trước" / "Trang sau" với icon ChevronLeft / ChevronRight
 * - Hiển thị: "Hiển thị X - Y trên tổng số Z <itemLabel>"
 * - Hiển thị số trang: "{currentPage + 1} / {totalPages}"
 */
export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  totalElements,
  pageSize,
  loading = false,
  itemLabel = 'mục',
  onPageChange,
}) => {
  if (totalPages <= 1) return null;

  const startItem = currentPage * pageSize + 1;
  const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div className="flex items-center justify-between pt-2 text-xs sm:text-sm text-muted-foreground">
      <div>
        Hiển thị {startItem} - {endItem} trên tổng số {totalElements} {itemLabel}
      </div>
      <div className="flex items-center gap-1">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(Math.max(0, currentPage - 1))}
          disabled={currentPage === 0 || loading}
        >
          <ChevronLeft className="w-4 h-4 mr-1" />
          Trang trước
        </Button>
        <span className="px-2 font-medium">
          {currentPage + 1} / {totalPages}
        </span>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
          disabled={currentPage >= totalPages - 1 || loading}
        >
          Trang sau
          <ChevronRight className="w-4 h-4 ml-1" />
        </Button>
      </div>
    </div>
  );
};

export default Pagination;
