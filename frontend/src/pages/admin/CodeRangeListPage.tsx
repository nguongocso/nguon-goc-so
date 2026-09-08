import {getCodeRangeStatus} from "@/api/codeRangeApi";
import {getSupplementRequests} from "@/api/codeRangeSupplementApi";
import {Button} from "@/components/ui/button";
import {TableCell, TableHead, TableRow} from "@/components/ui/table";
import {Pagination} from "@/components/common/Pagination";
import {ListPageHeader} from "@/components/common/ListPageHeader";
import {ListCard} from "@/components/common/ListCard";
import {ListToolbar} from "@/components/common/ListToolbar";
import {SearchInput} from "@/components/common/SearchInput";
import {FilterSelect} from "@/components/common/FilterSelect";
import {RefreshButton} from "@/components/common/RefreshButton";
import {DataTableShell} from "@/components/common/DataTableShell";
import {StatusBadge, type StatusTone} from "@/components/common/StatusBadge";
import {useSetBreadcrumb} from "@/components/common/AppBreadcrumb";
import type {CodeRangeStatusResponse} from "@/types/codeRange";
import type {CodeRangeSupplementRequest} from "@/types/codeRangeSupplement";
import {ClipboardCheck, Plus, QrCode} from "lucide-react";
import {HelpButton} from "@/components/help/HelpButton";
import {useCallback, useEffect, useMemo, useState} from "react";
import {Link, useNavigate} from "react-router-dom";
import {toast} from "sonner";
import {usePermission} from "@/hooks/usePermission";
import {ROLE_ACCESS} from "@/config/roleAccess";

const PAGE_SIZE = 10;

const STATUS_FILTER_OPTIONS = [
    {value: "ALL", label: "Tất cả trạng thái"},
    {value: "OK", label: "Còn đủ"},
    {value: "NEARLY_EXHAUSTED", label: "Gần hết"},
    {value: "EXHAUSTED", label: "Đã hết"},
];

const getStatusTone = (usagePercent: number): StatusTone => {
    if (usagePercent > 80) return "danger";
    if (usagePercent > 50) return "warning";
    return "success";
};

const getUsageTextClass = (tone: StatusTone): string => {
    switch (tone) {
        case "danger":
            return "text-red-600 font-semibold";
        case "warning":
            return "text-yellow-600 font-semibold";
        default:
            return "text-green-600";
    }
};

const STATUS_LABEL: Record<string, string> = {
    OK: 'Còn đủ',
    NEARLY_EXHAUSTED: 'Gần hết',
    EXHAUSTED: 'Đã hết',
};

const getStatusBadge = (status: string, tone: StatusTone) => {
    const label = STATUS_LABEL[status] || status;
    return <StatusBadge label={label} tone={tone} />;
};

