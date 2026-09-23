import { useState } from 'react';
import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { exportOpenData } from '@/api/exportApi';
import { getLocalDateString } from '@/utils/dateTime';
import type { ExportOpenDataFormValues } from '@/utils/validators';
import type { Qtn11ErrorDetail } from '../Qtn11ErrorModal';

export interface UseExportOpenDataParams {
  canFilterByUnit: boolean;
  unitIds: string[];
}

/** Hook quản lý trạng thái và thao tác xuất dữ liệu mở (gọi API, tải blob, xử lý lỗi QTN-11). */
export function useExportOpenData({ canFilterByUnit, unitIds }: UseExportOpenDataParams) {
  const [submitting, setSubmitting] = useState(false);
  const [qtn11ErrorModalOpen, setQtn11ErrorModalOpen] = useState(false);
  const [qtn11Errors, setQtn11Errors] = useState<Qtn11ErrorDetail[]>([]);

  const onSubmit = async (data: ExportOpenDataFormValues) => {
    setSubmitting(true);
    try {
      const payload: Record<string, unknown> = { format: data.format };
      if (data.organizationId) payload.organizationId = data.organizationId;
      if (data.fromDate) payload.fromDate = data.fromDate;
      if (data.toDate) payload.toDate = data.toDate;
      if (data.productCategoryIds?.length)
        payload.productCategoryIds = data.productCategoryIds;
      if (data.shipmentIds?.length) payload.shipmentIds = data.shipmentIds;
      if (canFilterByUnit && unitIds.length > 0) payload.unitIds = unitIds;
      if (data.templateId && data.templateId !== 'default') {
        payload.templateId = data.templateId;
      }

      const blob = await exportOpenData(payload as Parameters<typeof exportOpenData>[0]);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `export_${getLocalDateString()}.${data.format.toLowerCase()}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      toast.success('Xuất dữ liệu thành công!');
    } catch (error: unknown) {
      if (isAxiosError(error) && error.response?.data instanceof Blob) {
        const text = await error.response.data.text();
        try {
          const json: { message?: string; errors?: Qtn11ErrorDetail[] } = JSON.parse(text);
          if (json.errors && Array.isArray(json.errors) && json.errors.length > 0) {
            setQtn11Errors(json.errors);
            setQtn11ErrorModalOpen(true);
            toast.error(json.message || 'Không có lô hàng nào đáp ứng đủ quy tắc');
          } else {
            toast.error(json.message || 'Xuất dữ liệu thất bại');
          }
        } catch {
          toast.error('Xuất dữ liệu thất bại');
        }
      } else {
        const msg = error instanceof Error ? error.message : 'Xuất dữ liệu thất bại';
        toast.error(msg);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return {
    submitting,
    qtn11ErrorModalOpen,
    setQtn11ErrorModalOpen,
    qtn11Errors,
    onSubmit,
  };
}
