import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Pencil, Eye, EyeOff } from 'lucide-react';
import type { ProductCategory } from '@/types/productCategory';

interface Props {
  categories: ProductCategory[];
  loading: boolean;
  onEdit: (category: ProductCategory) => void;
  onToggleActive: (id: string, currentActive: boolean) => void;
  /** Có quyền sửa/ẩn-hiện hay không (mặc định true để không phá các nơi gọi cũ). */
  canManage?: boolean;
}

export const ProductCategoryList = ({ categories, loading, onEdit, onToggleActive, canManage = true }: Props) => {
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
                {category.tempMin !== undefined && category.tempMax !== undefined ? (
                  <span className="text-xs text-muted-foreground">
                    {category.tempMin}–{category.tempMax}°C / {category.humidityMin ?? '?'}–{category.humidityMax ?? '?'}%
                  </span>
                ) : (
                  <span className="text-xs text-muted-foreground">Chưa khai báo</span>
                )}
              </TableCell>
              <TableCell>{category.description || '—'}</TableCell>
              <TableCell>
                <Badge variant={category.isActive ? 'default' : 'secondary'}>
                  {category.isActive ? 'Đang hoạt động' : 'Đã ẩn'}
                </Badge>
              </TableCell>
              {canManage && (
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon-sm" onClick={() => onEdit(category)} className="hover:bg-muted">
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