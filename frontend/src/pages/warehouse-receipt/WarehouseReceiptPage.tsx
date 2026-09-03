import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Warehouse, Eye, Plus } from 'lucide-react';
import { HelpButton } from '@/components/help/HelpButton';
import { Button } from '@/components/ui/button';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { RefreshButton } from '@/components/common/RefreshButton';
import { DataTableShell } from '@/components/common/DataTableShell';
import { Pagination } from '@/components/common/Pagination';
import { StatusBadge } from '@/components/common/StatusBadge';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { useWarehouseReceipt } from '@/hooks/useWarehouseReceipt';
import { WarehouseReceiptCreateDialog } from './components/WarehouseReceiptCreateDialog';

const PAGE_SIZE = 10;

const formatDate = (iso: string) => {
  try {
    return new Date(iso).toLocaleDateString('vi-VN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
    });
  } catch {
    return iso;
  }
};

export default function WarehouseReceiptPage() {
  const { list, isLoadingList, error, fetchList } = useWarehouseReceipt();
  const [createOpen, setCreateOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const navigate = useNavigate();

  const handleCreated = () => {
    setCreateOpen(false);
    setSearch('');
    setPage(0);
    fetchList(0, 1000);
  };

  useSetBreadcrumb([
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Nhập kho' },
  ]);

  useEffect(() => {
    fetchList(0, 1000);
  }, [fetchList]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return list;
    return list.filter((r) =>
      (r.traceCode || '').toLowerCase().includes(q) ||
      r.shipmentName.toLowerCase().includes(q) ||
      r.recordedBy.toLowerCase().includes(q)
    );
  }, [list, search]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = useMemo(() => {
    const start = safePage * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, safePage]);

  const header = (
    <>
      <TableHead className="w-12 text-center">STT</TableHead>
      <TableHead>Mã lô</TableHead>
      <TableHead>Tên lô</TableHead>
      <TableHead className="text-right">Số lượng KN</TableHead>
      <TableHead className="text-right">Thực nhận</TableHead>
      <TableHead className="text-right">Chênh lệch</TableHead>
      <TableHead className="text-center">%</TableHead>
      <TableHead>Ngày nhập</TableHead>
      <TableHead>Người ghi</TableHead>
      <TableHead className="text-center">Thao tác</TableHead>
    </>
  );

  const body = paginated.map((receipt, index) => (
    <TableRow key={receipt.id} className="hover:bg-muted/40 transition-colors">
      <TableCell className="text-center font-medium text-muted-foreground">
        {safePage * PAGE_SIZE + index + 1}
      </TableCell>
      <TableCell className="font-mono text-xs">{receipt.traceCode || '—'}</TableCell>
      <TableCell className="font-medium">{receipt.shipmentName}</TableCell>
      <TableCell className="text-right">{receipt.declaredQuantity?.toLocaleString('vi-VN')}</TableCell>
      <TableCell className="text-right">{receipt.receivedQuantity?.toLocaleString('vi-VN')}</TableCell>
      <TableCell className="text-right">
        <span className={receipt.discrepancy !== 0 ? 'font-medium text-red-600' : 'text-emerald-600'}>
          {(receipt.discrepancy ?? 0) >= 0 ? '+' : ''}{receipt.discrepancy?.toLocaleString('vi-VN')}
        </span>
      </TableCell>
      <TableCell className="text-center">
        <StatusBadge
          tone={receipt.isDiscrepancyExceeded ? 'danger' : 'success'}
          label={`${receipt.discrepancyPercent ?? 0}%`}
        />
      </TableCell>
      <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
        {formatDate(receipt.receiptDate)}
      </TableCell>
      <TableCell className="text-sm">{receipt.recordedBy}</TableCell>
      <TableCell className="text-center">
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={() => navigate(`/warehouse-receipt/${receipt.id}`)}
          className="hover:bg-muted"
          title="Xem chi tiết"
        >
          <Eye className="size-4" />
        </Button>
      </TableCell>
    </TableRow>
  ));

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={Warehouse}
        iconBoxClassName="bg-blue-100 text-blue-700 dark:text-blue-400"
        title="Nhập kho"
        description="Theo dõi và ghi nhận nhập kho cho các lô hàng đã thu mua."
        actions={
          <>
            <HelpButton screenKey="warehouse-receipt" />
            <Button
              className="shrink-0"
              variant="create"
              size="sm"
              onClick={() => setCreateOpen(true)}
            >
              <Plus className="h-4 w-4 mr-1" />
              Nhập kho
            </Button>
          </>
        }
      />

      <ListCard>
        <ListToolbar
          left={
            <SearchInput
              placeholder="Tìm theo mã lô, tên lô hoặc người ghi..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          }
          right={<RefreshButton onClick={() => fetchList(0, 1000)} loading={isLoadingList} />}
        />

        <DataTableShell
          header={header}
          body={body}
          loading={isLoadingList}
          empty={!isLoadingList && filtered.length === 0}
          colSpan={10}
          loadingMessage="Đang tải danh sách nhập kho..."
          emptyMessage={
            search
              ? 'Không tìm thấy sự kiện nhập kho nào phù hợp với bộ lọc.'
              : 'Chưa có sự kiện nhập kho nào.'
          }
        />

        {!isLoadingList && filtered.length > 0 && (
          <Pagination
            currentPage={safePage}
            totalPages={totalPages}
            totalElements={filtered.length}
            pageSize={PAGE_SIZE}
            itemLabel="sự kiện"
            onPageChange={setPage}
          />
        )}
      </ListCard>

      {error && (
        <p className="text-sm text-red-600">{error}</p>
      )}

      <WarehouseReceiptCreateDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onCreated={handleCreated}
      />
    </div>
  );
}
