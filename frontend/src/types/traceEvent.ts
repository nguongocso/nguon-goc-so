/** Dữ liệu yêu cầu yêu cầu ghi nhận sự kiện thu hoạch nông sản từ vùng trồng. */
export interface HarvestEventPayload {
  productionLotId: string;
  harvestDate: string;
  quantity: number;
  latitude?: number;
  longitude?: number;
  images?: string[];
  earlyHarvestReason?: string;
}

/** Phản hồi chi tiết sau khi ghi nhận sự kiện thu hoạch. */
export interface HarvestEventResponse {
  success: boolean;
  status: number;
  data: {
    id: string;
    productionLotId: string;
    productionLotName: string;
    eventType: string;
    harvestDate: string;
    quantity: number;
    recordedByName: string;
    recordedAt: string;
  };
  timestamp: string;
}
