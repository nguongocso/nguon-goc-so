import { toast } from 'sonner';
import { recordWarehouseEntry, recordWarehouseExit } from '@/api/coopWarehouseApi';
import type {
  RecordWarehouseEntryFormValues,
  RecordWarehouseExitFormValues,
} from '@/utils/validators/coopWarehouseEventSchema';
import {
  formatDateTimeWithSeconds,
  getCoopWarehouseErrorMessage,
  type ShipmentStatusItem,
} from './coopWarehouseUtils';

interface SubmitOptions {
  items: ShipmentStatusItem[];
  hasValidationError: boolean;
  setServerError: (message: string | null) => void;
  onSuccess: () => void;
}

/** Ghi sự kiện nhập kho lần lượt cho các lô hàng đã chọn. */
export async function submitCoopWarehouseEntry(
  values: RecordWarehouseEntryFormValues,
  options: SubmitOptions,
): Promise<void> {
  if (options.items.length === 0) {
    toast.error('Vui lòng chọn ít nhất một lô hàng để ghi sự kiện.');
    return;
  }
  if (options.hasValidationError) {
    toast.error('Vui lòng bỏ chọn các lô hàng đang ở trong kho trước khi nhập kho mới.');
    return;
  }
  try {
    options.setServerError(null);
    for (const [index, item] of options.items.entries()) {
      const response = await recordWarehouseEntry({
        ...values,
        entryTime: formatDateTimeWithSeconds(values.entryTime),
        shipmentId: item.shipment.id,
      });
      if (!response?.success) {
        const message = response?.message || `Lỗi khi ghi nhập kho cho lô ${item.shipment.name}`;
        options.setServerError(message);
        toast.error(message);
        return;
      }
      if (index === options.items.length - 1) {
        toast.success(`Ghi sự kiện nhập kho HTX thành công cho ${options.items.length} lô hàng!`);
        options.onSuccess();
      }
    }
  } catch (error: unknown) {
    const message = getCoopWarehouseErrorMessage(error, 'Có lỗi xảy ra khi ghi sự kiện nhập kho HTX.');
    options.setServerError(message);
    toast.error(message);
  }
}

/** Ghi sự kiện xuất kho lần lượt cho các lô hàng đã chọn. */
export async function submitCoopWarehouseExit(
  values: RecordWarehouseExitFormValues,
  options: SubmitOptions,
): Promise<void> {
  if (options.items.length === 0) {
    toast.error('Vui lòng chọn ít nhất một lô hàng để ghi sự kiện.');
    return;
  }
  if (options.hasValidationError) {
    toast.error('Vui lòng nhập kho cho các lô hàng chưa ở trong kho trước khi ghi xuất kho.');
    return;
  }
  try {
    options.setServerError(null);
    let hasWarning = false;
    for (const item of options.items) {
      const response = await recordWarehouseExit({
        ...values,
        exitTime: formatDateTimeWithSeconds(values.exitTime),
        shipmentId: item.shipment.id,
      });
      if (!response?.success) {
        const message = response?.message || `Lỗi khi ghi xuất kho cho lô ${item.shipment.name}`;
        options.setServerError(message);
        toast.error(message);
        return;
      }
      hasWarning ||= Boolean(response.data?.isStorageExceeded);
    }
    const message = `Ghi sự kiện xuất kho HTX thành công cho ${options.items.length} lô hàng!`;
    if (hasWarning) toast.warning(`${message} Có lô vượt quá ngưỡng bảo quản.`);
    else toast.success(message);
    options.onSuccess();
  } catch (error: unknown) {
    const message = getCoopWarehouseErrorMessage(error, 'Có lỗi xảy ra khi ghi sự kiện xuất kho HTX.');
    options.setServerError(message);
    toast.error(message);
  }
}
