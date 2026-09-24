import React from 'react';
import { Pencil, Trash2 } from 'lucide-react';

import { TableCell, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';

import type { ProfileTemplate } from '@/types/profileTemplate';

/** Thuộc tính cho dòng dữ liệu mẫu hồ sơ trong bảng danh sách. */
export interface ProfileTemplateTableRowProps {
  template: ProfileTemplate;
  index: number;
  isManager: boolean;
  onEdit: (id: string) => void;
  onDelete: (template: ProfileTemplate) => void;
}

/** Thành phần hiển thị một hàng trong bảng danh sách mẫu hồ sơ truy xuất. */
export const ProfileTemplateTableRow: React.FC<ProfileTemplateTableRowProps> = ({
  template,
  index,
  isManager,
  onEdit,
  onDelete,
}) => {
  const isDefault = Boolean(template.isDefault || template.default);

  return (
    <TableRow className="hover:bg-table-hover transition-colors">
      <TableCell className="text-center font-medium text-muted-foreground text-sm">
        {index + 1}
      </TableCell>

      <TableCell className="font-medium">
        <div className="flex items-center gap-2">
          <span className="text-foreground">{template.name}</span>
          {isDefault && (
            <Badge variant="success" className="text-[11px] px-2 py-0.5">
              Mặc định
            </Badge>
          )}
        </div>
      </TableCell>

      <TableCell>
        {template.partnerName ? (
          <span className="text-sm text-foreground">{template.partnerName}</span>
        ) : (
          <span className="text-xs text-muted-foreground italic">
            Dùng chung (Nhiều đối tác)
          </span>
        )}
      </TableCell>

      <TableCell>
        <Badge variant="outline" className="text-xs">
          {template.fields?.length || 0} trường
        </Badge>
      </TableCell>

      <TableCell className="text-sm text-muted-foreground">
        {template.createdAt
          ? new Date(template.createdAt).toLocaleDateString('vi-VN')
          : '—'}
      </TableCell>

      <TableCell className="text-right">
        <div className="flex items-center justify-end gap-1">
          {isManager && (
            <>
              <Button
                variant="ghost"
                size="icon-sm"
                onClick={() => onEdit(template.id)}
                title="Chỉnh sửa mẫu hồ sơ"
                aria-label={`Chỉnh sửa ${template.name}`}
              >
                <Pencil className="size-4 text-muted-foreground hover:text-foreground" />
              </Button>
              <Button
                variant="ghost"
                size="icon-sm"
                onClick={() => onDelete(template)}
                disabled={isDefault}
                title={isDefault ? 'Không thể xóa mẫu mặc định' : 'Xóa mẫu hồ sơ'}
                aria-label={`Xóa ${template.name}`}
              >
                <Trash2
                  className={`size-4 ${
                    isDefault
                      ? 'text-disabled cursor-not-allowed'
                      : 'text-destructive hover:opacity-80'
                  }`}
                />
              </Button>
            </>
          )}
        </div>
      </TableCell>
    </TableRow>
  );
};
