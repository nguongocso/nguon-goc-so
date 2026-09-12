import type { ChainEventType } from '@/enums/chainEventType';

// ─────────────────────────────────────────────
// Event Type Labels (Vietnamese)
// ─────────────────────────────────────────────

export const EVENT_TYPE_VN_LABELS: Record<ChainEventType, string> = {
  HARVEST: 'Thu hoạch',
  PREPROCESSING: 'Sơ chế và phân loại',
  PACKAGING: 'Đóng gói',
  TRANSPORT: 'Vận chuyển',
  PROCUREMENT: 'Thu mua',
  CORRECTION: 'Điều chỉnh',
  WAREHOUSE_RECEIPT: 'Nhập kho',
  STORAGE_CONDITION: 'Điều kiện bảo quản',
  WAREHOUSE_ENTRY: 'Nhập kho HTX',
  WAREHOUSE_EXIT: 'Xuất kho HTX',
  SPLIT: 'Đã tách lô',
  HANDOVER: 'Bàn giao',
};

export function getEventTypeLabel(eventType: string): string {
  return EVENT_TYPE_VN_LABELS[eventType as ChainEventType] || eventType;
}

// ─────────────────────────────────────────────
// Known Business-Field Labels (Vietnamese)
// Keys: backend camelCase → Values: Vietnamese label
// ─────────────────────────────────────────────

const KNOWN_FIELD_LABELS: Record<string, string> = {
  // Preprocessing
  inputQuantity: 'Khối lượng đưa vào (kg)',
  outputQuantity: 'Khối lượng sau sơ chế (kg)',
  lossRate: 'Tỷ lệ hao hụt (%)',
  grade: 'Hạng phân loại',
  processingMethod: 'Phương pháp sơ chế',
  preprocessingDate: 'Ngày sơ chế',
  parentEventId: 'Mã sự kiện gốc',
  // Packaging
  packagingSpecification: 'Quy cách đóng gói',
  packagingDate: 'Ngày đóng gói',
  // Harvest
  harvestDate: 'Ngày thu hoạch',
  quantity: 'Số lượng (kg)',
  earlyHarvest: 'Thu hoạch sớm',
  earlyHarvestReason: 'Lý do thu hoạch sớm',
  eligibleHarvestDate: 'Ngày đủ điều kiện cách ly',
  unmatchedMaterials: 'Vật tư ngoài danh mục',
  // Transport
  fromLocation: 'Điểm xuất phát',
  toLocation: 'Điểm đến',
  transportDate: 'Ngày vận chuyển',
  transportMethod: 'Phương thức vận chuyển',
  // Procurement (matches backend RecordProcurementEventRequest + eventData)
  shipmentName: 'Tên lô hàng',
  receivedQuantity: 'Số lượng nhận',
  notes: 'Ghi chú',
  // Production Lot
  productionLotName: 'Tên lô sản xuất',
  productionLotId: 'Mã lô sản xuất',
  // Correction
  correctionReason: 'Lý do điều chỉnh',
  // Common
  seedType: 'Loại giống',
  plantingDate: 'Ngày trồng',
  specification: 'Quy cách',
  receivedWeight: 'Khối lượng nhận',
  deviceSource: 'Nguồn thiết bị',
  images: 'Ảnh',

  // Coop Warehouse (NCL-05-CN-011)
  warehouseName: 'Tên kho HTX',
  entryTime: 'Thời điểm nhập kho',
  exitTime: 'Thời điểm xuất kho',
  storageCondition: 'Điều kiện bảo quản',
  storageDurationDays: 'Thời gian lưu kho (ngày)',
  storageDurationHours: 'Thời gian lưu kho (giờ)',
  maxAllowedStorageDays: 'Ngưỡng lưu kho tối đa (ngày)',
  isStorageExceeded: 'Vượt ngưỡng bảo quản',
  warningMessage: 'Cảnh báo lưu kho',
  destination: 'Nơi chuyển đến',

  // ========== Warehouse Receipt (NCL-05-CN-006) ==========
  conditionNote: 'Tình trạng hàng',
  isDiscrepancyExceeded: 'Vượt ngưỡng',
  declaredQuantity: 'Số lượng khai báo',
  discrepancy: 'Chênh lệch',
  discrepancyPercent: 'Chênh lệch %',
  threshold: 'Ngưỡng cho phép',
  reason: 'Lý do chênh lệch',
  receiptDate: 'Ngày nhập kho',

  // Tách lô hàng
  sourceShipmentId: 'Mã lô hàng nguồn',
  sourceShipmentName: 'Tên lô hàng nguồn',
  recipientOrganizationId: 'Mã tổ chức nhận',
  recipientOrganizationName: 'Đối tác nhận hàng',
  allocatedQuantity: 'Số lượng phân bổ',
  fromCode: 'Mã bắt đầu',
  toCode: 'Mã kết thúc',
  sourceLastEventHash: 'Mã băm sự kiện nguồn',

  // ========== Handover (NCL-05-CN-009) ==========
  action: 'Hành động',
  fromOrgId: 'Mã bên giao',
  toOrgId: 'Mã bên nhận',
  fromOrganizationName: 'Bên giao',
  toOrganizationName: 'Bên nhận',
};

