import React from 'react';
import { Loader2, AlertCircle } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { DossierPdfPreview } from './DossierPdfPreview';
import { DossierCsvPreview } from './DossierCsvPreview';
import { DossierJsonPreview } from './DossierJsonPreview';

/** Thuộc tính cho nội dung hiển thị trong hộp thoại xem trước. */
export interface DossierPreviewContentProps {
  loading: boolean;
  error: string | null;
  format: 'pdf' | 'json' | 'csv';
  initialData?: Record<string, unknown> | null;
  pdfUrl: string | null;
  csvContent: string | null;
  jsonString: string;
  onRetry: () => void;
}

/** Hiển thị trạng thái tải, lỗi hoặc bản xem trước PDF, CSV và JSON. */
export const DossierPreviewContent: React.FC<DossierPreviewContentProps> = ({
  loading,
  error,
  format,
  initialData: _initialData,
  pdfUrl,
  csvContent,
  jsonString,
  onRetry,
}) => {
  if (loading) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center gap-3 text-muted-foreground">
        <Loader2 className="size-8 animate-spin text-emerald-600" />
        <div className="text-center">
          <p className="text-sm font-semibold text-foreground">
            Đang kết xuất tệp {format.toUpperCase()} thực tế...
          </p>
          <p className="text-xs text-muted-foreground mt-0.5">
            Hệ thống đang sinh dữ liệu và áp dụng bộ lọc trường của mẫu hồ sơ
          </p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center gap-3 text-muted-foreground p-6 text-center">
        <AlertCircle className="size-10 text-amber-500" />
        <div className="max-w-md">
          <p className="text-sm font-semibold text-foreground">Không thể tạo bản xem trước</p>
          <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{error}</p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={onRetry}
          className="mt-2 text-xs"
        >
          Thử tải lại
        </Button>
      </div>
    );
  }

  if (format === 'pdf') {
    return <DossierPdfPreview pdfUrl={pdfUrl} />;
  }

  if (format === 'csv') {
    return <DossierCsvPreview csvContent={csvContent} />;
  }

  return <DossierJsonPreview jsonString={jsonString} />;
};
