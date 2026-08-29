import {getCodeRangeStatus} from "@/api/codeRangeApi";
import {Button} from "@/components/ui/button";
import {TableCell, TableHead, TableRow} from "@/components/ui/table";
import {Pagination} from "@/components/common/Pagination";
import {ListPageHeader} from "@/components/common/ListPageHeader";
import {ListCard} from "@/components/common/ListCard";
import {DataTableShell} from "@/components/common/DataTableShell";
import {StatusBadge} from "@/components/common/StatusBadge";
import type {CodeRangeStatusResponse} from "@/types/codeRange";
import {Plus, QrCode} from "lucide-react";
import {HelpButton} from "@/components/help/HelpButton";
import {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import {toast} from "sonner";
import {usePermission} from "@/hooks/usePermission";
import {ROLE_ACCESS} from "@/config/roleAccess";

const PAGE_SIZE = 10;

const getStatusBadge = (status: string) => {
    switch (status) {
        case "OK":
            return <StatusBadge label="OK" tone="success" />;
        case "NEARLY_EXHAUSTED":
            return <StatusBadge label="Gần hết" tone="warning" />;
        case "EXHAUSTED":
            return <StatusBadge label="Đã hết" tone="danger" />;
        default:
            return <StatusBadge label={status} tone="neutral" />;
    }
};

const CodeRangeListPage: React.FC = () => {
    const [loading, setLoading] = useState(true);
    const [ranges, setRanges] = useState<CodeRangeStatusResponse[]>([]);
    const [currentPage, setCurrentPage] = useState(0);

    const canCreate = usePermission(ROLE_ACCESS.codeRangeList);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const data = await getCodeRangeStatus();
                setRanges(data);
            } catch (error) {
                toast.error("Không thể tải danh sách dải mã");
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    const totalPages = Math.ceil(ranges.length / PAGE_SIZE);
    const pagedRanges = ranges.slice(
        currentPage * PAGE_SIZE,
        currentPage * PAGE_SIZE + PAGE_SIZE
    );

    const header = (
        <>
            <TableHead className="w-12 text-center">STT</TableHead>
            <TableHead>Tổ chức</TableHead>
            <TableHead>Tiền tố</TableHead>
            <TableHead className="text-right">Hạn mức</TableHead>
            <TableHead className="text-right">Đã dùng</TableHead>
            <TableHead className="text-right">% sử dụng</TableHead>
            <TableHead className="text-center">Trạng thái</TableHead>
        </>
    );

    const body = pagedRanges.map((range, index) => (
        <TableRow
            key={range.id}
            className="transition-colors hover:bg-muted/50"
        >
            <TableCell className="text-center font-medium text-muted-foreground">
                {index + 1 + currentPage * PAGE_SIZE}
            </TableCell>
            <TableCell className="font-medium">
                {range.organizationName}
            </TableCell>
            <TableCell>
                <code className="rounded bg-muted px-2 py-0.5 text-sm font-mono">
                    {range.prefix}
                </code>
            </TableCell>
            <TableCell className="text-right font-medium">
                {range.totalLimit.toLocaleString()}
            </TableCell>
            <TableCell className="text-right">
                {range.usedCount.toLocaleString()}
            </TableCell>
            <TableCell className="text-right">
                <span
                    className={
                        range.usagePercent > 80
                            ? "text-red-600 font-semibold"
                            : range.usagePercent > 50
                                ? "text-yellow-600 font-semibold"
                                : "text-green-600"
                    }
                >
                    {range.usagePercent.toFixed(1)}%
                </span>
            </TableCell>
            <TableCell className="text-center">
                {getStatusBadge(range.status)}
            </TableCell>
        </TableRow>
    ));

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
                <DataTableShell
                    header={header}
                    body={body}
                    loading={loading}
                    empty={ranges.length === 0}
                    colSpan={7}
                    loadingMessage="Đang tải danh sách dải mã..."
                    emptyMessage="Chưa có dải mã nào"
                />

                <Pagination
                    currentPage={currentPage}
                    totalPages={totalPages}
                    totalElements={ranges.length}
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
