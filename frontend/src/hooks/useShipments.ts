import { useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';

import {
  activateShipmentStamps,
  createShipment,
  getShipmentsByProductionLot,
  recallShipment,
} from '@/api/shipmentApi';

import type {
  CreateShipmentPayload,
  Shipment,
} from '@/types/shipment';

export const useShipments = (productionLotId: string) => {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);

  const [activatingShipmentId, setActivatingShipmentId] =
    useState<string | null>(null);

  const [recallingShipmentId, setRecallingShipmentId] =
    useState<string | null>(null);

  const loadShipments = useCallback(async () => {
    if (!productionLotId) return;

    setIsLoading(true);

    try {
      const data = await getShipmentsByProductionLot(productionLotId);
      setShipments(data);
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        'Không thể tải danh sách lô hàng';

      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  }, [productionLotId]);

  useEffect(() => {
    void loadShipments();
  }, [loadShipments]);

  const createShipmentMutation = async (
    payload: CreateShipmentPayload,
  ) => {
    setIsCreating(true);

    try {
      const newShipment = await createShipment(payload);

      setShipments((previous) => [
        newShipment,
        ...previous,
      ]);

      toast.success('Tạo lô hàng thành công!');
      return newShipment;
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        'Có lỗi xảy ra khi tạo lô hàng.';

      toast.error(message);
      throw error;
    } finally {
      setIsCreating(false);
    }
  };

  const activateShipmentMutation = async (
    shipmentId: string,
  ) => {
    setActivatingShipmentId(shipmentId);

    try {
      const activatedShipment =
        await activateShipmentStamps(shipmentId);

      setShipments((previous) =>
        previous.map((shipment) =>
          shipment.id === shipmentId
            ? activatedShipment
            : shipment,
        ),
      );

      toast.success('Kích hoạt tem thành công!');
      return activatedShipment;
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        'Không thể kích hoạt tem.';

      toast.error(message);
      throw error;
    } finally {
      setActivatingShipmentId(null);
    }
  };

  const recallShipmentMutation = async (
    shipmentId: string,
    reason: string,
  ) => {
    setRecallingShipmentId(shipmentId);

    try {
      const result = await recallShipment(shipmentId, {
        reason,
      });

      /*
       * Gọi lại API danh sách để lấy trạng thái mới của Shipment
       * và trạng thái mới của toàn bộ TraceCode.
       */
      await loadShipments();

      toast.success(
        `Thu hồi lô hàng thành công. Đã cập nhật ${result.traceCodesUpdated} mã truy xuất.`,
      );

      return result;
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        'Không thể thu hồi lô hàng.';

      toast.error(message);
      throw error;
    } finally {
      setRecallingShipmentId(null);
    }
  };

  return {
    shipments,
    isLoading,
    isCreating,
    activatingShipmentId,
    recallingShipmentId,

    createShipment: createShipmentMutation,
    activateShipment: activateShipmentMutation,
    recallShipment: recallShipmentMutation,
    reload: loadShipments,
  };
};