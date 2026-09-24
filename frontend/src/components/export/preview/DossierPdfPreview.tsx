import React from 'react';

import { cn } from '@/lib/utils';

/** Thuộc tính cho thành phần xem trước tệp PDF. */
export interface DossierPdfPreviewProps {
  pdfUrl: string | null;
}

/** Thành phần hiển thị bản in PDF hồ sơ truy xuất nguồn gốc trong iframe. */
export const DossierPdfPreview: React.FC<DossierPdfPreviewProps> = ({ pdfUrl }) => {
  return (
    <div
      className={cn(
        'flex-1 w-full h-full min-h-0 flex flex-col rounded-lg border overflow-hidden',
        'bg-slate-200 dark:bg-slate-900 shadow-inner',
      )}
    >
      {pdfUrl ? (
        <iframe
          src={`${pdfUrl}#toolbar=1&navpanes=0&view=Fit`}
          className="w-full h-full min-h-[520px] border-0 flex-1 rounded-lg"
          title="Bản in PDF hồ sơ truy xuất"
        />
      ) : (
        <div className="flex-1 flex items-center justify-center text-xs text-muted-foreground">
          Chưa tải được tệp PDF
        </div>
      )}
    </div>
  );
};
