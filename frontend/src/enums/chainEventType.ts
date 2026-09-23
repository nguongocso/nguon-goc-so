/** Định nghĩa loại sự kiện chuỗi cung ứng bằng object hằng. */
export const ChainEventType = {
  HARVEST: 'HARVEST',
  PREPROCESSING: 'PREPROCESSING',
  PACKAGING: 'PACKAGING',
  TRANSPORT: 'TRANSPORT',
  PROCUREMENT: 'PROCUREMENT',
  CORRECTION: 'CORRECTION',
  WAREHOUSE_RECEIPT: 'WAREHOUSE_RECEIPT',
  STORAGE_CONDITION: 'STORAGE_CONDITION',
  WAREHOUSE_ENTRY: 'WAREHOUSE_ENTRY',
  WAREHOUSE_EXIT: 'WAREHOUSE_EXIT',
  SPLIT: 'SPLIT',
  HANDOVER: 'HANDOVER',
  FARM_LOG: 'FARM_LOG',
} as const;

/** Kiểu dữ liệu tương ứng với các giá trị sự kiện chuỗi cung ứng */
export type ChainEventType = (typeof ChainEventType)[keyof typeof ChainEventType];

/** Nhãn hiển thị tiếng Việt của từng loại sự kiện chuỗi cung ứng */
export const ChainEventTypeLabel: Record<ChainEventType, string> = {
  [ChainEventType.HARVEST]: 'Thu hoạch',
  [ChainEventType.PREPROCESSING]: 'Sơ chế và phân loại',
  [ChainEventType.PACKAGING]: 'Đóng gói',
  [ChainEventType.TRANSPORT]: 'Vận chuyển',
  [ChainEventType.PROCUREMENT]: 'Thu mua',
  [ChainEventType.CORRECTION]: 'Đính chính',
  [ChainEventType.WAREHOUSE_RECEIPT]: 'Nhập kho',
  [ChainEventType.STORAGE_CONDITION]: 'Điều kiện bảo quản',
  [ChainEventType.WAREHOUSE_ENTRY]: 'Nhập kho HTX',
  [ChainEventType.WAREHOUSE_EXIT]: 'Xuất kho HTX',
  [ChainEventType.SPLIT]: 'Đã tách lô',
  [ChainEventType.HANDOVER]: 'Bàn giao',
  [ChainEventType.FARM_LOG]: 'Nhật ký canh tác',
};

/** Nhãn hiển thị tiếng Anh của từng loại sự kiện chuỗi cung ứng */
export const ChainEventTypeEnLabel: Record<ChainEventType, string> = {
  [ChainEventType.HARVEST]: 'Harvesting',
  [ChainEventType.PREPROCESSING]: 'Preprocessing and grading',
  [ChainEventType.PACKAGING]: 'Packaging',
  [ChainEventType.TRANSPORT]: 'Transport',
  [ChainEventType.PROCUREMENT]: 'Procurement',
  [ChainEventType.CORRECTION]: 'Correction',
  [ChainEventType.WAREHOUSE_RECEIPT]: 'Warehouse Receipt',
  [ChainEventType.STORAGE_CONDITION]: 'Storage Condition',
  [ChainEventType.WAREHOUSE_ENTRY]: 'HTX Warehouse Entry',
  [ChainEventType.WAREHOUSE_EXIT]: 'HTX Warehouse Exit',
  [ChainEventType.SPLIT]: 'Shipment split',
  [ChainEventType.HANDOVER]: 'Handover',
  [ChainEventType.FARM_LOG]: 'Farm log',
};
