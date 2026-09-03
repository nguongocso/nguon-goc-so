import {useEffect, useState, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';
import {Sprout, Plus} from 'lucide-react';
import {Button} from '@/components/ui/button';
import {HelpButton} from '@/components/help/HelpButton';
import {Pagination} from '@/components/common/Pagination';
import {ListPageHeader} from '@/components/common/ListPageHeader';
import {ListCard} from '@/components/common/ListCard';
import {ListToolbar} from '@/components/common/ListToolbar';
import {SearchInput} from '@/components/common/SearchInput';
import {FilterSelect} from '@/components/common/FilterSelect';
import {RefreshButton} from '@/components/common/RefreshButton';
import {getProductCategories, updateProductCategory} from '@/api/productCategoryApi';
import {setMandatoryInspection} from '@/api/inspectionCriterionApi';
import type {ProductCategory} from '@/types/productCategory';
import {ProductCategoryList} from '@/components/admin/product-category/ProductCategoryList';
import {usePermission} from '@/hooks/usePermission';

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
    {value: 'ALL', label: 'Tất cả trạng thái'},
    {value: 'true', label: 'Đang hoạt động'},
    {value: 'false', label: 'Đã ẩn'},
];

export default function ProductCategoryManagementPage() {
    const navigate = useNavigate();
    const canManage = usePermission(['VT-01'] as const);
    const [categories, setCategories] = useState<ProductCategory[]>([]);
    const [loading, setLoading] = useState(true);
    const [togglingMandatoryId, setTogglingMandatoryId] = useState<string | null>(null);

    // Tìm kiếm, lọc (client-side) & phân trang
    const [search, setSearch] = useState('');
    const [group, setGroup] = useState('ALL');
    const [status, setStatus] = useState('ALL');
    const [page, setPage] = useState(0);

    const fetchCategories = async () => {
        try {
            setLoading(true);
            const data = await getProductCategories();
            setCategories(data);
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Không thể tải danh sách');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCategories();
    }, []);

    const groupOptions = useMemo(() => {
        const groups = Array.from(new Set(categories.map((c) => c.group).filter(Boolean)));
        return [
            {value: 'ALL', label: 'Tất cả nhóm hàng'},
            ...groups.map((g) => ({value: g, label: g})),
        ];
    }, [categories]);

    // Lọc theo từ khóa + nhóm + trạng thái (client-side, auto khi gõ)
    const filtered = useMemo(() => {
        const q = search.toLowerCase().trim();
        return categories.filter((c) => {
            const matchKeyword =
                !q || c.name.toLowerCase().includes(q) || (c.description?.toLowerCase().includes(q) ?? false);
            const matchGroup = group === 'ALL' || c.group === group;
            const matchStatus =
                status === 'ALL' || (status === 'true' ? c.isActive : !c.isActive);
            return matchKeyword && matchGroup && matchStatus;
        });
    }, [categories, search, group, status]);

    const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    const safePage = Math.min(page, totalPages - 1);
    const paginated = useMemo(() => {
        const start = safePage * PAGE_SIZE;
        return filtered.slice(start, start + PAGE_SIZE);
    }, [filtered, safePage]);

    const handleToggleActive = async (id: string, currentActive: boolean) => {
        const category = categories.find((c) => c.id === id);
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
            fetchCategories();
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Không thể cập nhật trạng thái');
        }
    };

    const handleToggleMandatory = async (category: ProductCategory, required: boolean) => {
        if (togglingMandatoryId) return;
        setTogglingMandatoryId(category.id);
        try {
            await setMandatoryInspection(category.id, required);
            toast.success(required ? 'Đã bật bắt buộc kiểm nghiệm' : 'Đã tắt bắt buộc kiểm nghiệm');
            fetchCategories();
        } catch (error: any) {
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

    const handleAssignMilestones = (category: ProductCategory) => {
        navigate(`/admin/product-categories/${category.id}/milestones`);
    };


    return (
        <div className="space-y-6">
            {/* Header trang */}
            <ListPageHeader
                icon={Sprout}
                title="Quản lý danh mục loại nông sản"
                description="Thêm, sửa, ẩn/hiện các loại nông sản dùng chung"
                actions={
                    <>
                        <HelpButton screenKey="admin-product-categories" />
                        {canManage && (
                            <Button onClick={() => navigate('/admin/product-categories/create')} variant="create">
                                <Plus className="h-4 w-4 mr-1" /> Thêm loại nông sản
                            </Button>
                        )}
                    </>
                }
            />

            {/* Thẻ chung: bộ lọc (auto-search) + bảng + phân trang */}
            <ListCard>
                <ListToolbar
                    left={
                        <>
                            <SearchInput
                                placeholder="Tìm theo tên loại nông sản..."
                                value={search}
                                onChange={(e) => {
                                    setSearch(e.target.value);
                                    setPage(0);
                                }}
                            />
                            <FilterSelect
                                value={group}
                                onValueChange={(val) => {
                                    setGroup(val || 'ALL');
                                    setPage(0);
                                }}
                                options={groupOptions}
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
                    right={<RefreshButton onClick={fetchCategories} loading={loading} />}
                />

                <ProductCategoryList
                    categories={paginated}
                    loading={loading}
                    startIndex={safePage * PAGE_SIZE}
                    onEdit={handleEdit}
                    onToggleActive={handleToggleActive}
                    canManage={canManage}
                    onToggleMandatory={handleToggleMandatory}
                    togglingMandatoryId={togglingMandatoryId}
                    onAssignCriteria={handleAssignCriteria}
                    onAssignMilestones={handleAssignMilestones}
                />

                <Pagination
                    currentPage={safePage}
                    totalPages={totalPages}
                    totalElements={filtered.length}
                    pageSize={PAGE_SIZE}
                    loading={loading}
                    itemLabel="loại nông sản"
                    onPageChange={setPage}
                />
            </ListCard>
        </div>
    );
}
