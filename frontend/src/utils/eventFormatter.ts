import type { ChainEventType } from '@/enums/chainEventType';

// ─────────────────────────────────────────────
// Event Type Labels (Vietnamese & English)
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

export const EVENT_TYPE_EN_LABELS: Record<ChainEventType, string> = {
  HARVEST: 'Harvest',
  PREPROCESSING: 'Preprocessing and Grading',
  PACKAGING: 'Packaging',
  TRANSPORT: 'Transport',
  PROCUREMENT: 'Procurement',
  CORRECTION: 'Correction',
  WAREHOUSE_RECEIPT: 'Warehouse Receipt',
  STORAGE_CONDITION: 'Storage Condition',
  WAREHOUSE_ENTRY: 'HTX Warehouse Inbound',
  WAREHOUSE_EXIT: 'HTX Warehouse Outbound',
  SPLIT: 'Shipment Split',
  HANDOVER: 'Shipment Handover',
};

export function getEventTypeLabel(eventType: string, lang: 'vi' | 'en' = 'vi'): string {
  if (lang === 'en') {
    return EVENT_TYPE_EN_LABELS[eventType as ChainEventType] || eventType;
  }
  return EVENT_TYPE_VN_LABELS[eventType as ChainEventType] || eventType;
}

// ─────────────────────────────────────────────
// Known Business-Field Labels (Vietnamese & English)
// Keys: backend camelCase → Values: Human-readable label
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
  // Procurement
  shipmentName: 'Tên lô hàng',
  receivedQuantity: 'Số lượng nhận (kg)',
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
  receivedWeight: 'Khối lượng nhận (kg)',
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

