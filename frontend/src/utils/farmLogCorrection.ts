import {
  Bug,
  Droplets,
  FlaskConical,
  MoreHorizontal,
  Scissors,
  Sprout,
  Wheat,
  type LucideIcon,
} from 'lucide-react';
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

/** Biểu tượng trực quan cho từng loại hoạt động canh tác. */
export const ACTIVITY_TYPE_ICONS: Record<string, LucideIcon> = {
  PLANTING: Sprout,
  WATERING: Droplets,
  FERTILIZING: FlaskConical,
  PESTICIDE: Bug,
  WEEDING: Scissors,
  HARVESTING: Wheat,
  OTHER: MoreHorizontal,
};

export const getActivityLabel = (value: string): string =>
  ACTIVITY_TYPE_LABELS[value] || value;

/**
 * Một nhóm nhật ký: bản gốc + danh sách các bản đính chính của nó
 * (sắp xếp mới nhất trước). Hiển thị nhóm như một dòng duy nhất.
 */
export interface FarmLogGroup {
  original: FarmLog;
  /** Các bản đính chính, bản mới nhất đứng đầu. */
  corrections: FarmLog[];
}

/**
 * Nhóm danh sách nhật ký theo mối quan hệ bản gốc – bản đính chính.
 * Bản đính chính lẻ (bản gốc không nằm trong danh sách hiện tại) được giữ
 * như nhóm độc lập để không bị mất.
 */
export function buildFarmLogGroups(logs: FarmLog[]): FarmLogGroup[] {
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

  for (const arr of correctionByOriginal.values()) {
    arr.sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  }

  const groups: FarmLogGroup[] = originals.map((original) => ({
    original,
    corrections: correctionByOriginal.get(original.id) ?? [],
  }));

  // Bản đính chính lẻ
  const seenOriginals = new Set(originals.map((o) => o.id));
  for (const [originalId, arr] of correctionByOriginal.entries()) {
    if (!seenOriginals.has(originalId)) {
      for (const c of arr) {
        groups.push({ original: c, corrections: [] });
      }
    }
  }

  return groups;
}

/**
 * Giá trị hiệu lực của nhóm: bản đính chính mới nhất nếu có, ngược lại là bản gốc.
 */
export function getLatestEffective(group: FarmLogGroup): FarmLog {
  return group.corrections.length > 0 ? group.corrections[0] : group.original;
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

