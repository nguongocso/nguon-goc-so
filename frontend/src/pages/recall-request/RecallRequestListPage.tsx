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
import { getRecallRequests } from '@/api/recallApi';
import type {
  RecallRequest,
  RecallRequestStatus,
} from '@/types/recallRequest';

const PAGE_SIZE = 10;

const STATUS_FILTER_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'PENDING', label: 'Chờ duyệt' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'REJECTED', label: 'Đã từ chối' },
];

const STATUS_TONE: Record<RecallRequestStatus, 'warning' | 'success' | 'danger'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

const STATUS_LABEL: Record<RecallRequestStatus, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
};

export const RecallRequestListPage = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<RecallRequest[]>([]);
  const [loading, setLoading] = useState(false);

  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ALL');
  const [page, setPage] = useState(0);

  useSetBreadcrumb([
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Yêu cầu thu hồi' },
  ]);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const result = await getRecallRequests({
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
    load();
  }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return data.filter((item) => {
      const matchKeyword =
        !q ||
        item.lotName.toLowerCase().includes(q) ||
        (item.requestedBy?.fullName ?? '').toLowerCase().includes(q) ||
        item.reason.toLowerCase().includes(q);
      const matchStatus =
        status === 'ALL' ||
        (status === 'PENDING' && item.status === 'PENDING') ||
        (status === 'APPROVED' && item.status === 'APPROVED') ||
        (status === 'REJECTED' && item.status === 'REJECTED');
      return matchKeyword && matchStatus;
    });
  }, [data, search, status]);

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
        icon={PackageX}
        title="Yêu cầu thu hồi lô sản xuất"
        description="Quản lý và xét duyệt các yêu cầu thu hồi lô sản xuất trong hệ thống."
        actions={<HelpButton screenKey="recall-request-list" />}
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo tên lô, người yêu cầu hoặc lý do..."
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
          className="px-3"
          colSpan={7}
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Lô sản xuất</TableHead>
              <TableHead>Người yêu cầu</TableHead>
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
              <TableCell className="font-medium">{item.lotName}</TableCell>
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
                  onClick={() => navigate(`/recall-requests/${item.id}`)}
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
              ? 'Không tìm thấy yêu cầu thu hồi nào phù hợp với bộ lọc.'
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
