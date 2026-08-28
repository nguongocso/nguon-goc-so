import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import {toast} from "sonner";
import {
    Eye,
    EyeOff,
    Pencil,
    Plus,
    RefreshCw,
    Search,
    Trash2,
} from "lucide-react";
import {Button} from "@/components/ui/button";
import {Badge} from "@/components/ui/badge";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
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
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogPopup,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {useSetBreadcrumb} from "@/components/common/AppBreadcrumb";
import {HelpButton} from "@/components/help/HelpButton";
import {Pagination} from "@/components/common/Pagination";
import {usePermission} from "@/hooks/usePermission";
import {ROLE_ACCESS} from "@/config/roleAccess";
import {
    deleteInspectionCriterion,
    disableInspectionCriterion,
    enableInspectionCriterion,
    getInspectionCriteria,
} from "@/api/inspectionCriterionApi";
import type {
    InspectionCriterion,
    InspectionCriterionStatus,
} from "@/types/inspectionCriterion";
import {InspectionCriterionForm} from "@/components/admin/inspection-criterion/InspectionCriterionForm";

const PAGE_SIZE = 10;

const statusFilterOptions = [
    {value: "all", label: "Tất cả"},
    {value: "ACTIVE", label: "Đang hoạt động"},
    {value: "INACTIVE", label: "Ngừng sử dụng"},
];

