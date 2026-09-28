import React from 'react';
import { FileText, FileCode, FileSpreadsheet } from 'lucide-react';

import { Label } from '@/components/ui/label';

/** Kiểu định dạng xuất hồ sơ truy xuất. */
export type DossierExportFormat = 'pdf' | 'json' | 'csv';

/** Thuộc tính cho thành phần lựa chọn định dạng tệp hồ sơ. */
export interface DossierFormatSelectorProps {
  selectedFormat: DossierExportFormat;
  onSelectFormat: (format: DossierExportFormat) => void;
  disabled?: boolean;
}

/** Thành phần lựa chọn định dạng tệp xuất hồ sơ (PDF, JSON, CSV). */
export const DossierFormatSelector: React.FC<DossierFormatSelectorProps> = ({
  selectedFormat,
  onSelectFormat,
  disabled = false,
}) => {
  return (
    <div className="space-y-2">
      <Label className="text-sm font-semibold">Định dạng tệp xuất</Label>
      <div className="grid grid-cols-3 gap-2">
        <button
          type="button"
          disabled={disabled}
          onClick={() => onSelectFormat('pdf')}
          className={`p-2.5 rounded-lg border text-left transition-all flex flex-col justify-between ${
            selectedFormat === 'pdf'
              ? 'border-emerald-600 bg-emerald-50/50 dark:bg-emerald-950/20 ring-1 ring-emerald-600'
              : 'border-slate-200 hover:border-slate-300 dark:border-slate-800'
          }`}
        >
          <FileText
            className={`size-5 mb-1.5 ${
              selectedFormat === 'pdf' ? 'text-emerald-600' : 'text-slate-500'
            }`}
          />
          <div>
            <div className="font-semibold text-xs text-foreground">Hồ sơ PDF</div>
            <div className="text-[10px] text-muted-foreground">In ấn & nộp đối tác</div>
          </div>
        </button>

        <button
          type="button"
          disabled={disabled}
          onClick={() => onSelectFormat('json')}
          className={`p-2.5 rounded-lg border text-left transition-all flex flex-col justify-between ${
            selectedFormat === 'json'
              ? 'border-emerald-600 bg-emerald-50/50 dark:bg-emerald-950/20 ring-1 ring-emerald-600'
              : 'border-slate-200 hover:border-slate-300 dark:border-slate-800'
          }`}
        >
          <FileCode
            className={`size-5 mb-1.5 ${
              selectedFormat === 'json' ? 'text-emerald-600' : 'text-slate-500'
            }`}
          />
          <div>
            <div className="font-semibold text-xs text-foreground">Dữ liệu JSON</div>
            <div className="text-[10px] text-muted-foreground">Tích hợp phần mềm</div>
          </div>
        </button>

        <button
          type="button"
          disabled={disabled}
          onClick={() => onSelectFormat('csv')}
          className={`p-2.5 rounded-lg border text-left transition-all flex flex-col justify-between ${
            selectedFormat === 'csv'
              ? 'border-emerald-600 bg-emerald-50/50 dark:bg-emerald-950/20 ring-1 ring-emerald-600'
              : 'border-slate-200 hover:border-slate-300 dark:border-slate-800'
          }`}
        >
          <FileSpreadsheet
            className={`size-5 mb-1.5 ${
              selectedFormat === 'csv' ? 'text-emerald-600' : 'text-slate-500'
            }`}
          />
          <div>
            <div className="font-semibold text-xs text-foreground">Bảng tính CSV</div>
            <div className="text-[10px] text-muted-foreground">Phân tích số liệu</div>
          </div>
        </button>
      </div>
    </div>
  );
};
