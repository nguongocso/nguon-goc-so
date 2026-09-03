import {useEffect, useState, useMemo} from "react";
import {useNavigate} from "react-router-dom";
import {toast} from "sonner";
import {Eye, EyeOff, Pencil, Plus, Trash2, FlaskConical} from "lucide-react";
import {Button} from "@/components/ui/button";
import {TableCell, TableHead, TableRow} from "@/components/ui/table";
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
import {ListPageHeader} from "@/components/common/ListPageHeader";
import {ListCard} from "@/components/common/ListCard";
import {ListToolbar} from "@/components/common/ListToolbar";
import {SearchInput} from "@/components/common/SearchInput";
import {FilterSelect} from "@/components/common/FilterSelect";
import {RefreshButton} from "@/components/common/RefreshButton";
import {DataTableShell} from "@/components/common/DataTableShell";
import {StatusBadge} from "@/components/common/StatusBadge";
import {usePermission} from "@/hooks/usePermission";
import {ROLE_ACCESS} from "@/config/roleAccess";
import {
    deleteInspectionCriterion,
    disableInspectionCriterion,
    enableInspectionCriterion,
    getInspectionCriteria,
} from "@/api/inspectionCriterionApi";
import type {InspectionCriterion} from "@/types/inspectionCriterion";
import {InspectionCriterionForm} from "@/components/admin/inspection-criterion/InspectionCriterionForm";

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
    {value: "ALL", label: "Tất cả trạng thái"},
    {value: "ACTIVE", label: "Đang hoạt động"},
    {value: "INACTIVE", label: "Ngừng sử dụng"},
];

