import type { ShipmentHandover } from "@/types/shipmentHandover";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export type HandoverStatus = ShipmentHandover["status"];

/** Nhãn tiếng Việt duy nhất cho trạng thái phiếu bàn giao (NCL-05-CN-008/CN-009). */
export const HANDOVER_STATUS_LABELS: Record<HandoverStatus, string> = {
  PENDING_CONFIRMATION: "Chờ xác nhận",
  ACCEPTED: "Đã xác nhận",
  REJECTED: "Đã từ chối",
  EXPIRED: "Hết hạn",
  CANCELLED: "Đã hủy",
};

/**
 * Bảng màu hiển thị theo trạng thái phiếu bàn giao.
 * Giữ nguyên bộ màu đã có của HandoverDetailPage để không đổi giao diện cũ.
 */
const HANDOVER_STATUS_CLASSES: Record<HandoverStatus, string> = {
  PENDING_CONFIRMATION: "bg-amber-100 text-amber-800",
  ACCEPTED: "bg-emerald-100 text-emerald-800",
  REJECTED: "bg-red-100 text-red-700",
  EXPIRED: "bg-slate-100 text-slate-600",
  CANCELLED: "bg-slate-100 text-slate-600",
};

interface HandoverStatusBadgeProps {
  status: HandoverStatus;
  className?: string;
}

/**
 * Component hiển thị trạng thái phiếu bàn giao.
 * Là nơi duy nhất định nghĩa nhãn/màu trạng thái phiếu bàn giao, được dùng ở
 * trang chi tiết và danh sách phiếu bàn giao nhận. Không chứa logic phân quyền.
 */
export function HandoverStatusBadge({
  status,
  className,
}: HandoverStatusBadgeProps) {
  return (
    <Badge
      variant="secondary"
      className={cn(HANDOVER_STATUS_CLASSES[status], className)}
    >
      {HANDOVER_STATUS_LABELS[status]}
    </Badge>
  );
}