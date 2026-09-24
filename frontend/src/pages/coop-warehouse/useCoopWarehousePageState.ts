import { useCallback, useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';

import { useAutoGeolocation } from '@/hooks/useAutoGeolocation';

import { getShipmentWarehouseStatus } from '@/api/coopWarehouseApi';
import { getProductionLotById } from '@/api/productionLotApi';
import { getShipmentById, getShipmentsByProductionLot } from '@/api/shipmentApi';
import type { Shipment } from '@/types/shipment';

import type { ShipmentStatusItem } from './coopWarehouseUtils';

interface ShipmentStateOptions {
  productionLotId: string;
  queryParam: string;
  initialShipmentIds: string[];
  invalidStatus: 'IN_WAREHOUSE' | 'NOT_IN_WAREHOUSE';
  onShipmentSelected: (shipmentId: string) => void;
}

/** Tải lô hàng và trạng thái kho dùng chung cho trang nhập, xuất kho. */
export function useCoopWarehouseShipmentState({
  productionLotId,
  queryParam,
  initialShipmentIds,
  invalidStatus,
  onShipmentSelected,
}: ShipmentStateOptions) {
  const [shipments, setShipments] = useState<ShipmentStatusItem[]>([]);
  const [selectedIds, setSelectedIds] = useState(initialShipmentIds);
  const [loadingShipments, setLoadingShipments] = useState(true);
  const [serverError, setServerError] = useState<string | null>(null);
  const [lotName, setLotName] = useState<string | null>(null);

  useEffect(() => {
    if (!productionLotId) return;
    getProductionLotById(productionLotId)
      .then((lot) => setLotName(lot.name || lot.code || null))
      .catch(() => setLotName(null));
  }, [productionLotId]);

  useEffect(() => {
    async function loadShipments() {
      try {
        setLoadingShipments(true);
        setServerError(null);
        let list: Shipment[] = [];
        if (productionLotId) {
          list = await getShipmentsByProductionLot(productionLotId);
        } else if (initialShipmentIds.length > 0) {
          const loaded = await Promise.all(
            initialShipmentIds.map((id) => getShipmentById(id).catch(() => null)),
          );
          list = loaded.filter((shipment): shipment is Shipment => shipment !== null);
        }
        const items = await Promise.all(
          list.map(async (shipment) => ({
            shipment,
            warehouseStatus: await getShipmentWarehouseStatus(shipment.id),
          })),
        );
        setShipments(items);
        const nextIds = initialShipmentIds.length > 0
          ? initialShipmentIds
          : items[0]
            ? [items[0].shipment.id]
            : [];
        setSelectedIds(nextIds);
        if (nextIds[0]) onShipmentSelected(nextIds[0]);
      } catch {
        setServerError('Không thể tải thông tin hoặc kiểm tra trạng thái lô hàng.');
      } finally {
        setLoadingShipments(false);
      }
    }
    void loadShipments();
  }, [productionLotId, queryParam, onShipmentSelected]);

  const selectedItems = useMemo(
    () => shipments.filter((item) => selectedIds.includes(item.shipment.id)),
    [selectedIds, shipments],
  );
  const invalidItems = useMemo(
    () => selectedItems.filter((item) => item.warehouseStatus === invalidStatus),
    [invalidStatus, selectedItems],
  );

  return {
    selectedItems,
    invalidItems,
    loadingShipments,
    serverError,
    setServerError,
    lotName,
    hasValidationError: invalidItems.length > 0,
  };
}

interface LocationStateOptions {
  latitude?: number;
  longitude?: number;
  onLocationChange: (latitude: number, longitude: number) => void;
}

/** Quản lý vị trí bản đồ và tự động định vị kho. */
export function useCoopWarehouseLocation({
  latitude,
  longitude,
  onLocationChange,
}: LocationStateOptions) {
  const handleLocationSelect = useCallback(
    (nextLatitude: number, nextLongitude: number) => {
      onLocationChange(nextLatitude, nextLongitude);
    },
    [onLocationChange],
  );

  useAutoGeolocation({
    onLocation: (nextLatitude, nextLongitude) => {
      handleLocationSelect(nextLatitude, nextLongitude);
      toast.success('Đã lấy vị trí hiện tại');
    },
    onError: (message) => toast.error(`Không thể lấy vị trí hiện tại: ${message}`),
  });

  const currentPosition =
    typeof latitude === 'number' &&
    Number.isFinite(latitude) &&
    typeof longitude === 'number' &&
    Number.isFinite(longitude)
      ? { lat: latitude, lng: longitude }
      : undefined;

  return { currentPosition, handleLocationSelect };
}
