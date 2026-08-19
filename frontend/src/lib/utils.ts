import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Che ID nhạy cảm (UUID): chỉ giữ `visibleChars` ký tự đầu, phần còn lại thay bằng 'xxxx'.
 * VD: "123e4567-e89b-12d3-a456-426614174000" → "123e-xxxx-xxxx-xxxx-xxxx"
 */
export function maskId(id?: string | null, visibleChars = 4): string {
  if (!id) return "";
  const trimmed = id.trim();
  if (trimmed.length <= visibleChars) return trimmed;
  return `${trimmed.slice(0, visibleChars)}-xxxx-xxxx-xxxx-xxxx`;
}