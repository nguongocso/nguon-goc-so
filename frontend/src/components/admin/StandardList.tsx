import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import {toast} from "sonner";
import {Button} from "@/components/ui/button";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import {Badge} from "@/components/ui/badge";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {Plus, Pencil, RefreshCw} from "lucide-react";
import {
    getStandards,
} from "@/api/standardApi";
import type {Standard} from "@/types/standard";
import {usePermission} from "@/hooks/usePermission";
import {ROLE_ACCESS} from "@/config/roleAccess";
import {Pagination} from "@/components/common/Pagination";

const PAGE_SIZE = 10;

const statusFilterOptions = [
    {value: "all", label: "Tất cả"},
    {value: "true", label: "Đang hoạt động"},
    {value: "false", label: "Không hoạt động"},
];

export const StandardList: React.FC = () => {
    const navigate = useNavigate();
    const canManage = usePermission(ROLE_ACCESS.standardManagement);
    const [standards, setStandards] = useState<Standard[]>([]);
    const [totalElements, setTotalElements] = useState(0);
    const [currentPage, setCurrentPage] = useState(0);
    const [isActiveFilter, setIsActiveFilter] = useState<boolean | undefined>(
        undefined,
    );
    const [loading, setLoading] = useState(true);

    const fetchStandards = async () => {
        setLoading(true);
        try {
            const data = await getStandards({
                isActive: isActiveFilter,
                page: currentPage,
                size: PAGE_SIZE,
            });
            setStandards(data.items);
            setTotalElements(data.totalElements);
        } catch (error: any) {
            toast.error(
                error.response?.data?.message || "Không thể tải danh sách tiêu chuẩn",
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchStandards();
    }, [currentPage, isActiveFilter]);

    const openCreateDialog = () => {
        navigate("/admin/standards/create");
    };

    const openEditDialog = (standard: Standard) => {
        navigate(`/admin/standards/${standard.id}/edit`);
    };

    const totalPages = Math.ceil(totalElements / PAGE_SIZE);

    const getStatusFilterLabel = (value: string) => {
        const option = statusFilterOptions.find((opt) => opt.value === value);
        return option ? option.label : "Trạng thái";
    };

    const currentFilterValue =
        isActiveFilter === undefined ? "all" : String(isActiveFilter);

    return (
        <>
            <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
                <CardHeader className="border-b border-slate-100 pb-4">
                    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                        <CardTitle className="text-xl font-bold text-slate-900">Danh mục tiêu chuẩn chất
                            lượng</CardTitle>
                        <div className="flex flex-wrap items-center gap-2">
                            <Select
                                value={currentFilterValue}
                                onValueChange={(val) => {
                                    if (val === "all") setIsActiveFilter(undefined);
                                    else setIsActiveFilter(val === "true");
                                }}
                            >
                                <SelectTrigger size="sm" className="w-[180px]">
                                    <SelectValue placeholder="Trạng thái">
                                        {getStatusFilterLabel(currentFilterValue)}
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
                                onClick={fetchStandards}
                                disabled={loading}
                            >
                                <RefreshCw
                                    className={`h-4 w-4 mr-1 ${loading ? "animate-spin" : ""}`}
                                />
                                Làm mới
                            </Button>
                            {canManage && (
                                <Button variant="create" size="sm" onClick={openCreateDialog}>
                                    <Plus className="h-4 w-4 mr-1"/>
                                    Thêm tiêu chuẩn
                                </Button>
                            )}
                        </div>
                    </div>
                </CardHeader>
                <CardContent className="p-4 space-y-4">
                    {loading ? (
                        <div className="text-center py-12 text-muted-foreground">Đang tải...</div>
                    ) : standards.length === 0 ? (
                        <div className="text-center py-12 text-muted-foreground">
                            Chưa có tiêu chuẩn nào trong danh mục.
                        </div>
                    ) : (
                        <>
                            <div className="rounded-md border overflow-x-auto">
                                <Table>
                                    <TableHeader>
                                        <TableRow className="bg-muted/50">
                                            <TableHead className="w-12 text-center">STT</TableHead>
                                            <TableHead>Tên tiêu chuẩn</TableHead>
                                            <TableHead>Cơ quan ban hành</TableHead>
                                            <TableHead>Mô tả</TableHead>
                                            <TableHead>Trạng thái</TableHead>
                                            <TableHead>Ngày tạo</TableHead>
                                            {canManage && (
                                                <TableHead className="text-right">Thao tác</TableHead>
                                            )}
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {standards.map((std, index) => {
                                            const isActive = std.isActive;
                                            return (
                                                <TableRow
                                                    key={std.id}
                                                    className={`hover:bg-muted/40 transition-colors${
                                                        !isActive ? " opacity-60" : ""
                                                    }`}
                                                >
                                                    <TableCell
                                                        className="text-center font-medium text-muted-foreground">
                                                        {index + 1 + currentPage * PAGE_SIZE}
                                                    </TableCell>
                                                    <TableCell className="font-medium">
                                                        {std.name}
                                                    </TableCell>
                                                    <TableCell>{std.issuingBody || "—"}</TableCell>
                                                    <TableCell className="max-w-xs truncate">
                                                        {std.description || "—"}
                                                    </TableCell>
                                                    <TableCell>
                                                        <Badge
                                                            variant={isActive ? "default" : "outline"}
                                                            className={
                                                                !isActive
                                                                    ? "text-muted-foreground bg-muted/50 border-muted-foreground/20"
                                                                    : ""
                                                            }
                                                        >
                                                            {isActive ? "Hoạt động" : "Không hoạt động"}
                                                        </Badge>
                                                    </TableCell>
                                                    <TableCell>
                                                        {new Date(std.createdAt).toLocaleDateString("vi-VN")}
                                                    </TableCell>
                                                    {canManage && (
                                                        <TableCell className="text-right">
                                                            <div className="flex justify-end items-center gap-2">
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon-sm"
                                                                    onClick={() => openEditDialog(std)}
                                                                    disabled={!isActive}
                                                                    className={`hover:bg-muted${!isActive ? " text-muted-foreground" : ""}`}
                                                                    title="Sửa tiêu chuẩn"
                                                                >
                                                                    <Pencil className="h-4 w-4"/>
                                                                </Button>
                                                            </div>
                                                        </TableCell>
                                                    )}
                                                </TableRow>
                                            );
                                        })}
                                    </TableBody>
                                </Table>
                            </div>

                            <Pagination
                                currentPage={currentPage}
                                totalPages={totalPages}
                                totalElements={totalElements}
                                pageSize={PAGE_SIZE}
                                loading={loading}
                                itemLabel="tiêu chuẩn"
                                onPageChange={setCurrentPage}
                            />
                        </>
                    )}
                </CardContent>
            </Card>
        </>
    );
};