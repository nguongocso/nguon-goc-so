import { ArrowRightLeft } from "lucide-react";
import { Badge } from "@/components/ui/badge";

/**
 * Nhãn "Đang bàn giao" hiển thị khi lô hàng có phiếu bàn giao
 * ở trạng thái chờ xác nhận (NCL-05-CN-008).
 * Trạng thái derived từ API has-pending-handover, không phải cột mới.
 */
export function HandoverPendingBadge({ className }: { className?: string }) {
  return (
    <Badge variant="info" className={className}>
      <ArrowRightLeft className="size-3" />
      Đang bàn giao
    </Badge>
  );
}
