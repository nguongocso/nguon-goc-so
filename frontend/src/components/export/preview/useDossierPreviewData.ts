import { useState, useEffect, useRef } from 'react';

import { exportDossier } from '@/api/dossierApi';
import { exportShipmentWithTemplate } from '@/api/exportApi';
import { getOpenDataPreview } from '@/api/profileTemplateApi';
import { toApiError } from '@/api/apiError';

/** Tham số đầu vào cho hook nạp dữ liệu xem trước hồ sơ. */
export interface UseDossierPreviewDataParams {
  open: boolean;
  shipmentId?: string;
  templateId?: string;
  format: 'pdf' | 'json' | 'csv';
  initialData?: Record<string, unknown> | null;
}

/** Quản lý việc tải và lưu trữ dữ liệu xem trước hồ sơ theo định dạng PDF/CSV/JSON. */
export function useDossierPreviewData({
  open,
  shipmentId,
  templateId,
  format,
  initialData,
}: UseDossierPreviewDataParams) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pdfBlob, setPdfBlob] = useState<Blob | null>(null);
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [csvContent, setCsvContent] = useState<string | null>(null);
  const [jsonString, setJsonString] = useState<string>(
    initialData ? JSON.stringify(initialData, null, 2) : ''
  );

  const currentPdfUrlRef = useRef<string | null>(null);

  // Thu hồi URL Blob khi thành phần bị gỡ để tránh rò rỉ bộ nhớ.
  useEffect(() => {
    return () => {
      if (currentPdfUrlRef.current) {
        URL.revokeObjectURL(currentPdfUrlRef.current);
        currentPdfUrlRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!open) {
      if (currentPdfUrlRef.current) {
        URL.revokeObjectURL(currentPdfUrlRef.current);
        currentPdfUrlRef.current = null;
      }
      setPdfUrl(null);
      setPdfBlob(null);
      setCsvContent(null);
      if (!initialData) setJsonString('');
      setError(null);
      return;
    }

    if (initialData && !shipmentId) {
      setJsonString(JSON.stringify(initialData, null, 2));
      return;
    }

    if (!shipmentId) return;

    let isMounted = true;
    const fetchPreviewData = async () => {
      setLoading(true);
      setError(null);

      try {
        if (format === 'pdf') {
          const blob = await exportDossier(shipmentId, templateId);
          if (!isMounted) return;
          if (currentPdfUrlRef.current) URL.revokeObjectURL(currentPdfUrlRef.current);
          const url = URL.createObjectURL(blob);
          currentPdfUrlRef.current = url;
          setPdfBlob(blob);
          setPdfUrl(url);
        } else if (format === 'csv') {
          const blob = await exportShipmentWithTemplate(shipmentId, templateId, 'csv');
          if (!isMounted) return;
          const text = await blob.text();
          setCsvContent(text);
        } else {
          const res = await getOpenDataPreview(shipmentId, templateId);
          if (!isMounted) return;
          setJsonString(JSON.stringify(res, null, 2));
        }
      } catch (err: unknown) {
        if (!isMounted) return;
        const msg = toApiError(err, 'Không thể khởi tạo bản xem trước hồ sơ').message;
        setError(msg);
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    void fetchPreviewData();
    return () => {
      isMounted = false;
    };
  }, [open, shipmentId, templateId, format, initialData]);

  return {
    loading,
    error,
    pdfBlob,
    pdfUrl,
    csvContent,
    jsonString,
  };
}
