import React, { useState, useEffect } from 'react';
import { toast } from 'sonner';

import { Dialog, DialogContent } from '@/components/ui/dialog';
import { DossierPreviewHeader } from './preview/DossierPreviewHeader';
import { DossierPreviewContent } from './preview/DossierPreviewContent';
import { DossierPreviewFooter } from './preview/DossierPreviewFooter';
import { useDossierPreviewData } from './preview/useDossierPreviewData';

/** Thuộc tính truyền vào hộp thoại xem trước hồ sơ truy xuất. */
export interface DossierPreviewDialogProps {
  open: boolean;
  onClose: () => void;
  shipmentId?: string;
  shipmentName?: string;
  templateId?: string;
  templateName?: string;
  activeFormat?: 'pdf' | 'json' | 'csv';
  initialData?: Record<string, unknown> | null;
}

/** Hiển thị bản xem trước hồ sơ PDF, CSV hoặc JSON và các thao tác liên quan. */
export const DossierPreviewDialog: React.FC<DossierPreviewDialogProps> = ({
  open,
  onClose,
  shipmentId,
  shipmentName,
  templateId,
  templateName,
  activeFormat = 'pdf',
  initialData,
}) => {
  const [format, setFormat] = useState<'pdf' | 'json' | 'csv'>(activeFormat);
  const [copied, setCopied] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);

  const {
    loading,
    error,
    pdfBlob,
    pdfUrl,
    csvContent,
    jsonString,
  } = useDossierPreviewData({
    open,
    shipmentId,
    templateId,
    format,
    initialData,
  });

  useEffect(() => {
    if (open) {
      setFormat(activeFormat);
      setIsFullscreen(false);
    }
  }, [open, activeFormat]);

  const handleDownloadCurrent = () => {
    let blob: Blob | null = null;
    let fileName = `dossier_profile_${shipmentName || shipmentId}.${format}`;
    if (format === 'pdf' && pdfBlob) {
      blob = pdfBlob;
      fileName = `Ho_so_truy_xuat_${shipmentName || shipmentId}.pdf`;
    } else if (format === 'csv' && csvContent) {
      blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    } else if (format === 'json' && jsonString) {
      blob = new Blob([jsonString], { type: 'application/json;charset=utf-8;' });
    }

    if (blob) {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      toast.success(`Đã tải tệp ${format.toUpperCase()} về máy`);
    }
  };

  const handleCopyText = (content: string, typeName: string) => {
    if (!content) return;
    navigator.clipboard.writeText(content);
    setCopied(true);
    toast.success(`Đã sao chép nội dung ${typeName} vào bộ nhớ tạm`);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Dialog open={open} onOpenChange={(val) => !val && onClose()}>
      <DialogContent
        className={
          isFullscreen
            ? [
                '!fixed !inset-0 !z-50 !h-screen !max-h-none !w-screen !max-w-none',
                '!left-0 !top-0 !m-0 !translate-x-0 !translate-y-0 !rounded-none',
                '!p-4 flex flex-col bg-background shadow-2xl',
              ].join(' ')
            : [
                'flex h-[92vh] max-h-[94vh] w-[96vw] flex-col p-4 sm:max-w-[95vw]',
                'sm:p-5 md:max-w-5xl lg:max-w-6xl xl:max-w-7xl',
              ].join(' ')
        }
      >
        <DossierPreviewHeader
          format={format}
          setFormat={setFormat}
          templateName={templateName}
          shipmentName={shipmentName}
          hasInitialData={Boolean(initialData)}
          isFullscreen={isFullscreen}
          setIsFullscreen={setIsFullscreen}
        />

        <div className="flex-1 w-full h-full min-h-0 py-2 flex flex-col overflow-hidden">
          <DossierPreviewContent
            loading={loading}
            error={error}
            format={format}
            initialData={initialData}
            pdfUrl={pdfUrl}
            csvContent={csvContent}
            jsonString={jsonString}
            onRetry={() => setFormat((prev) => prev)}
          />
        </div>

        <DossierPreviewFooter
          format={format}
          pdfUrl={pdfUrl}
          csvContent={csvContent}
          jsonString={jsonString}
          copied={copied}
          loading={loading}
          hasInitialData={Boolean(initialData)}
          hasShipmentId={Boolean(shipmentId)}
          onCopyText={handleCopyText}
          onDownloadCurrent={handleDownloadCurrent}
          onClose={onClose}
        />
      </DialogContent>
    </Dialog>
  );
};

export default DossierPreviewDialog;
