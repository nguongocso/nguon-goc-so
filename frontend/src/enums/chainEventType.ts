// Dùng object thay cho enum để tương thích với erasableSyntaxOnly
export const ChainEventType = {
  HARVEST: 'HARVEST',
  PACKAGING: 'PACKAGING',
  TRANSPORT: 'TRANSPORT',
  PROCUREMENT: 'PROCUREMENT',
  CORRECTION: 'CORRECTION',
  WAREHOUSE_RECEIPT: 'WAREHOUSE_RECEIPT',
} as const;

export type ChainEventType = (typeof ChainEventType)[keyof typeof ChainEventType];

// Nhãn hiển thị tiếng Việt
export const ChainEventTypeLabel: Record<ChainEventType, string> = {
  [ChainEventType.HARVEST]: 'Thu hoạch',
  [ChainEventType.PACKAGING]: 'Đóng gói',
  [ChainEventType.TRANSPORT]: 'Vận chuyển',
  [ChainEventType.PROCUREMENT]: 'Thu mua',
  [ChainEventType.CORRECTION]: 'Đính chính',
  [ChainEventType.WAREHOUSE_RECEIPT]: 'Nhập kho',
};

// English display labels for event types
export const ChainEventTypeEnLabel: Record<ChainEventType, string> = {
  [ChainEventType.HARVEST]: 'Harvesting',
  [ChainEventType.PACKAGING]: 'Packaging',
  [ChainEventType.TRANSPORT]: 'Transport',
  [ChainEventType.PROCUREMENT]: 'Procurement',
  [ChainEventType.CORRECTION]: 'Correction',
  [ChainEventType.WAREHOUSE_RECEIPT]: 'Warehouse Receipt',
};
