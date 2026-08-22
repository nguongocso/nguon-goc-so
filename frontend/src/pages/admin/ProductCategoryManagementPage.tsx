import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';
import { toast } from 'sonner';
import { getProductCategories, updateProductCategory } from '@/api/productCategoryApi';
import type { ProductCategory, ProductCategoryQueryParams } from '@/types/productCategory';
import { ProductCategoryFilter } from '@/components/admin/product-category/ProductCategoryFilter';
import { ProductCategoryList } from '@/components/admin/product-category/ProductCategoryList';
import { ProductCategoryForm } from '@/components/admin/product-category/ProductCategoryForm';
import { HelpButton } from '@/components/help/HelpButton';
import { usePermission } from '@/hooks/usePermission';

export default function ProductCategoryManagementPage() {
  const canManage = usePermission(['VT-01'] as const);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [openForm, setOpenForm] = useState(false);
  const [editingCategory, setEditingCategory] = useState<ProductCategory | null>(null);
  const [filterParams, setFilterParams] = useState<ProductCategoryQueryParams>({});

  const fetchCategories = async (params?: ProductCategoryQueryParams) => {
    try {
      setLoading(true);
      const data = await getProductCategories(params);
      setCategories(data);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchCategories(filterParams); }, []);

  const handleFilter = (params: ProductCategoryQueryParams) => {
    setFilterParams(params);
    fetchCategories(params);
  };

  const handleReset = () => {
    setFilterParams({});
    fetchCategories({});
  };

  const handleToggleActive = async (id: string, currentActive: boolean) => {
    const category = categories.find(c => c.id === id);
    if (!category) return;
    try {
      await updateProductCategory(id, {
        name: category.name,
        group: category.group,
        description: category.description || undefined,
        isActive: !currentActive,
      });
      toast.success(`Đã ${!currentActive ? 'hiện' : 'ẩn'} loại nông sản`);
      fetchCategories(filterParams);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật trạng thái');
    }
  };

  const handleEdit = (category: ProductCategory) => {
    setEditingCategory(category);
    setOpenForm(true);
  };

  const handleFormSuccess = () => {
    setEditingCategory(null);
    fetchCategories(filterParams);
  };

  const handleFormClose = () => {
    setOpenForm(false);
    setEditingCategory(null);
  };

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Quản lý danh mục loại nông sản</h1>
          <p className="text-sm text-muted-foreground">Thêm, sửa, ẩn/hiện các loại nông sản dùng chung</p>
        </div>
        <div className="flex items-center gap-3">
          {canManage && (
            <Button onClick={() => setOpenForm(true)} variant="create">
              <Plus className="h-4 w-4 mr-1" /> Thêm loại nông sản
            </Button>
          )}
          <HelpButton screenKey="admin-product-categories" />
        </div>
      </div>

      <ProductCategoryFilter onFilter={handleFilter} onReset={handleReset} loading={loading} />

      <ProductCategoryList
        categories={categories}
        loading={loading}
        onEdit={handleEdit}
        onToggleActive={handleToggleActive}
        canManage={canManage}
      />

      <ProductCategoryForm
        open={openForm}
        onClose={handleFormClose}
        onSuccess={handleFormSuccess}
        category={editingCategory}
      />
    </div>
  );
}