const CodeRangeListPage: React.FC = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [ranges, setRanges] = useState<CodeRangeStatusResponse[]>([]);
    const [currentPage, setCurrentPage] = useState(0);

    const [search, setSearch] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");

    const [pendingByOrg, setPendingByOrg] = useState<
        Record<string, CodeRangeSupplementRequest[]>
    >({});

    useSetBreadcrumb([
        {label: "Tổng quan", href: "/dashboard"},
        {label: "Quản lý dải mã truy xuất"},
    ]);

    const canCreate = usePermission(ROLE_ACCESS.codeRangeList);

    const loadPending = useCallback(async () => {
        try {
            const result = await getSupplementRequests({
                status: "PENDING",
                page: 0,
                size: 1000,
            });
            const map: Record<string, CodeRangeSupplementRequest[]> = {};
            for (const item of result.items) {
                if (item.organizationId) {
                    (map[item.organizationId] ??= []).push(item);
                }
            }
            setPendingByOrg(map);
        } catch (error) {
            toast.error("Không thể tải danh sách yêu cầu cấp bổ sung mã");
        }
    }, []);

    const loadAll = useCallback(async () => {
        try {
            setLoading(true);
            const rangeData = await getCodeRangeStatus();
            await loadPending();
            setRanges(rangeData);
        } catch (error) {
            toast.error("Không thể tải danh sách dải mã");
        } finally {
            setLoading(false);
        }
    }, [loadPending]);

    useEffect(() => {
        void loadAll();
    }, [loadAll]);

    const filtered = useMemo(() => {
        const q = search.trim().toLowerCase();
        return ranges.filter((range) => {
            const matchKeyword =
                !q ||
                range.organizationName.toLowerCase().includes(q) ||
                range.prefix.toLowerCase().includes(q);
            const matchStatus =
                statusFilter === "ALL" || range.status === statusFilter;
            return matchKeyword && matchStatus;
        });
    }, [ranges, search, statusFilter]);

    const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    const safePage = Math.min(currentPage, totalPages - 1);
    const pagedRanges = filtered.slice(
        safePage * PAGE_SIZE,
        safePage * PAGE_SIZE + PAGE_SIZE
    );

    const header = (
        <>
            <TableHead className="w-14 text-center">STT</TableHead>
            <TableHead>Tổ chức</TableHead>
            <TableHead>Tiền tố</TableHead>
            <TableHead>Hạn mức</TableHead>
            <TableHead>Đã dùng</TableHead>
            <TableHead>% sử dụng</TableHead>
            <TableHead className="text-center">Trạng thái</TableHead>
            <TableHead className="text-center">Thao tác</TableHead>
        </>
    );

    const body = pagedRanges.map((range, index) => {
        const pendingCount = pendingByOrg[range.organizationId]?.length ?? 0;
        const tone = getStatusTone(range.usagePercent);
        return (
            <TableRow
                key={range.id}
                className="transition-colors hover:bg-muted/40"
            >
                <TableCell className="text-center font-medium text-muted-foreground">
                    {index + 1 + safePage * PAGE_SIZE}
                </TableCell>
                <TableCell className="font-medium">
                    {range.organizationName}
                </TableCell>
                <TableCell>
                    <code className="rounded bg-muted px-2 py-0.5 text-sm font-mono">
                        {range.prefix}
                    </code>
                </TableCell>
                <TableCell className="font-medium tabular-nums">
                    {range.totalLimit.toLocaleString("vi-VN")}
                </TableCell>
                <TableCell className="tabular-nums">
                    {range.usedCount.toLocaleString("vi-VN")}
                </TableCell>
                <TableCell className={`tabular-nums ${getUsageTextClass(tone)}`}>
                    {range.usagePercent.toLocaleString("vi-VN", {
                        minimumFractionDigits: 1,
                        maximumFractionDigits: 1,
                    })}
                    %
                </TableCell>
                <TableCell>
                    <div className="flex justify-center">
                        {getStatusBadge(range.status, tone)}
                    </div>
                </TableCell>
                <TableCell className="text-center">
                    {pendingCount > 0 ? (
                        <Button
                            size="sm"
                            variant="outline"
                            onClick={() =>
                                navigate(
                                    `/admin/code-range-supplements/${pendingByOrg[range.organizationId][0].id}`
                                )
                            }
                            aria-label="Duyệt bổ sung"
                        >
                            <ClipboardCheck className="size-4" />
                        </Button>
                    ) : (
                        <span className="text-muted-foreground">—</span>
                    )}
                </TableCell>
            </TableRow>
        );
    });

    return (
        <div className="space-y-6">
            <ListPageHeader
                icon={QrCode}
                title="Quản lý dải mã truy xuất"
                description="Quản lý các dải mã truy xuất đã cấp cho tổ chức"
                actions={
                    <>
                        <HelpButton screenKey="admin-code-range-list" />
                        {canCreate && (
                            <Link to="/admin/code-ranges/create">
                                <Button className="shrink-0" variant="create" size="sm">
                                    <Plus className="h-4 w-4 mr-2" />
                                    Cấp dải mã mới
                                </Button>
                            </Link>
                        )}
                    </>
                }
            />

            <ListCard>
                <ListToolbar
                    left={
                        <>
                            <SearchInput
                                placeholder="Tìm theo tên tổ chức hoặc tiền tố..."
                                value={search}
                                onChange={(e) => {
                                    setSearch(e.target.value);
                                    setCurrentPage(0);
                                }}
                            />
                            <FilterSelect
                                value={statusFilter}
                                onValueChange={(val) => {
                                    setStatusFilter(val || "ALL");
                                    setCurrentPage(0);
                                }}
                                options={STATUS_FILTER_OPTIONS}
                            />
                        </>
                    }
                    right={
                        <RefreshButton onClick={() => void loadAll()} loading={loading} />
                    }
                />

                <DataTableShell
                    header={header}
                    body={body}
                    loading={loading}
                    empty={filtered.length === 0}
                    colSpan={8}
                    loadingMessage="Đang tải danh sách dải mã..."
                    emptyMessage={
                        search || statusFilter !== "ALL"
                            ? "Không tìm thấy dải mã nào phù hợp với bộ lọc."
                            : "Chưa có dải mã nào"
                    }
                />

                <Pagination
                    currentPage={safePage}
                    totalPages={totalPages}
                    totalElements={filtered.length}
                    pageSize={PAGE_SIZE}
                    loading={loading}
                    itemLabel="dải mã"
                    onPageChange={setCurrentPage}
                />
            </ListCard>
        </div>
    );
};

export default CodeRangeListPage;