import { useEffect, useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { PackageCheck, Clock, Eye, Edit2, Trash2, ShieldAlert } from 'lucide-react';
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

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-emerald-500/10 text-emerald-700 dark:text-emerald-400">
            <PackageCheck className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-gray-100">
              Quản lý danh mục vật tư đầu vào
            </h1>
            <p className="text-xs sm:text-sm text-muted-foreground">
              Khai báo danh mục vật tư đầu vào kèm thời gian cách ly (PHI) dùng chung toàn hệ thống
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <HelpButton screenKey="admin-input-materials" />
          {canManage && (
            <Button onClick={() => navigate('/admin/input-materials/create')} variant="create">
              + Thêm vật tư mới
            </Button>
          )}
        </div>
      </div>

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

        {loading ? (
          <div className="flex flex-col items-center justify-center gap-2 py-8 text-muted-foreground">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-emerald-600 border-t-transparent" />
            <span>Đang tải danh mục vật tư đầu vào...</span>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-3 py-12 text-muted-foreground">
            <ShieldAlert className="h-10 w-10 text-amber-500/80" />
            <p className="text-base font-medium">Không tìm thấy vật tư đầu vào phù hợp</p>
            <p className="text-sm">Vui lòng thử thay đổi bộ lọc hoặc thêm mới vật tư đầu vào.</p>
          </div>
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[60px] text-center">STT</TableHead>
                  <TableHead className="min-w-[200px]">Tên vật tư</TableHead>
                  <TableHead className="min-w-[150px]">Nhóm vật tư</TableHead>
                  <TableHead className="w-[100px] text-center">Đơn vị</TableHead>
                  <TableHead className="min-w-[140px] text-center">Thời gian cách ly (PHI)</TableHead>
                  <TableHead className="w-[110px] text-center">Trạng thái</TableHead>
                  <TableHead className="w-[140px] text-center">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {paginated.map((item, index) => {
                  const variant = MATERIAL_GROUP_VARIANTS[item.materialGroup];
                  return (
                    <TableRow key={item.id} className="hover:bg-gray-50/50 dark:hover:bg-gray-900/30">
                      <TableCell className="text-center font-medium text-gray-500">
                        {safePage * PAGE_SIZE + index + 1}
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-col">
                          <span
                            className="font-semibold text-gray-900 dark:text-gray-100 hover:text-emerald-600 dark:hover:text-emerald-400 cursor-pointer"
                            onClick={() => navigate(`/admin/input-materials/${item.id}`)}
                          >
                            {item.name}
                          </span>
                          {item.activeIngredient && (
                            <span className="text-xs text-muted-foreground truncate max-w-[240px]">
                              Hoạt chất: <span className="italic">{item.activeIngredient}</span>
                            </span>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={`${variant.bgClass} ${variant.textClass} ${variant.borderClass} font-medium px-2.5 py-0.5`}
                        >
                          {variant.label}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-center font-medium text-gray-700 dark:text-gray-300">
                        {item.unit}
                      </TableCell>
                      <TableCell className="text-center">
                        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-700 dark:text-amber-400 font-semibold text-xs border border-amber-200 dark:border-amber-800">
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
                          <Badge variant={item.isActive ? 'default' : 'secondary'}>
                            {item.isActive ? 'Đang dùng' : 'Ngừng dùng'}
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-center">
                        <div className="flex items-center justify-center gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => navigate(`/admin/input-materials/${item.id}`)}
                            className="h-8 w-8 text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 dark:hover:bg-emerald-950/50"
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
                                className="h-8 w-8 text-blue-600 hover:text-blue-700 hover:bg-blue-50 dark:hover:bg-blue-950/50"
                                title="Chỉnh sửa vật tư"
                              >
                                <Edit2 className="h-4 w-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => handleDelete(item)}
                                className="h-8 w-8 text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950/50"
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
                })}
              </TableBody>
            </Table>
            <Pagination
              currentPage={safePage}
              totalPages={totalPages}
              totalElements={filtered.length}
              pageSize={PAGE_SIZE}
              loading={loading}
              itemLabel="vật tư"
              onPageChange={setPage}
            />
          </>
        )}
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
