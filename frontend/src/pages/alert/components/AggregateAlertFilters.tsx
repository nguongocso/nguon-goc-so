import React from 'react';
import { Search, RotateCcw } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { AggregateAlertFilterParams } from '@/types/aggregateAlert';

interface AggregateAlertFiltersProps {
  filters: AggregateAlertFilterParams;
  onFilterChange: (newFilters: Partial<AggregateAlertFilterParams>) => void;
  onReset: () => void;
  isAdmin?: boolean;
}

const ALERT_TYPES = [
  { value: 'ALL', label: 'Tất cả loại nguồn cảnh báo' },
  { value: 'SCAN_ANOMALY', label: 'Tem quét bất thường' },
  { value: 'CERT_EXPIRING', label: 'Chứng nhận sắp hết hạn' },
  { value: 'CERT_EXPIRED', label: 'Chứng nhận đã hết hạn' },
  { value: 'INSPECTION_EXPIRING', label: 'Kiểm nghiệm sắp hết hiệu lực' },
  { value: 'INSPECTION_EXPIRED', label: 'Kiểm nghiệm đã hết hiệu lực' },
  { value: 'UNPROCESSED_FEEDBACK', label: 'Phản ánh chưa xử lý' },
  { value: 'CODE_RANGE_QUOTA', label: 'Hạn mức dải mã sắp hết' },
  { value: 'OVERDUE_MILESTONE', label: 'Mốc canh tác quá hạn' },
  { value: 'OPEN_RECALL_CASE', label: 'Vụ việc thu hồi đang mở' },
];

const SEVERITIES = [
  { value: 'ALL', label: 'Tất cả mức khẩn cấp' },
  { value: 'HIGH', label: 'Mức cao / Khẩn cấp' },
  { value: 'MEDIUM', label: 'Mức trung bình' },
];

const STATUSES = [
  { value: 'OPEN', label: 'Đang mở (Cần xử lý)' },
  { value: 'RESOLVED', label: 'Đã giải quyết / Đóng' },
  { value: 'ALL', label: 'Tất cả trạng thái' },
];

export const AggregateAlertFilters: React.FC<AggregateAlertFiltersProps> = ({
  filters,
  onFilterChange,
  onReset,
}) => {
  return (
    <div className="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4 shadow-sm sm:flex-row sm:flex-wrap sm:items-center">
      {/* Tìm kiếm từ khóa */}
      <div className="relative flex-1 min-w-[220px]">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <Input
          placeholder="Tìm kiếm nội dung, đối tượng liên quan..."
          value={filters.keyword || ''}
          onChange={(e) => onFilterChange({ keyword: e.target.value, page: 0 })}
          className="pl-9 text-sm"
        />
      </div>

      {/* Lọc loại cảnh báo */}
      <div className="w-full sm:w-[220px]">
        <Select
          value={filters.type || 'ALL'}
          onValueChange={(val) => onFilterChange({ type: val === 'ALL' ? undefined : val, page: 0 })}
        >
          <SelectTrigger className="text-sm">
            <SelectValue placeholder="Loại nguồn cảnh báo" />
          </SelectTrigger>
          <SelectContent>
            {ALERT_TYPES.map((t) => (
              <SelectItem key={t.value} value={t.value}>
                {t.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Lọc mức khẩn cấp */}
      <div className="w-full sm:w-[170px]">
        <Select
          value={filters.severity || 'ALL'}
          onValueChange={(val) => onFilterChange({ severity: val === 'ALL' ? undefined : val, page: 0 })}
        >
          <SelectTrigger className="text-sm">
            <SelectValue placeholder="Mức khẩn cấp" />
          </SelectTrigger>
          <SelectContent>
            {SEVERITIES.map((s) => (
              <SelectItem key={s.value} value={s.value}>
                {s.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Lọc trạng thái */}
      <div className="w-full sm:w-[170px]">
        <Select
          value={filters.status || 'OPEN'}
          onValueChange={(val) => onFilterChange({ status: val, page: 0 })}
        >
          <SelectTrigger className="text-sm">
            <SelectValue placeholder="Trạng thái" />
          </SelectTrigger>
          <SelectContent>
            {STATUSES.map((st) => (
              <SelectItem key={st.value} value={st.value}>
                {st.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Nút đặt lại bộ lọc */}
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={onReset}
        className="h-10 text-gray-600 hover:text-gray-900"
        title="Đặt lại toàn bộ bộ lọc"
      >
        <RotateCcw className="mr-1.5 h-4 w-4" />
        Đặt lại
      </Button>
    </div>
  );
};
