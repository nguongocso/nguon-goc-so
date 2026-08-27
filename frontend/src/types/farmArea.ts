export interface CropType {
  id: string;
  name: string;
}

// ── Mở rộng AreaUnit ──────────────────────────────────────
export type AreaUnit = 'HA' | 'KM2' | 'M2' | 'SAO' | 'CONG' | 'MAU';

export const AREA_UNIT_LABELS: Record<AreaUnit, string> = {
  HA: 'Hecta (ha)',
  KM2: 'Kilômét vuông (km²)',
  M2: 'Mét vuông (m²)',
  SAO: 'Sào (1.000 m²)',
  CONG: 'Công (1.000 m²)',
  MAU: 'Mẫu (10.000 m²)',
};

// ── Hằng số quy đổi (chuẩn Nam Bộ) ────────────────────────
const M2_PER_HA = 10_000;
const SAO_PER_HA = 10;   // 1 sào = 1000 m² = 0.1 ha
const CONG_PER_HA = 10;  // 1 công = 1000 m² = 0.1 ha
const MAU_PER_HA = 1;    // 1 mẫu = 10.000 m² = 1 ha

/**
 * Chuyển đổi diện tích từ ha về đơn vị hiển thị.
 * Backend luôn lưu diện tích gốc theo ha.
 */
export function convertAreaFromHa(areaInHa: number, unit: AreaUnit): number {
  switch (unit) {
    case 'KM2':
      return areaInHa / 100;          // 1 km² = 100 ha
    case 'M2':
      return areaInHa * M2_PER_HA;
    case 'SAO':
      return areaInHa * SAO_PER_HA;
    case 'CONG':
      return areaInHa * CONG_PER_HA;
    case 'MAU':
      return areaInHa * MAU_PER_HA;
    default:
      return areaInHa;               // HA giữ nguyên
  }
}

/**
 * Chuyển đổi diện tích từ đơn vị người dùng về ha.
 * Dùng trước khi gửi lên backend nếu backend chỉ hiểu HA/KM2.
 */
export function convertAreaToHa(area: number, unit: AreaUnit): number {
  switch (unit) {
    case 'KM2':
      return area * 100;
    case 'M2':
      return area / M2_PER_HA;
    case 'SAO':
      return area / SAO_PER_HA;
    case 'CONG':
      return area / CONG_PER_HA;
    case 'MAU':
      return area / MAU_PER_HA;
    default:
      return area; // HA
  }
}

export interface FarmArea {
  id: string;
  name: string;
  organizationId: string;
  organizationName: string;
  cropTypeId: string;
  cropTypeName: string;
  latitude: number;
  longitude: number;
  area: number; // luôn là ha, backend trả về
  areaUnit: AreaUnit;
  isActive: boolean;
  associatedLotsCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFarmAreaRequest {
  name: string;
  cropType: string;
  latitude: number;
  longitude: number;
  area: number;
  areaUnit: AreaUnit;
}

export interface UpdateFarmAreaRequest {
  name: string;
  cropType: string;
  latitude: number;
  longitude: number;
  area: number;
  areaUnit: AreaUnit;
}

export interface CreateFarmAreaResponse {
  success: boolean;
  status: number;
  data: FarmArea;
  timestamp: string;
}