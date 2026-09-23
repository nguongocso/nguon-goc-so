import React, { useState, useMemo } from 'react';
import { Table as TableIcon, Code2 } from 'lucide-react';
import { cn } from '@/lib/utils';

/** Dòng dữ liệu sau khi phân tích từ CSV */
export interface CsvParsedRow {
  isHeader?: boolean;
  isSection?: boolean;
  cells: string[];
}

/** Props cho component xem trước file CSV */
export interface DossierCsvPreviewProps {
  csvContent: string | null;
}

/**
 * Phân tích nội dung văn bản CSV thành danh sách các hàng có định dạng (tiêu đề, nhóm mục, ô dữ liệu).
 */
export const parseCsvText = (text: string): CsvParsedRow[] => {
  const lines = text.split(/\r?\n/);
  const result: CsvParsedRow[] = [];

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) continue;

    if (line.startsWith('#')) {
      result.push({
        isSection: true,
        cells: [line.replace(/^#\s*/, '')],
      });
      continue;
    }

    const cells: string[] = [];
    let current = '';
    let insideQuote = false;

    for (let i = 0; i < line.length; i++) {
      const char = line[i];
      if (char === '"') {
        if (insideQuote && line[i + 1] === '"') {
          current += '"';
          i++;
        } else {
          insideQuote = !insideQuote;
        }
      } else if (char === ',' && !insideQuote) {
        cells.push(current.trim());
        current = '';
      } else {
        current += char;
      }
    }
    cells.push(current.trim());

    const isHeaderRow =
      cells.includes('Nhóm thông tin') ||
      cells.includes('STT') ||
      cells.includes('Trường dữ liệu');

    result.push({
      isHeader: isHeaderRow,
      cells,
    });
  }

  return result;
};

/**
 * Component hiển thị bản xem trước tệp CSV dưới dạng bảng dữ liệu hoặc định dạng văn bản thô.
 */
export const DossierCsvPreview: React.FC<DossierCsvPreviewProps> = ({ csvContent }) => {
  const [csvViewMode, setCsvViewMode] = useState<'table' | 'raw'>('table');

  const csvParsed = useMemo(() => {
    return csvContent ? parseCsvText(csvContent) : [];
  }, [csvContent]);

  return (
    <div className="flex-1 w-full h-full min-h-0 flex flex-col rounded-lg border overflow-hidden bg-background">
      <div className="flex items-center justify-between px-3 py-2 border-b bg-muted/40 text-xs shrink-0">
        <div className="flex items-center gap-2">
          <span className="font-semibold text-muted-foreground">Chế độ xem:</span>
          <div className="inline-flex rounded-md border bg-background p-0.5">
            <button
              type="button"
              onClick={() => setCsvViewMode('table')}
              className={`flex items-center gap-1 px-2.5 py-1 rounded text-xs transition-colors ${
                csvViewMode === 'table'
                  ? 'bg-muted font-semibold text-foreground'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              <TableIcon className="size-3" />
              Bảng tính
            </button>
            <button
              type="button"
              onClick={() => setCsvViewMode('raw')}
              className={`flex items-center gap-1 px-2.5 py-1 rounded text-xs transition-colors ${
                csvViewMode === 'raw'
                  ? 'bg-muted font-semibold text-foreground'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              <Code2 className="size-3" />
              Dữ liệu thô
            </button>
          </div>
        </div>
        <div className="text-[11px] text-muted-foreground hidden sm:block">
          Chuẩn mã hóa UTF-8 BOM cho Microsoft Excel
        </div>
      </div>

      {csvViewMode === 'table' ? (
        <div className="flex-1 min-h-0 overflow-auto p-2">
          <table className="w-full text-xs border-collapse border border-slate-200 dark:border-slate-800">
            <tbody>
              {csvParsed.map((row, rIdx) => {
                if (row.isSection) {
                  return (
                    <tr key={rIdx} className="bg-emerald-50/80 dark:bg-emerald-950/40 border-b">
                      <td
                        colSpan={4}
                        className={cn(
                          'px-3 py-2 font-bold text-emerald-800 dark:text-emerald-300',
                          'text-xs uppercase tracking-wide',
                        )}
                      >
                        {row.cells[0]}
                      </td>
                    </tr>
                  );
                }

                if (row.isHeader) {
                  return (
                    <tr key={rIdx} className="bg-slate-100 dark:bg-slate-800/80 border-b font-semibold">
                      {row.cells.map((cell, cIdx) => (
                        <th
                          key={cIdx}
                          className={cn(
                            'px-3 py-2 text-left border-r border-slate-200 dark:border-slate-700',
                            'last:border-r-0 font-semibold text-foreground',
                          )}
                        >
                          {cell}
                        </th>
                      ))}
                    </tr>
                  );
                }

                return (
                  <tr
                    key={rIdx}
                    className="border-b border-slate-100 dark:border-slate-800/60 hover:bg-muted/40 transition-colors"
                  >
                    {row.cells.map((cell, cIdx) => (
                      <td
                        key={cIdx}
                        className={cn(
                          'px-3 py-1.5 border-r border-slate-100 dark:border-slate-800',
                          'last:border-r-0 text-foreground whitespace-pre-wrap',
                        )}
                      >
                        {cell}
                      </td>
                    ))}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="flex-1 min-h-0 overflow-auto p-3 bg-slate-950 text-slate-100 font-mono text-xs rounded-b-lg">
          <pre className="whitespace-pre">{csvContent || 'Không có dữ liệu CSV'}</pre>
        </div>
      )}
    </div>
  );
};
