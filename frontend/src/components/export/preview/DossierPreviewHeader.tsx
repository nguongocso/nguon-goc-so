import React from 'react';
import {
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import {
  FileJson,
  FileText,
  FileSpreadsheet,
  Maximize2,
  Minimize2,
} from 'lucide-react';
import { cn } from '@/lib/utils';

/** Props cho header của hộp thoại xem trước hồ sơ */
export interface DossierPreviewHeaderProps {
  format: 'pdf' | 'json' | 'csv';
  setFormat: (format: 'pdf' | 'json' | 'csv') => void;
  templateName?: string;
  shipmentName?: string;
  hasInitialData: boolean;
  isFullscreen: boolean;
  setIsFullscreen: (fn: (prev: boolean) => boolean) => void;
}

/** Hiển thị tiêu đề, đối tượng, định dạng và chế độ toàn màn hình. */
export const DossierPreviewHeader: React.FC<DossierPreviewHeaderProps> = ({
  format,
  setFormat,
  templateName,
  shipmentName,
  hasInitialData,
  isFullscreen,
  setIsFullscreen,
}) => {
  return (
    <DialogHeader className="space-y-1.5 pb-2.5 border-b shrink-0">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pr-8">
        <div className="flex items-center gap-2.5">
          <div
            className={cn(
              'p-2 rounded-lg bg-emerald-50 text-emerald-600',
              'dark:bg-emerald-950/40 dark:text-emerald-400 shrink-0',
            )}
          >
            {format === 'pdf' ? (
              <FileText className="size-5" />
            ) : format === 'csv' ? (
              <FileSpreadsheet className="size-5" />
            ) : (
              <FileJson className="size-5" />
            )}
          </div>
          <div>
            <DialogTitle className="text-base sm:text-lg font-bold text-foreground">
              Bản xem trước xuất hồ sơ
            </DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground mt-0.5">
              Mẫu áp dụng: <strong className="text-foreground">{templateName || 'Mặc định'}</strong>
              {shipmentName && (
                <>
                  {' '}&bull; Lô hàng: <span className="font-semibold text-foreground">{shipmentName}</span>
                </>
              )}
            </DialogDescription>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {!hasInitialData && (
            <div className="flex items-center gap-1 bg-muted/70 p-1 rounded-lg border text-xs">
              <button
                type="button"
                onClick={() => setFormat('pdf')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md font-medium transition-all ${
                  format === 'pdf'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <FileText className="size-3.5 text-emerald-600" />
                <span>Bản in PDF</span>
              </button>

              <button
                type="button"
                onClick={() => setFormat('csv')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md font-medium transition-all ${
                  format === 'csv'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <FileSpreadsheet className="size-3.5 text-emerald-600" />
                <span>Bảng CSV</span>
              </button>

              <button
                type="button"
                onClick={() => setFormat('json')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md font-medium transition-all ${
                  format === 'json'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <FileJson className="size-3.5 text-emerald-600" />
                <span>Dữ liệu JSON</span>
              </button>
            </div>
          )}

          <Button
            type="button"
            variant="outline"
            size="icon"
            onClick={() => setIsFullscreen((prev) => !prev)}
            className="h-8 w-8 text-muted-foreground hover:text-foreground hidden sm:inline-flex shrink-0"
            title={isFullscreen ? 'Thu nhỏ giao diện' : 'Phóng to toàn màn hình'}
          >
            {isFullscreen ? <Minimize2 className="size-4" /> : <Maximize2 className="size-4" />}
          </Button>
        </div>
      </div>
    </DialogHeader>
  );
};
