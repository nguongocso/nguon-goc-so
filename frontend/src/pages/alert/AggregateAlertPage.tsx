import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { Pagination } from '@/components/common/Pagination';
import { getAggregateAlerts } from '@/api/aggregateAlertApi';
import { AggregateAlertSummaryCards } from './components/AggregateAlertSummaryCards';
import { AggregateAlertFilters } from './components/AggregateAlertFilters';
import { AggregateAlertTable } from './components/AggregateAlertTable';
import type {
  AggregateAlertFilterParams,
  AggregateAlertPageResponse,
} from '@/types/aggregateAlert';

export default function AggregateAlertPage() {
  const { user } = useAuth();
  const isAdmin = user?.roleCode === 'VT-01';

  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<AggregateAlertPageResponse | null>(null);

  const [filters, setFilters] = useState<AggregateAlertFilterParams>({
    status: 'OPEN',
    page: 0,
    size: 10,
  });

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getAggregateAlerts(filters);
      setData(res);
    } catch (err: unknown) {
      const errorMsg =
        err instanceof Error ? err.message : 'Không thể tải danh sách cảnh báo';
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleFilterChange = (newFilters: Partial<AggregateAlertFilterParams>) => {
    setFilters((prev) => ({
      ...prev,
      ...newFilters,
      page: newFilters.page !== undefined ? newFilters.page : 0,
    }));
  };

  const handleResetFilters = () => {
    setFilters({
      status: 'OPEN',
      page: 0,
      size: 10,
    });
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  const hasActiveFilters = Boolean(
    filters.keyword ||
    filters.type ||
    filters.severity ||
    (filters.status && filters.status !== 'OPEN')
  );

  return (
    <div className="space-y-6">
      {/* Tiêu đề trang chuẩn hệ thống */}
      <ListPageHeader
        icon={ShieldAlert}
        title="Tổng hợp cảnh báo"
        description={
          isAdmin
            ? 'Theo dõi và quản lý tập trung toàn bộ các cảnh báo đang mở trên toàn nền tảng'
            : 'Giám sát tập trung các cảnh báo đang mở của tổ chức từ 7 nguồn và xử lý dứt điểm'
        }
      />

      {/* Thẻ thống kê tóm tắt theo mức độ khẩn cấp */}
      <AggregateAlertSummaryCards
        summaryCounts={data?.summaryCounts}
        selectedSeverity={filters.severity}
        onSelectSeverity={(sev) => handleFilterChange({ severity: sev || undefined, page: 0 })}
      />

      {/* Khung danh sách cảnh báo chuẩn ListCard giống trang Lô sản xuất */}
      <ListCard>
        {/* Bộ lọc dữ liệu */}
        <AggregateAlertFilters
          filters={filters}
          onFilterChange={handleFilterChange}
          onReset={handleResetFilters}
          onRefresh={fetchData}
          loading={loading}
          hasActiveFilters={hasActiveFilters}
          isAdmin={isAdmin}
        />

        {/* Bảng danh sách cảnh báo qua DataTableShell */}
        <AggregateAlertTable
          items={data?.items || []}
          isAdmin={isAdmin}
          page={filters.page || 0}
          pageSize={filters.size || 10}
          loading={loading}
          hasActiveFilters={hasActiveFilters}
          onResetFilters={handleResetFilters}
        />

        {/* Phân trang */}
        {data && data.totalElements > 0 && (
          <Pagination
            currentPage={filters.page || 0}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            pageSize={filters.size || 10}
            loading={loading}
            itemLabel="cảnh báo"
            onPageChange={handlePageChange}
          />
        )}
      </ListCard>
    </div>
  );
}

