import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { ProductionLot } from "@/types/productionLot";

export type ProductionLotStatus = ProductionLot["status"];

/** Single source of truth for production lot status labels (Vietnamese). */
export const PRODUCTION_LOT_STATUS_LABELS: Record<ProductionLotStatus, string> = {
  DRAFT: "Bản nháp",
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Bị từ chối",
  HARVESTED: "Đã thu hoạch",
  PREPROCESSED: "Đã sơ chế",
  PACKAGED: "Đã đóng gói",
  CLOSED: "Đã kết thúc",
  RECALLED: "Đã thu hồi",
  CANCELLED: "Đã hủy",
  DISPOSED: "Đã loại bỏ",
};

/**
 * Presentation-only mapping using the project's --status-* design tokens.
 * Colors mirror the existing pills (bg-status-x/10 + text-status-x) with a
 * subtle border, matching the ShipmentStatusBadge pattern.
 */
export const PRODUCTION_LOT_STATUS_CLASSES: Record<ProductionLotStatus, string> = {
  DRAFT: "bg-status-draft/10 text-status-draft border-status-draft/20",
  PENDING: "bg-status-pending/10 text-status-pending border-status-pending/20",
  APPROVED: "bg-status-approved/10 text-status-approved border-status-approved/20",
  REJECTED: "bg-status-rejected/10 text-status-rejected border-status-rejected/20",
  HARVESTED: "bg-status-harvested/10 text-status-harvested border-status-harvested/20",
  PREPROCESSED: "bg-status-shipped/10 text-status-shipped border-status-shipped/20",
  PACKAGED: "bg-status-packaged/10 text-status-packaged border-status-packaged/20",
  CLOSED: "bg-status-completed/10 text-status-completed border-status-completed/20",
  RECALLED: "bg-status-rejected/10 text-status-rejected border-status-rejected/20",
  CANCELLED: "bg-status-rejected/10 text-status-rejected border-status-rejected/20",
  DISPOSED: "bg-status-rejected/10 text-status-rejected border-status-rejected/20",
};

interface ProductionLotStatusBadgeProps {
  status: ProductionLotStatus;
  className?: string;
}

/** Single presentation component for production lot statuses. */
export function ProductionLotStatusBadge({ status, className }: ProductionLotStatusBadgeProps) {
  return (
    <Badge
      variant="outline"
      className={cn(PRODUCTION_LOT_STATUS_CLASSES[status], className)}
    >
      {PRODUCTION_LOT_STATUS_LABELS[status]}
    </Badge>
  );
}