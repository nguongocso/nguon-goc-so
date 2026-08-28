import React, {useEffect, useState, useMemo} from "react";
import {useNavigate} from "react-router-dom";
import {toast} from "sonner";
import {Button} from "@/components/ui/button";
import {TableCell, TableHead, TableRow} from "@/components/ui/table";
import {Pencil, Plus, Award} from "lucide-react";
import {getStandards} from "@/api/standardApi";
import type {Standard} from "@/types/standard";
import {usePermission} from "@/hooks/usePermission";
import {ROLE_ACCESS} from "@/config/roleAccess";
import {Pagination} from "@/components/common/Pagination";
import {ListPageHeader} from "@/components/common/ListPageHeader";
import {ListCard} from "@/components/common/ListCard";
import {ListToolbar} from "@/components/common/ListToolbar";
import {SearchInput} from "@/components/common/SearchInput";
import {FilterSelect} from "@/components/common/FilterSelect";
import {RefreshButton} from "@/components/common/RefreshButton";
import {DataTableShell} from "@/components/common/DataTableShell";
import {StatusBadge} from "@/components/common/StatusBadge";
import {HelpButton} from "@/components/help/HelpButton";

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
    {value: "ALL", label: "Tất cả trạng thái"},
    {value: "true", label: "Đang hoạt động"},
    {value: "false", label: "Không hoạt động"},
];

export const StandardList: React.FC = () => {
    const navigate = useNavigate();
    const canManage = usePermission(ROLE_ACCESS.standardManagement);
    const [standards, setStandards] = useState<Standard[]>([]);
    const [loading, setLoading] = useState(true);

    // Tìm kiếm, lọc (client-side) & phân trang
    const [search, setSearch] = useState("");
    const [status, setStatus] = useState("ALL");
    const [page, setPage] = useState(0);

    const fetchStandards = async () => {
        setLoading(true);
        try {
            const data = await getStandards({ isActive: undefined, page: 0, size: 1000 });
            setStandards(data.items);
        } catch (error: any) {
            toast.error(error.response?.data?.message || "Không thể tải danh sách tiêu chuẩn");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchStandards();
    }, []);

    const filtered = useMemo(() => {
        const q = search.toLowerCase().trim();
        return standards.filter((s) => {
            const matchKeyword =
                !q ||
                s.name.toLowerCase().includes(q) ||
                (s.issuingBody?.toLowerCase().includes(q) ?? false);
            const matchStatus =
                status === "ALL" || (status === "true" ? s.isActive : !s.isActive);
            return matchKeyword && matchStatus;
        });
    }, [standards, search, status]);

    const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    const safePage = Math.min(page, totalPages - 1);
    const paginated = useMemo(() => {
        const start = safePage * PAGE_SIZE;
        return filtered.slice(start, start + PAGE_SIZE);
    }, [filtered, safePage]);

    const openCreateDialog = () => {
        navigate("/admin/standards/create");
    };

    const openEditDialog = (standard: Standard) => {
        navigate(`/admin/standards/${standard.id}/edit`);
    };

    const header = (
        <>
            <TableHead className="w-12 text-center">STT</TableHead>
            <TableHead>Tên tiêu chuẩn</TableHead>
            <TableHead>Cơ quan ban hành</TableHead>
            <TableHead>Mô tả</TableHead>
            <TableHead>Trạng thái</TableHead>
            <TableHead>Ngày tạo</TableHead>
            {canManage && <TableHead className="text-right">Thao tác</TableHead>}
        </>
    );

    const body = paginated.map((std, index) => (
        <TableRow key={std.id} className="hover:bg-muted/40 transition-colors">
            <TableCell className="text-center font-medium text-muted-foreground">
                {safePage * PAGE_SIZE + index + 1}
            </TableCell>
            <TableCell className="font-medium text-foreground">{std.name}</TableCell>
            <TableCell>{std.issuingBody || "—"}</TableCell>
            <TableCell className="max-w-xs truncate">{std.description || "—"}</TableCell>
            <TableCell>
                {std.isActive ? (
                    <StatusBadge label="Hoạt động" tone="success" />
                ) : (
                    <StatusBadge label="Không hoạt động" tone="neutral" />
                )}
            </TableCell>
            <TableCell>{new Date(std.createdAt).toLocaleDateString("vi-VN")}</TableCell>
            {canManage && (
                <TableCell className="text-right">
                    <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => openEditDialog(std)}
                        disabled={!std.isActive}
                        title="Sửa tiêu chuẩn"
                    >
                        <Pencil className="h-4 w-4" />
                    </Button>
                </TableCell>
            )}
        </TableRow>
    ));


    return (
        <div className="space-y-6">
            {/* Header trang */}
            <ListPageHeader
                icon={Award}
                title="Quản lý tiêu chuẩn chất lượng"
                description="Quản lý danh mục tiêu chuẩn chất lượng dùng chung cho toàn nền tảng."
                actions={
                    <>
                        <HelpButton screenKey="admin-standards" />
                        {canManage && (
                            <Button variant="create" size="sm" onClick={openCreateDialog}>
                                <Plus className="h-4 w-4 mr-1" />
                                Thêm tiêu chuẩn
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
                                placeholder="Tìm theo tên tiêu chuẩn hoặc cơ quan ban hành..."
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
                    right={<RefreshButton onClick={fetchStandards} loading={loading} />}
                />

                <DataTableShell
                    header={header}
                    body={body}
                    loading={loading}
                    empty={filtered.length === 0}
                    colSpan={canManage ? 7 : 6}
                    loadingMessage="Đang tải danh sách tiêu chuẩn..."
                    emptyMessage="Chưa có tiêu chuẩn nào trong danh mục."
                />

                <Pagination
                    currentPage={safePage}
                    totalPages={totalPages}
                    totalElements={filtered.length}
                    pageSize={PAGE_SIZE}
                    loading={loading}
                    itemLabel="tiêu chuẩn"
                    onPageChange={setPage}
                />
            </ListCard>
        </div>
    );
};
