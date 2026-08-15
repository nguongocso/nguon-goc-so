import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import {
  activateShipmentStamps,
  createShipment,
  getShipmentsByProductionLot,
} from '@/api/shipmentApi';
import type { Shipment, CreateShipmentPayload } from '@/types/shipment';

export const useShipments = (productionLotId: string) => {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [activatingShipmentId, setActivatingShipmentId] = useState<string | null>(
    null,
  );

  const loadShipments = useCallback(async () => {
    if (!productionLotId) return;
    setIsLoading(true);
    try {
      const data = await getShipmentsByProductionLot(productionLotId);
      setShipments(data);
    } catch (error: any) {
      const message = error.response?.data?.message || 'Không thể tải danh sách lô hàng';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  }, [productionLotId]);

  useEffect(() => {
    loadShipments();
  }, [loadShipments]);

  const createShipmentMutation = async (payload: CreateShipmentPayload) => {
    setIsCreating(true);
    try {
      const newShipment = await createShipment(payload);
      setShipments((prev) => [newShipment, ...prev]);
      toast.success('Tạo lô hàng thành công!');
      return newShipment;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Có lỗi xảy ra khi tạo lô hàng.';
      toast.error(message);
      throw error;
    } finally {
      setIsCreating(false);
    }
  };

  const activateShipmentMutation = async (shipmentId: string) => {
    setActivatingShipmentId(shipmentId);

    try {
      const activatedShipment = await activateShipmentStamps(shipmentId);
      setShipments((prev) =>
        prev.map((shipment) =>
          shipment.id === shipmentId ? activatedShipment : shipment,
        ),
      );
      toast.success('Kích hoạt tem thành công!');
      return activatedShipment;
    } catch (error: any) {
      const message =
        error.response?.data?.message || 'Không thể kích hoạt tem.';
      toast.error(message);
      throw error;
    } finally {
      setActivatingShipmentId(null);
    }
  };

  return {
    shipments,
    isLoading,
    isCreating,
    activatingShipmentId,
    createShipment: createShipmentMutation,
    activateShipment: activateShipmentMutation,
    reload: loadShipments,
  };
};