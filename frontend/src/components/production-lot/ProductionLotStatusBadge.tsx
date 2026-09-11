import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { ProductionLot } from "@/types/productionLot";
import type { InspectionValidityStatus } from "@/types/certification";

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

// ============================================================
// NCL-11-CN-004: Trạng thái hiệu lực kết quả kiểm nghiệm
// ============================================================

/** Nhãn hiển thị cho trạng thái hiệu lực kiểm nghiệm (tiếng Việt). */
export const INSPECTION_VALIDITY_LABELS: Record<InspectionValidityStatus, string> = {
  NOT_REQUIRED: "Không yêu cầu kiểm nghiệm",
  NO_VALID_RESULT: "Chưa có kết quả kiểm nghiệm",
  VALID: "Còn hiệu lực",
  EXPIRING: "Sắp hết hiệu lực",
  EXPIRED: "Hết hiệu lực",
};

/** Ánh xạ màu cho trạng thái hiệu lực kiểm nghiệm. */
export const INSPECTION_VALIDITY_CLASSES: Record<InspectionValidityStatus, string> = {
  NOT_REQUIRED: "bg-slate-500/10 text-slate-600 border-slate-200",
  NO_VALID_RESULT: "bg-amber-500/10 text-amber-700 border-amber-200",
  VALID: "bg-emerald-500/10 text-emerald-700 border-emerald-200",
  EXPIRING: "bg-orange-500/10 text-orange-700 border-orange-200",
  EXPIRED: "bg-rose-500/10 text-rose-700 border-rose-200",
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

interface InspectionValidityBadgeProps {
  status: InspectionValidityStatus;
  className?: string;
}

/**
 * Badge trạng thái hiệu lực kết quả kiểm nghiệm (NCL-11-CN-004).
 * Tái sử dụng Badge component hiện có, không tạo component mới.
 */
export function InspectionValidityBadge({ status, className }: InspectionValidityBadgeProps) {
  return (
    <Badge
      variant="outline"
      className={cn(INSPECTION_VALIDITY_CLASSES[status], className)}
    >
      {INSPECTION_VALIDITY_LABELS[status]}
    </Badge>
  );
}