import React, {useState} from 'react';
import {toast} from 'sonner';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from '@/components/ui/dialog';
import {Button} from '@/components/ui/button';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {Input} from '@/components/ui/input';
import {LoaderCircle, AlertCircle, CheckCircle2} from 'lucide-react';
import {createBulkRecallRequest} from '@/api/recallApi';
import {StatusBadge} from '@/components/common/StatusBadge';
import type {ShipmentTraceDto} from '@/types/impactScopeTrace';

// Mapping trạng thái lô sang hiển thị
const LOT_STATUS_MAP: Record<string, { label: string; tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral' }> = {
    ACTIVATED: {label: 'Đã kích hoạt', tone: 'success'},
    DRAFT: {label: 'Dự thảo', tone: 'neutral'},
    RECALLED: {label: 'Đã thu hồi', tone: 'danger'},
    CODE_PRINTED: {label: 'Đã in mã', tone: 'info'},
    APPROVED: {label: 'Đã duyệt', tone: 'success'},
    PACKAGED: {label: 'Đã đóng gói', tone: 'info'},
    CANCELLED: {label: 'Đã hủy', tone: 'neutral'},
};

export interface BulkRecallRequestModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    productionLotId: string;
    productionLotName: string;
    shipments: ShipmentTraceDto[];
    onSuccess: (requestId: string) => void;
}

interface LotSelectionItem {
    shipmentId: string;
    shipmentName: string;
    status: string;
    organizationId: string;
    organizationName: string;
    isRecalled: boolean;
    included: boolean;
    exclusionReason: string;
}

/**
 * Modal tạo đề nghị thu hồi hàng loạt theo phạm vi ảnh hưởng (NCL-08-CN-011).
 *
 * Người dùng chọn/bỏ chọn lô từ kết quả truy vết, nhập lý do chung và lý do loại từng lô.
 */
