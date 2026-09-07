import { useNavigate } from "react-router-dom";
import { Eye } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { InspectionRequestStatusDisplay } from "@/types/certification";

interface InspectionRequestActionButtonsProps {
  testRequestId: string;
  lotId: string;
  status: InspectionRequestStatusDisplay;
}

/**
 * Shared action buttons for an inspection request row.
 *
 * Reused by:
 *   - InspectionRequestHistoryModal  (modal lịch sử tìm kiếm)
 *   - ProductionLotDetailPage        (màn hình lịch sử chính — card list)
 *
 * Rendering rules (Action Matrix):
 *   PENDING           → "Ghi nhận kết quả" text button
 *   PASSED / FAILED   → Eye icon (Xem chi tiết)
 *   CANCELLED / other → null (no action)
 *
 * Both actions navigate to the SAME route handled by RecordInspectionResultPage:
 *   /production-lots/{lotId}/inspection-requests/{testRequestId}/results
 */
export const InspectionRequestActionButtons = ({
  testRequestId,
  lotId,
  status,
}: InspectionRequestActionButtonsProps) => {
  const navigate = useNavigate();

  const handleNavigate = () => {
    navigate(
      `/production-lots/${lotId}/inspection-requests/${testRequestId}/results`,
    );
  };

  // PASSED / FAILED → Eye icon "Xem chi tiết"
  if (status === "PASSED" || status === "FAILED") {
    return (
      <Button
        size="sm"
        variant="outline"
        className="h-8 w-8 p-0 text-slate-600 hover:bg-emerald-50 hover:text-emerald-800"
        title="Xem chi tiết"
        onClick={handleNavigate}
      >
        <Eye className="h-4 w-4" />
      </Button>
    );
  }

  // PENDING → "Ghi nhận kết quả"
  if (status === "PENDING") {
    return (
      <Button
        size="sm"
        variant="outline"
        className="text-xs font-semibold"
        title="Ghi nhận kết quả"
        onClick={handleNavigate}
      >
        Ghi nhận kết quả
      </Button>
    );
  }

  // CANCELLED or any other status → no action
  return null;
};
