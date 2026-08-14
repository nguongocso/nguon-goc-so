// src/hooks/useExportIndustryReport.ts
import { useState } from 'react';
import { toast } from 'sonner';
import { getIndustrySummary, exportIndustrySummary } from '@/api/reportApi';
import type { IndustryReportResponse, IndustryReportParams } from '@/types/report';

interface UseExportIndustryReportResult {
  report: IndustryReportResponse | null;
  isLoading: boolean;
  isExporting: boolean;
  error: string | null;
  fetchReport: (params: IndustryReportParams) => Promise<void>;
  exportReport: (params: IndustryReportParams, format?: 'PDF' | 'EXCEL') => Promise<void>;
  reset: () => void;
}

export const useExportIndustryReport = (): UseExportIndustryReportResult => {
  const [report, setReport] = useState<IndustryReportResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchReport = async (params: IndustryReportParams) => {
    setIsLoading(true);
    setError(null);
    setReport(null);
    try {
      const data = await getIndustrySummary(params);
      setReport(data);
      if (!data.hasData) {
        toast.info(data.message || 'Chưa có dữ liệu cho địa bàn và khoảng thời gian đã chọn.');
      }
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        (err.response ? 'Không thể tải báo cáo.' : 'Không thể kết nối đến máy chủ.');
      setError(message);
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const exportReport = async (params: IndustryReportParams, format: 'PDF' | 'EXCEL' = 'PDF') => {
    setIsExporting(true);
    try {
      const result = await exportIndustrySummary({ ...params, format });
      downloadBlob(result.blob, result.fileName);
      toast.success(`Xuất báo cáo ${format} thành công.`);
    } catch (err: any) {
      const message =
        extractErrorMessage(err) ||
        (err.response ? 'Không thể xuất báo cáo.' : 'Không thể kết nối đến máy chủ.');
      toast.error(message);
    } finally {
      setIsExporting(false);
    }
  };

  const reset = () => {
    setReport(null);
    setError(null);
  };

  return { report, isLoading, isExporting, error, fetchReport, exportReport, reset };
};

/**
 * Kích hoạt trình duyệt tải file từ Blob.
 */
function downloadBlob(blob: Blob, fileName: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

/**
 * Khi backend trả lỗi (BusinessException/4xx/5xx) kèm responseType blob,
 * error.response.data là Blob chứa JSON. Hàm này parse Blob để lấy message.
 */
async function extractErrorMessage(err: any): Promise<string | null> {
  const data = err.response?.data;
  if (!data) return null;

  // Trường hợp response lỗi thường (JSON object trực tiếp)
  if (typeof data === 'object' && data.message) {
    return data.message as string;
  }

  // Trường hợp response lỗi là Blob JSON (do responseType: 'blob')
  if (data instanceof Blob && data.type?.includes('application/json')) {
    try {
      const text = await data.text();
      const parsed = JSON.parse(text);
      return parsed?.message || parsed?.error || null;
    } catch {
      return null;
    }
  }

  return null;
}