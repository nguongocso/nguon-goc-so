import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Pencil, Eye, EyeOff, ListChecks } from 'lucide-react';
import type { ProductCategory } from '@/types/productCategory';

interface Props {
  categories: ProductCategory[];
  loading: boolean;
  onEdit: (category: ProductCategory) => void;
  onToggleActive: (id: string, currentActive: boolean) => void;
  /** Có quyền sửa/ẩn-hiện hay không (mặc định true để không phá các nơi gọi cũ). */
  canManage?: boolean;
  /**
   * Bật/tắt bắt buộc kiểm nghiệm (NCL-09-CN-009).
   * Chỉ hiển thị cột khi cung cấp callback.
   */
  onToggleMandatory?: (category: ProductCategory, required: boolean) => void;
  /** Id category đang gọi API bật/tắt để khóa Switch, tránh gọi trùng. */
  togglingMandatoryId?: string | null;
  /** Mở dialog gán bộ chỉ tiêu kiểm nghiệm cho category. */
  onAssignCriteria?: (category: ProductCategory) => void;
}

export const ProductCategoryList = ({
  categories,
  loading,
  onEdit,
  onToggleActive,
  canManage = true,
  onToggleMandatory,
  togglingMandatoryId,
  onAssignCriteria,
}: Props) => {
  const showMandatoryColumn = !!onToggleMandatory;

  if (loading) return <div className="text-center py-8">Đang tải...</div>;
  if (!categories || categories.length === 0) return <div className="text-center py-8 text-muted-foreground">Không có loại nông sản nào.</div>;

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Tên</TableHead>
            <TableHead>Nhóm hàng</TableHead>
            <TableHead>Ngưỡng bảo quản</TableHead>
            <TableHead>Mô tả</TableHead>
            {showMandatoryColumn && <TableHead>Bắt buộc kiểm nghiệm</TableHead>}
            <TableHead>Trạng thái</TableHead>
            {canManage && <TableHead className="text-right">Thao tác</TableHead>}
          </TableRow>
        </TableHeader>
        <TableBody>
          {categories.map((category) => (
            <TableRow key={category.id}>
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
                <TableCell>
                  {canManage ? (
                    <Switch
                      checked={!!category.requiresInspection}
                      disabled={togglingMandatoryId === category.id}
                      onCheckedChange={(checked) => onToggleMandatory(category, checked)}
                      aria-label={`Bắt buộc kiểm nghiệm ${category.name}`}
                    />
                  ) : (
                    <Badge variant={category.requiresInspection ? 'default' : 'secondary'}>
                      {category.requiresInspection ? 'Bắt buộc' : 'Không'}
                    </Badge>
                  )}
                </TableCell>
              )}
              <TableCell>
                <Badge variant={category.isActive ? 'default' : 'secondary'}>
                  {category.isActive ? 'Đang hoạt động' : 'Đã ẩn'}
                </Badge>
              </TableCell>
              {canManage && (
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    {onAssignCriteria && (
                      <Button variant="ghost" size="icon-sm" onClick={() => onAssignCriteria(category)} title="Gán bộ chỉ tiêu kiểm nghiệm" className="hover:bg-muted">
                        <ListChecks className="h-4 w-4" />
                      </Button>
                    )}
                    <Button variant="ghost" size="icon-sm" onClick={() => onEdit(category)} className="hover:bg-muted" title="Sửa loại nông sản">
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon-sm" onClick={() => onToggleActive(category.id, category.isActive)} className="hover:bg-muted">
                      {category.isActive ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </Button>
                  </div>
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};
