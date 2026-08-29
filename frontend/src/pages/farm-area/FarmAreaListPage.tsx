import { useEffect, useState, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import {
  TableCell,
  TableHead,
  TableRow,
} from '@/components/ui/table';
import { Plus, MapPin, ExternalLink, Pencil, Trash2, Eye, EyeOff } from 'lucide-react';
import { HelpButton } from '@/components/help/HelpButton';
import { toast } from 'sonner';
import { getFarmAreas, toggleFarmAreaStatus } from '@/api/farmAreaApi';
import type { FarmArea } from '@/types/farmArea';
import { AREA_UNIT_LABELS, convertAreaFromHa } from '@/types/farmArea';
import { useNavigate } from 'react-router-dom';
import { usePermission } from '@/hooks/usePermission';
import { ROLE_ACCESS } from '@/config/roleAccess';
import { FarmAreaDeleteDialog } from '@/components/farm-area/FarmAreaDeleteDialog';
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

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'ACTIVE', label: 'Đang sử dụng' },
  { value: 'INACTIVE', label: 'Ngừng sử dụng' },
];

const FarmAreaListPage: React.FC = () => {
  const navigate = useNavigate();
  const [areas, setAreas] = useState<FarmArea[]>([]);
  const [loading, setLoading] = useState(true);

  // Tìm kiếm, lọc (client-side) & phân trang
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ALL');
  const [page, setPage] = useState(0);

  // Modals state
  const [deletingFarmArea, setDeletingFarmArea] = useState<FarmArea | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const canCreate = usePermission(ROLE_ACCESS.farmAreaCreate);

  const fetchAreas = async () => {
    try {
      setLoading(true);
      const data = await getFarmAreas();
      setAreas(data);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách vùng trồng');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAreas();
  }, []);

  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim();
    return areas.filter((area) => {
      const matchKeyword = !q || area.name.toLowerCase().includes(q);
      const matchStatus =
        status === 'ALL' ||
        (status === 'ACTIVE' && area.isActive) ||
        (status === 'INACTIVE' && !area.isActive);
      return matchKeyword && matchStatus;
    });
  }, [areas, search, status]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = useMemo(() => {
    const start = safePage * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, safePage]);

  const handleToggleStatus = async (area: FarmArea) => {
    if (togglingId !== null) return;
    const newStatus = !area.isActive;
    setTogglingId(area.id);
    try {
      await toggleFarmAreaStatus(area.id, newStatus);
      toast.success(
        newStatus
          ? `Đã kích hoạt lại vùng trồng '${area.name}'`
          : `Đã chuyển vùng trồng '${area.name}' sang Ngừng sử dụng`
      );
      fetchAreas();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể đổi trạng thái vùng trồng');
    } finally {
      setTogglingId(null);
    }
  };

  useSetBreadcrumb([
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Vùng trồng' },
  ]);

  const header = (
    <>
      <TableHead className="w-12 text-center">STT</TableHead>
      <TableHead>Tên vùng</TableHead>
      <TableHead>Loại cây trồng</TableHead>
      <TableHead>Diện tích</TableHead>
      <TableHead>Vị trí (tọa độ)</TableHead>
      <TableHead>Trạng thái</TableHead>
      <TableHead>Ngày tạo</TableHead>
      <TableHead className="text-center">Thao tác</TableHead>
    </>
  );

  const body = paginated.map((area, index) => (
    <TableRow key={area.id} className="hover:bg-muted/40 transition-colors">
      <TableCell className="text-center font-medium text-muted-foreground">
        {safePage * PAGE_SIZE + index + 1}
      </TableCell>
      <TableCell className="font-medium">
        {area.name}
        {area.associatedLotsCount && area.associatedLotsCount > 0 ? (
          <span
            className="ml-2 inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700 border border-blue-100"
            title="Số lô sản xuất liên quan"
          >
            {area.associatedLotsCount} lô
          </span>
        ) : null}
      </TableCell>
      <TableCell>{area.cropTypeName}</TableCell>
      <TableCell>
        {convertAreaFromHa(area.area, area.areaUnit).toLocaleString('vi-VN', {
          maximumFractionDigits: 2,
        })}{' '}
        {AREA_UNIT_LABELS[area.areaUnit]}
      </TableCell>
      <TableCell>
        <a
          href={`https://www.google.com/maps?q=${area.latitude},${area.longitude}`}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1 text-xs font-mono text-emerald-700 hover:text-emerald-800 hover:underline"
          title="Xem trên Google Maps"
        >
          <MapPin className="h-3 w-3 text-emerald-500" />
          {area.latitude.toFixed(4)}, {area.longitude.toFixed(4)}
          <ExternalLink className="h-3 w-3 opacity-50" />
        </a>
      </TableCell>
      <TableCell>
        {area.isActive ? (
          <StatusBadge label="Đang sử dụng" tone="success" />
        ) : (
          <StatusBadge label="Ngừng sử dụng" tone="neutral" />
        )}
      </TableCell>
      <TableCell className="text-muted-foreground">
        {new Date(area.createdAt).toLocaleDateString('vi-VN')}
      </TableCell>
      <TableCell className="text-center">
        <div className="flex justify-center items-center gap-1">
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => navigate(`/chinhsuavungtrong/${area.id}`)}
            title="Sửa thông tin vùng trồng"
          >
            <Pencil className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => handleToggleStatus(area)}
            disabled={togglingId === area.id}
            title={area.isActive ? 'Ngừng sử dụng vùng trồng' : 'Kích hoạt lại vùng trồng'}
          >
            {area.isActive ? (
              <EyeOff className="h-4 w-4" />
            ) : (
              <Eye className="h-4 w-4" />
            )}
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => setDeletingFarmArea(area)}
            title="Xóa vùng trồng"
            className="text-destructive hover:text-destructive hover:bg-muted"
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </TableCell>
    </TableRow>
  ));

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={MapPin}
        title="Vùng trồng"
        description="Quản lý các vùng trồng của tổ chức"
        actions={
          <>
            <HelpButton screenKey="farm-area-list" />
            {canCreate && (
              <Button onClick={() => navigate('/farm-areas/create')} variant="create" size="sm">
                <Plus className="h-4 w-4 mr-1" />
                Tạo vùng trồng
              </Button>
            )}
          </>
        }
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo tên vùng trồng..."
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
                options={STATUS_OPTIONS}
              />
            </>
          }
          right={<RefreshButton onClick={fetchAreas} loading={loading} />}
        />

        <DataTableShell
          header={header}
          body={body}
          loading={loading}
          empty={!loading && filtered.length === 0}
          colSpan={8}
          loadingMessage="Đang tải danh sách vùng trồng..."
          emptyMessage="Chưa có vùng trồng nào."
        />

        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filtered.length}
          pageSize={PAGE_SIZE}
          loading={loading}
          itemLabel="vùng trồng"
          onPageChange={setPage}
        />
      </ListCard>

      <FarmAreaDeleteDialog
        open={Boolean(deletingFarmArea)}
        onClose={() => setDeletingFarmArea(null)}
        onSuccess={fetchAreas}
        farmArea={deletingFarmArea}
      />
    </div>
  );
};

export default FarmAreaListPage;
