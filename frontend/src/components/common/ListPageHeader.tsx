import React from 'react';
import type { LucideIcon } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ListPageHeaderProps {
  /** Icon hiển thị trong ô bo góc bên cạnh tiêu đề. */
  icon: LucideIcon;
  /** Tiêu đề trang. */
  title: string;
  /** Mô tả dưới tiêu đề. */
  description?: string;
  /** Class tùy chỉnh cho ô nền icon (mặc định emerald theo bảng chuẩn API key). */
  iconBoxClassName?: string;
  /** Khu vực nút thao tác bên phải (vd: HelpButton, nút thêm mới). */
  actions?: React.ReactNode;
}

/**
 * Header trang danh sách chuẩn toàn dự án (lấy từ PartnerApiKeyListPage).
 * Gồm icon-box + tiêu đề + mô tả + khu action bên phải.
 */
export const ListPageHeader: React.FC<ListPageHeaderProps> = ({
  icon: Icon,
  title,
  description,
  iconBoxClassName,
  actions,
}) => {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <div className="flex items-center gap-2">
          <div
            className={cn(
              'p-2 rounded-lg text-emerald-600 dark:text-emerald-400',
              iconBoxClassName
            )}
          >
            <Icon className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{title}</h1>
        </div>
        {description && (
          <p className="text-sm text-muted-foreground mt-1">{description}</p>
        )}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
};