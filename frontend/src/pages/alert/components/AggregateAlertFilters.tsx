import React from 'react';
import { RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { RefreshButton } from '@/components/common/RefreshButton';
import type {
  AggregateAlertFilterParams,
  AggregateAlertType,
  AggregateAlertSeverity,
  AggregateAlertStatus,
} from '@/types/aggregateAlert';

interface AggregateAlertFiltersProps {
  filters: AggregateAlertFilterParams;
  onFilterChange: (newFilters: Partial<AggregateAlertFilterParams>) => void;
  onReset: () => void;
  onRefresh?: () => void;
  loading?: boolean;
  hasActiveFilters?: boolean;
  isAdmin?: boolean;
}

const ALERT_TYPES = [
  { value: 'ALL', label: 'Tất cả nguồn cảnh báo' },
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
  { value: 'ALL', label: 'Tất cả mức độ' },
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
  onRefresh,
  loading = false,
  hasActiveFilters = false,
}) => {
  return (
    <ListToolbar
      left={
        <>
          {/* Tìm kiếm từ khóa */}
          <SearchInput
            placeholder="Tìm kiếm nội dung, đối tượng liên quan..."
            value={filters.keyword || ''}
            onChange={(e) => onFilterChange({ keyword: e.target.value, page: 0 })}
            className="w-full sm:w-auto flex-1 min-w-[240px]"
          />

          {/* Lọc nguồn cảnh báo */}
          <FilterSelect
            value={filters.type || 'ALL'}
            onValueChange={(val) =>
              onFilterChange({
                type: !val || val === 'ALL' ? undefined : (val as AggregateAlertType),
                page: 0,
              })
            }
            options={ALERT_TYPES}
            className="w-full sm:w-auto min-w-[200px]"
          />

          {/* Lọc mức khẩn cấp */}
          <FilterSelect
            value={filters.severity || 'ALL'}
            onValueChange={(val) =>
              onFilterChange({
                severity: !val || val === 'ALL' ? undefined : (val as AggregateAlertSeverity),
                page: 0,
              })
            }
            options={SEVERITIES}
            className="w-full sm:w-auto min-w-[170px]"
          />

          {/* Lọc trạng thái */}
          <FilterSelect
            value={filters.status || 'OPEN'}
            onValueChange={(val) =>
              onFilterChange({
                status: !val || val === 'ALL' ? undefined : (val as AggregateAlertStatus),
                page: 0,
              })
            }
            options={STATUSES}
            className="w-full sm:w-auto min-w-[190px]"
          />
        </>
      }
      right={
        <div className="flex items-center gap-2">
          {hasActiveFilters && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onReset}
              className="h-9 gap-1.5 text-xs text-muted-foreground hover:text-foreground"
            >
              <RotateCcw className="size-3.5" />
              Đặt lại
            </Button>
          )}
          {onRefresh && (
            <RefreshButton onClick={onRefresh} loading={loading} />
          )}
        </div>
      }
    />
  );
};