const HIDDEN_EVENT_FIELDS = new Set([
  'shipmentId',
  'productionLotId',
  'deviceSource',
  'images',
  'fromOrgId',
  'toOrgId',
]);

const SPLIT_HIDDEN_FIELDS = new Set([
  'sourceShipmentId',
  'recipientOrganizationId',
  'sourceLastEventHash',
]);

const SPLIT_FIELD_ORDER = [
  'sourceShipmentName',
  'recipientOrganizationName',
  'allocatedQuantity',
  'fromCode',
  'toCode',
];

/**
 * Converts a camelCase backend field name into a human-readable Vietnamese label.
 *
 * 1. Exact match from KNOWN_FIELD_LABELS (preferred).
 * 2. Fallback: camelCase → Title Case Words.
 */
export function formatFieldLabel(key: string): string {
  if (KNOWN_FIELD_LABELS[key]) {
    return KNOWN_FIELD_LABELS[key];
  }
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (s) => s.toUpperCase())
    .trim();
}

// ─────────────────────────────────────────────
// Value Formatting
// ─────────────────────────────────────────────

function isISODateString(value: string): boolean {
  return /^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}:\d{2})?$/.test(value);
}

const HANDOVER_ACTION_TRANSLATIONS: Record<string, string> = {
  ACCEPTED: 'Đã xác nhận',
  REJECTED: 'Từ chối',
  EXPIRED: 'Hết hiệu lực',
  PENDING: 'Chờ xác nhận',
};

export function formatEventValue(value: unknown): string {
  if (value === null || value === undefined) {
    return '';
  }

  if (Array.isArray(value)) {
    if (value.length === 0) {
      return '';
    }
    return value.map((v) => formatEventValue(v)).join(', ');
  }

  if (typeof value === 'boolean') {
    return value ? 'Có' : 'Không';
  }

  if (typeof value === 'number') {
    return value.toLocaleString('vi-VN');
  }

  if (typeof value === 'string') {
    if (HANDOVER_ACTION_TRANSLATIONS[value]) {
      return HANDOVER_ACTION_TRANSLATIONS[value];
    }
    if (isISODateString(value)) {
      try {
        return new Date(value).toLocaleDateString('vi-VN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
        });
      } catch {
        return value;
      }
    }
    return value;
  }

  return String(value);
}

export function isEventValueEmpty(value: unknown): boolean {
  return value === null || value === undefined || value === '';
}

export function getDisplayEventDataEntries(
  eventType: string,
  data: Record<string, unknown>,
): Array<[string, unknown]> {
  return Object.entries(data)
    .filter(([key, value]) => {
      if (HIDDEN_EVENT_FIELDS.has(key)) return false;
      if (eventType === 'SPLIT' && SPLIT_HIDDEN_FIELDS.has(key)) return false;
      if (key === 'earlyHarvest' && (value === false || value === 'false')) return false;
      if (key === 'unmatchedMaterials' && Array.isArray(value) && value.length === 0) return false;
      return !isEventValueEmpty(value);
    })
    .sort(([firstKey], [secondKey]) => {
      if (eventType !== 'SPLIT') return 0;
      const firstIndex = SPLIT_FIELD_ORDER.indexOf(firstKey);
      const secondIndex = SPLIT_FIELD_ORDER.indexOf(secondKey);
      return (firstIndex === -1 ? Number.MAX_SAFE_INTEGER : firstIndex)
        - (secondIndex === -1 ? Number.MAX_SAFE_INTEGER : secondIndex);
    });
}

// ─────────────────────────────────────────────
// Date / DateTime formatting (vi-VN)
// ─────────────────────────────────────────────

export function formatDisplayDateTime(iso: string): string {
  try {
    const date = new Date(iso);
    if (isNaN(date.getTime())) return iso;

    const datePart = date.toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });

    const timePart = date.toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });

    return `${datePart} ${timePart}`;
  } catch {
    return iso;
  }
}

export function formatDisplayDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  } catch {
    return iso;
  }
}

// ─────────────────────────────────────────────
// Shared translation helper — used by Timeline
// and RouteMap to produce labelled event data
// ─────────────────────────────────────────────

/**
 * Translates a raw eventData map into { VietnameseLabel: formattedValue }.
 *
 * - Skips null/undefined/empty values.
 * - Uses formatFieldLabel() for key → label.
 * - Uses formatEventValue() for value formatting.
 *
 * Returns a flat Record<string, string> ready for display.
 */
export function getTranslatedEventData(
  eventType: string,
  data: Record<string, unknown>,
): Record<string, string> {
  const result: Record<string, string> = {};

  for (const [key, value] of getDisplayEventDataEntries(eventType, data)) {
    const label = formatFieldLabel(key);
    const formatted = formatEventValue(value);
    if (formatted) {
      result[label] = formatted;
    }
  }

  return result;
}
