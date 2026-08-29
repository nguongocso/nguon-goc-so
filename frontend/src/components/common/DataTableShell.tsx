import React from 'react';
import { RefreshCw } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { cn } from '@/lib/utils';

interface DataTableShellProps {
  /** Hàng header của bảng (các `<TableHead>`). */
  header: React.ReactNode;
  /** Nội dung thân bảng (các `<TableRow>`). */
  body: React.ReactNode;
  /** Đang tải — hiện spinner giữa bảng. */
  loading?: boolean;
  /** Danh sách rỗng — hiện text căn giữa. */
  empty?: boolean;
  /** Số cột dùng cho cell trải dài (loading/empty). Mặc định 1. */
  colSpan?: number;
  /** Thông báo khi loading. */
  loadingMessage?: string;
  /** Thông báo khi rỗng. */
  emptyMessage?: string;
  /** Nút hành động tùy chọn hiển thị dưới thông báo khi danh sách rỗng. */
  emptyAction?: React.ReactNode;
  /** Class bọc ngoài bảng (mặc định theo chuẩn API key). */
  className?: string;
}

/**
 * Khung bảng danh sách chuẩn toàn dự án (lấy từ khối bảng của PartnerApiKeyListPage).
 * Bao gồm: wrapper `rounded-md border`, header `bg-muted/50`, trạng thái loading
 * (spinner `RefreshCw` giữa bảng) và empty (1 dòng text căn giữa).
 */
export const DataTableShell: React.FC<DataTableShellProps> = ({
  header,
  body,
  loading = false,
  empty = false,
  colSpan = 1,
  loadingMessage = 'Đang tải dữ liệu...',
  emptyMessage = 'Không tìm thấy dữ liệu.',
  emptyAction,
  className,
}) => {
  const showStateRow = loading || empty;

  return (
    <div className={cn('rounded-md border overflow-x-auto', className)}>
      <Table>
        <TableHeader>
          <TableRow className="bg-muted/50">{header}</TableRow>
        </TableHeader>
        <TableBody>
          {showStateRow ? (
            <TableRow>
              <TableCell colSpan={colSpan} className="h-32 text-center text-muted-foreground">
                {loading ? (
                  <div className="flex flex-col items-center justify-center gap-2">
                    <RefreshCw className="w-6 h-6 animate-spin text-emerald-600" />
                    <span>{loadingMessage}</span>
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center gap-2">
                    <span>{emptyMessage}</span>
                    {emptyAction}
                  </div>
                )}
              </TableCell>
            </TableRow>
          ) : (
            body
          )}
        </TableBody>
      </Table>
    </div>
  );
};