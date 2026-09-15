import { describe, expect, it } from 'vitest';
import {
  calculateGeodesicAreaHa,
  calculateAreaDeviation,
  hasSelfIntersection,
  parseCoordinatesText,
} from '../geoAreaCalculator';

describe('geoAreaCalculator', () => {
  describe('calculateGeodesicAreaHa', () => {
    it('trả về 0 khi danh sách đỉnh rỗng hoặc < 3 đỉnh', () => {
      expect(calculateGeodesicAreaHa([])).toBe(0);
      expect(calculateGeodesicAreaHa([{ latitude: 21.0, longitude: 105.0 }])).toBe(0);
      expect(
        calculateGeodesicAreaHa([
          { latitude: 21.0, longitude: 105.0 },
          { latitude: 21.1, longitude: 105.1 },
        ])
      ).toBe(0);
    });

    it('tính diện tích đa giác xấp xỉ hợp lý', () => {
      // Đa giác 4 điểm khoảng 0.01 độ vĩ/kinh (khoảng 1km x 1km = 100 ha)
      const points = [
        { latitude: 21.0, longitude: 105.0 },
        { latitude: 21.01, longitude: 105.0 },
        { latitude: 21.01, longitude: 105.01 },
        { latitude: 21.0, longitude: 105.01 },
      ];
      const areaHa = calculateGeodesicAreaHa(points);
      expect(areaHa).toBeGreaterThan(100);
      expect(areaHa).toBeLessThan(130);
    });
  });

  describe('calculateAreaDeviation', () => {
    it('tính phần trăm chênh lệch chính xác', () => {
      expect(calculateAreaDeviation(1.0, 1.45)).toBe(45.0);
      expect(calculateAreaDeviation(2.0, 1.6)).toBe(20.0);
      expect(calculateAreaDeviation(1.0, 1.0)).toBe(0.0);
    });

    it('trả về 0 khi diện tích khai báo <= 0', () => {
      expect(calculateAreaDeviation(0, 5.0)).toBe(0);
    });
  });

  describe('hasSelfIntersection', () => {
    it('không báo lỗi với đa giác hợp lệ', () => {
      expect(
        hasSelfIntersection([
          { latitude: 21, longitude: 105 },
          { latitude: 21, longitude: 105.01 },
          { latitude: 21.01, longitude: 105.01 },
          { latitude: 21.01, longitude: 105 },
        ])
      ).toBe(false);
    });

    it('phát hiện đa giác hình nơ có hai cạnh tự cắt', () => {
      expect(
        hasSelfIntersection([
          { latitude: 21, longitude: 105 },
          { latitude: 21.01, longitude: 105.01 },
          { latitude: 21.01, longitude: 105 },
          { latitude: 21, longitude: 105.01 },
        ])
      ).toBe(true);
    });

    it('phát hiện ranh giới tự cắt theo tọa độ tái hiện từ màn hình chỉnh sửa', () => {
      expect(
        hasSelfIntersection([
          { latitude: 21.586174, longitude: 105.807344 },
          { latitude: 21.584359, longitude: 105.807001 },
          { latitude: 21.584658, longitude: 105.807816 },
          { latitude: 21.585915, longitude: 105.806604 },
        ])
      ).toBe(true);
    });

    it('không báo lỗi với tam giác', () => {
      expect(
        hasSelfIntersection([
          { latitude: 21, longitude: 105 },
          { latitude: 21, longitude: 105.01 },
          { latitude: 21.01, longitude: 105 },
        ])
      ).toBe(false);
    });
  });

  describe('parseCoordinatesText', () => {
    it('parse thành công danh sách hợp lệ phân cách bằng dấu phẩy', () => {
      const text = `
        21.0285, 105.8542
        21.0300, 105.8560
        21.0270, 105.8580
      `;
      const result = parseCoordinatesText(text);
      expect(result.errors).toHaveLength(0);
      expect(result.points).toHaveLength(3);
      expect(result.points[0]).toEqual({ latitude: 21.0285, longitude: 105.8542 });
    });

    it('báo lỗi khi nội dung rỗng', () => {
      const result = parseCoordinatesText('   ');
      expect(result.errors.length).toBeGreaterThan(0);
      expect(result.points).toHaveLength(0);
    });

    it('báo lỗi khi ít hơn 3 đỉnh', () => {
      const text = `
        21.0285, 105.8542
        21.0300, 105.8560
      `;
      const result = parseCoordinatesText(text);
      expect(result.errors.some((e) => e.includes('tối thiểu 3 đỉnh'))).toBe(true);
    });

    it('báo lỗi khi tọa độ ngoài miền hợp lệ', () => {
      const text = `
        121.0285, 105.8542
        21.0300, 105.8560
        21.0270, 105.8580
      `;
      const result = parseCoordinatesText(text);
      expect(result.errors.some((e) => e.includes('Vĩ độ'))).toBe(true);
    });

    it('báo lỗi khi đỉnh liên tiếp trùng nhau', () => {
      const text = `
        21.0285, 105.8542
        21.0285, 105.8542
        21.0270, 105.8580
      `;
      const result = parseCoordinatesText(text);
      expect(result.errors.some((e) => e.includes('trùng lặp'))).toBe(true);
    });
  });
});
