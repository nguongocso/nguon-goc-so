import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { isAxiosError } from "axios";

import type {
  ProductFeedbackSeverity,
  ProductFeedbackStatus,
} from "@/types/productFeedback";

export const PRODUCT_FEEDBACK_STATUS_LABELS: Record<ProductFeedbackStatus, string> = {
  NEW: "Mới",
  IN_PROGRESS: "Đang xử lý",
  ESCALATED_TO_RECALL: "Đã chuyển thu hồi",
  CLOSED: "Đã đóng",
};

export const PRODUCT_FEEDBACK_SEVERITY_LABELS: Record<ProductFeedbackSeverity, string> = {
  INFORMATION: "Thông tin",
  QUALITY_SUSPECTED: "Nghi ngờ chất lượng",
  COUNTERFEIT_SUSPECTED: "Nghi ngờ tem giả",
};

export function formatProductFeedbackDate(value?: string): string {
  if (!value) return "—";
  try {
    return format(new Date(value), "dd/MM/yyyy HH:mm", { locale: vi });
  } catch {
    return value;
  }
}

export function getProductFeedbackErrorMessage(error: unknown, fallback: string): string {
  return isAxiosError<{ message?: string }>(error)
    ? error.response?.data?.message ?? fallback
    : fallback;
}

export function ProductFeedbackStatusPill({ status }: { status: ProductFeedbackStatus }) {
  const color = {
    NEW: "bg-blue-50 text-blue-700",
    IN_PROGRESS: "bg-amber-50 text-amber-700",
    ESCALATED_TO_RECALL: "bg-red-50 text-red-700",
    CLOSED: "bg-emerald-50 text-emerald-700",
  }[status];

  return (
    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${color}`}>
      {PRODUCT_FEEDBACK_STATUS_LABELS[status]}
    </span>
  );
}
