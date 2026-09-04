import {useEffect, useState} from "react";
import {useParams} from "react-router-dom";
import {toast} from "sonner";
import {
    ListChecks,
    Loader2,
    RefreshCw,
    Save,
    Search,
} from "lucide-react";

import {Button} from "@/components/ui/button";
import {Badge} from "@/components/ui/badge";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {Switch} from "@/components/ui/switch";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import {Pagination} from "@/components/common/Pagination";
import {useSetBreadcrumb} from "@/components/common/AppBreadcrumb";
import {HelpButton} from "@/components/help/HelpButton";

import {getProductCategories} from "@/api/productCategoryApi";
import {
    assignProductCategoryCriteria,
    getInspectionCriteria,
    getProductCategoryCriteria,
} from "@/api/inspectionCriterionApi";
import type {InspectionCriterion} from "@/types/inspectionCriterion";
import type {ProductCategory} from "@/types/productCategory";

/** Số chỉ tiêu hiển thị trên mỗi trang (client-side pagination). */
const PAGE_SIZE = 10;

/**
 * Kích thước tải danh mục chỉ tiêu ACTIVE khớp keyword.
 * API GET /inspection-criteria hỗ trợ phân trang + search theo keyword.
 * Tải trọn tập khớp rồi lọc theo trạng thái gán (backend không có filter gán)
 * và phân trang tại client — đồng bộ convention ProductCategoryManagementPage.
 */
const CATALOG_SIZE = 1000;

type AssignmentFilter = "all" | "assigned" | "unassigned";

const filterOptions: { value: AssignmentFilter; label: string }[] = [
    {value: "all", label: "Tất cả"},
    {value: "assigned", label: "Đã gán"},
    {value: "unassigned", label: "Chưa gán"},
];

/**
 * Trang "Gán bộ chỉ tiêu kiểm nghiệm" cho một loại nông sản.
 * Route: /admin/product-categories/:id/criteria
 *
 * Semantics: PUT /api/v1/product-categories/{id}/criteria — REPLACE toàn bộ.
 * Trang luôn gửi TẬP hợp đầy đủ các chỉ tiêu đã chọn (không gửi phần delta/thêm mới),
 * nên không bao giờ vi phạm unique (category_id, criterion_id).
 */
