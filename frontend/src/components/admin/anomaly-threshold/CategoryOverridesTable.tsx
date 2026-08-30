import React, { useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Plus, Pencil, Trash2, Layers, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { deleteCategoryOverride } from '@/api/anomalyThresholdApi';
import type { AnomalyThresholdConfig } from '@/types/anomalyThreshold';

interface CategoryOverridesTableProps {
  overrides: AnomalyThresholdConfig[];
  onAddClick: () => void;
  onEditClick: (item: AnomalyThresholdConfig) => void;
  onRefresh: () => void;
  loading?: boolean;
}

export const CategoryOverridesTable: React.FC<CategoryOverridesTableProps> = ({
  overrides,
  onAddClick,
  onEditClick,
  onRefresh,
  loading = false,
}) => {
  const [deleteTarget, setDeleteTarget] = useState<AnomalyThresholdConfig | null>(null);
  const [deleting, setDeleting] = useState(false);

  const handleDeleteConfirm = async () => {
    if (!deleteTarget || !deleteTarget.id) return;

    try {
      setDeleting(true);
      await deleteCategoryOverride(deleteTarget.id);
      toast.success(`Đã xóa cấu hình ghi đè cho "${deleteTarget.productCategoryName}".`);
      setDeleteTarget(null);
      onRefresh();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể xóa cấu hình ghi đè');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-2">
          <Layers className="h-5 w-5 text-emerald-600" />
          <h3 className="text-base font-semibold">Cấu hình ghi đè theo loại nông sản</h3>
          <Badge variant="secondary" className="font-normal text-xs">
            {overrides.length} cấu hình
          </Badge>
        </div>

        <Button onClick={onAddClick} size="sm" className="bg-emerald-600 hover:bg-emerald-700 text-white">
          <Plus className="h-4 w-4 mr-1.5" />
          Thêm cấu hình theo loại
        </Button>
      </div>

      <div className="rounded-md border bg-white overflow-hidden shadow-sm">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/40">
              <TableHead className="font-semibold text-xs">Loại nông sản</TableHead>
              <TableHead className="font-semibold text-xs text-center">Quét / giờ</TableHead>
              <TableHead className="font-semibold text-xs text-center">Quét / ngày (24h)</TableHead>
              <TableHead className="font-semibold text-xs text-center">Khoảng cách tối đa</TableHead>
              <TableHead className="font-semibold text-xs text-center">Thời gian di chuyển</TableHead>
              <TableHead className="font-semibold text-xs text-center">Hạn kích hoạt</TableHead>
              <TableHead className="font-semibold text-xs text-right pr-4">Thao tác</TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={7} className="h-28 text-center text-muted-foreground">
                  <div className="flex items-center justify-center gap-2">
                    <Loader2 className="h-5 w-5 animate-spin text-emerald-600" />
                    <span>Đang tải danh sách...</span>
                  </div>
                </TableCell>
              </TableRow>
            ) : overrides.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="h-28 text-center text-muted-foreground">
                  <div className="flex flex-col items-center justify-center space-y-1">
                    <Layers className="h-7 w-7 text-muted-foreground/40 mb-1" />
                    <p className="font-medium text-sm">Chưa có cấu hình ghi đè nào</p>
                    <p className="text-xs">Tất cả loại nông sản đang sử dụng cấu hình mặc định toàn cục.</p>
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              overrides.map((item) => (
                <TableRow key={item.id || item.productCategoryId} className="hover:bg-muted/20">
                  <TableCell className="font-medium text-emerald-950">
                    {item.productCategoryName || 'Danh mục nông sản'}
                  </TableCell>
                  <TableCell className="text-center font-mono text-xs">
                    {item.maxScansPerHour} lượt
                  </TableCell>
                  <TableCell className="text-center font-mono text-xs font-semibold">
                    {item.maxScansPerDay} lượt
                  </TableCell>
                  <TableCell className="text-center font-mono text-xs">
                    {item.maxDistanceKmPer30Min} km
                  </TableCell>
                  <TableCell className="text-center font-mono text-xs">
                    {item.minTimeBetweenScansMinutes} phút
                  </TableCell>
                  <TableCell className="text-center font-mono text-xs">
                    {item.activationAgeDays} ngày
                  </TableCell>
                  <TableCell className="text-right pr-4">
                    <div className="flex items-center justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => onEditClick(item)}
                        className="h-8 w-8 text-muted-foreground hover:text-emerald-700 hover:bg-emerald-50"
                        title="Chỉnh sửa"
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setDeleteTarget(item)}
                        className="h-8 w-8 text-muted-foreground hover:text-destructive hover:bg-destructive/10"
                        title="Xóa cấu hình"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <AlertDialog open={Boolean(deleteTarget)} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa cấu hình ghi đè danh mục?</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc chắn muốn xóa cấu hình ghi đè cho loại nông sản "{deleteTarget?.productCategoryName}"? Sau khi xóa, loại nông sản này sẽ áp dụng cấu hình toàn cục.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleting}>Hủy</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              disabled={deleting}
              className="bg-destructive hover:bg-destructive/90 text-white"
            >
              {deleting ? 'Đang xóa...' : 'Xóa cấu hình'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
};

