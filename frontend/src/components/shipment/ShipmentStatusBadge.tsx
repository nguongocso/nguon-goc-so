import type { Shipment } from "@/types/shipment";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export type ShipmentStatus = Shipment["status"];

/** Single source of truth for shipment status labels (Vietnamese). */
export const SHIPMENT_STATUS_LABELS: Record<ShipmentStatus, string> = {
  DRAFT: "Nháp",
  CODE_PRINTED: "Đã in mã",
  ACTIVATED: "Đã kích hoạt",
  RECALLED: "Đã thu hồi",
};

/**
 * Presentation-only mapping using the project's --status-* design tokens.
 * Colors mirror the existing pills (bg-status-x/10 + text-status-x), with the
 * subtle border already used by ShipmentDetailPage.
 */
const SHIPMENT_STATUS_CLASSES: Record<ShipmentStatus, string> = {
  DRAFT: "bg-status-draft/10 text-status-draft border-status-draft/20",
  CODE_PRINTED:
    "bg-status-packaged/10 text-status-packaged border-status-packaged/20",
  ACTIVATED:
    "bg-status-approved/10 text-status-approved border-status-approved/20",
  RECALLED:
    "bg-status-rejected/10 text-status-rejected border-status-rejected/20",
};

interface ShipmentStatusBadgeProps {
  status: ShipmentStatus;
  className?: string;
}

/**
 * Single presentation component for shipment statuses.
 * Replaces the duplicated label/class maps previously kept independently in
 * ShipmentDetailDialog, ShipmentDetailPage, ShipmentList and
 * ProcurementShipmentList. It intentionally contains NO permission or business
 * logic — authorization stays with callers (usePermission / ROLE_ACCESS).
 */
export function ShipmentStatusBadge({ status, className }: ShipmentStatusBadgeProps) {
  return (
    <Badge
      variant="outline"
      className={cn(SHIPMENT_STATUS_CLASSES[status], className)}
    >
      {SHIPMENT_STATUS_LABELS[status]}
    </Badge>
  );
}
