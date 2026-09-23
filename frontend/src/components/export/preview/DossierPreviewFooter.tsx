import React from 'react';
import { DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import {
  Copy,
  Check,
  X,
  Download,
  ExternalLink,
  ShieldCheck,
} from 'lucide-react';

/** Props cho footer của hộp thoại xem trước hồ sơ */
export interface DossierPreviewFooterProps {
  format: 'pdf' | 'json' | 'csv';
  pdfUrl: string | null;
  csvContent: string | null;
  jsonString: string;
  copied: boolean;
  loading: boolean;
  hasInitialData: boolean;
  hasShipmentId: boolean;
  onCopyText: (content: string, typeName: string) => void;
  onDownloadCurrent: () => void;
  onClose: () => void;
}

/** Hiển thị bảo chứng và các thao tác của hộp thoại xem trước hồ sơ. */
export const DossierPreviewFooter: React.FC<DossierPreviewFooterProps> = ({
  format,
  pdfUrl,
  csvContent,
  jsonString,
  copied,
  loading,
  hasInitialData,
  hasShipmentId,
  onCopyText,
  onDownloadCurrent,
  onClose,
}) => {
  return (
    <DialogFooter className="sm:justify-between items-center gap-2 border-t pt-3 shrink-0">
      <div className="text-xs text-muted-foreground flex items-center gap-1.5">
        <ShieldCheck className="size-4 text-emerald-600 shrink-0" />
        <span>Nội dung xem trước trùng khớp 100% với tệp tải về</span>
      </div>

      <div className="flex items-center gap-2 flex-wrap">
        {format === 'pdf' && pdfUrl && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => window.open(pdfUrl, '_blank')}
            className="gap-1.5 text-xs"
          >
            <ExternalLink className="size-3.5" />
            <span>Mở tab mới</span>
          </Button>
        )}

        {format === 'csv' && csvContent && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onCopyText(csvContent, 'CSV')}
            className="gap-1.5 text-xs"
          >
            {copied ? <Check className="size-3.5 text-emerald-600" /> : <Copy className="size-3.5" />}
            <span>{copied ? 'Đã sao chép' : 'Sao chép CSV'}</span>
          </Button>
        )}

        {format === 'json' && jsonString && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onCopyText(jsonString, 'JSON')}
            className="gap-1.5 text-xs"
          >
            {copied ? <Check className="size-3.5 text-emerald-600" /> : <Copy className="size-3.5" />}
            <span>{copied ? 'Đã sao chép' : 'Sao chép'}</span>
          </Button>
        )}

        {!hasInitialData && hasShipmentId && (
          <Button
            type="button"
            size="sm"
            onClick={onDownloadCurrent}
            disabled={loading}
            className="gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold"
          >
            <Download className="size-3.5" />
            <span>Tải tệp này về máy ({format.toUpperCase()})</span>
          </Button>
        )}

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={onClose}
          className="gap-1 text-xs"
        >
          <X className="size-3.5" />
          <span>Đóng</span>
        </Button>
      </div>
    </DialogFooter>
  );
};
