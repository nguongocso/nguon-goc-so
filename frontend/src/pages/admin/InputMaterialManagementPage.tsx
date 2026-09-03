import { useEffect, useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { PackageCheck, Clock, Eye, Edit2, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { usePermission } from '@/hooks/usePermission';
import { getInputMaterials, toggleInputMaterialStatus } from '@/api/inputMaterialApi';
import { InputMaterialDeleteDialog } from '@/components/admin/input-material/InputMaterialDeleteDialog';
import { HelpButton } from '@/components/help/HelpButton';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { RefreshButton } from '@/components/common/RefreshButton';
import { ListToolbar } from '@/components/common/ListToolbar';
import { ListCard } from '@/components/common/ListCard';
import { Pagination } from '@/components/common/Pagination';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { DataTableShell } from '@/components/common/DataTableShell';
import { StatusBadge } from '@/components/common/StatusBadge';
import { MaterialGroup, MATERIAL_GROUP_LABELS, MATERIAL_GROUP_VARIANTS } from '@/enums/materialGroup';
import type { InputMaterial } from '@/types/inputMaterial';

const PAGE_SIZE = 10;

export default function InputMaterialManagementPage() {
  const navigate = useNavigate();
  const canManage = usePermission(['VT-01'] as const);

  const [allMaterials, setAllMaterials] = useState<InputMaterial[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [group, setGroup] = useState('ALL');
  const [status, setStatus] = useState('ALL');

  const [deletingMaterial, setDeletingMaterial] = useState<InputMaterial | null>(null);
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);

  const fetchAll = useCallback(async () => {
    try {
      setLoading(true);
      const res = await getInputMaterials({ size: 1000 });
      setAllMaterials(res.content);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh mục vật tư đầu vào');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  const filtered = useMemo(() => {
    const q = keyword.toLowerCase().trim();
    return allMaterials.filter((m) => {
      const matchKeyword = !q || m.name.toLowerCase().includes(q) || (m.activeIngredient?.toLowerCase().includes(q) ?? false);
      const matchGroup = group === 'ALL' || m.materialGroup === group;
      const matchStatus = status === 'ALL' ||
        (status === 'true' && m.isActive) ||
        (status === 'false' && !m.isActive);
      return matchKeyword && matchGroup && matchStatus;
    });
  }, [allMaterials, keyword, group, status]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = filtered.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE);

  const handleKeywordChange = (value: string) => {
    setKeyword(value);
    setPage(0);
  };

  const handleGroupChange = (value: string | null) => {
    setGroup(value || 'ALL');
    setPage(0);
  };

  const handleStatusChange = (value: string | null) => {
    setStatus(value || 'ALL');
    setPage(0);
  };

  const handleToggleActive = async (id: string, currentActive: boolean) => {
    try {
      await toggleInputMaterialStatus(id, !currentActive);
      toast.success(`Đã ${!currentActive ? 'kích hoạt' : 'ngừng sử dụng'} vật tư thành công`);
      fetchAll();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật trạng thái vật tư');
    }
  };

  const handleDelete = (material: InputMaterial) => {
    setDeletingMaterial(material);
    setOpenDeleteDialog(true);
  };

  const header = (
    <>
      <TableHead className="w-[60px] text-center">STT</TableHead>
      <TableHead className="min-w-[200px]">Tên vật tư</TableHead>
      <TableHead className="min-w-[150px]">Nhóm vật tư</TableHead>
      <TableHead className="w-[100px] text-center">Đơn vị</TableHead>
      <TableHead className="min-w-[140px] text-center">Thời gian cách ly (PHI)</TableHead>
      <TableHead className="w-[110px] text-center">Trạng thái</TableHead>
      <TableHead className="w-[140px] text-center">Thao tác</TableHead>
    </>
  );

  const body = paginated.map((item, index) => {
    const variant = MATERIAL_GROUP_VARIANTS[item.materialGroup];
    return (
      <TableRow key={item.id} className="hover:bg-muted/40 transition-colors">
        <TableCell className="text-center font-medium text-muted-foreground">
          {safePage * PAGE_SIZE + index + 1}
        </TableCell>
        <TableCell>
          <div className="flex flex-col">
            <span
              className="cursor-pointer font-semibold text-foreground hover:text-emerald-600 dark:hover:text-emerald-400"
              onClick={() => navigate(`/admin/input-materials/${item.id}`)}
            >
              {item.name}
            </span>
            {item.activeIngredient && (
              <span className="max-w-[240px] truncate text-xs text-muted-foreground">
                Hoạt chất: <span className="italic">{item.activeIngredient}</span>
              </span>
            )}
          </div>
        </TableCell>
        <TableCell>
          <Badge
            variant="outline"
            className={`${variant.bgClass} ${variant.textClass} ${variant.borderClass} px-2.5 py-0.5 font-medium`}
          >
            {variant.label}
          </Badge>
        </TableCell>
        <TableCell className="text-center font-medium text-muted-foreground">
          {item.unit}
        </TableCell>
        <TableCell className="text-center">
          <div className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-500/10 px-2.5 py-1 text-xs font-semibold text-amber-700 dark:border-amber-800 dark:text-amber-400">
            <Clock className="h-3.5 w-3.5" />
            <span>{item.quarantineDays} ngày</span>
          </div>
        </TableCell>
        <TableCell className="text-center">
          {canManage ? (
            <div className="flex items-center justify-center">
              <Switch
                checked={item.isActive}
                onCheckedChange={() => handleToggleActive(item.id, item.isActive)}
                title={item.isActive ? 'Bấm để ngừng sử dụng' : 'Bấm để kích hoạt lại'}
              />
            </div>
          ) : (
            <div className="flex justify-center">
              <StatusBadge
                label={item.isActive ? 'Đang dùng' : 'Ngừng dùng'}
                tone={item.isActive ? 'success' : 'neutral'}
              />
            </div>
          )}
        </TableCell>
        <TableCell className="text-center">
          <div className="flex items-center justify-center gap-1">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate(`/admin/input-materials/${item.id}`)}
              className="h-8 w-8 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700 dark:hover:bg-emerald-950/50"
              title="Xem chi tiết vật tư"
            >
              <Eye className="h-4 w-4" />
            </Button>
            {canManage && (
              <>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => navigate(`/admin/input-materials/${item.id}/edit`)}
                  className="h-8 w-8 text-blue-600 hover:bg-blue-50 hover:text-blue-700 dark:hover:bg-blue-950/50"
                  title="Chỉnh sửa vật tư"
                >
                  <Edit2 className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => handleDelete(item)}
                  className="h-8 w-8 text-red-600 hover:bg-red-50 hover:text-red-700 dark:hover:bg-red-950/50"
                  title="Xóa vật tư"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </>
            )}
          </div>
        </TableCell>
      </TableRow>
    );
  });

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={PackageCheck}
        iconBoxClassName="bg-emerald-500/10 text-emerald-700 dark:text-emerald-400"
        title="Quản lý danh mục vật tư đầu vào"
        description="Khai báo danh mục vật tư đầu vào kèm thời gian cách ly (PHI) dùng chung toàn hệ thống"
        actions={
          <>
            <HelpButton screenKey="admin-input-materials" />
            {canManage && (
              <Button onClick={() => navigate('/admin/input-materials/create')} variant="create">
                + Thêm vật tư mới
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
                placeholder="Tìm theo tên / hoạt chất..."
                value={keyword}
                onChange={(e) => handleKeywordChange(e.target.value)}
              />
              <FilterSelect
                value={group}
                onValueChange={handleGroupChange}
                options={[
                  { value: 'ALL', label: 'Tất cả nhóm vật tư' },
                  { value: MaterialGroup.PESTICIDE, label: MATERIAL_GROUP_LABELS[MaterialGroup.PESTICIDE] },
                  { value: MaterialGroup.FERTILIZER, label: MATERIAL_GROUP_LABELS[MaterialGroup.FERTILIZER] },
                  { value: MaterialGroup.BIOLOGICAL, label: MATERIAL_GROUP_LABELS[MaterialGroup.BIOLOGICAL] },
                  { value: MaterialGroup.OTHER, label: MATERIAL_GROUP_LABELS[MaterialGroup.OTHER] },
                ]}
                className="sm:w-48"
              />
              <FilterSelect
                value={status}
                onValueChange={handleStatusChange}
                options={[
                  { value: 'ALL', label: 'Tất cả trạng thái' },
                  { value: 'true', label: 'Đang sử dụng' },
                  { value: 'false', label: 'Ngừng sử dụng' },
                ]}
              />
            </>
          }
          right={<RefreshButton onClick={fetchAll} loading={loading} />}
        />

        <DataTableShell
          header={header}
          body={body}
          loading={loading}
          empty={!loading && filtered.length === 0}
          colSpan={7}
          loadingMessage="Đang tải danh mục vật tư đầu vào..."
          emptyMessage="Không tìm thấy vật tư đầu vào phù hợp."
        />

        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filtered.length}
          pageSize={PAGE_SIZE}
          loading={loading}
          itemLabel="vật tư"
          onPageChange={setPage}
        />
      </ListCard>

      <InputMaterialDeleteDialog
        open={openDeleteDialog}
        onClose={() => {
          setOpenDeleteDialog(false);
          setDeletingMaterial(null);
        }}
        onSuccess={fetchAll}
        material={deletingMaterial}
      />
    </div>
  );
}
