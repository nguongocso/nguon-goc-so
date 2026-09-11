import React, { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { AlertCircle, ChevronLeft, ChevronRight, LoaderCircle, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Input } from '@/components/ui/input';
import { Checkbox } from '@/components/ui/checkbox';
import { createBulkRecallRequest } from '@/api/recallApi';
import type { ShipmentTraceDto, ProductionLotTraceDto } from '@/types/impactScopeTrace';

const LOT_STATUS_MAP: Record<string, { label: string; tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral' }> = {
    ACTIVATED: { label: 'Đã kích hoạt', tone: 'success' },
    DRAFT: { label: 'Dự thảo', tone: 'neutral' },
    RECALLED: { label: 'Đã thu hồi', tone: 'danger' },
    SPLIT: { label: 'Đã tách', tone: 'neutral' },
    CODE_PRINTED: { label: 'Đã in mã', tone: 'info' },
    APPROVED: { label: 'Đã duyệt', tone: 'success' },
    PACKAGED: { label: 'Đã đóng gói', tone: 'info' },
    CANCELLED: { label: 'Đã hủy', tone: 'neutral' },
};

export interface BulkRecallRequestFormProps {
    productionLotId: string;
    productionLot: ProductionLotTraceDto;
    shipments: ShipmentTraceDto[];
    onSuccess: (requestId: string) => void;
    onCancel?: () => void;
}

type ScopeStatusFilter = 'ALL' | 'SELECTABLE' | 'INCLUDED' | 'RECALLED' | 'EXCLUDED';

const STATUS_FILTER_OPTIONS: Array<{ value: ScopeStatusFilter; label: string }> = [
    { value: 'ALL', label: 'Tất cả trạng thái' },
    { value: 'SELECTABLE', label: 'Có thể thu hồi' },
    { value: 'INCLUDED', label: 'Đã chọn' },
    { value: 'RECALLED', label: 'Đã thu hồi' },
    { value: 'EXCLUDED', label: 'Đã loại' },
];

// Các lựa chọn số dòng mỗi trang của bảng "Phạm vi thu hồi" (chỉ ảnh hưởng hiển thị).
const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

interface LotSelectionItem {
    shipmentId: string;
    shipmentName: string;
    status: string;
    organizationId: string;
    organizationName: string;
    isRecalled: boolean;
    isSplitParent: boolean;
    included: boolean;
    exclusionReason: string;
}

export const BulkRecallRequestForm: React.FC<BulkRecallRequestFormProps> = ({
    productionLotId,
    productionLot,
    shipments,
    onSuccess,
    onCancel,
}) => {
    const [generalReason, setGeneralReason] = useState('');
    const [evidence, setEvidence] = useState('');
    const [creating, setCreating] = useState(false);
    // Trạng thái hiển thị của bảng "Phạm vi thu hồi" (chỉ ảnh hưởng UI, không thay đổi business logic).
    const [searchKeyword, setSearchKeyword] = useState('');
    const [statusFilter, setStatusFilter] = useState<ScopeStatusFilter>('ALL');
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);

    const initialItems: LotSelectionItem[] = useMemo(() =>
        shipments.map((ship) => {
            const status = ship.status || '';
            const isRecalled = status === 'RECALLED';
            const isSplitParent = status === 'SPLIT';
            const isUnavailable = isRecalled || isSplitParent;
            const orgName = ship.receivingOrganizations?.[0]?.organizationName || '';
            return {
                shipmentId: ship.id,
                shipmentName: ship.name,
                status,
                organizationId: ship.receivingOrganizations?.[0]?.organizationId || '',
                organizationName: orgName,
                isRecalled,
                isSplitParent,
                included: !isUnavailable,
                exclusionReason: isRecalled
                    ? 'Lô đã được thu hồi trước đó'
                    : isSplitParent
                      ? 'Lô cha đã tách chỉ dùng để truy vết'
                      : '',
            };
        }),
        [shipments]
    );

    const [items, setItems] = useState<LotSelectionItem[]>(initialItems);

    const handleToggleIncluded = (shipmentId: string) => {
        setItems(prev => prev.map(item => {
            if (item.shipmentId !== shipmentId) return item;
            if (item.isRecalled || item.isSplitParent) return item;
            const newIncluded = !item.included;
            return {
                ...item,
                included: newIncluded,
                exclusionReason: newIncluded ? '' : item.exclusionReason,
            };
        }));
    };

    const handleToggleAll = () => {
        const allIncluded = selectableItems.every(item => item.included);
        setItems(prev => prev.map(item => {
            if (item.isRecalled || item.isSplitParent) return item;
            return {
                ...item,
                included: !allIncluded,
                exclusionReason: allIncluded ? 'Loại khỏi phạm vi' : '',
            };
        }));
    };

    const updateExclusionReason = (shipmentId: string, reason: string) => {
        setItems(prev => prev.map(item =>
            item.shipmentId === shipmentId ? { ...item, exclusionReason: reason } : item
        ));
    };

    const selectableItems = items.filter(item => !item.isRecalled && !item.isSplitParent);
    const allSelected = selectableItems.length > 0 && selectableItems.every(item => item.included);

    const includedItems = items.filter(item => item.included);
    const excludedItems = items.filter(item => !item.included && !item.isRecalled && !item.isSplitParent);
    const excludedWithoutReason = excludedItems.filter(item => !item.exclusionReason.trim());
    const hasScope = includedItems.length > 0;

    // Danh sách hiển thị: kết hợp tìm kiếm theo mã lô và bộ lọc trạng thái (client-side).
    const filteredItems = useMemo(() => {
        const keyword = searchKeyword.trim().toLowerCase();
        return items.filter((item) => {
            const matchesKeyword = !keyword || item.shipmentName.toLowerCase().includes(keyword);
            if (!matchesKeyword) return false;
            switch (statusFilter) {
                case 'SELECTABLE':
                    return !item.isRecalled && !item.isSplitParent;
                case 'INCLUDED':
                    return !item.isRecalled && !item.isSplitParent && item.included;
                case 'RECALLED':
                    return item.isRecalled;
                case 'EXCLUDED':
                    return !item.included;
                case 'ALL':
                default:
                    return true;
            }
        });
    }, [items, searchKeyword, statusFilter]);

    // Phân trang client-side. STT được tính từ vị trí trong danh sách đã lọc.
    const totalFiltered = filteredItems.length;
    const totalPages = Math.max(1, Math.ceil(totalFiltered / pageSize));
    const safeCurrentPage = Math.min(currentPage, totalPages);
    const pagedItems = useMemo(() => {
        const startIndex = (safeCurrentPage - 1) * pageSize;
        return filteredItems.slice(startIndex, startIndex + pageSize);
    }, [filteredItems, safeCurrentPage, pageSize]);
    const rangeStart = totalFiltered === 0 ? 0 : (safeCurrentPage - 1) * pageSize + 1;
    const rangeEnd = totalFiltered === 0 ? 0 : Math.min(safeCurrentPage * pageSize, totalFiltered);

    // Khi từ khóa/bộ lọc/page-size thay đổi thì luôn quay về trang 1.
    useEffect(() => {
        setCurrentPage(1);
    }, [searchKeyword, statusFilter, pageSize]);
    // Khi danh sách bị rút ngắn (ví dụ sau khi lọc), kẹp trang hiện tại trong giới hạn hợp lệ.
    useEffect(() => {
        if (currentPage > totalPages) setCurrentPage(totalPages);
    }, [currentPage, totalPages]);

    const handleSubmit = async () => {
        if (!generalReason.trim()) {
            toast.error('Vui lòng nhập lý do thu hồi.');
            return;
        }
        if (!hasScope) {
            toast.error('Vui lòng chọn ít nhất một lô để tạo yêu cầu thu hồi.');
            return;
        }
        if (excludedWithoutReason.length > 0) {
            toast.error('Vui lòng nhập lý do loại cho tất cả các lô bị loại.');
            return;
        }

        setCreating(true);
        try {
            const includedShipmentIds = includedItems.map(item => item.shipmentId);
            const excludedShipments = excludedItems.map(item => ({
                shipmentId: item.shipmentId,
                exclusionReason: item.exclusionReason,
            }));

            const result = await createBulkRecallRequest({
                productionLotId,
                reason: generalReason.trim(),
                evidence: evidence.trim() || undefined,
                includedShipmentIds,
                excludedShipments,
            });

            toast.success('Tạo yêu cầu thu hồi hàng loạt thành công.');
            onSuccess(result.id);
        } catch (err: any) {
            const message = err.response?.data?.message || 'Không thể tạo yêu cầu thu hồi.';
            toast.error(message);
        } finally {
            setCreating(false);
        }
    };

    const statusDisplay = (status: string) => LOT_STATUS_MAP[status] || { label: status, tone: 'neutral' as const };

    return (
        <div className="space-y-6">
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
                <h3 className="text-sm font-semibold text-slate-700 mb-2">Lô sản xuất nguồn</h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm">
                    <div>
                        <span className="text-slate-500">Tên lô:</span>{' '}
                        <span className="font-medium text-slate-900">{productionLot.name}</span>
                    </div>
                    <div>
                        <span className="text-slate-500">Mã lô:</span>{' '}
                        <span className="font-medium text-slate-900">{productionLot.code}</span>
                    </div>
                    {productionLot.plantingDate && (
                        <div>
                            <span className="text-slate-500">Ngày trồng:</span>{' '}
                            <span className="font-medium text-slate-900">
                                {new Date(productionLot.plantingDate).toLocaleDateString('vi-VN')}
                            </span>
                        </div>
                    )}
                    {productionLot.harvestDate && (
                        <div>
                            <span className="text-slate-500">Ngày thu hoạch:</span>{' '}
                            <span className="font-medium text-slate-900">
                                {new Date(productionLot.harvestDate).toLocaleDateString('vi-VN')}
                            </span>
                        </div>
                    )}
                </div>
            </div>

            <div className="space-y-4">
                <h3 className="text-sm font-semibold text-slate-700">Thông tin yêu cầu thu hồi</h3>
                <div className="space-y-1.5">
                    <Label htmlFor="generalReason">
                        Lý do thu hồi <span className="text-red-600">*</span>
                    </Label>
                    <Textarea
                        id="generalReason"
                        placeholder="Nhập lý do chung cho yêu cầu thu hồi..."
                        value={generalReason}
                        onChange={(e) => setGeneralReason(e.target.value)}
                        rows={3}
                        disabled={creating}
                    />
                </div>
                <div className="space-y-1.5">
                    <Label htmlFor="evidence">Bằng chứng (tùy chọn)</Label>
                    <Textarea
                        id="evidence"
                        placeholder="Nhập bằng chứng, kết quả kiểm tra..."
                        value={evidence}
                        onChange={(e) => setEvidence(e.target.value)}
                        rows={2}
                        disabled={creating}
                    />
                </div>
            </div>

            <div className="space-y-4">
                <div className="flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-slate-700">Phạm vi thu hồi</h3>
                    <span className="text-sm text-slate-500">
                        Đã chọn {includedItems.length}/{selectableItems.length} lô
                    </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    <div className="rounded-lg bg-blue-50 border border-blue-200 p-3 text-center">
                        <div className="text-2xl font-bold text-blue-700">{items.length}</div>
                        <div className="text-xs text-blue-600">Phạm vi truy vết</div>
                    </div>
                    <div className="rounded-lg bg-emerald-50 border border-emerald-200 p-3 text-center">
                        <div className="text-2xl font-bold text-emerald-700">{selectableItems.length}</div>
                        <div className="text-xs text-emerald-600">Có thể thu hồi</div>
                    </div>
                    <div className="rounded-lg bg-amber-50 border border-amber-200 p-3 text-center">
                        <div className="text-2xl font-bold text-amber-700">{includedItems.length}</div>
                        <div className="text-xs text-amber-600">Đã chọn</div>
                    </div>
                    <div className="rounded-lg bg-red-50 border border-red-200 p-3 text-center">
                        <div className="text-2xl font-bold text-red-700">{items.filter(i => i.isRecalled).length}</div>
                        <div className="text-xs text-red-600">Đã loại</div>
                    </div>
                </div>

                {selectableItems.length > 0 && (
                    <div className="flex items-center gap-2 pb-2 border-b border-slate-200">
                        <Checkbox
                            id="selectAll"
                            checked={allSelected}
                            onCheckedChange={handleToggleAll}
                            disabled={creating}
                        />
                        <Label htmlFor="selectAll" className="text-sm font-medium cursor-pointer">
                            Chọn tất cả
                        </Label>
                    </div>
                )}

                <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
                    <div className="relative flex-1">
                        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                        <Input
                            value={searchKeyword}
                            onChange={(e) => setSearchKeyword(e.target.value)}
                            placeholder="Tìm theo mã lô..."
                            className="pl-9"
                            disabled={creating}
                            aria-label="Tìm theo mã lô"
                        />
                    </div>
                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value as typeof statusFilter)}
                        disabled={creating}
                        aria-label="Lọc trạng thái"
                        className="h-11 rounded-lg border border-input bg-white px-3 text-sm text-slate-700 outline-none focus-visible:border-ring disabled:cursor-not-allowed disabled:opacity-50 sm:w-52"
                    >
                        {STATUS_FILTER_OPTIONS.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="border border-slate-200 rounded-lg overflow-hidden">
                    <div className="max-h-96 overflow-y-auto">
                        <table className="w-full text-sm">
                            <thead className="bg-slate-50 sticky top-0">
                                <tr>
                                    <th className="w-10 px-3 py-2 text-left"></th>
                                    <th className="w-14 px-3 py-2 text-left font-medium text-slate-700">STT</th>
                                    <th className="px-3 py-2 text-left font-medium text-slate-700">Mã lô</th>
                                    <th className="px-3 py-2 text-left font-medium text-slate-700">Trạng thái</th>
                                    <th className="px-3 py-2 text-left font-medium text-slate-700">Tổ chức nhận</th>
                                    <th className="px-3 py-2 text-left font-medium text-slate-700">Lý do loại</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {pagedItems.length === 0 ? (
                                    <tr>
                                        <td colSpan={6} className="px-3 py-8 text-center text-sm text-slate-500">
                                            {items.length === 0
                                                ? 'Chưa có lô hàng trong phạm vi thu hồi.'
                                                : 'Không tìm thấy lô hàng phù hợp với tìm kiếm/bộ lọc.'}
                                        </td>
                                    </tr>
                                ) : (
                                    pagedItems.map((item, index) => {
                                        const status = statusDisplay(item.status);
                                        const serialNumber = (safeCurrentPage - 1) * pageSize + index + 1;
                                    return (
                                        <tr key={item.shipmentId} className={item.isRecalled || item.isSplitParent ? 'bg-red-50/50' : ''}>
                                            <td className="px-3 py-2">
                                                <Checkbox
                                                    checked={item.included}
                                                    onCheckedChange={() => handleToggleIncluded(item.shipmentId)}
                                                    disabled={creating || item.isRecalled || item.isSplitParent}
                                                />
                                            </td>
                                            <td className="px-3 py-2 text-center text-slate-500">
                                                {serialNumber}
                                            </td>
                                            <td className="px-3 py-2 font-medium text-slate-900">
                                                {item.shipmentName}
                                            </td>
                                            <td className="px-3 py-2">
                                                <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${
                                                    status.tone === 'danger' ? 'bg-red-100 text-red-700' :
                                                    status.tone === 'success' ? 'bg-emerald-100 text-emerald-700' :
                                                    status.tone === 'warning' ? 'bg-amber-100 text-amber-700' :
                                                    status.tone === 'info' ? 'bg-blue-100 text-blue-700' :
                                                    'bg-slate-100 text-slate-700'
                                                }`}>
                                                    {status.label}
                                                </span>
                                            </td>
                                            <td className="px-3 py-2 text-slate-600">
                                                {item.organizationName || '—'}
                                            </td>
                                            <td className="px-3 py-2">
                                                {item.isRecalled || item.isSplitParent ? (
                                                    <span className="text-sm text-slate-500">
                                                        {item.exclusionReason}
                                                    </span>
                                                ) : !item.included ? (
                                                    <Input
                                                        placeholder="Nhập lý do loại..."
                                                        value={item.exclusionReason}
                                                        onChange={(e) =>
                                                            updateExclusionReason(
                                                                item.shipmentId,
                                                                e.target.value
                                                            )
                                                        }
                                                        className="text-xs"
                                                        disabled={creating}
                                                    />
                                                ) : (
                                                    <span className="text-sm text-slate-400">—</span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                }))}
                            </tbody>
                        </table>
                    </div>
                </div>

                <div className="flex flex-col gap-2 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between">
                    <span>
                        Hiển thị {rangeStart}–{rangeEnd} trong tổng số {totalFiltered} lô
                    </span>
                    <div className="flex flex-wrap items-center gap-2">
                        <select
                            value={pageSize}
                            onChange={(e) => setPageSize(Number(e.target.value))}
                            disabled={creating}
                            aria-label="Số dòng mỗi trang"
                            className="h-9 rounded-lg border border-input bg-white px-2 text-sm text-slate-700 outline-none focus-visible:border-ring disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {PAGE_SIZE_OPTIONS.map((option) => (
                                <option key={option} value={option}>
                                    {option}
                                </option>
                            ))}
                        </select>
                        <Button
                            variant="outline"
                            size="icon-sm"
                            onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                            disabled={creating || safeCurrentPage <= 1}
                            aria-label="Trang trước"
                        >
                            <ChevronLeft className="h-4 w-4" />
                        </Button>
                        <span className="min-w-16 text-center text-sm text-slate-700">
                            {safeCurrentPage} / {totalPages}
                        </span>
                        <Button
                            variant="outline"
                            size="icon-sm"
                            onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
                            disabled={creating || safeCurrentPage >= totalPages}
                            aria-label="Trang sau"
                        >
                            <ChevronRight className="h-4 w-4" />
                        </Button>
                    </div>
                </div>

                {!hasScope && generalReason.trim() && (
                    <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-red-700 text-sm">
                        <AlertCircle className="w-4 h-4 flex-shrink-0" />
                        <span>Vui lòng chọn ít nhất một lô để tạo yêu cầu thu hồi.</span>
                    </div>
                )}

                {excludedWithoutReason.length > 0 && (
                    <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-red-700 text-sm">
                        <AlertCircle className="w-4 h-4 flex-shrink-0" />
                        <span>
                            Các lô sau chưa có lý do loại:{' '}
                            {excludedWithoutReason.map((i) => i.shipmentName).join(', ')}
                        </span>
                    </div>
                )}
            </div>

            <div className="flex justify-end gap-2 pt-4 border-t border-slate-200">
                {onCancel && (
                    <Button variant="outline" onClick={onCancel} disabled={creating}>
                        Hủy
                    </Button>
                )}
                <Button
                    onClick={handleSubmit}
                    disabled={creating || !hasScope || excludedWithoutReason.length > 0 || !generalReason.trim()}
                >
                    {creating ? (
                        <>
                            <LoaderCircle className="w-4 h-4 mr-2 animate-spin" />
                            Đang tạo...
                        </>
                    ) : (
                        'Tạo yêu cầu thu hồi'
                    )}
                </Button>
            </div>
        </div>
    );
};

export default BulkRecallRequestForm;
