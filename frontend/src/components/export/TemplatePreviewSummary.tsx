import React from 'react';
import { CheckCircle2, ChevronDown } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';

/** Thông tin mẫu hồ sơ đang được cấu hình để xem trước hoặc tải xuống. */
export interface TemplatePreviewInfo {
  name: string;
  partnerName?: string;
  isDefault?: boolean;
  selectedFieldsCount: number;
  selectedFieldKeys?: string[];
  mockData?: Record<string, unknown>;
}

/** Hiển thị tóm tắt mẫu hồ sơ được áp dụng trong chế độ xem trước. */
export const TemplatePreviewSummary: React.FC<{ info: TemplatePreviewInfo }> = ({ info }) => (
  <div className="space-y-2">
    <div className="flex items-center justify-between">
      <Label htmlFor="preview-template-name" className="text-sm font-semibold">
        Mẫu hồ sơ áp dụng
      </Label>
      {info.partnerName && (
        <Badge
          variant="outline"
          className={[
            'border-emerald-500/40 bg-emerald-50 text-emerald-700',
            'dark:bg-emerald-950/40 dark:text-emerald-300 text-[11px] font-semibold',
          ].join(' ')}
        >
          Đối tác: {info.partnerName}
        </Badge>
      )}
    </div>

    <div
      id="preview-template-name"
      aria-label="Mẫu hồ sơ áp dụng"
      className="flex h-10 w-full items-center justify-between rounded-md border bg-background px-3 py-2 text-sm shadow-xs"
    >
      <div className="flex items-center gap-1.5 truncate">
        <span className="font-semibold">{info.name || 'Mẫu hồ sơ mới'}</span>
        {info.partnerName && <span className="text-muted-foreground">({info.partnerName})</span>}
        {info.isDefault && (
          <span className="ml-1 rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-medium text-amber-800 dark:bg-amber-950 dark:text-amber-300">
            Mặc định
          </span>
        )}
      </div>
      <ChevronDown className="size-4 text-muted-foreground opacity-60" />
    </div>

    <div
      className={[
        'flex items-start gap-2 rounded-lg border border-emerald-200 bg-emerald-50/50 p-2.5',
        'text-xs text-muted-foreground dark:border-emerald-800/40 dark:bg-emerald-950/20',
      ].join(' ')}
    >
      <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-emerald-600 dark:text-emerald-400" />
      <span>
        Áp dụng mẫu <strong className="text-foreground">{info.name || 'Mẫu đang tạo'}</strong>
        {info.partnerName ? ` thiết kế cho đối tác ${info.partnerName}` : ''}. Hồ sơ xuất ra
        được lọc theo{' '}
        <strong className="font-semibold text-emerald-700 dark:text-emerald-300">
          {info.selectedFieldsCount} trường đã chọn
        </strong>
        .
      </span>
    </div>
  </div>
);