export default function InspectionCriteriaManagementPage() {
    const navigate = useNavigate();
    const canManage = usePermission(ROLE_ACCESS.inspectionCriteriaManagement);
    const [criteria, setCriteria] = useState<InspectionCriterion[]>([]);
    const [loading, setLoading] = useState(true);

    // Tìm kiếm, lọc (client-side) & phân trang
    const [search, setSearch] = useState("");
    const [status, setStatus] = useState("ALL");
    const [page, setPage] = useState(0);

    const [formOpen, setFormOpen] = useState(false);
    const [editingCriterion, setEditingCriterion] = useState<InspectionCriterion | null>(null);
    const [togglingId, setTogglingId] = useState<number | null>(null);
    const [deleteTarget, setDeleteTarget] = useState<InspectionCriterion | null>(null);
    const [deleteSubmitting, setDeleteSubmitting] = useState(false);

    const fetchAll = async () => {
        setLoading(true);
        try {
            const data = await getInspectionCriteria({ page: 0, size: 1000 });
            setCriteria(data.items);
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
        fetchAll();
    }, []);

    const filtered = useMemo(() => {
        const q = search.toLowerCase().trim();
        return criteria.filter((c) => {
            const matchKeyword =
                !q ||
                c.name.toLowerCase().includes(q) ||
                (c.referenceStandard?.toLowerCase().includes(q) ?? false);
            const matchStatus = status === "ALL" || c.status === status;
            return matchKeyword && matchStatus;
        });
    }, [criteria, search, status]);

    const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    const safePage = Math.min(page, totalPages - 1);
    const paginated = useMemo(() => {
        const start = safePage * PAGE_SIZE;
        return filtered.slice(start, start + PAGE_SIZE);
    }, [filtered, safePage]);

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
            fetchAll();
        } catch (error: any) {
            toast.error(error.response?.data?.message || "Không thể cập nhật trạng thái chỉ tiêu");
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
            fetchAll();
        } catch (error: any) {
            toast.error(error.response?.data?.message || "Không thể xóa chỉ tiêu");
        } finally {
            setDeleteSubmitting(false);
        }
    };

    useSetBreadcrumb([
        {label: "Dashboard", href: "/dashboard"},
        {label: "Chỉ tiêu kiểm nghiệm"},
    ]);

    const header = (
        <>
            <TableHead className="w-12 text-center">STT</TableHead>
            <TableHead>Tên chỉ tiêu</TableHead>
            <TableHead>Đơn vị</TableHead>
            <TableHead className="text-center">Ngưỡng tối đa</TableHead>
            <TableHead>Tiêu chuẩn tham chiếu</TableHead>
            <TableHead>Trạng thái</TableHead>
            {canManage && <TableHead className="text-center">Thao tác</TableHead>}
        </>
    );

    const body = paginated.map((criterion, index) => (
        <TableRow key={criterion.id} className="hover:bg-muted/40 transition-colors">
            <TableCell className="text-center font-medium text-muted-foreground">
                {safePage * PAGE_SIZE + index + 1}
            </TableCell>
            <TableCell className="font-medium text-foreground">{criterion.name}</TableCell>
            <TableCell>{criterion.unit}</TableCell>
            <TableCell className="text-center">{criterion.maxThreshold}</TableCell>
            <TableCell>{criterion.referenceStandard || "—"}</TableCell>
            <TableCell className="text-center">
                {criterion.status === "ACTIVE" ? (
                    <StatusBadge label="Đang hoạt động" tone="success" />
                ) : (
                    <StatusBadge label="Ngừng sử dụng" tone="neutral" />
                )}
            </TableCell>
            {canManage && (
                <TableCell className="text-center">
                    <div className="flex justify-center items-center gap-1">
                        <Button
                            variant="ghost"
                            size="icon-sm"
                            onClick={() => handleToggleStatus(criterion)}
                            disabled={togglingId === criterion.id}
                            title={
                                criterion.status === "ACTIVE"
                                    ? "Ngừng sử dụng chỉ tiêu"
                                    : "Kích hoạt lại chỉ tiêu"
                            }
                        >
                            {criterion.status === "ACTIVE" ? (
                                <EyeOff className="h-4 w-4" />
                            ) : (
                                <Eye className="h-4 w-4" />
                            )}
                        </Button>
                        {!criterion.referenced && (
                            <>
                                <Button
                                    variant="ghost"
                                    size="icon-sm"
                                    onClick={() => openEditDialog(criterion)}
                                    title="Sửa chỉ tiêu"
                                >
                                    <Pencil className="h-4 w-4" />
                                </Button>
                                <Button
                                    variant="ghost"
                                    size="icon-sm"
                                    onClick={() => setDeleteTarget(criterion)}
                                    title="Xóa chỉ tiêu"
                                    className="text-destructive hover:text-destructive hover:bg-muted"
                                >
                                    <Trash2 className="h-4 w-4" />
                                </Button>
                            </>
                        )}
                        {criterion.referenced && (
                            <span className="text-xs text-muted-foreground">
                                Không xóa
                            </span>
                        )}
                    </div>
                </TableCell>
            )}
        </TableRow>
    ));

    return (
        <div className="space-y-6">
            {/* Header trang */}
            <ListPageHeader
                icon={FlaskConical}
                title="Quản lý danh mục chỉ tiêu kiểm nghiệm"
                description="Thêm, sửa, ngừng sử dụng các chỉ tiêu kiểm nghiệm dùng chung"
                actions={
                    <>
                        <HelpButton screenKey="admin-inspection-criteria" />
                        {canManage && (
                            <Button variant="create" size="sm" onClick={() => navigate("/admin/inspection-criteria/create")}>
                                <Plus className="h-4 w-4 mr-1" /> Thêm chỉ tiêu
                            </Button>
                        )}
                    </>
                }
            />

            {/* Thẻ chung: bộ lọc + bảng + phân trang */}
            <ListCard>
                <ListToolbar
                    left={
                        <>
                            <SearchInput
                                placeholder="Tìm theo tên hoặc tiêu chuẩn tham chiếu..."
                                value={search}
                                onChange={(e) => {
                                    setSearch(e.target.value);
                                    setPage(0);
                                }}
                            />
                            <FilterSelect
                                value={status}
                                onValueChange={(val) => {
                                    setStatus(val || "ALL");
                                    setPage(0);
                                }}
                                options={STATUS_OPTIONS}
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
                    colSpan={canManage ? 7 : 6}
                    loadingMessage="Đang tải danh sách chỉ tiêu..."
                    emptyMessage="Chưa có chỉ tiêu kiểm nghiệm nào."
                />

                <Pagination
                    currentPage={safePage}
                    totalPages={totalPages}
                    totalElements={filtered.length}
                    pageSize={PAGE_SIZE}
                    loading={loading}
                    itemLabel="chỉ tiêu"
                    onPageChange={setPage}
                />
            </ListCard>

            <InspectionCriterionForm
                open={formOpen}
                onClose={closeForm}
                onSuccess={fetchAll}
                criterion={editingCriterion}
            />

            <AlertDialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
                <AlertDialogPopup>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Xóa chỉ tiêu kiểm nghiệm</AlertDialogTitle>
                        <AlertDialogDescription>
                            Bạn có chắc muốn xóa chỉ tiêu <strong>{deleteTarget?.name}</strong>{" "}
                            không? Hành động này không thể hoàn tác.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setDeleteTarget(null)} disabled={deleteSubmitting}>
                            Hủy
                        </AlertDialogCancel>
                        <AlertDialogAction onClick={handleDelete} disabled={deleteSubmitting} className="bg-red-600 hover:bg-red-700">
                            {deleteSubmitting ? "Đang xóa..." : "Xóa"}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogPopup>
            </AlertDialog>
        </div>
    );
}