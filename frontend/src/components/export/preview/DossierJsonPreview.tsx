import React from 'react';
import { cn } from '@/lib/utils';

/** Props cho component xem trước dữ liệu JSON */
export interface DossierJsonPreviewProps {
  jsonString: string;
}

/**
 * Component hiển thị cấu trúc dữ liệu JSON hồ sơ truy xuất nguồn gốc.
 */
export const DossierJsonPreview: React.FC<DossierJsonPreviewProps> = ({ jsonString }) => {
  return (
    <div
      className={cn(
        'flex-1 w-full h-full min-h-0 flex flex-col rounded-lg border overflow-hidden',
        'bg-slate-950 text-slate-100',
      )}
    >
      <div
        className={cn(
          'flex items-center justify-between px-3 py-1.5 border-b border-slate-800',
          'bg-slate-900/60 text-xs shrink-0',
        )}
      >
        <span className="text-slate-400 font-mono">application/json</span>
        <span className="text-emerald-400 font-medium text-[11px]">
          Cấu trúc phân cấp chuẩn theo mẫu
        </span>
      </div>
      <div className="flex-1 min-h-0 overflow-auto p-4 font-mono text-xs leading-relaxed">
        <pre className="text-emerald-300 whitespace-pre">{jsonString}</pre>
      </div>
    </div>
  );
};
