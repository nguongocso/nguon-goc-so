import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Hash } from 'lucide-react';
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
import { getSupplementRequests } from '@/api/codeRangeSupplementApi';
import type {
  CodeRangeSupplementRequest,
  CodeRangeSupplementStatus,
} from '@/types/codeRangeSupplement';

const PAGE_SIZE = 10;

const STATUS_FILTER_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'PENDING', label: 'Chờ duyệt' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'REJECTED', label: 'Đã từ chối' },
];

const STATUS_TONE: Record<CodeRangeSupplementStatus, 'warning' | 'success' | 'danger'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

const STATUS_LABEL: Record<CodeRangeSupplementStatus, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
};

export const CodeRangeSupplementListPage = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<CodeRangeSupplementRequest[]>([]);
  const [loading, setLoading] = useState(false);

  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ALL');
  const [page, setPage] = useState(0);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Duyệt bổ sung dải mã' },
  ]);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const result = await getSupplementRequests({
        page: 0,
        size: 1000,
      });
      setData(result.items);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể tải danh sách yêu cầu');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return data.filter((item) => {
      const matchKeyword =
        !q ||
        item.organizationName.toLowerCase().includes(q) ||
        (item.requestedBy?.fullName ?? '').toLowerCase().includes(q) ||
        item.reason.toLowerCase().includes(q);
      const matchStatus = status === 'ALL' || item.status === status;
      return matchKeyword && matchStatus;
    });
  }, [data, search, status]);

  const pendingCount = useMemo(() => data.filter((item) => item.status === 'PENDING').length, [data]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = useMemo(() => {
    const start = safePage * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, safePage]);

  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={Hash}
        title="Duyệt yêu cầu cấp bổ sung dải mã"
        description={
          pendingCount > 0
            ? `Có ${pendingCount} yêu cầu đang chờ duyệt.`
            : 'Xét duyệt các yêu cầu cấp bổ sung dải mã truy xuất của các tổ chức.'
        }
        actions={<HelpButton screenKey="code-range-supplement-list" />}
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo tổ chức, người yêu cầu hoặc lý do..."
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
              <TableHead>Tổ chức</TableHead>
              <TableHead>SL đề nghị</TableHead>
              <TableHead>Người yêu cầu</TableHead>
              <TableHead>Thời điểm</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-center">Thao tác</TableHead>
            </>
          }
          body={paginated.map((item, index) => (
            <TableRow key={item.id} className="hover:bg-muted/40 transition-colors">
              <TableCell className="text-center font-medium text-muted-foreground">
                {safePage * PAGE_SIZE + index + 1}
              </TableCell>
              <TableCell className="font-medium">{item.organizationName}</TableCell>
              <TableCell>{item.requestedQuantity.toLocaleString()}</TableCell>
              <TableCell>{item.requestedBy?.fullName || '—'}</TableCell>
              <TableCell>{formatDate(item.requestedAt)}</TableCell>
              <TableCell>
                <StatusBadge label={STATUS_LABEL[item.status]} tone={STATUS_TONE[item.status]} />
              </TableCell>
              <TableCell className="text-center">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => navigate(`/admin/code-range-supplements/${item.id}`)}
                >
                  Chi tiết
                </Button>
              </TableCell>
            </TableRow>
          ))}
          loading={loading}
          empty={!loading && filtered.length === 0}
          loadingMessage="Đang tải danh sách yêu cầu bổ sung mã..."
          emptyMessage={
            search || status !== 'ALL'
              ? 'Không tìm thấy yêu cầu nào phù hợp với bộ lọc.'
              : 'Chưa có yêu cầu cấp bổ sung nào.'
          }
        />

        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filtered.length}
          pageSize={PAGE_SIZE}
          loading={loading}
          itemLabel="yêu cầu bổ sung mã"
          onPageChange={setPage}
        />
      </ListCard>
    </div>
  );
};
