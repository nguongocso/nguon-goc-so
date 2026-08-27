import {useEffect, useState, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {Button} from '@/components/ui/button';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {Plus} from 'lucide-react';
import {toast} from 'sonner';
import {getProductCategories, updateProductCategory} from '@/api/productCategoryApi';
import {setMandatoryInspection} from '@/api/inspectionCriterionApi';
import type {ProductCategory, ProductCategoryQueryParams} from '@/types/productCategory';
import {ProductCategoryFilter} from '@/components/admin/product-category/ProductCategoryFilter';
import {ProductCategoryList} from '@/components/admin/product-category/ProductCategoryList';
import {HelpButton} from '@/components/help/HelpButton';
import {Pagination} from '@/components/common/Pagination';
import {usePermission} from '@/hooks/usePermission';

const PAGE_SIZE = 10;

export default function ProductCategoryManagementPage() {
    const navigate = useNavigate();
    const canManage = usePermission(['VT-01'] as const);
    const [categories, setCategories] = useState<ProductCategory[]>([]);
    const [loading, setLoading] = useState(true);
    const [filterParams, setFilterParams] = useState<ProductCategoryQueryParams>({});
    const [togglingMandatoryId, setTogglingMandatoryId] = useState<string | null>(null);

    // Phân trang (client-side)
    const [page, setPage] = useState(0);

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

    useEffect(() => {
        fetchCategories(filterParams);
    }, []);

    const handleFilter = (params: ProductCategoryQueryParams) => {
        setFilterParams(params);
        setPage(0);
        fetchCategories(params);
    };

    const handleReset = () => {
        setFilterParams({});
        setPage(0);
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
                tempMin: category.tempMin ?? undefined,
                tempMax: category.tempMax ?? undefined,
                humidityMin: category.humidityMin ?? undefined,
                humidityMax: category.humidityMax ?? undefined,
            });
            toast.success(`Đã ${!currentActive ? 'hiện' : 'ẩn'} loại nông sản`);
            fetchCategories(filterParams);
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Không thể cập nhật trạng thái');
        }
    };

    const handleToggleMandatory = async (category: ProductCategory, required: boolean) => {
        // Chặn gọi trùng khi Switch đang pending (TC-02 — không giữ success giả)
        if (togglingMandatoryId) return;
        setTogglingMandatoryId(category.id);
        try {
            await setMandatoryInspection(category.id, required);
            toast.success(required ? 'Đã bật bắt buộc kiểm nghiệm' : 'Đã tắt bắt buộc kiểm nghiệm');
            fetchCategories(filterParams);
        } catch (error: any) {
            // BR-3: backend từ chối nếu category chưa có chỉ tiêu kiểm nghiệm nào
            toast.error(error.response?.data?.message || 'Không thể cập nhật bắt buộc kiểm nghiệm');
        } finally {
            setTogglingMandatoryId(null);
        }
    };

    const handleEdit = (category: ProductCategory) => {
        navigate(`/admin/product-categories/${category.id}/edit`);
    };

    const handleAssignCriteria = (category: ProductCategory) => {
        navigate(`/admin/product-categories/${category.id}/criteria`);
    };

    // Client-side pagination
    const totalPages = Math.max(1, Math.ceil(categories.length / PAGE_SIZE));
    const paginatedCategories = useMemo(() => {
        const start = page * PAGE_SIZE;
        return categories.slice(start, start + PAGE_SIZE);
    }, [categories, page]);

    const startIndex = page * PAGE_SIZE;

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">Quản lý danh mục loại nông sản</h1>
                    <p className="text-sm text-muted-foreground">Thêm, sửa, ẩn/hiện các loại nông sản dùng chung</p>
                </div>
                <div className="flex items-center gap-3">
                    <HelpButton screenKey="admin-product-categories"/>
                    {canManage && (
                        <Button onClick={() => navigate('/admin/product-categories/create')} variant="create">
                            <Plus className="h-4 w-4 mr-1"/> Thêm loại nông sản
                        </Button>
                    )}
                </div>
            </div>

            <ProductCategoryFilter onFilter={handleFilter} onReset={handleReset} loading={loading}/>

            {/* Khung chứa (Card) đồng bộ với các trang danh sách quản lý khác */}
            <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
                <CardHeader className="border-b border-slate-100 pb-4">
                    <CardTitle className="text-xl font-bold text-slate-900">
                        Danh mục loại nông sản
                    </CardTitle>
                </CardHeader>
                <CardContent className="p-4 space-y-4">
                    <ProductCategoryList
                        categories={paginatedCategories}
                        loading={loading}
                        startIndex={startIndex}
                        onEdit={handleEdit}
                        onToggleActive={handleToggleActive}
                        canManage={canManage}
                        onToggleMandatory={handleToggleMandatory}
                        togglingMandatoryId={togglingMandatoryId}
                        onAssignCriteria={handleAssignCriteria}
                    />

                    <Pagination
                        currentPage={page}
                        totalPages={totalPages}
                        totalElements={categories.length}
                        pageSize={PAGE_SIZE}
                        loading={loading}
                        itemLabel="loại nông sản"
                        onPageChange={setPage}
                    />
                </CardContent>
            </Card>

        </div>
    );
}