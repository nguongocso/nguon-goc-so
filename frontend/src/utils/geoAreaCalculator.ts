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

const GEOMETRY_EPSILON = 1e-12;

/** Tính tích có hướng của ba điểm trên mặt phẳng kinh độ/vĩ độ. */
function orientation(a: LatLng, b: LatLng, c: LatLng): number {
  return (
    (b.longitude - a.longitude) * (c.latitude - a.latitude) -
    (b.latitude - a.latitude) * (c.longitude - a.longitude)
  );
}

/** Kiểm tra một điểm có nằm trên đoạn thẳng hay không, kể cả hai đầu mút. */
function isPointOnSegment(point: LatLng, start: LatLng, end: LatLng): boolean {
  return (
    Math.abs(orientation(start, end, point)) <= GEOMETRY_EPSILON &&
    point.longitude >= Math.min(start.longitude, end.longitude) - GEOMETRY_EPSILON &&
    point.longitude <= Math.max(start.longitude, end.longitude) + GEOMETRY_EPSILON &&
    point.latitude >= Math.min(start.latitude, end.latitude) - GEOMETRY_EPSILON &&
    point.latitude <= Math.max(start.latitude, end.latitude) + GEOMETRY_EPSILON
  );
}

/** Kiểm tra hai đoạn thẳng có giao nhau hay không. */
function doSegmentsIntersect(a: LatLng, b: LatLng, c: LatLng, d: LatLng): boolean {
  const o1 = orientation(a, b, c);
  const o2 = orientation(a, b, d);
  const o3 = orientation(c, d, a);
  const o4 = orientation(c, d, b);

  if (
    ((o1 > GEOMETRY_EPSILON && o2 < -GEOMETRY_EPSILON) ||
      (o1 < -GEOMETRY_EPSILON && o2 > GEOMETRY_EPSILON)) &&
    ((o3 > GEOMETRY_EPSILON && o4 < -GEOMETRY_EPSILON) ||
      (o3 < -GEOMETRY_EPSILON && o4 > GEOMETRY_EPSILON))
  ) {
    return true;
  }

  return (
    isPointOnSegment(c, a, b) ||
    isPointOnSegment(d, a, b) ||
    isPointOnSegment(a, c, d) ||
    isPointOnSegment(b, c, d)
  );
}

/**
 * Kiểm tra đa giác có cạnh tự cắt nhau hay không.
 * Các cặp cạnh kề nhau được bỏ qua vì chúng luôn dùng chung một đỉnh hợp lệ.
 */
export function hasSelfIntersection(points: LatLng[]): boolean {
  if (points.length < 4) {
    return false;
  }

  for (let firstEdge = 0; firstEdge < points.length; firstEdge++) {
    const firstEdgeEnd = (firstEdge + 1) % points.length;

    for (let secondEdge = firstEdge + 1; secondEdge < points.length; secondEdge++) {
      const secondEdgeEnd = (secondEdge + 1) % points.length;
      const areAdjacent = firstEdgeEnd === secondEdge || secondEdgeEnd === firstEdge;

      if (areAdjacent) {
        continue;
      }

      if (
        doSegmentsIntersect(
          points[firstEdge],
          points[firstEdgeEnd],
          points[secondEdge],
          points[secondEdgeEnd]
        )
      ) {
        return true;
      }
    }
  }

  return false;
}

export interface ParseResult {
  points: LatLng[];
  errors: string[];
}

/** Giới hạn số đỉnh để bảo đảm request và snapshot lịch sử có kích thước an toàn. */
export const MAX_BOUNDARY_POINTS = 500;

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
  let firstContentIndex = 0;
  let lastContentIndex = lines.length - 1;
  while (firstContentIndex <= lastContentIndex && !lines[firstContentIndex].trim()) {
    firstContentIndex++;
  }
  while (lastContentIndex >= firstContentIndex && !lines[lastContentIndex].trim()) {
    lastContentIndex--;
  }

  for (let index = firstContentIndex; index <= lastContentIndex; index++) {
    const rawLine = lines[index].trim();
    if (!rawLine) {
      errors.push(`Dòng ${index + 1}: Không được để dòng trống giữa danh sách tọa độ.`);
      continue;
    }

    const lineNumber = index + 1;

    // Tách bằng dấu phẩy, tab, hoặc khoảng trắng
    let parts = rawLine.includes(',')
      ? rawLine.split(',').map((s) => s.trim())
      : rawLine.split(/\s+/).map((s) => s.trim());

    if (parts.length !== 2 || parts.some((part) => part.length === 0)) {
      errors.push(`Dòng ${lineNumber}: Sai định dạng (cần "vĩ độ, kinh độ"): "${rawLine}"`);
      continue;
    }

    const lat = Number(parts[0]);
    const lng = Number(parts[1]);

    if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
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

  if (points.length > MAX_BOUNDARY_POINTS) {
    errors.push(`Ranh giới chỉ được có tối đa ${MAX_BOUNDARY_POINTS} đỉnh (hiện có ${points.length} đỉnh).`);
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
