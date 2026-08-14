import { useState } from 'react';
import { toast } from 'sonner';
import { recallShipment as recallShipmentApi } from '@/api/recallApi';

/**
 * Hook xử lý thao tác thu hồi lô hàng (NCL-08-CN-003).
 * Tách riêng khỏi useShipments để hạn chế sửa đổi các file dùng chung.
 * @param onSuccess callback gọi lại sau khi thu hồi thành công (VD: reload() của useShipments)
 */
export const useRecallShipment = (onSuccess?: () => void) => {
  const [recallingShipmentId, setRecallingShipmentId] = useState<string | null>(
    null,
  );

  const recallShipment = async (shipmentId: string, reason: string) => {
    setRecallingShipmentId(shipmentId);
    try {
      const result = await recallShipmentApi(shipmentId, reason);
      toast.success(
        `Thu hồi lô hàng thành công. ${result.traceCodesUpdated} mã truy xuất đã được cập nhật.`,
      );
      onSuccess?.();
      return result;
    } catch (error: any) {
      const message =
        error.response?.data?.message || 'Không thể thu hồi lô hàng.';
      toast.error(message);
      throw error;
    } finally {
      setRecallingShipmentId(null);
    }
  };

  return {
    recallingShipmentId,
    recallShipment,
  };
};