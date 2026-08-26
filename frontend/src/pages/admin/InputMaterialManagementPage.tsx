import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Plus, PackageCheck } from 'lucide-react';
import { toast } from 'sonner';
import { usePermission } from '@/hooks/usePermission';
import { getInputMaterials, toggleInputMaterialStatus } from '@/api/inputMaterialApi';
import { InputMaterialFilter } from '@/components/admin/input-material/InputMaterialFilter';
import { InputMaterialList } from '@/components/admin/input-material/InputMaterialList';
import { InputMaterialDeleteDialog } from '@/components/admin/input-material/InputMaterialDeleteDialog';
import { HelpButton } from '@/components/help/HelpButton';
import type { InputMaterial, InputMaterialQueryParams } from '@/types/inputMaterial';

export default function InputMaterialManagementPage() {
  const navigate = useNavigate();
  const canManage = usePermission(['VT-01'] as const);

  const [materials, setMaterials] = useState<InputMaterial[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filterParams, setFilterParams] = useState<InputMaterialQueryParams>({});

  // Delete dialog state
  const [deletingMaterial, setDeletingMaterial] = useState<InputMaterial | null>(null);
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);

  const fetchMaterials = useCallback(async (params?: InputMaterialQueryParams) => {
    try {
      setLoading(true);
      const res = await getInputMaterials(params);
      setMaterials(res.content);
      setPage(res.pageable.pageNumber);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh mục vật tư đầu vào');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMaterials(filterParams);
  }, [fetchMaterials, filterParams]);

  const handleFilter = (params: InputMaterialQueryParams) => {
    const updated = { ...filterParams, ...params, page: 0 };
    setFilterParams(updated);
  };

  const handleReset = () => {
    setFilterParams({});
  };

  const handlePageChange = (newPage: number) => {
    const updated = { ...filterParams, page: newPage };
    setFilterParams(updated);
  };

  const handleToggleActive = async (id: string, currentActive: boolean) => {
    try {
      await toggleInputMaterialStatus(id, !currentActive);
      toast.success(`Đã ${!currentActive ? 'kích hoạt' : 'ngừng sử dụng'} vật tư thành công`);
      fetchMaterials(filterParams);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật trạng thái vật tư');
    }
  };

  const handleCreate = () => {
    navigate('/admin/input-materials/create');
  };

  const handleEdit = (material: InputMaterial) => {
    navigate(`/admin/input-materials/${material.id}/edit`);
  };

  const handleDelete = (material: InputMaterial) => {
    setDeletingMaterial(material);
    setOpenDeleteDialog(true);
  };

  return (
    <div className="container mx-auto py-6 space-y-6">
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
            <Button onClick={handleCreate} variant="create">
              <Plus className="h-4 w-4 mr-1.5" /> Thêm vật tư mới
            </Button>
          )}
        </div>
      </div>

      {/* Filter */}
      <InputMaterialFilter onFilter={handleFilter} onReset={handleReset} loading={loading} />

      {/* List Table */}
      <InputMaterialList
        materials={materials}
        loading={loading}
        page={page}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={handlePageChange}
        onEdit={handleEdit}
        onToggleActive={handleToggleActive}
        onDelete={handleDelete}
        canManage={canManage}
      />

      {/* Delete Dialog */}
      <InputMaterialDeleteDialog
        open={openDeleteDialog}
        onClose={() => {
          setOpenDeleteDialog(false);
          setDeletingMaterial(null);
        }}
        onSuccess={() => fetchMaterials(filterParams)}
        material={deletingMaterial}
      />
    </div>
  );
}
