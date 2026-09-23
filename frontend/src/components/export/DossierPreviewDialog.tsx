import React, { useState, useEffect, useRef } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import {
  Loader2,
  Copy,
  Check,
  X,
  FileJson,
  FileText,
  FileSpreadsheet,
  Download,
  ExternalLink,
  ShieldCheck,
  AlertCircle,
  Table as TableIcon,
  Code2,
  Maximize2,
  Minimize2,
} from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '@/hooks/useAuth';
import { exportDossier } from '@/api/dossierApi';
import { exportShipmentWithTemplate } from '@/api/exportApi';
import { getOpenDataPreview, previewTemplatePdf } from '@/api/profileTemplateApi';
import { convertDossierDataToCsv } from '@/utils/dossierCsvConverter';

export interface DossierPreviewDialogProps {
  open: boolean;
  onClose: () => void;
  shipmentId?: string;
  shipmentName?: string;
  templateId?: string;
  templateName?: string;
  activeFormat?: 'pdf' | 'json' | 'csv';
  initialData?: Record<string, unknown> | null;
  selectedFieldKeys?: string[];
  partnerName?: string | null;
}

interface CsvParsedRow {
  isHeader?: boolean;
  isSection?: boolean;
  cells: string[];
}

export const DossierPreviewDialog: React.FC<DossierPreviewDialogProps> = ({
  open,
  onClose,
  shipmentId,
  shipmentName,
  templateId,
  templateName,
  activeFormat = 'pdf',
  initialData,
  selectedFieldKeys,
  partnerName,
}) => {
  const { user } = useAuth();
  const organizationId = user?.organizationId || '';

  const [format, setFormat] = useState<'pdf' | 'json' | 'csv'>(activeFormat);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);

  // PDF state
  const [pdfBlob, setPdfBlob] = useState<Blob | null>(null);
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);

  // CSV state
  const [csvContent, setCsvContent] = useState<string | null>(null);
  const [csvParsed, setCsvParsed] = useState<CsvParsedRow[]>([]);
  const [csvViewMode, setCsvViewMode] = useState<'table' | 'raw'>('table');

  // JSON state
  const [jsonString, setJsonString] = useState<string>(
    initialData ? JSON.stringify(initialData, null, 2) : ''
  );

  // Lưu trữ tham chiếu URL để giải phóng bộ nhớ khi đóng/chuyển
  const currentPdfUrlRef = useRef<string | null>(null);

  // Đồng bộ format ban đầu khi mở dialog
  useEffect(() => {
    if (open) {
      setFormat(activeFormat);
      setError(null);
      setIsFullscreen(false);
    }
  }, [open, activeFormat]);

  // Giải phóng URL đối tượng blob khi hủy modal hoặc đổi URL
  useEffect(() => {
    return () => {
      if (currentPdfUrlRef.current) {
        URL.revokeObjectURL(currentPdfUrlRef.current);
        currentPdfUrlRef.current = null;
      }
    };
  }, []);

  // Hàm parse nội dung CSV thành bảng trực quan
  const parseCsvText = (text: string): CsvParsedRow[] => {
    const lines = text.split(/\r?\n/);
    const result: CsvParsedRow[] = [];

    for (const rawLine of lines) {
      const line = rawLine.trim();
      if (!line) continue;

      // Dòng chú thích / tiêu đề phần bắt đầu bằng #
      if (line.startsWith('#')) {
        result.push({
          isSection: true,
          cells: [line.replace(/^#\s*/, '')],
        });
        continue;
      }

      // Tách ô bằng dấu phẩy có xử lý dấu ngoặc kép
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

      // Phán đoán dòng tiêu đề cột
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

  // Nạp dữ liệu xem trước theo đúng định dạng đang chọn
  useEffect(() => {
    if (!open) {
      if (currentPdfUrlRef.current) {
        URL.revokeObjectURL(currentPdfUrlRef.current);
        currentPdfUrlRef.current = null;
      }
      setPdfUrl(null);
      setPdfBlob(null);
      setCsvContent(null);
      setCsvParsed([]);
      if (!initialData) {
        setJsonString('');
      }
      setError(null);
      return;
    }

    // Thiết lập dữ liệu tĩnh JSON và CSV nếu có initialData
    if (initialData) {
      setJsonString(JSON.stringify(initialData, null, 2));
      const csv = convertDossierDataToCsv(initialData);
      setCsvContent(csv);
      setCsvParsed(parseCsvText(csv));
    }

    let isMounted = true;

    const fetchPreviewData = async () => {
      setLoading(true);
      setError(null);

      try {
        if (format === 'pdf') {
          let blob: Blob;
          if (shipmentId) {
            // Lấy đúng file PDF thực tế của lô hàng do backend tạo ra
            blob = await exportDossier(shipmentId, templateId);
          } else {
            // Chế độ thiết kế mẫu hồ sơ: nạp PDF xem trước từ backend theo các trường đã chọn
            blob = await previewTemplatePdf(organizationId, {
              name: templateName || 'Mẫu hồ sơ mới',
              partnerName: partnerName || undefined,
              selectedFieldKeys,
              shipmentId,
            });
          }

          if (!isMounted) return;

          if (currentPdfUrlRef.current) {
            URL.revokeObjectURL(currentPdfUrlRef.current);
          }
          const url = URL.createObjectURL(blob);
          currentPdfUrlRef.current = url;
          setPdfBlob(blob);
          setPdfUrl(url);
        } else if (format === 'csv') {
          if (!csvContent && shipmentId) {
            // Lấy đúng file CSV thực tế
            const blob = await exportShipmentWithTemplate(shipmentId, templateId, 'csv');
            if (!isMounted) return;

            const text = await blob.text();
            setCsvContent(text);
            setCsvParsed(parseCsvText(text));
          }
        } else {
          // Lấy dữ liệu JSON chuẩn nếu chưa có initialData
          if (!initialData && shipmentId) {
            const res = await getOpenDataPreview(shipmentId, templateId);
            if (!isMounted) return;

            setJsonString(JSON.stringify(res, null, 2));
          }
        }
      } catch (err: unknown) {
        if (!isMounted) return;
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
          (err as { message?: string })?.message ||
          'Không thể khởi tạo bản xem trước hồ sơ';
        setError(msg);
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchPreviewData();

    return () => {
      isMounted = false;
    };
  }, [open, shipmentId, templateId, format, initialData, organizationId, partnerName, selectedFieldKeys, templateName]);

  // Xử lý tải file trực tiếp từ modal
  const handleDownloadCurrent = () => {
    const rawTemplateName = (templateName || shipmentName || shipmentId || 'mau_ho_so').replace(/\s+/g, '_');
    if (format === 'pdf') {
      if (pdfBlob) {
        const url = URL.createObjectURL(pdfBlob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Ho_so_truy_xuat_${rawTemplateName}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        toast.success('Đã tải tệp PDF về máy');
      } else if (pdfUrl) {
        const a = document.createElement('a');
        a.href = pdfUrl;
        a.download = `Ho_so_truy_xuat_${rawTemplateName}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        toast.success('Đã tải tệp PDF về máy');
      }
    } else if (format === 'csv' && csvContent) {
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `dossier_profile_${rawTemplateName}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      toast.success('Đã tải tệp CSV về máy');
    } else if (format === 'json' && jsonString) {
      const blob = new Blob([jsonString], { type: 'application/json;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `dossier_profile_${rawTemplateName}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      toast.success('Đã tải tệp JSON về máy');
    }
  };

  // Sao chép nội dung text (JSON hoặc CSV)
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
            ? '!fixed !inset-0 !z-50 !w-screen !h-screen !max-w-none !max-h-none !translate-x-0 !translate-y-0 !top-0 !left-0 !rounded-none !p-4 !m-0 flex flex-col bg-background shadow-2xl'
            : 'w-[96vw] sm:max-w-[95vw] md:max-w-5xl lg:max-w-6xl xl:max-w-7xl h-[92vh] max-h-[94vh] flex flex-col p-4 sm:p-5'
        }
      >
        {/* Header */}
        <DialogHeader className="space-y-1.5 pb-2.5 border-b shrink-0">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pr-8">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400 shrink-0">
                {format === 'pdf' ? (
                  <FileText className="size-5" />
                ) : format === 'csv' ? (
                  <FileSpreadsheet className="size-5" />
                ) : (
                  <FileJson className="size-5" />
                )}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <DialogTitle className="text-base sm:text-lg font-bold text-foreground">
                    Bản xem trước xuất hồ sơ
                  </DialogTitle>
                </div>
                <DialogDescription className="text-xs text-muted-foreground mt-0.5">
                  Mẫu áp dụng: <strong className="text-foreground">{templateName || 'Mặc định'}</strong>
                  {shipmentName ? (
                    <>
                      {' '}&bull; Lô hàng: <span className="font-semibold text-foreground">{shipmentName}</span>
                    </>
                  ) : (
                    <>
                      {' '}&bull; Lô hàng: <span className="font-semibold text-foreground">SHIP-MOCK-2026-DEMO</span>
                    </>
                  )}
                </DialogDescription>
              </div>
            </div>

            {/* Bộ chuyển đổi định dạng xem trước & nút toàn màn hình */}
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1 bg-muted/70 p-1 rounded-lg border text-xs">
                <button
                  type="button"
                  onClick={() => setFormat('pdf')}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md font-medium transition-all ${format === 'pdf'
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
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md font-medium transition-all ${format === 'csv'
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
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md font-medium transition-all ${format === 'json'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                    }`}
                >
                  <FileJson className="size-3.5 text-emerald-600" />
                  <span>Dữ liệu JSON</span>
                </button>
              </div>

              {/* Nút phóng to / thu nhỏ toàn màn hình */}
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={() => setIsFullscreen(!isFullscreen)}
                className="h-8 w-8 text-muted-foreground hover:text-foreground hidden sm:inline-flex shrink-0"
                title={isFullscreen ? 'Thu nhỏ giao diện' : 'Phóng to toàn màn hình'}
              >
                {isFullscreen ? <Minimize2 className="size-4" /> : <Maximize2 className="size-4" />}
              </Button>
            </div>
          </div>
        </DialogHeader>

        {/* Thân hiển thị nội dung xem trước */}
        <div className="flex-1 w-full h-full min-h-0 py-2 flex flex-col overflow-hidden">
          {loading ? (
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
          ) : error ? (
            <div className="flex-1 flex flex-col items-center justify-center gap-3 text-muted-foreground p-6 text-center">
              <AlertCircle className="size-10 text-amber-500" />
              <div className="max-w-md">
                <p className="text-sm font-semibold text-foreground">Không thể tạo bản xem trước</p>
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{error}</p>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setFormat((prev) => (prev === 'pdf' ? 'pdf' : prev))}
                className="mt-2 text-xs"
              >
                Thử tải lại
              </Button>
            </div>
          ) : format === 'pdf' ? (
            /* =================== XEM TRƯỚC PDF THẬT 100% ĐỒNG BỘ =================== */
            pdfUrl ? (
              <div className="flex-1 w-full h-full min-h-0 flex flex-col rounded-lg border overflow-hidden bg-slate-200 dark:bg-slate-900 shadow-inner">
                <iframe
                  src={`${pdfUrl}#toolbar=1&navpanes=0&view=Fit`}
                  className="w-full h-full min-h-[520px] border-0 flex-1 rounded-lg"
                  title="Bản in PDF hồ sơ truy xuất"
                />
              </div>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center gap-2 text-xs text-muted-foreground">
                <Loader2 className="size-6 animate-spin text-emerald-600" />
                <span>Đang kết xuất bản in PDF...</span>
              </div>
            )
          ) : format === 'csv' ? (
            /* =================== XEM TRƯỚC CSV THẬT 100% =================== */
            <div className="flex-1 w-full h-full min-h-0 flex flex-col rounded-lg border overflow-hidden bg-background">
              {/* Thanh công cụ xem CSV */}
              <div className="flex items-center justify-between px-3 py-2 border-b bg-muted/40 text-xs shrink-0">
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-muted-foreground">Chế độ xem:</span>
                  <div className="inline-flex rounded-md border bg-background p-0.5">
                    <button
                      type="button"
                      onClick={() => setCsvViewMode('table')}
                      className={`flex items-center gap-1 px-2.5 py-1 rounded text-xs transition-colors ${csvViewMode === 'table'
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
                      className={`flex items-center gap-1 px-2.5 py-1 rounded text-xs transition-colors ${csvViewMode === 'raw'
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
                                className="px-3 py-2 font-bold text-emerald-800 dark:text-emerald-300 text-xs uppercase tracking-wide"
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
                                  className="px-3 py-2 text-left border-r border-slate-200 dark:border-slate-700 last:border-r-0 font-semibold text-foreground"
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
                                className="px-3 py-1.5 border-r border-slate-100 dark:border-slate-800 last:border-r-0 text-foreground whitespace-pre-wrap"
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
          ) : (
            /* =================== XEM TRƯỚC JSON THẬT 100% =================== */
            <div className="flex-1 w-full h-full min-h-0 flex flex-col rounded-lg border overflow-hidden bg-slate-950 text-slate-100">
              <div className="flex items-center justify-between px-3 py-1.5 border-b border-slate-800 bg-slate-900/60 text-xs shrink-0">
                <span className="text-slate-400 font-mono">application/json</span>
                <span className="text-emerald-400 font-medium text-[11px]">
                  Cấu trúc phân cấp chuẩn theo mẫu
                </span>
              </div>
              <div className="flex-1 min-h-0 overflow-auto p-4 font-mono text-xs leading-relaxed">
                <pre className="text-emerald-300 whitespace-pre">{jsonString}</pre>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <DialogFooter className="sm:justify-between items-center gap-2 border-t pt-3 shrink-0">
          <div className="text-xs text-muted-foreground flex items-center gap-1.5">
            <ShieldCheck className="size-4 text-emerald-600 shrink-0" />
            <span>Nội dung xem trước trùng khớp 100% với tệp tải về</span>
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            {/* Nút mở tab mới cho PDF */}
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

            {/* Nút sao chép cho CSV/JSON */}
            {format === 'csv' && csvContent && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => handleCopyText(csvContent, 'CSV')}
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
                onClick={() => handleCopyText(jsonString, 'JSON')}
                className="gap-1.5 text-xs"
              >
                {copied ? <Check className="size-3.5 text-emerald-600" /> : <Copy className="size-3.5" />}
                <span>{copied ? 'Đã sao chép' : 'Sao chép'}</span>
              </Button>
            )}

            {/* Nút tải file trực tiếp từ bản xem trước - hiển thị đầy đủ cả khi xuất lô lẫn tạo mẫu */}
            <Button
              type="button"
              size="sm"
              onClick={handleDownloadCurrent}
              disabled={loading || (format === 'pdf' && !pdfBlob && !pdfUrl)}
              className="gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold"
            >
              <Download className="size-3.5" />
              <span>Tải tệp này về máy ({format.toUpperCase()})</span>
            </Button>

            <Button type="button" variant="outline" size="sm" onClick={onClose} className="gap-1 text-xs">
              <X className="size-3.5" />
              <span>Đóng</span>
            </Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default DossierPreviewDialog;