const KNOWN_FIELD_LABELS_EN: Record<string, string> = {
  // Preprocessing
  inputQuantity: 'Input Quantity (kg)',
  outputQuantity: 'Output Quantity (kg)',
  lossRate: 'Loss Rate (%)',
  grade: 'Grade',
  processingMethod: 'Processing Method',
  preprocessingDate: 'Preprocessing Date',
  parentEventId: 'Parent Event ID',
  // Packaging
  packagingSpecification: 'Packaging Specification',
  packagingDate: 'Packaging Date',
  // Harvest
  harvestDate: 'Harvest Date',
  quantity: 'Quantity (kg)',
  earlyHarvest: 'Early Harvest',
  earlyHarvestReason: 'Early Harvest Reason',
  eligibleHarvestDate: 'Pre-harvest Isolation Date',
  unmatchedMaterials: 'Unlisted Materials',
  // Transport
  fromLocation: 'Origin',
  toLocation: 'Destination',
  transportDate: 'Transport Date',
  transportMethod: 'Transport Method',
  // Procurement
  shipmentName: 'Shipment Name',
  receivedQuantity: 'Received Quantity (kg)',
  notes: 'Notes',
  // Production Lot
  productionLotName: 'Production Lot Name',
  productionLotId: 'Production Lot ID',
  // Correction
  correctionReason: 'Correction Reason',
  // Common
  seedType: 'Seed Type',
  plantingDate: 'Planting Date',
  specification: 'Specification',
  receivedWeight: 'Received Weight (kg)',
  deviceSource: 'Device Source',
  images: 'Images',

  // Coop Warehouse
  warehouseName: 'HTX Warehouse Name',
  entryTime: 'Inbound Time',
  exitTime: 'Outbound Time',
  storageCondition: 'Storage Condition',
  storageDurationDays: 'Storage Duration (days)',
  storageDurationHours: 'Storage Duration (hours)',
  maxAllowedStorageDays: 'Max Allowed Storage (days)',
  isStorageExceeded: 'Storage Exceeded',
  warningMessage: 'Storage Warning',
  destination: 'Destination',

  // Warehouse Receipt
  conditionNote: 'Condition Note',
  isDiscrepancyExceeded: 'Threshold Exceeded',
  declaredQuantity: 'Declared Quantity',
  discrepancy: 'Discrepancy',
  discrepancyPercent: 'Discrepancy %',
  threshold: 'Allowed Threshold',
  reason: 'Reason for Discrepancy',
  receiptDate: 'Receipt Date',

  // Split
  sourceShipmentId: 'Source Shipment ID',
  sourceShipmentName: 'Source Shipment Name',
  recipientOrganizationId: 'Recipient Org ID',
  recipientOrganizationName: 'Recipient Partner',
  allocatedQuantity: 'Allocated Quantity (kg)',
  fromCode: 'From Code',
  toCode: 'To Code',
  sourceLastEventHash: 'Source Event Hash',

  // Handover
  action: 'Action',
  fromOrgId: 'Sender Org ID',
  toOrgId: 'Receiver Org ID',
  fromOrganizationName: 'Deliverer / Sender',
  toOrganizationName: 'Receiver',
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
 * Converts a camelCase backend field name into a human-readable label.
 */
export function formatFieldLabel(key: string, lang: 'vi' | 'en' = 'vi'): string {
  if (lang === 'en') {
    if (KNOWN_FIELD_LABELS_EN[key]) {
      return KNOWN_FIELD_LABELS_EN[key];
    }
  } else {
    if (KNOWN_FIELD_LABELS[key]) {
      return KNOWN_FIELD_LABELS[key];
    }
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

const HANDOVER_ACTION_TRANSLATIONS: Record<string, { vi: string; en: string }> = {
  ACCEPTED: { vi: 'Đã xác nhận', en: 'Confirmed' },
  REJECTED: { vi: 'Từ chối', en: 'Rejected' },
  EXPIRED: { vi: 'Hết hiệu lực', en: 'Expired' },
  PENDING: { vi: 'Chờ xác nhận', en: 'Pending' },
};

export function formatEventValue(value: unknown, lang: 'vi' | 'en' = 'vi'): string {
  if (value === null || value === undefined) {
    return '';
  }

  if (Array.isArray(value)) {
    if (value.length === 0) {
      return '';
    }
    return value.map((v) => formatEventValue(v, lang)).join(', ');
  }

  if (typeof value === 'boolean') {
    return lang === 'en' ? (value ? 'Yes' : 'No') : (value ? 'Có' : 'Không');
  }

  if (typeof value === 'number') {
    return value.toLocaleString(lang === 'en' ? 'en-US' : 'vi-VN');
  }

  if (typeof value === 'string') {
    if (HANDOVER_ACTION_TRANSLATIONS[value]) {
      return HANDOVER_ACTION_TRANSLATIONS[value][lang] || HANDOVER_ACTION_TRANSLATIONS[value].vi;
    }
    if (isISODateString(value)) {
      try {
        return new Date(value).toLocaleDateString(lang === 'en' ? 'en-US' : 'vi-VN', {
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
// Date / DateTime formatting
// ─────────────────────────────────────────────

export function formatDisplayDateTime(iso: string, lang: 'vi' | 'en' = 'vi'): string {
  try {
    const date = new Date(iso);
    if (isNaN(date.getTime())) return iso;

    const datePart = date.toLocaleDateString(lang === 'en' ? 'en-US' : 'vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });

    const timePart = date.toLocaleTimeString(lang === 'en' ? 'en-US' : 'vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });

    return `${datePart} ${timePart}`;
  } catch {
    return iso;
  }
}

export function formatDisplayDate(iso: string, lang: 'vi' | 'en' = 'vi'): string {
  try {
    return new Date(iso).toLocaleDateString(lang === 'en' ? 'en-US' : 'vi-VN', {
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
 * Translates a raw eventData map into { Label: formattedValue }.
 */
export function getTranslatedEventData(
  eventType: string,
  data: Record<string, unknown>,
  lang: 'vi' | 'en' = 'vi',
): Record<string, string> {
  const result: Record<string, string> = {};

  for (const [key, value] of getDisplayEventDataEntries(eventType, data)) {
    const label = formatFieldLabel(key, lang);
    const formatted = formatEventValue(value, lang);
    if (formatted) {
      result[label] = formatted;
    }
  }

  return result;
}
