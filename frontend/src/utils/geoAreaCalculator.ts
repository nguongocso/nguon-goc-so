import type { LatLng } from '@/types/farmArea';

/** Bán kính Trái Đất trung bình theo chuẩn WGS84 (mét). */
const EARTH_RADIUS_METERS = 6378137.0;

/**
 * Tính diện tích đa giác trắc địa trên mặt cầu WGS84 (đơn vị hecta).
 * Dùng để hiển thị diện tích tạm tính (preview) cho người dùng trước khi lưu.
 *
 * @param points Danh sách các đỉnh theo thứ tự nối vòng.
 * @returns Diện tích theo hecta (làm tròn 4 chữ số thập phân), trả về 0 nếu < 3 đỉnh.
 */
export function calculateGeodesicAreaHa(points: LatLng[]): number {
  if (!points || points.length < 3) {
    return 0;
  }

  let totalAngle = 0.0;
  const numPoints = points.length;

  for (let i = 0; i < numPoints; i++) {
    const p1 = points[i];
    const p2 = points[(i + 1) % numPoints];

    const lat1Rad = (p1.latitude * Math.PI) / 180;
    const lat2Rad = (p2.latitude * Math.PI) / 180;
    const lngDiffRad = ((p2.longitude - p1.longitude) * Math.PI) / 180;

    // Tích phân mặt cầu (spherical trapezoidal area)
    totalAngle += lngDiffRad * (2 + Math.sin(lat1Rad) + Math.sin(lat2Rad));
  }

  const areaSqMeters = Math.abs((totalAngle * EARTH_RADIUS_METERS * EARTH_RADIUS_METERS) / 2.0);
  const areaHa = areaSqMeters / 10000.0;

  return Math.round(areaHa * 10000) / 10000;
}

/**
 * Tính tỷ lệ chênh lệch phần trăm giữa diện tích tính toán và diện tích khai báo.
 *
 * @param declaredArea Diện tích khai báo (ha).
 * @param calculatedArea Diện tích tính toán (ha).
 * @returns Tỷ lệ phần trăm chênh lệch (làm tròn 2 chữ số thập phân).
 */
export function calculateAreaDeviation(declaredArea: number, calculatedArea: number): number {
  if (declaredArea <= 0) {
    return 0;
  }
  const deviation = (Math.abs(calculatedArea - declaredArea) / declaredArea) * 100;
  return Math.round(deviation * 100) / 100;
}

export interface ParseResult {
  points: LatLng[];
  errors: string[];
}

/**
 * Phân tích danh sách tọa độ văn bản (ví dụ copy từ Google Earth/GPS).
 * Hỗ trợ các định dạng dòng: "vĩ độ, kinh độ", "vĩ độ kinh độ", hoặc cách nhau bởi tab.
 *
 * @param text Chuỗi văn bản dán từ clipboard.
 * @returns Danh sách đỉnh hợp lệ hoặc danh sách lỗi theo từng dòng.
 */
export function parseCoordinatesText(text: string): ParseResult {
  const points: LatLng[] = [];
  const errors: string[] = [];

  if (!text || text.trim().length === 0) {
    return { points: [], errors: ['Nội dung tọa độ đang để trống.'] };
  }

  const lines = text.split(/\r?\n/);

  for (let index = 0; index < lines.length; index++) {
    const rawLine = lines[index].trim();
    if (!rawLine) {
      continue; // Bỏ qua dòng trống
    }

    const lineNumber = index + 1;

    // Tách bằng dấu phẩy, tab, hoặc khoảng trắng
    let parts = rawLine.includes(',')
      ? rawLine.split(',').map((s) => s.trim())
      : rawLine.split(/\s+/).map((s) => s.trim());

    if (parts.length !== 2) {
      errors.push(`Dòng ${lineNumber}: Sai định dạng (cần "vĩ độ, kinh độ"): "${rawLine}"`);
      continue;
    }

    const lat = Number(parts[0]);
    const lng = Number(parts[1]);

    if (isNaN(lat) || isNaN(lng)) {
      errors.push(`Dòng ${lineNumber}: Tọa độ không phải là số hợp lệ: "${rawLine}"`);
      continue;
    }

    if (lat < -90 || lat > 90) {
      errors.push(`Dòng ${lineNumber}: Vĩ độ (${lat}) vượt ngoài dải hợp lệ [-90, 90]`);
      continue;
    }

    if (lng < -180 || lng > 180) {
      errors.push(`Dòng ${lineNumber}: Kinh độ (${lng}) vượt ngoài dải hợp lệ [-180, 180]`);
      continue;
    }

    // Kiểm tra trùng đỉnh liên tiếp
    if (points.length > 0) {
      const prev = points[points.length - 1];
      if (Math.abs(prev.latitude - lat) < 1e-7 && Math.abs(prev.longitude - lng) < 1e-7) {
        errors.push(`Dòng ${lineNumber}: Đỉnh trùng lặp với đỉnh liền trước.`);
        continue;
      }
    }

    points.push({ latitude: lat, longitude: lng });
  }

  // Kiểm tra nếu người dùng lặp điểm đầu ở cuối (vòng khép kín thừa)
  if (points.length >= 2) {
    const first = points[0];
    const last = points[points.length - 1];
    if (Math.abs(first.latitude - last.latitude) < 1e-7 && Math.abs(first.longitude - last.longitude) < 1e-7) {
      errors.push('Điểm cuối cùng trùng với điểm bắt đầu (không cần lặp điểm đóng vòng).');
    }
  }

  if (errors.length === 0 && points.length < 3) {
    errors.push(`Ranh giới phải có tối thiểu 3 đỉnh phân biệt (hiện chỉ có ${points.length} đỉnh).`);
  }

  return { points, errors };
}
