import {Button} from '@/components/ui/button';
import {Switch} from '@/components/ui/switch';
import {TableCell, TableHead, TableRow} from '@/components/ui/table';
import {Pencil, Eye, EyeOff, ListChecks} from 'lucide-react';
import type {ProductCategory} from '@/types/productCategory';
import {DataTableShell} from '@/components/common/DataTableShell';
import {StatusBadge} from '@/components/common/StatusBadge';

interface Props {
    categories: ProductCategory[];
    loading: boolean;
    onEdit: (category: ProductCategory) => void;
    onToggleActive: (id: string, currentActive: boolean) => void;
    /** Có quyền sửa/ẩn-hiện hay không (mặc định true để không phá các nơi gọi cũ). */
    canManage?: boolean;
    /** Bắt đầu STT tính từ trang hiện tại (dùng cho phân trang). */
    startIndex?: number;
    /**
     * Bật/tắt bắt buộc kiểm nghiệm (NCL-09-CN-009).
     * Chỉ hiển thị cột khi cung cấp callback.
     */
    onToggleMandatory?: (category: ProductCategory, required: boolean) => void;
    /** Id category đang gọi API bật/tắt để khóa Switch, tránh gọi trùng. */
    togglingMandatoryId?: string | null;
    /** Điều hướng đến trang gán bộ chỉ tiêu kiểm nghiệm cho category. */
    onAssignCriteria?: (category: ProductCategory) => void;
}

export const ProductCategoryList = ({
                                        categories,
                                        loading,
                                        onEdit,
                                        onToggleActive,
                                        canManage = true,
                                        startIndex = 0,
    onToggleMandatory,
    togglingMandatoryId,
    onAssignCriteria,
}: Props) => {
    const showMandatoryColumn = !!onToggleMandatory;
    const colSpan = 6 + (showMandatoryColumn ? 1 : 0) + (canManage ? 1 : 0);

    const header = (
        <>
            <TableHead className="w-12 text-center">STT</TableHead>
            <TableHead>Tên</TableHead>
            <TableHead>Nhóm hàng</TableHead>
            <TableHead>Ngưỡng bảo quản</TableHead>
            <TableHead>Mô tả</TableHead>
            {showMandatoryColumn && <TableHead className="text-center">Bắt buộc kiểm nghiệm</TableHead>}
            <TableHead>Trạng thái</TableHead>
            {canManage && <TableHead className="text-center">Thao tác</TableHead>}
        </>
    );

    const body = categories.map((category, index) => (
        <TableRow key={category.id} className="hover:bg-muted/40 transition-colors">
            <TableCell className="text-center font-medium text-muted-foreground">
                {startIndex + index + 1}
            </TableCell>
            <TableCell className="font-medium">{category.name}</TableCell>
            <TableCell>{category.group}</TableCell>
            <TableCell>
                {(() => {
                    const hasTemp = category.tempMin != null && category.tempMax != null;
                    const hasHumidity = category.humidityMin != null && category.humidityMax != null;
                    if (!hasTemp && !hasHumidity) {
                        return <span className="text-xs text-muted-foreground">—</span>;
                    }
                    return (
                        <span className="text-xs text-muted-foreground">
                            {hasTemp ? `${category.tempMin}–${category.tempMax}°C` : '—°C'} /{' '}
                            {hasHumidity ? `${category.humidityMin}–${category.humidityMax}%` : '—%'}
                        </span>
                    );
                })()}
            </TableCell>
            <TableCell>{category.description || '—'}</TableCell>
            {showMandatoryColumn && (
                <TableCell className="text-center">
                    {canManage ? (
                        <div className="flex justify-center">
                            <Switch
                                checked={!!category.requiresInspection}
                                disabled={togglingMandatoryId === category.id}
                                onCheckedChange={(checked) => onToggleMandatory(category, checked)}
                                aria-label={`Bắt buộc kiểm nghiệm ${category.name}`}
                            />
                        </div>
                    ) : (
                        <div className="flex justify-center">
                            <StatusBadge
                                label={category.requiresInspection ? 'Bắt buộc' : 'Không'}
                                tone={category.requiresInspection ? 'warning' : 'neutral'}
                            />
                        </div>
                    )}
                </TableCell>
            )}
            <TableCell>
                <StatusBadge
                    label={category.isActive ? 'Đang hoạt động' : 'Đã ẩn'}
                    tone={category.isActive ? 'success' : 'neutral'}
                />
            </TableCell>
            {canManage && (
                <TableCell className="text-center">
                    <div className="flex justify-center gap-1">
                        {onAssignCriteria && (
                            <Button variant="ghost" size="icon-sm"
                                    onClick={() => onAssignCriteria(category)}
                                    title="Gán bộ chỉ tiêu kiểm nghiệm" className="hover:bg-muted">
                                    <ListChecks className="h-4 w-4"/>
                            </Button>
                        )}
                        <Button variant="ghost" size="icon-sm" onClick={() => onEdit(category)}
                                className="hover:bg-muted" title="Sửa loại nông sản">
                            <Pencil className="h-4 w-4"/>
                        </Button>
                        <Button variant="ghost" size="icon-sm"
                                onClick={() => onToggleActive(category.id, category.isActive)}
                                className="hover:bg-muted">
                            {category.isActive ? <EyeOff className="h-4 w-4"/> :
                                <Eye className="h-4 w-4"/>}
                        </Button>
                    </div>
                </TableCell>
            )}
        </TableRow>
    ));

    return (
        <DataTableShell
            header={header}
            body={body}
            loading={loading}
            empty={!loading && categories.length === 0}
            colSpan={colSpan}
            loadingMessage="Đang tải danh sách loại nông sản..."
            emptyMessage="Không có loại nông sản nào."
        />
    );
};
