import type { ShipmentSplitAllocation, ShipmentSplitPreview } from '@/types/shipmentSplit';

export interface SplitValidationResult {
  errors: string[];
  allocatedQuantity: number;
  remainingQuantity: number;
  isValid: boolean;
}

interface ParsedCode {
  prefix: string;
  value: number;
}

function parseCode(code: string): ParsedCode | null {
  const match = code.trim().match(/^(.*?)(\d+)$/);
  if (!match) return null;
  return { prefix: match[1], value: Number(match[2]) };
}

export function validateShipmentSplit(
  preview: ShipmentSplitPreview,
  allocations: ShipmentSplitAllocation[],
): SplitValidationResult {
  const errors: string[] = [];
  const allocatedQuantity = allocations.reduce(
    (total, item) => total + (Number.isFinite(item.quantity) ? item.quantity : 0),
    0,
  );
  const remainingQuantity = preview.assignableQuantity - allocatedQuantity;

  if (allocations.length < 2) errors.push('Cần tạo ít nhất 2 lô con.');

  const partnerIds = allocations.map((item) => item.recipientOrganizationId).filter(Boolean);
  if (new Set(partnerIds).size !== partnerIds.length) {
    errors.push('Mỗi đối tác chỉ được nhận một lô con.');
  }

  if (allocations.some((item) => !item.recipientOrganizationId || !item.name.trim())) {
    errors.push('Vui lòng nhập đủ đối tác nhận và tên lô con.');
  }
  if (allocations.some((item) => item.name.trim().length > 255)) {
    errors.push('Tên lô con không được vượt quá 255 ký tự.');
  }
  if (allocations.some((item) => !Number.isInteger(item.quantity) || item.quantity <= 0)) {
    errors.push('Số lượng của mỗi lô con phải là số nguyên lớn hơn 0.');
  }
  if (allocations.some((item) => !item.fromCode.trim() || !item.toCode.trim())) {
    errors.push('Vui lòng nhập đủ mã bắt đầu và mã kết thúc.');
  }
  if (allocations.some((item) => (item.packagingInfo?.length ?? 0) > 500)) {
    errors.push('Quy cách đóng gói không được vượt quá 500 ký tự.');
  }
  if (remainingQuantity !== 0) {
    errors.push(
      remainingQuantity > 0
        ? `Còn thiếu ${remainingQuantity.toLocaleString('vi-VN')} tem chưa phân bổ.`
        : `Đã phân bổ vượt ${Math.abs(remainingQuantity).toLocaleString('vi-VN')} tem.`,
    );
  }

  const parentStart = parseCode(preview.availableCodeRange.fromCode);
  const parentEnd = parseCode(preview.availableCodeRange.toCode);
  const parsedRanges = allocations.map((item) => ({
    start: parseCode(item.fromCode),
    end: parseCode(item.toCode),
    quantity: item.quantity,
  }));
  const expectedPrefix = parentStart?.prefix;
  const codesAreComparable = Boolean(
    parentStart &&
      parentEnd &&
      parentStart.prefix === parentEnd.prefix &&
      parsedRanges.every(
        ({ start, end }) => start && end && start.prefix === expectedPrefix && end.prefix === expectedPrefix,
      ),
  );

  if (!codesAreComparable) {
    errors.push('Các mã phải có cùng tiền tố và kết thúc bằng số để kiểm tra dải mã.');
  } else {
    const ranges = parsedRanges as Array<{ start: ParsedCode; end: ParsedCode; quantity: number }>;
    if (ranges[0]?.start.value !== parentStart!.value) {
      errors.push('Mã bắt đầu của lô con đầu tiên phải trùng mã bắt đầu của lô cha.');
    }
    if (ranges.at(-1)?.end.value !== parentEnd!.value) {
      errors.push('Mã kết thúc của lô con cuối cùng phải trùng mã kết thúc của lô cha.');
    }
    ranges.forEach((range, index) => {
      if (range.end.value < range.start.value || range.end.value - range.start.value + 1 !== range.quantity) {
        errors.push(`Khoảng mã của lô con ${index + 1} không khớp số lượng.`);
      }
      if (index > 0 && range.start.value !== ranges[index - 1].end.value + 1) {
        errors.push(`Khoảng mã giữa lô con ${index} và ${index + 1} bị hở hoặc chồng lấn.`);
      }
    });
  }

  return { errors: [...new Set(errors)], allocatedQuantity, remainingQuantity, isValid: errors.length === 0 };
}
