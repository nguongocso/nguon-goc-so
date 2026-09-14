import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { FileSignature, Eye } from 'lucide-react';
import { HelpButton } from '@/components/help/HelpButton';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { RefreshButton } from '@/components/common/RefreshButton';
import { DataTableShell } from '@/components/common/DataTableShell';
import { Pagination } from '@/components/common/Pagination';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { getHandovers } from '@/api/handoverApi';
import type { HandoverSummary, HandoverStatus } from '@/types/handover';

const PAGE_SIZE = 10;

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'PENDING', label: 'Chờ xác nhận' },
  { value: 'ACCEPTED', label: 'Đã xác nhận' },
  { value: 'REJECTED', label: 'Đã từ chối' },
  { value: 'EXPIRED', label: 'Hết hiệu lực' },
];

const STATUS_CONFIG: Record<
  string,
  { label: string; className: string }
> = {
  PENDING: {
    label: 'Chờ xác nhận',
    className: 'bg-amber-100 text-amber-800 border-amber-200',
  },
  PENDING_CONFIRMATION: {
    label: 'Chờ xác nhận',
    className: 'bg-amber-100 text-amber-800 border-amber-200',
  },
  ACCEPTED: {
    label: 'Đã xác nhận',
    className: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  },
  REJECTED: {
    label: 'Đã từ chối',
    className: 'bg-red-100 text-red-700 border-red-200',
  },
  EXPIRED: {
    label: 'Hết hiệu lực',
    className: 'bg-slate-100 text-slate-600 border-slate-200',
  },
  CANCELLED: {
    label: 'Đã hủy',
    className: 'bg-slate-100 text-slate-600 border-slate-200',
  },
};

function StatusBadge({ status }: { status: HandoverStatus }) {
  const config = STATUS_CONFIG[status] || {
    label: status,
    className: 'bg-slate-100 text-slate-600 border-slate-200',
  };

  return (
    <Badge variant="secondary" className={config.className}>
      {config.label}
    </Badge>
  );
}

const formatDateTime = (iso?: string | null): string => {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return iso;
  }
};

/**
 * Danh sách phiếu bàn giao đã gửi dành riêng cho Quản lý Hợp tác xã (VT-02).
 */
export function SentHandoverListPage() {
  const navigate = useNavigate();
  const [handovers, setHandovers] = useState<HandoverSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Phiếu bàn giao đã gửi' },
  ]);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const res = await getHandovers({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        search: search.trim() || undefined,
        page,
        size: PAGE_SIZE,
      });
      setHandovers(res.items || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          'Không thể tải danh sách phiếu bàn giao đã gửi.',
      );
    } finally {
      setIsLoading(false);
    }
  }, [page, search, statusFilter]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={FileSignature}
        iconBoxClassName="bg-blue-500/10"
        title="Phiếu bàn giao đã gửi"
        description="Xem các phiếu bàn giao lô hàng do tổ chức bạn tạo ra và gửi tới doanh nghiệp thu mua."
        actions={<HelpButton screenKey="dashboard" />}
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo tên lô hàng hoặc tổ chức nhận..."
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                aria-label="Tìm kiếm phiếu bàn giao đã gửi"
              />
              <FilterSelect
                value={statusFilter}
                onValueChange={(value) => {
                  setStatusFilter(value ?? 'ALL');
                  setPage(0);
                }}
                options={STATUS_OPTIONS}
              />
            </>
          }
          right={<RefreshButton onClick={() => void loadData()} loading={isLoading} />}
        />

        <DataTableShell
          colSpan={7}
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Tên lô hàng</TableHead>
              <TableHead>Tổ chức nhận</TableHead>
              <TableHead className="text-right">Số lượng</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Ngày tạo</TableHead>
              <TableHead className="text-center">Thao tác</TableHead>
            </>
          }
          body={handovers.map((handover, index) => (
            <TableRow
              key={handover.id}
              className="cursor-pointer hover:bg-muted/40 transition-colors"
              onClick={() => navigate(`/handover/${handover.id}`)}
            >
              <TableCell className="text-center font-medium text-muted-foreground">
                {page * PAGE_SIZE + index + 1}
              </TableCell>
              <TableCell>
                <div className="font-semibold text-foreground">
                  {handover.shipmentName}
                </div>
              </TableCell>
              <TableCell className="text-muted-foreground">
                {handover.toOrganizationName || '—'}
              </TableCell>
              <TableCell className="text-right text-muted-foreground">
                {handover.quantity.toLocaleString('vi-VN')} {handover.unit || 'kg'}
              </TableCell>
              <TableCell>
                <StatusBadge status={handover.status} />
              </TableCell>
              <TableCell className="text-muted-foreground">
                {formatDateTime(handover.createdAt)}
              </TableCell>
              <TableCell
                className="text-center"
                onClick={(e) => e.stopPropagation()}
              >
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => navigate(`/handover/${handover.id}`)}
                >
                  <Eye className="mr-1.5 h-4 w-4" />
                  Xem chi tiết
                </Button>
              </TableCell>
            </TableRow>
          ))}
          loading={isLoading}
          empty={!isLoading && handovers.length === 0}
          loadingMessage="Đang tải danh sách phiếu bàn giao..."
          emptyMessage={
            search.trim() || statusFilter !== 'ALL'
              ? 'Không tìm thấy phiếu bàn giao phù hợp. Hãy thử thay đổi từ khóa tìm kiếm.'
              : 'Chưa có phiếu bàn giao nào được tạo bởi tổ chức của bạn.'
          }
        />

        <Pagination
          currentPage={page}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={PAGE_SIZE}
          loading={isLoading}
          itemLabel="phiếu bàn giao"
          onPageChange={setPage}
        />
      </ListCard>
    </div>
  );
}

export default SentHandoverListPage;
