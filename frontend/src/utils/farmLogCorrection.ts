import type { FarmLog } from '@/types/farmLog';

/**
 * NCL-03-CN-006 – GAP: các helper hiển thị đính chính nhật ký canh tác
 * (nhóm bản gốc + bản đính chính, so sánh trường thay đổi, định dạng hiển thị).
 */

export const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  PLANTING: 'Gieo trồng',
  WATERING: 'Tưới nước',
  FERTILIZING: 'Bón phân',
  PESTICIDE: 'Phun thuốc',
  WEEDING: 'Làm cỏ',
  HARVESTING: 'Thu hoạch',
  OTHER: 'Khác',
};

export const getActivityLabel = (value: string): string =>
  ACTIVITY_TYPE_LABELS[value] || value;

/**
 * Nhóm danh sách nhật ký: mỗi bản gốc được theo sau bởi các bản đính chính
 * của nó (mới nhất trước). Bản đính chính lẻ (bản gốc không nằm trong danh
 * sách hiện tại) được nối ở cuối để không bị mất.
 */
export function groupLogsWithCorrections(logs: FarmLog[]): FarmLog[] {
  const correctionByOriginal = new Map<string, FarmLog[]>();
  const originals: FarmLog[] = [];

  for (const log of logs) {
    if (log.isCorrection && log.originalFarmLogId) {
      const arr = correctionByOriginal.get(log.originalFarmLogId) ?? [];
      arr.push(log);
      correctionByOriginal.set(log.originalFarmLogId, arr);
    } else {
      originals.push(log);
    }
  }

  const grouped: FarmLog[] = [];
  const seenIds = new Set<string>();

  for (const original of originals) {
    grouped.push(original);
    seenIds.add(original.id);
    const corrections = correctionByOriginal.get(original.id);
    if (corrections) {
      corrections.sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
      );
      for (const c of corrections) {
        seenIds.add(c.id);
        grouped.push(c);
      }
    }
  }

  for (const log of logs) {
    if (!seenIds.has(log.id)) {
      grouped.push(log);
    }
  }

  return grouped;
}

/**
 * Map: originalId → bản đính chính mới nhất (dùng để so sánh trường nào
 * thay đổi so với bản gốc).
 */
export function buildCorrectionMap(logs: FarmLog[]): Map<string, FarmLog> {
  const map = new Map<string, FarmLog>();

  for (const log of logs) {
    if (log.isCorrection && log.originalFarmLogId) {
      const existing = map.get(log.originalFarmLogId);
      if (!existing || new Date(log.createdAt) > new Date(existing.createdAt)) {
        map.set(log.originalFarmLogId, log);
      }
    }
  }

  return map;
}

/**
 * So sánh giá trị hiện tại của bản gốc với bản đính chính mới nhất để đánh
 * dấu gạch ngang trên đúng trường đã bị thay đổi (chuẩn GAP).
 */
export function isFieldChanged(
  original: FarmLog,
  correction: FarmLog | undefined,
  field: keyof FarmLog,
): boolean {
  if (!original.isCorrected || !correction) return false;
  return String(original[field] ?? '') !== String(correction[field] ?? '');
}

export const formatDateTime = (dateStr: string) => {
  try {
    const d = new Date(dateStr);
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  } catch {
    return dateStr;
  }
};
