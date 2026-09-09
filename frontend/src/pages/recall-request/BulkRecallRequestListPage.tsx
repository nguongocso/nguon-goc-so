import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { PackageX } from 'lucide-react';
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
import { getBulkRecallRequests } from '@/api/recallApi';
import type {
  BulkRecallRequest,
  BulkRecallRequestStatus,
} from '@/types/bulkRecall';

const PAGE_SIZE = 10;

const STATUS_FILTER_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'PENDING', label: 'Chờ duyệt' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'REJECTED', label: 'Đã từ chối' },
];

const STATUS_TONE: Record<BulkRecallRequestStatus, 'warning' | 'success' | 'danger'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

const STATUS_LABEL: Record<BulkRecallRequestStatus, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
};

/**
 * Danh sách yêu cầu thu hồi theo phạm vi ảnh hưởng (NCL-08-CN-011).
 * Cho phép quản lý (VT-02) tra cứu và mở chi tiết để phê duyệt.
 */
export const BulkRecallRequestListPage = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<BulkRecallRequest[]>([]);
  const [loading, setLoading] = useState(false);

  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ALL');
  const [page, setPage] = useState(0);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Yêu cầu thu hồi theo phạm vi' },
  ]);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const result = await getBulkRecallRequests({
        page: 0,
        size: 1000,
      });
      setData(result.items);
    } catch (err: any) {
      toast.error(
        err.response?.data?.message || 'Không thể tải danh sách yêu cầu thu hồi',
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
        item.productionLotName.toLowerCase().includes(q) ||
        (item.requestedBy?.fullName ?? '').toLowerCase().includes(q) ||
        item.reason.toLowerCase().includes(q);
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

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="space-y-4">
      <ListPageHeader
        icon={PackageX}
        title="Yêu cầu thu hồi theo phạm vi ảnh hưởng"
        description="Danh sách yêu cầu thu hồi hàng loạt theo kết quả truy vết phạm vi ảnh hưởng."
        actions={<HelpButton screenKey="bulk-recall-request-list" />}
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo lô sản xuất, người tạo hoặc lý do..."
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
          colSpan={7}
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Lô sản xuất nguồn</TableHead>
              <TableHead>Người tạo</TableHead>
              <TableHead>Thời điểm</TableHead>
              <TableHead>Lý do</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-center">Thao tác</TableHead>
            </>
          }
          body={paginated.map((item, index) => (
            <TableRow key={item.id} className="hover:bg-muted/40 transition-colors">
              <TableCell className="text-center font-medium text-muted-foreground">
                {safePage * PAGE_SIZE + index + 1}
              </TableCell>
              <TableCell className="font-medium">{item.productionLotName}</TableCell>
              <TableCell>{item.requestedBy?.fullName || '—'}</TableCell>
              <TableCell>{formatDate(item.requestedAt)}</TableCell>
              <TableCell className="max-w-[240px] truncate" title={item.reason}>
                {item.reason}
              </TableCell>
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
                  onClick={() => navigate(`/recall-requests/bulk/${item.id}`)}
                >
                  Chi tiết
                </Button>
              </TableCell>
            </TableRow>
          ))}
          loading={loading}
          empty={!loading && filtered.length === 0}
          loadingMessage="Đang tải danh sách yêu cầu thu hồi..."
          emptyMessage={
            search || status !== 'ALL'
              ? 'Không tìm thấy yêu cầu nào phù hợp với bộ lọc.'
              : 'Chưa có yêu cầu thu hồi nào.'
          }
        />

        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filtered.length}
          pageSize={PAGE_SIZE}
          loading={loading}
          itemLabel="yêu cầu thu hồi"
          onPageChange={setPage}
        />
      </ListCard>
    </div>
  );
};

export default BulkRecallRequestListPage;