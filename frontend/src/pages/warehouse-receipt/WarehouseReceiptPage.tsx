import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Warehouse, Plus } from 'lucide-react';
import { HelpButton } from '@/components/help/HelpButton';
import { Button } from '@/components/ui/button';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { RefreshButton } from '@/components/common/RefreshButton';
import { DataTableShell } from '@/components/common/DataTableShell';
import { Pagination } from '@/components/common/Pagination';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { useWarehouseReceipt } from '@/hooks/useWarehouseReceipt';
import { WarehouseReceiptCreateDialog } from './components/WarehouseReceiptCreateDialog';
import {
  WarehouseReceiptTableBody,
  WarehouseReceiptTableHeader,
} from './components/WarehouseReceiptTable';

const PAGE_SIZE = 10;

/**
 * Trang danh sách sự kiện nhập kho nông sản.
 */
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
    { label: 'Tổng quan', href: '/dashboard' },
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

  const header = <WarehouseReceiptTableHeader />;
  const body = (
    <WarehouseReceiptTableBody
      receipts={paginated}
      page={safePage}
      pageSize={PAGE_SIZE}
      onView={(receiptId) => navigate(`/warehouse-receipt/${receiptId}`)}
    />
  );

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