export default function InspectionCriteriaManagementPage() {
    const navigate = useNavigate();
    const canManage = usePermission(ROLE_ACCESS.inspectionCriteriaManagement);
    const [criteria, setCriteria] = useState<InspectionCriterion[]>([]);
    const [totalElements, setTotalElements] = useState(0);
    const [currentPage, setCurrentPage] = useState(0);
    const [statusFilter, setStatusFilter] = useState<
        InspectionCriterionStatus | undefined
    >(undefined);
    const [searchInput, setSearchInput] = useState("");
    const [keyword, setKeyword] = useState("");
    const [loading, setLoading] = useState(true);

    const [formOpen, setFormOpen] = useState(false);
    const [editingCriterion, setEditingCriterion] =
        useState<InspectionCriterion | null>(null);
    const [togglingId, setTogglingId] = useState<number | null>(null);
    const [deleteTarget, setDeleteTarget] = useState<InspectionCriterion | null>(
        null
    );
    const [deleteSubmitting, setDeleteSubmitting] = useState(false);

    const fetchCriteria = async () => {
        setLoading(true);
        try {
            const data = await getInspectionCriteria({
                keyword: keyword || undefined,
                status: statusFilter,
                page: currentPage,
                size: PAGE_SIZE,
            });
            setCriteria(data.items);
            setTotalElements(data.totalElements);
        } catch (error: any) {
            toast.error(
                error.response?.data?.message ||
                "Không thể tải danh sách chỉ tiêu kiểm nghiệm"
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCriteria();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentPage, keyword, statusFilter]);

    const totalPages = Math.ceil(totalElements / PAGE_SIZE);

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        const next = searchInput.trim();
        if (next === keyword) return;
        setKeyword(next);
        setCurrentPage(0);
    };

    const openEditDialog = (criterion: InspectionCriterion) => {
        setEditingCriterion(criterion);
        setFormOpen(true);
    };

    const closeForm = () => {
        setFormOpen(false);
        setEditingCriterion(null);
    };

    const handleToggleStatus = async (criterion: InspectionCriterion) => {
        if (togglingId !== null) return;
        setTogglingId(criterion.id);
        try {
            if (criterion.status === "ACTIVE") {
                await disableInspectionCriterion(criterion.id);
                toast.success(`Đã ngừng sử dụng chỉ tiêu "${criterion.name}"`);
            } else {
                await enableInspectionCriterion(criterion.id);
                toast.success(`Đã kích hoạt lại chỉ tiêu "${criterion.name}"`);
            }
            fetchCriteria();
        } catch (error: any) {
            toast.error(
                error.response?.data?.message ||
                "Không thể cập nhật trạng thái chỉ tiêu"
            );
        } finally {
            setTogglingId(null);
        }
    };

    const handleDelete = async () => {
        if (!deleteTarget || deleteSubmitting) return;
        setDeleteSubmitting(true);
        try {
            await deleteInspectionCriterion(deleteTarget.id);
            toast.success("Xóa chỉ tiêu kiểm nghiệm thành công");
            setDeleteTarget(null);
            if (criteria.length === 1 && currentPage > 0) {
                setCurrentPage(currentPage - 1);
            } else {
                fetchCriteria();
            }
        } catch (error: any) {
            toast.error(error.response?.data?.message || "Không thể xóa chỉ tiêu");
        } finally {
            setDeleteSubmitting(false);
        }
    };

    const getStatusFilterLabel = (value: string) => {
        const option = statusFilterOptions.find((opt) => opt.value === value);
        return option ? option.label : "Trạng thái";
    };

    const currentStatusValue = statusFilter ?? "all";

    useSetBreadcrumb([
        {label: "Dashboard", href: "/dashboard"},
        {label: "Chỉ tiêu kiểm nghiệm"},
    ]);

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                        Quản lý danh mục chỉ tiêu kiểm nghiệm
                    </h1>
                    <p className="text-sm text-muted-foreground">
                        Thêm, sửa, ngừng sử dụng các chỉ tiêu kiểm nghiệm dùng chung
                    </p>
                </div>
                <div className="flex items-center gap-3">
                    <HelpButton screenKey="admin-inspection-criteria"/>
                    {canManage && (
                        <Button
                            onClick={() => navigate("/admin/inspection-criteria/create")}
                            variant="create"
                        >
                            <Plus className="h-4 w-4 mr-1"/> Thêm chỉ tiêu
                        </Button>
                    )}
                </div>
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
                                    placeholder="Tìm theo tên hoặc tiêu chuẩn..."
                                    className="h-9 pl-9"
                                />
                            </form>
                            <Select
                                value={currentStatusValue}
                                onValueChange={(val) => {
                                    setStatusFilter(
                                        val === "all"
                                            ? undefined
                                            : (val as InspectionCriterionStatus)
                                    );
                                    setCurrentPage(0);
                                }}
                            >
                                <SelectTrigger size="sm" className="w-[180px]">
                                    <SelectValue placeholder="Trạng thái">
                                        {getStatusFilterLabel(currentStatusValue)}
                                    </SelectValue>
                                </SelectTrigger>
                                <SelectContent>
                                    {statusFilterOptions.map((opt) => (
                                        <SelectItem key={opt.value} value={opt.value}>
                                            {opt.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={fetchCriteria}
                                disabled={loading}
                            >
                                <RefreshCw
                                    className={`h-4 w-4 mr-1 ${loading ? "animate-spin" : ""}`}/>
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
                                    <TableHead>Ngưỡng tối da</TableHead>
                                    <TableHead>Tiêu chuẩn tham chiếu</TableHead>
                                    <TableHead>Trạng thái</TableHead>
                                    {canManage && (
                                        <TableHead className="text-right">Thao tác</TableHead>
                                    )}
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {loading ? (
                                    <TableRow>
                                        <TableCell colSpan={canManage ? 7 : 6}
                                                   className="h-32 text-center text-muted-foreground">
                                            <div className="flex flex-col items-center justify-center gap-2">
                                                <RefreshCw className="w-6 h-6 animate-spin text-emerald-600"/>
                                                <span>Đang tải danh sách chỉ tiêu kiểm nghiệm...</span>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ) : criteria.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={canManage ? 7 : 6}
                                                   className="h-32 text-center text-muted-foreground">
                                            Chưa có chỉ tiêu kiểm nghiệm nào trong danh mục.
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    criteria.map((criterion, index) => {
                                        const isActive = criterion.status === "ACTIVE";
                                        return (
                                            <TableRow
                                                key={criterion.id}
                                                className={`hover:bg-muted/40 transition-colors${
                                                    isActive ? "" : " opacity-60"
                                                }`}
                                            >
                                                <TableCell className="text-center font-medium text-muted-foreground">
                                                    {index + 1 + currentPage * PAGE_SIZE}
                                                </TableCell>
                                                <TableCell className="font-medium">{criterion.name}</TableCell>
                                                <TableCell>{criterion.unit}</TableCell>
                                                <TableCell>{Number(criterion.maxThreshold)}</TableCell>
                                                <TableCell className="max-w-xs truncate">
                                                    {criterion.referenceStandard || "—"}
                                                </TableCell>
                                                <TableCell>
                                                    <Badge variant={isActive ? "default" : "secondary"}>
                                                        {isActive ? "Đang hoạt động" : "Ngừng sử dụng"}
                                                    </Badge>
                                                </TableCell>
                                                {canManage && (
                                                    <TableCell className="text-right">
                                                        <div className="flex justify-end gap-1">
                                                            <Button
                                                                variant="ghost"
                                                                size="icon-sm"
                                                                onClick={() => openEditDialog(criterion)}
                                                                className="hover:bg-muted"
                                                                title="Sửa chỉ tiêu"
                                                            >
                                                                <Pencil className="h-4 w-4"/>
                                                            </Button>
                                                            {isActive ? (
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon-sm"
                                                                    onClick={() => handleToggleStatus(criterion)}
                                                                    disabled={togglingId === criterion.id}
                                                                    className="hover:bg-muted"
                                                                    title="Ngừng sử dụng"
                                                                >
                                                                    <EyeOff className="h-4 w-4"/>
                                                                </Button>
                                                            ) : (
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon-sm"
                                                                    onClick={() => handleToggleStatus(criterion)}
                                                                    disabled={togglingId === criterion.id}
                                                                    className="hover:bg-muted"
                                                                    title="Kích hoạt"
                                                                >
                                                                    <Eye className="h-4 w-4"/>
                                                                </Button>
                                                            )}
                                                            <Button
                                                                variant="ghost"
                                                                size="icon-sm"
                                                                onClick={() => setDeleteTarget(criterion)}
                                                                title="Xóa chỉ tiêu"
                                                                className="text-destructive hover:text-destructive hover:bg-muted"
                                                            >
                                                                <Trash2 className="h-4 w-4"/>
                                                            </Button>
                                                        </div>
                                                    </TableCell>
                                                )}
                                            </TableRow>
                                        );
                                    })
                                )}
                            </TableBody>
                        </Table>
                    </div>

                    {/* Controls phân trang */}
                    <Pagination
                        currentPage={currentPage}
                        totalPages={totalPages}
                        totalElements={totalElements}
                        pageSize={PAGE_SIZE}
                        loading={loading}
                        itemLabel="chỉ tiêu"
                        onPageChange={setCurrentPage}
                    />
                </CardContent>
            </Card>

            <InspectionCriterionForm
                open={formOpen}
                onClose={closeForm}
                onSuccess={fetchCriteria}
                criterion={editingCriterion}
            />

            <AlertDialog
                open={!!deleteTarget}
                onOpenChange={(open) => !open && setDeleteTarget(null)}
            >
                <AlertDialogPopup>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Xóa chỉ tiêu kiểm nghiệm</AlertDialogTitle>
                        <AlertDialogDescription>
                            Bạn có chắc muốn xóa chỉ tiêu <strong>{deleteTarget?.name}</strong>{" "}
                            không? Hành động này không thể hoàn tác.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel
                            onClick={() => setDeleteTarget(null)}
                            disabled={deleteSubmitting}
                        >
                            Hủy
                        </AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleDelete}
                            disabled={deleteSubmitting}
                            className="bg-red-600 hover:bg-red-700"
                        >
                            {deleteSubmitting ? "Đang xóa..." : "Xóa"}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogPopup>
            </AlertDialog>
        </div>
    );
}