export default function AssignInspectionCriteriaPage() {
    const {id} = useParams<{ id: string }>();
    // Loại nông sản đang được cấu hình dữ liệu.
    const [category, setCategory] = useState<ProductCategory | null>(null);
    const [categoryLoading, setCategoryLoading] = useState(true);

    // Danh mục chỉ số ACTIVE khớp keyword (tải từ API danh mục chỉ số).
    const [catalog, setCatalog] = useState<InspectionCriterion[]>([]);
    const [catalogLoading, setCatalogLoading] = useState(true);

    /** Trạng thái gán THỰC TẾ đã lưu ở DB (baseline) — dùng cho cột Trạng thái & filter. */
    const [assignedIds, setAssignedIds] = useState<Set<number>>(new Set());
    /** Lựa chọn HIỆN TẠI của người dùng (đang chuẩn bị lưu) — bền vững qua search/filter/pagination. */
    const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

    const [searchInput, setSearchInput] = useState("");
    const [keyword, setKeyword] = useState("");
    const [filter, setFilter] = useState<AssignmentFilter>("all");
    const [page, setPage] = useState(0);
    const [saving, setSaving] = useState(false);

    // Load loại nông sản + bộ chỉ tiêu đã gán (baseline).
    useEffect(() => {
        if (!id) return;
        let cancelled = false;

        const fetchCategory = async () => {
            try {
                // Tái sử dụng API list sẵn có (không phân trang ở backend), không tạo API mới.
                const categories = await getProductCategories({});
                if (cancelled) return;
                const found = categories.find((c) => c.id === id) ?? null;
                if (cancelled) return;
                setCategory(found ?? null);
                if (!found) {
                    toast.error("Không tìm thấy loại nông sản.");
                }
            } catch (error: any) {
                if (!cancelled) {
                    toast.error(
                        error.response?.data?.message ||
                        "Không thể tải thông tin loại nông sản"
                    );
                }
            } finally {
                if (!cancelled) setCategoryLoading(false);
            }
        };

        const fetchAssigned = async () => {
            try {
                const assigned = await getProductCategoryCriteria(id, true);
                if (cancelled) return;
                const ids = new Set(assigned.map((c) => c.id));
                setAssignedIds(ids);
                setSelectedIds(ids);
            } catch (error: any) {
                if (!cancelled) {
                    toast.error(
                        error.response?.data?.message ||
                        "Không thể tải bộ chỉ tiêu của loại nông sản"
                    );
                }
            }
        };

        void fetchCategory();
        void fetchAssigned();

        return () => {
            cancelled = true;
        };
    }, [id]);

    // Load danh mục chỉ tiêu ACTIVE khớp keyword (search server-side qua API sẵn có).
    useEffect(() => {
        if (!id) return;
        let cancelled = false;
        setCatalogLoading(true);
        getInspectionCriteria({
            keyword: keyword || undefined,
            status: "ACTIVE",
            page: 0,
            size: CATALOG_SIZE,
        })
            .then((data) => {
                if (!cancelled) setCatalog(data.items);
            })
            .catch((error: any) => {
                if (!cancelled) {
                    toast.error(
                        error.response?.data?.message ||
                        "Không thể tải danh sách chỉ tiêu kiểm nghiệm"
                    );
                }
            })
            .finally(() => {
                if (!cancelled) setCatalogLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, [id, keyword]);

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        const next = searchInput.trim();
        if (next === keyword) return;
        setKeyword(next);
        setPage(0);
    };

    const handleRefresh = () => {
        // Làm mới: tải lại danh mục chỉ tiêu theo keyword hiện tại.
        setPage(0);
        void getInspectionCriteria({
            keyword: keyword || undefined,
            status: "ACTIVE",
            page: 0,
            size: CATALOG_SIZE,
        })
            .then((data) => setCatalog(data.items))
            .catch((error: any) =>
                toast.error(
                    error.response?.data?.message ||
                    "Không thể tải danh sách chỉ tiêu kiểm nghiệm"
                )
            );
    };

    const handleFilterChange = (value: AssignmentFilter | null) => {
        if (!value) return;
        setFilter(value);
        setPage(0);
    };

    const handleToggle = (criterionId: number, checked: boolean) => {
        // Chỉ cập nhật lựa chọn đang chuẩn bị lưu; không gọi API từng row.
        setSelectedIds((prev) => {
            const next = new Set(prev);
            if (checked) {
                next.add(criterionId);
            } else {
                next.delete(criterionId);
            }
            return next;
        });
    };

    const handleSave = async () => {
        if (!category || saving) return;
        setSaving(true);
        try {
            // REPLACE: gửi TẬP đầy đủ selectedIds hiện tại.
            const saved = await assignProductCategoryCriteria(
                category.id,
                Array.from(selectedIds)
            );
            toast.success(`Đã cập nhật bộ chỉ tiêu cho "${category.name}"`);
            const ids = new Set(saved.map((c) => c.id));
            setAssignedIds(ids);
            setSelectedIds(ids);
            setPage(0);
        } catch (error: any) {
            toast.error(
                error.response?.data?.message ||
                "Không thể cập nhật bộ chỉ tiêu"
            );
        } finally {
            setSaving(false);
        }
    };
// Bộ lọc phản ánh trạng thái gán THỰC TẾ (baseline) của loại nông sản.
    const visibleList = catalog.filter((criterion) => {
        if (filter === "assigned") return assignedIds.has(criterion.id);
        if (filter === "unassigned") return !assignedIds.has(criterion.id);
        return true;
    });

    const totalElements = visibleList.length;
    const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));
    const currentSafePage = Math.min(page, totalPages - 1);
    const startIndex = currentSafePage * PAGE_SIZE;
    const paginated = visibleList.slice(startIndex, startIndex + PAGE_SIZE);

    useSetBreadcrumb(
        category
            ? [
                {label: "Tổng quan", href: "/dashboard"},
                {label: "Danh mục nông sản", href: "/admin/product-categories"},
                {label: `Gán bộ chỉ tiêu — ${category.name}`},
            ]
            : null
    );

    if (categoryLoading && !category) {
        return (
            <div className="flex items-center justify-center py-20 text-muted-foreground">
                <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600"/>
                Đang tải thông tin loại nông sản...
            </div>
        );
    }

    if (!category) {
        return (
            <div className="space-y-6">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                        Gán bộ chỉ tiêu kiểm nghiệm
                    </h1>
                </div>
                <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
                    <CardContent className="p-8 text-center text-muted-foreground">
                        Loại nông sản không tồn tại hoặc đã bị xóa.
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
                        <ListChecks className="size-6 text-emerald-600"/>
                        Gán bộ chỉ tiêu kiểm nghiệm
                    </h1>
                    <p className="text-sm text-muted-foreground mt-1">
                        Loại nông sản:{" "}
                        <span className="font-semibold text-slate-900">
                            {category.name}
                        </span>{" "}
                        — Chọn các chỉ tiêu kiểm nghiệm áp dụng cho loại nông sản
                        này.
                    </p>
                </div>
                <HelpButton screenKey="admin-product-categories"/>
            </div>

            <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
                <CardHeader className="border-b border-slate-100 pb-4">
                    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                        <CardTitle className="text-xl font-bold text-slate-900">
                            Danh sách chỉ tiêu
                        </CardTitle>
                        <div className="flex flex-wrap items-center gap-2">
                            <form onSubmit={handleSearch} className="relative w-full sm:w-64">
                                <Search
                                    className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"/>
                                <Input
                                    value={searchInput}
                                    onChange={(e) => setSearchInput(e.target.value)}
                                    placeholder="Tìm theo tên chỉ tiêu..."
                                    className="h-9 pl-9"
                                />
                            </form>
                            <Select value={filter} onValueChange={handleFilterChange}
                                items={filterOptions}
                            >
                                <SelectTrigger size="sm" className="w-[180px]">
                                    <SelectValue placeholder="Trạng thái" />
                                </SelectTrigger>
                                <SelectContent>
                                    {filterOptions.map((opt) => (
                                        <SelectItem key={opt.value} value={opt.value}>
                                            {opt.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <Button variant="outline" size="sm" onClick={handleRefresh} disabled={catalogLoading}>
                                <RefreshCw className={`h-4 w-4 mr-1 ${catalogLoading ? "animate-spin" : ""}`}/>
                                Làm mới
                            </Button>
                        </div>
                    </div>
                </CardHeader>

                <CardContent className="p-4 space-y-4">
                    <div className="rounded-md border overflow-x-auto">
                        <Table>
                            <TableHeader>
                                <TableRow className="bg-muted/50">
                                    <TableHead className="w-12 text-center">STT</TableHead>
                                    <TableHead>Tên chỉ tiêu</TableHead>
                                    <TableHead>Đơn vị</TableHead>
                                    <TableHead>Ngưỡng tối đa</TableHead>
                                    <TableHead>Tiêu chuẩn tham chiếu</TableHead>
                                    <TableHead>Trạng thái</TableHead>
                                    <TableHead className="text-center">Thao tác</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {catalogLoading ? (
                                    <TableRow>
                                        <TableCell colSpan={7} className="h-32 text-center text-muted-foreground">
                                            <div className="flex flex-col items-center justify-center gap-2">
                                                <RefreshCw className="w-6 h-6 animate-spin text-emerald-600"/>
                                                <span>Đang tải danh sách chỉ tiêu kiểm nghiệm...</span>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ) : paginated.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={7} className="h-32 text-center text-muted-foreground">
                                            Không có chỉ tiêu kiểm nghiệm phù hợp.
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    paginated.map((criterion, index) => {
                                        const isAssigned = assignedIds.has(criterion.id);
                                        const isSelected = selectedIds.has(criterion.id);
                                        return (
                                            <TableRow key={criterion.id}
                                                      className="hover:bg-muted/40 transition-colors">
                                                <TableCell className="text-center font-medium text-muted-foreground">
                                                    {index + 1 + currentSafePage * PAGE_SIZE}
                                                </TableCell>
                                                <TableCell className="font-medium">{criterion.name}</TableCell>
                                                <TableCell>{criterion.unit}</TableCell>
                                                <TableCell>{Number(criterion.maxThreshold)}</TableCell>
                                                <TableCell className="max-w-xs truncate">
                                                    {criterion.referenceStandard || "—"}
                                                </TableCell>
                                                <TableCell>
                                                    <Badge variant={isAssigned ? "default" : "secondary"}>
                                                        {isAssigned ? "Đã gán" : "Chưa gán"}
                                                    </Badge>
                                                </TableCell>
                                                <TableCell className="text-center">
                                                    <div className="flex items-center justify-center gap-2">
                                                        <span className="text-xs text-muted-foreground">
                                                            {isSelected ? "Đã gán" : "Chưa gán"}
                                                        </span>
                                                        <Switch
                                                            checked={isSelected}
                                                            size="sm"
                                                            onCheckedChange={(checked) => handleToggle(criterion.id, checked)}
                                                            aria-label={`Chọn/bỏ ${criterion.name}`}
                                                        />
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        );
                                    })
                                )}
                            </TableBody>
                        </Table>
                    </div>
                    <Pagination
                        currentPage={currentSafePage}
                        totalPages={totalPages}
                        totalElements={totalElements}
                        pageSize={PAGE_SIZE}
                        loading={catalogLoading}
                        itemLabel="chỉ tiêu"
                        onPageChange={setPage}
                    />

                    {selectedIds.size < assignedIds.size && (
                        <p className="text-xs text-amber-600">
                            Bạn đang bỏ gán một số chỉ tiêu đã lưu. Sau khi lưu,
                            bộ chỉ tiêu sẽ được cập nhật theo lựa chọn mới nhất.
                        </p>
                    )}
                </CardContent>
            </Card>

            {/* Footer actions */}
            <div className="flex items-center justify-end border-t pt-4">
                <Button onClick={handleSave} disabled={catalogLoading || saving}>
                    {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin"/> : <Save className="h-4 w-4 mr-1"/>}
                    {saving ? "Đang lưu..." : "Lưu bộ chỉ tiêu"}
                </Button>
            </div>
        </div>
    );
}