export const BulkRecallRequestModal: React.FC<BulkRecallRequestModalProps> = ({
                                                                                  open,
                                                                                  onOpenChange,
                                                                                  productionLotId,
                                                                                  productionLotName,
                                                                                  shipments,
                                                                                  onSuccess,
                                                                              }) => {
    const [generalReason, setGeneralReason] = useState('');
    const [evidence, setEvidence] = useState('');
    const [creating, setCreating] = useState(false);

    // Khởi tạo danh sách lô từ kết quả truy vết
    // - Lô đã RECALLED: mặc định excluded, không cho chọn lại
    // - Lô bình thường: mặc định included
    // Lưu ý: organizationName lấy từ receivingOrganizations đầu tiên (nếu có)
    // Backend sẽ tự kiểm tra organization ownership, không tin frontend
    const initialItems: LotSelectionItem[] = shipments.map((ship) => {
        const status = ship.status || '';
        const isRecalled = status === 'RECALLED';
        // Lấy tên tổ chức đầu tiên từ danh sách tổ chức nhận lô
        const orgName = ship.receivingOrganizations?.[0]?.organizationName || '';
        return {
            shipmentId: ship.id,
            shipmentName: ship.name,
            status,
            organizationId: ship.receivingOrganizations?.[0]?.organizationId || '',
            organizationName: orgName,
            isRecalled,
            included: !isRecalled,
            exclusionReason: isRecalled
                ? 'Lô đã được thu hồi trước đó'
                : '',
        };
    });

    const [items, setItems] = useState<LotSelectionItem[]>(initialItems);

    // Reset state khi modal đóng/mở lại
    const handleOpenChange = (newOpen: boolean) => {
        if (!newOpen) {
            setGeneralReason('');
            setEvidence('');
            setItems(initialItems);
        }
        onOpenChange(newOpen);
    };

    // Đảo ngược lựa chọn include/exclude cho một lô
    const toggleInclude = (shipmentId: string) => {
        setItems((prev) =>
            prev.map((item) =>
                item.shipmentId === shipmentId && !item.isRecalled
                    ? {...item, included: !item.included}
                    : item
            )
        );
    };

    // Cập nhật lý do loại cho một lô
    const updateExclusionReason = (shipmentId: string, reason: string) => {
        setItems((prev) =>
            prev.map((item) =>
                item.shipmentId === shipmentId
                    ? {...item, exclusionReason: reason}
                    : item
            )
        );
    };

    // Lấy danh sách lô đã chọn (included)
    const includedItems = items.filter((item) => item.included);
    // Lấy danh sách lô bị loại
    const excludedItems = items.filter((item) => !item.included);

    // Kiểm tra có lô nào thiếu lý do loại không
    const excludedWithoutReason = excludedItems.filter(
        (item) => !item.isRecalled && !item.exclusionReason.trim()
    );

    // Kiểm tra scope có rỗng không
    const hasScope = includedItems.length > 0;

    // Validate trước khi submit
    const getValidationMessage = (): string | null => {
        if (!generalReason.trim()) {
            return 'Vui lòng nhập lý do chung cho đề nghị thu hồi.';
        }
        if (!hasScope) {
            return 'Vui lòng chọn ít nhất một lô để tạo đề nghị thu hồi.';
        }
        if (excludedWithoutReason.length > 0) {
            return `Các lô sau chưa có lý do loại: ${excludedWithoutReason.map((i) => i.shipmentName).join(', ')}`;
        }
        return null;
    };

    const handleSubmit = async () => {
        const validationMsg = getValidationMessage();
        if (validationMsg) {
            toast.error(validationMsg);
            return;
        }

        // Chuẩn bị payload theo API contract (CreateBulkRecallRequest)
        const includedShipmentIds = items
            .filter((item) => item.included && !item.isRecalled)
            .map((item) => item.shipmentId);

        const excludedShipments = items
            .filter((item) => !item.isRecalled && !item.included)
            .map((item) => ({
                shipmentId: item.shipmentId,
                exclusionReason: item.exclusionReason,
            }));

        setCreating(true);
        try {
            const response = await createBulkRecallRequest({
                productionLotId,
                reason: generalReason.trim(),
                evidence: evidence.trim() || undefined,
                includedShipmentIds,
                excludedShipments,
            });
            toast.success('Đề nghị thu hồi đã được tạo thành công');
            onSuccess(response.id);
        } catch (err: any) {
            const msg =
                err.response?.data?.message ||
                'Không thể tạo đề nghị thu hồi. Vui lòng thử lại.';
            toast.error(msg);
        } finally {
            setCreating(false);
        }
    };

    const toggleItem = (shipmentId: string) => {
        const item = items.find((i) => i.shipmentId === shipmentId);
        if (!item || item.isRecalled) return;
        toggleInclude(shipmentId);
    };

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogContent className="max-w-5xl max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle>Tạo đề nghị thu hồi theo phạm vi ảnh hưởng</DialogTitle>
                </DialogHeader>

                <div className="space-y-6">
                    {/* Thông tin lô nguồn */}
                    <div className="p-4 bg-slate-50 rounded-lg border">
                        <Label className="text-sm font-medium text-slate-700">
                            Lô sản xuất nguồn
                        </Label>
                        <p className="text-sm font-semibold text-slate-900 mt-1">
                            {productionLotName}
                        </p>
                    </div>

                    {/* Lý do chung */}
                    <div className="space-y-2">
                        <Label htmlFor="generalReason">
                            Lý do thu hồi <span className="text-red-500">*</span>
                        </Label>
                        <Textarea
                            id="generalReason"
                            placeholder="Nhập lý do chung cho đề nghị thu hồi..."
                            value={generalReason}
                            onChange={(e) => setGeneralReason(e.target.value)}
                            rows={3}
                            className="min-h-[80px]"
                        />
                    </div>

                    {/* Bằng chứng (tùy chọn) */}
                    <div className="space-y-2">
                        <Label htmlFor="evidence">Bằng chứng (tùy chọn)</Label>
                        <Textarea
                            id="evidence"
                            placeholder="Nhập bằng chứng, kết quả kiểm tra..."
                            value={evidence}
                            onChange={(e) => setEvidence(e.target.value)}
                            rows={2}
                        />
                    </div>

                    {/* Danh sách lô trong phạm vi */}
                    <div className="space-y-2">
                        <div className="flex items-center justify-between">
                            <Label>Danh sách lô trong phạm vi ảnh hưởng</Label>
                            <span className="text-xs text-slate-500">
                Đã chọn: {includedItems.length}/{items.length} lô
              </span>
                        </div>

                        <div className="border rounded-lg overflow-hidden">
                            <table className="w-full text-sm">
                                <thead className="bg-slate-100">
                                <tr>
                                    <th className="px-3 py-2 text-left">Chọn</th>
                                    <th className="px-3 py-2 text-left">Mã lô</th>
                                    <th className="px-3 py-2 text-left">Trạng thái</th>
                                    <th className="px-3 py-2 text-left">Tổ chức</th>
                                    <th className="px-3 py-2 text-left">Lý do loại</th>
                                </tr>
                                </thead>
                                <tbody>
                                {items.map((item) => (
                                    <tr key={item.shipmentId} className="border-t">
                                        <td className="px-3 py-2">
                                            {item.isRecalled ? (
                                                <div
                                                    className="w-4 h-4 rounded bg-gray-300 flex items-center justify-center">
                                                    <span className="text-gray-500">✕</span>
                                                </div>
                                            ) : (
                                                <button
                                                    type="button"
                                                    onClick={() => toggleItem(item.shipmentId)}
                                                    className={`w-4 h-4 rounded border flex items-center justify-center transition-colors ${
                                                        item.included
                                                            ? 'bg-blue-600 border-blue-600 text-white'
                                                            : 'bg-white border-gray-400'
                                                    }`}
                                                >
                                                    {item.included && <CheckCircle2 className="w-3 h-3"/>}
                                                </button>
                                            )}
                                        </td>
                                        <td className="px-3 py-2">
                                            <div className="flex items-center gap-2">
                          <span className="font-medium">
                            {item.shipmentName}
                          </span>
                                                {item.isRecalled && (
                                                    <span
                                                        className="text-xs bg-gray-200 text-gray-600 px-2 py-0.5 rounded-full">
                              Đã thu hồi
                            </span>
                                                )}
                                            </div>
                                        </td>
                                        <td className="px-3 py-2">
                                            <StatusBadge
                                                label={
                                                    LOT_STATUS_MAP[item.status]?.label || item.status
                                                }
                                                tone={
                                                    LOT_STATUS_MAP[item.status]?.tone || 'neutral'
                                                }
                                            />
                                        </td>
                                        <td className="px-3 py-2 text-sm text-slate-600">
                                            {item.organizationName || '—'}
                                        </td>
                                        <td className="px-3 py-2">
                                            {item.isRecalled ? (
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
                                                />
                                            ) : (
                                                <span className="text-sm text-slate-400">—</span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Validation warning - empty scope */}
                    {!hasScope && (
                        <div
                            className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-red-700 text-sm">
                            <AlertCircle className="w-4 h-4 flex-shrink-0"/>
                            <span>
                Vui lòng chọn ít nhất một lô để tạo đề nghị thu hồi.
              </span>
                        </div>
                    )}

                    {/* Validation warning - excluded without reason */}
                    {excludedWithoutReason.length > 0 && (
                        <div
                            className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-red-700 text-sm">
                            <AlertCircle className="w-4 h-4 flex-shrink-0"/>
                            <span>
                Các lô sau chưa có lý do loại:{' '}
                                {excludedWithoutReason.map((i) => i.shipmentName).join(', ')}
              </span>
                        </div>
                    )}
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={() => handleOpenChange(false)}>
                        Hủy
                    </Button>
                    <Button
                        onClick={handleSubmit}
                        disabled={creating || !hasScope || excludedWithoutReason.length > 0}
                    >
                        {creating ? (
                            <>
                                <LoaderCircle className="w-4 h-4 mr-2 animate-spin"/>
                                Đang tạo...
                            </>
                        ) : (
                            'Tạo đề nghị'
                        )}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default BulkRecallRequestModal;
