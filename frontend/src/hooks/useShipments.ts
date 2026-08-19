import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import {
  activateShipmentStamps,
  createShipment,
  getShipmentsByProductionLotPaged,
} from '@/api/shipmentApi';
import type { PageResponse, Shipment, CreateShipmentPayload } from '@/types/shipment';

const PAGE_SIZE = 10;

export const useShipments = (productionLotId: string) => {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [activatingShipmentId, setActivatingShipmentId] = useState<string | null>(null);

  // Pagination state
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const loadShipments = useCallback(async (targetPage: number) => {
    if (!productionLotId) return;
    setIsLoading(true);
    try {
      const data: PageResponse<Shipment> = await getShipmentsByProductionLotPaged(
        productionLotId,
        targetPage,
        PAGE_SIZE,
      );
      setShipments(data.items);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (error: any) {
      const message = error.response?.data?.message || 'Không thể tải danh sách lô hàng';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  }, [productionLotId]);

  useEffect(() => {
    void loadShipments(page);
  }, [productionLotId, page, loadShipments]);

  const createShipmentMutation = async (payload: CreateShipmentPayload) => {
    setIsCreating(true);
    try {
      await createShipment(payload);
      toast.success('Tạo lô hàng thành công!');
      // Về trang đầu để thấy lô hàng mới nhất (sort DESC)
      if (page === 0) {
        void loadShipments(0);
      } else {
        setPage(0);
      }
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
      await activateShipmentStamps(shipmentId);
      toast.success('Kích hoạt tem thành công!');
      // Reload trang hiện tại để cập nhật status
      void loadShipments(page);
    } catch (error: any) {
      const message = error.response?.data?.message || 'Không thể kích hoạt tem.';
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
    page,
    totalPages,
    totalElements,
    setPage,
    createShipment: createShipmentMutation,
    activateShipment: activateShipmentMutation,
    reload: () => void loadShipments(page),
  };
};