import React from 'react';
import { ShieldCheck } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';

interface EmptyAlertStateProps {
  hasFilters?: boolean;
  onResetFilters?: () => void;
}

/**
 * Giao diện hiển thị khi không có cảnh báo nào đang mở (TC-04).
 */
export const EmptyAlertState: React.FC<EmptyAlertStateProps> = ({
  hasFilters = false,
  onResetFilters,
}) => {
  return (
    <Card className="border-dashed border-emerald-200 bg-emerald-50/40">
      <CardContent className="flex flex-col items-center justify-center py-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 mb-4">
          <ShieldCheck className="h-8 w-8" />
        </div>
        <h3 className="text-lg font-semibold text-gray-900 mb-1">
          {hasFilters ? 'Không tìm thấy cảnh báo phù hợp' : 'Không có cảnh báo nào đang mở'}
        </h3>
        <p className="text-sm text-gray-500 max-w-md mb-4">
          {hasFilters
            ? 'Không có cảnh báo nào khớp với các tiêu chí tìm kiếm hoặc bộ lọc hiện tại của bạn.'
            : 'Tất cả các nguồn cảnh báo của tổ chức hiện đang ở trạng thái an toàn hoặc đã được giải quyết dứt điểm.'}
        </p>
        {hasFilters && onResetFilters && (
          <button
            type="button"
            onClick={onResetFilters}
            className="text-sm font-medium text-emerald-600 hover:text-emerald-700 hover:underline"
          >
            Đặt lại bộ lọc
          </button>
        )}
      </CardContent>
    </Card>
  );
};
