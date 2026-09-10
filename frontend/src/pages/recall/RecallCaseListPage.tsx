import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { PackageCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { HelpButton } from '@/components/help/HelpButton';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { RefreshButton } from '@/components/common/RefreshButton';
import { DataTableShell } from '@/components/common/DataTableShell';
import { Pagination } from '@/components/common/Pagination';
import { StatusBadge } from '@/components/common/StatusBadge';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { getRecallCases } from '@/api/recallCaseApi';
import type { RecallCase, RecallCaseStatus } from '@/types/recallCase';

const PAGE_SIZE = 10;

const STATUS_FILTER_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'OPEN', label: 'Đang xử lý' },
  { value: 'CLOSED', label: 'Đã xử lý xong' },
];

const STATUS_TONE: Record<RecallCaseStatus, 'warning' | 'success'> = {
  OPEN: 'warning',
  CLOSED: 'success',
};

const STATUS_LABEL: Record<RecallCaseStatus, string> = {
  OPEN: 'Đang xử lý',
  CLOSED: 'Đã xử lý xong',
};

/**
 * Danh sách vụ việc thu hồi (NCL-08-CN-012).
 * Cho phép Quản lý hợp tác xã (VT-02) theo dõi và mở chi tiết
 * để nhập kết quả xử lý, biện pháp khắc phục và kết thúc vụ việc.
 */
export const RecallCaseListPage = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<RecallCase[]>([]);
  const [loading, setLoading] = useState(false);

  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ALL');
  const [page, setPage] = useState(0);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Vụ việc thu hồi' },
  ]);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const result = await getRecallCases();
      setData(result);
    } catch (err: any) {
      toast.error(
        err.response?.data?.message || 'Không thể tải danh sách vụ việc thu hồi',
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return data.filter((item) => {
      const matchKeyword =
        !q ||
        item.caseCode.toLowerCase().includes(q) ||
        item.productionLotName.toLowerCase().includes(q);
      const matchStatus =
        status === 'ALL' || item.status === status;
      return matchKeyword && matchStatus;
    });
  }, [data, search, status]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = filtered.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleDateString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="space-y-4">
      <ListPageHeader
        icon={PackageCheck}
        title="Vụ việc thu hồi"
        description="Theo dõi kết quả xử lý các lô bị thu hồi và kết thúc vụ việc thu hồi."
        actions={<HelpButton screenKey="recall-case-list" />}
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo mã vụ việc hoặc lô sản xuất..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
              />
              <FilterSelect
                value={status}
                onValueChange={(val) => {
                  setStatus(val || 'ALL');
                  setPage(0);
                }}
                options={STATUS_FILTER_OPTIONS}
              />
            </>
          }
          right={<RefreshButton onClick={load} loading={loading} />}
        />

        <DataTableShell
          colSpan={8}
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Mã vụ việc</TableHead>
              <TableHead>Lô sản xuất</TableHead>
              <TableHead className="text-center">Số lô hàng</TableHead>
              <TableHead>Ngày mở</TableHead>
              <TableHead>Ngày đóng</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-center">Thao tác</TableHead>
            </>
          }
          body={paginated.map((item, index) => (
            <TableRow key={item.id} className="hover:bg-muted/40 transition-colors">
              <TableCell className="text-center font-medium text-muted-foreground">
                {safePage * PAGE_SIZE + index + 1}
              </TableCell>
              <TableCell className="font-mono text-sm font-medium">
                {item.caseCode}
              </TableCell>
              <TableCell className="font-medium">{item.productionLotName}</TableCell>
              <TableCell className="text-center">{item.shipmentCount}</TableCell>
              <TableCell>{formatDate(item.createdAt)}</TableCell>
              <TableCell>{formatDate(item.closedAt)}</TableCell>
              <TableCell>
                <StatusBadge
                  label={STATUS_LABEL[item.status]}
                  tone={STATUS_TONE[item.status]}
                />
              </TableCell>
              <TableCell className="text-center">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => navigate(`/recall-cases/${item.id}`)}
                >
                  Chi tiết
                </Button>
              </TableCell>
            </TableRow>
          ))}
          loading={loading}
          empty={!loading && filtered.length === 0}
          loadingMessage="Đang tải danh sách vụ việc thu hồi..."
          emptyMessage={
            search || status !== 'ALL'
              ? 'Không tìm thấy vụ việc thu hồi nào phù hợp với bộ lọc.'
              : 'Chưa có vụ việc thu hồi nào. Vụ việc được tạo tự động khi có lô hàng bị thu hồi.'
          }
        />

        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filtered.length}
          pageSize={PAGE_SIZE}
          loading={loading}
          itemLabel="vụ việc thu hồi"
          onPageChange={setPage}
        />
      </ListCard>
    </div>
  );
};
