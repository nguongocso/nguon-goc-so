// Dùng object thay cho enum để tương thích với erasableSyntaxOnly
export const ChainEventType = {
  HARVEST: 'HARVEST',
  PREPROCESSING: 'PREPROCESSING',
  PACKAGING: 'PACKAGING',
  TRANSPORT: 'TRANSPORT',
  PROCUREMENT: 'PROCUREMENT',
  CORRECTION: 'CORRECTION',
  WAREHOUSE_RECEIPT: 'WAREHOUSE_RECEIPT',
  STORAGE_CONDITION: 'STORAGE_CONDITION',
} as const;

export type ChainEventType = (typeof ChainEventType)[keyof typeof ChainEventType];

// Nhãn hiển thị tiếng Việt
export const ChainEventTypeLabel: Record<ChainEventType, string> = {
  [ChainEventType.HARVEST]: 'Thu hoạch',
  [ChainEventType.PREPROCESSING]: 'Sơ chế và phân loại',
  [ChainEventType.PACKAGING]: 'Đóng gói',
  [ChainEventType.TRANSPORT]: 'Vận chuyển',
  [ChainEventType.PROCUREMENT]: 'Thu mua',
  [ChainEventType.CORRECTION]: 'Đính chính',
  [ChainEventType.WAREHOUSE_RECEIPT]: 'Nhập kho',
  [ChainEventType.STORAGE_CONDITION]: 'Điều kiện bảo quản',
};

// English display labels for event types
export const ChainEventTypeEnLabel: Record<ChainEventType, string> = {
  [ChainEventType.HARVEST]: 'Harvesting',
  [ChainEventType.PREPROCESSING]: 'Preprocessing and grading',
  [ChainEventType.PACKAGING]: 'Packaging',
  [ChainEventType.TRANSPORT]: 'Transport',
  [ChainEventType.PROCUREMENT]: 'Procurement',
  [ChainEventType.CORRECTION]: 'Correction',
  [ChainEventType.WAREHOUSE_RECEIPT]: 'Warehouse Receipt',
  [ChainEventType.STORAGE_CONDITION]: 'Storage Condition',
};
