import { useState } from 'react';
import { toast } from 'sonner';
import { deleteDraft } from '@/api/eventValidationApi';
import type { Shipment } from '@/types/shipment';

/**
 * Hook xử lý hủy bản nháp lô hàng (NCL-xx).
 * Tách theo mẫu của useRecallShipment: hook sở hữu pending state và toast
 * phản hồi; caller quyết định hành vi sau khi thành công (reload / navigate).
 *
 * Chỉ chứa orchestration (API call + feedback). Điều kiện eligibility
 * (DRAFT/CODE_PRINTED) và quyền vẫn nằm ở UI gọi hook.
 */
export const useDeleteDraftShipment = (onSuccess?: () => void) => {
  const [deletingDraft, setDeletingDraft] = useState(false);

  const deleteDraftShipment = async (shipment: Shipment) => {
    setDeletingDraft(true);
    try {
      await deleteDraft(shipment.id);
      toast.success('Hủy bản nháp thành công');
      onSuccess?.();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || 'Không thể hủy bản nháp',
      );
    } finally {
      setDeletingDraft(false);
    }
  };

  return {
    deletingDraft,
    deleteDraftShipment,
  };
};
