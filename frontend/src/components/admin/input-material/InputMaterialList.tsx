import { useState } from 'react';
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
import { Switch } from '@/components/ui/switch';
import { Card, CardContent } from '@/components/ui/card';
import { Edit2, Trash2, Clock, ShieldAlert, ChevronLeft, ChevronRight } from 'lucide-react';
import { MATERIAL_GROUP_VARIANTS } from '@/enums/materialGroup';
import type { InputMaterial } from '@/types/inputMaterial';

interface Props {
  materials: InputMaterial[];
  loading: boolean;
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (newPage: number) => void;
  onEdit: (material: InputMaterial) => void;
  onToggleActive: (id: string, currentActive: boolean) => void;
  onDelete: (material: InputMaterial) => void;
  canManage: boolean;
}

export const InputMaterialList = ({
  materials,
  loading,
  page,
  totalPages,
  totalElements,
  onPageChange,
  onEdit,
  onToggleActive,
  onDelete,
  canManage,
}: Props) => {
  if (loading) {
    return (
      <Card className="shadow-sm">
        <CardContent className="p-8 text-center text-muted-foreground">
          <div className="flex flex-col items-center justify-center gap-2">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-emerald-600 border-t-transparent" />
            <span>Đang tải danh mục vật tư đầu vào...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (materials.length === 0) {
    return (
      <Card className="shadow-sm">
        <CardContent className="p-12 text-center text-muted-foreground">
          <div className="flex flex-col items-center justify-center gap-3">
            <ShieldAlert className="h-10 w-10 text-amber-500/80" />
            <p className="text-base font-medium">Không tìm thấy vật tư đầu vào phù hợp</p>
            <p className="text-sm">Vui lòng thử thay đổi bộ lọc hoặc thêm mới vật tư đầu vào.</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="shadow-sm overflow-hidden border-gray-200 dark:border-gray-800">
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader className="bg-gray-50/80 dark:bg-gray-900/50">
              <TableRow>
                <TableHead className="w-[60px] text-center font-semibold">STT</TableHead>
                <TableHead className="min-w-[180px] font-semibold">Tên vật tư & Hoạt chất</TableHead>
                <TableHead className="min-w-[150px] font-semibold">Nhóm vật tư</TableHead>
                <TableHead className="w-[100px] text-center font-semibold">Đơn vị</TableHead>
                <TableHead className="min-w-[140px] text-center font-semibold">Thời gian cách ly (PHI)</TableHead>
                <TableHead className="min-w-[160px] font-semibold">Nông sản áp dụng</TableHead>
                <TableHead className="min-w-[180px] font-semibold">Nguồn quy định</TableHead>
                <TableHead className="w-[110px] text-center font-semibold">Trạng thái</TableHead>
                {canManage && <TableHead className="w-[100px] text-center font-semibold">Thao tác</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {materials.map((item, index) => {
                const variant = MATERIAL_GROUP_VARIANTS[item.materialGroup];
                return (
                  <TableRow key={item.id} className="hover:bg-gray-50/50 dark:hover:bg-gray-900/30">
                    {/* STT */}
                    <TableCell className="text-center font-medium text-gray-500">
                      {page * 20 + index + 1}
                    </TableCell>

                    {/* Tên vật tư & Hoạt chất */}
                    <TableCell>
                      <div className="flex flex-col">
                        <span className="font-semibold text-gray-900 dark:text-gray-100">
                          {item.name}
                        </span>
                        {item.activeIngredient && (
                          <span className="text-xs text-muted-foreground">
                            Hoạt chất: <span className="italic">{item.activeIngredient}</span>
                          </span>
                        )}
                      </div>
                    </TableCell>

                    {/* Nhóm vật tư */}
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={`${variant.bgClass} ${variant.textClass} ${variant.borderClass} font-medium px-2.5 py-0.5`}
                      >
                        {variant.label}
                      </Badge>
                    </TableCell>

                    {/* Đơn vị tính */}
                    <TableCell className="text-center font-medium text-gray-700 dark:text-gray-300">
                      {item.unit}
                    </TableCell>

                    {/* Thời gian cách ly */}
                    <TableCell className="text-center">
                      <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-700 dark:text-amber-400 font-semibold text-xs border border-amber-200 dark:border-amber-800">
                        <Clock className="h-3.5 w-3.5" />
                        <span>{item.quarantineDays} ngày</span>
                      </div>
                    </TableCell>

                    {/* Nông sản áp dụng */}
                    <TableCell>
                      {item.applyToAllCrops ? (
                        <span className="text-xs font-medium text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/50 px-2 py-0.5 rounded">
                          Tất cả nông sản
                        </span>
                      ) : item.applicableCropTypes && item.applicableCropTypes.length > 0 ? (
                        <div className="flex flex-wrap gap-1">
                          {item.applicableCropTypes.map((c) => (
                            <Badge key={c.id} variant="secondary" className="text-[11px] px-1.5 py-0">
                              {c.name}
                            </Badge>
                          ))}
                        </div>
                      ) : (
                        <span className="text-xs text-muted-foreground">Chưa chỉ định</span>
                      )}
                    </TableCell>

                    {/* Nguồn quy định tham chiếu */}
                    <TableCell>
                      <span className="text-xs text-gray-600 dark:text-gray-400 line-clamp-2" title={item.referenceSource || ''}>
                        {item.referenceSource || '—'}
                      </span>
                    </TableCell>

                    {/* Trạng thái */}
                    <TableCell className="text-center">
                      {canManage ? (
                        <div className="flex items-center justify-center">
                          <Switch
                            checked={item.isActive}
                            onCheckedChange={() => onToggleActive(item.id, item.isActive)}
                            title={item.isActive ? 'Bấm để ngừng sử dụng' : 'Bấm để kích hoạt lại'}
                          />
                        </div>
                      ) : (
                        <Badge variant={item.isActive ? 'default' : 'secondary'}>
                          {item.isActive ? 'Đang dùng' : 'Ngừng dùng'}
                        </Badge>
                      )}
                    </TableCell>

                    {/* Thao tác */}
                    {canManage && (
                      <TableCell className="text-center">
                        <div className="flex items-center justify-center gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => onEdit(item)}
                            className="h-8 w-8 text-blue-600 hover:text-blue-700 hover:bg-blue-50 dark:hover:bg-blue-950/50"
                            title="Chỉnh sửa vật tư"
                          >
                            <Edit2 className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => onDelete(item)}
                            className="h-8 w-8 text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950/50"
                            title="Xóa vật tư"
                          >
                            <Trash2 className="h-4 w-4" />
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

        {/* Phân trang */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-900/30">
            <span className="text-xs text-muted-foreground">
              Hiển thị <span className="font-semibold text-gray-900 dark:text-gray-100">{materials.length}</span> / <span className="font-semibold text-gray-900 dark:text-gray-100">{totalElements}</span> vật tư
            </span>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => onPageChange(page - 1)}
                disabled={page === 0}
                className="h-8 px-2"
              >
                <ChevronLeft className="h-4 w-4 mr-1" /> Trước
              </Button>
              <span className="text-xs font-medium px-2">
                Trang {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => onPageChange(page + 1)}
                disabled={page >= totalPages - 1}
                className="h-8 px-2"
              >
                Sau <ChevronRight className="h-4 w-4 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
