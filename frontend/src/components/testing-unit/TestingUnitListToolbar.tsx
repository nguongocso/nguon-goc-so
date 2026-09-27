import type { FormEvent } from "react";
import { RefreshCw, Search } from "lucide-react";

import { Button } from "@/components/ui/button";
import { CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

/** Lựa chọn lọc theo trạng thái hoạt động của đơn vị kiểm nghiệm. */
export type TestingUnitStatusFilter = "all" | "active" | "inactive";

const filterOptions: { value: TestingUnitStatusFilter; label: string }[] = [
  { value: "all", label: "Tất cả" },
  { value: "active", label: "Đang hoạt động" },
  { value: "inactive", label: "Ngừng hoạt động" },
];

interface TestingUnitListToolbarProps {
  /** Nội dung đang gõ trong ô tìm kiếm. */
  searchInput: string;
  /** Gọi khi nội dung ô tìm kiếm thay đổi. */
  onSearchInputChange: (value: string) => void;
  /** Gọi khi người dùng submit ô tìm kiếm (nhấn Enter). */
  onSearchSubmit: (event: FormEvent) => void;
  /** Trạng thái lọc hiện tại. */
  filter: TestingUnitStatusFilter;
  /** Gọi khi người dùng đổi trạng thái lọc. */
  onFilterChange: (value: TestingUnitStatusFilter) => void;
  /** Đang tải dữ liệu hay không (vô hiệu hoá nút làm mới, quay icon). */
  loading: boolean;
  /** Gọi khi nhấn nút làm mới. */
  onRefresh: () => void;
}

/**
 * Thanh công cụ trên đầu bảng danh sách đơn vị kiểm nghiệm:
 * bên trái là ô tìm kiếm + bộ lọc trạng thái, bên phải là nút làm mới.
 */
export const TestingUnitListToolbar = ({
  searchInput,
  onSearchInputChange,
  onSearchSubmit,
  filter,
  onFilterChange,
  loading,
  onRefresh,
}: TestingUnitListToolbarProps) => {
  return (
    <CardHeader className="border-b border-slate-100 pb-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex flex-1 flex-col gap-2 sm:flex-row sm:items-center">
          <form
            onSubmit={onSearchSubmit}
            className="relative w-full sm:w-64"
          >
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={searchInput}
              onChange={(e) => onSearchInputChange(e.target.value)}
              placeholder="Tìm theo tên hoặc mã công nhận..."
              className="h-9 pl-9"
            />
          </form>
          <Select
            value={filter}
            onValueChange={(val) => onFilterChange(val as TestingUnitStatusFilter)}
          >
            <SelectTrigger size="sm" className="w-full sm:w-[180px]">
              <SelectValue placeholder="Trạng thái">
                {filterOptions.find((opt) => opt.value === filter)?.label}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {filterOptions.map((opt) => (
                <SelectItem key={opt.value} value={opt.value}>
                  {opt.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={onRefresh}
          disabled={loading}
          className="shrink-0 self-start sm:self-auto"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          Làm mới
        </Button>
      </div>
    </CardHeader>
  );
};
