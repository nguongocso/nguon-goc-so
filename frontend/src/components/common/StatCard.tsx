import React from 'react';
import type { LucideIcon } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';

interface StatCardProps {
  /** Nhãn mô tả thẻ (vd: "Tổng số khóa API"). */
  label: string;
  /** Giá trị số hiển thị. */
  value: React.ReactNode;
  /** Icon hiển thị trong ô tròn bên phải. */
  icon: LucideIcon;
  /** Class cho giá trị (vd: màu emerald/rose mặc định nếu cần). */
  valueClassName?: string;
  /** Class cho ô tròn nền icon (vd: "bg-emerald-500/10 text-emerald-600"). */
  iconClassName?: string;
}

/**
 * Thẻ thống kê tổng quan chuẩn toàn dự án (lấy từ 3 card của PartnerApiKeyListPage).
 * Đặt trong lưới (vd: `grid sm:grid-cols-3 gap-4`) để tạo nhóm thẻ.
 */
export const StatCard: React.FC<StatCardProps> = ({
  label,
  value,
  icon: Icon,
  valueClassName,
  iconClassName,
}) => {
  return (
    <Card className="bg-card">
      <CardContent className="p-4 flex items-center justify-between">
        <div>
          <p className="text-xs font-medium text-muted-foreground">{label}</p>
          <div className={cn('text-2xl font-bold mt-1 text-foreground', valueClassName)}>
            {value}
          </div>
        </div>
        <div
          className={cn(
            'p-3 rounded-full bg-blue-500/10 text-blue-600 dark:text-blue-400',
            iconClassName
          )}
        >
          <Icon className="w-5 h-5" />
        </div>
      </CardContent>
    </Card>
  );
};