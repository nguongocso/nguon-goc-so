import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Eye, Link as LinkIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { InspectionRequestStatusDisplay } from "@/types/certification";
import { IssueInspectionResultLinkDialog } from "./IssueInspectionResultLinkDialog";

interface InspectionRequestActionButtonsProps {
  testRequestId: string;
  lotId: string;
  status: InspectionRequestStatusDisplay;
  testingUnitName?: string;
  testingUnitEmail?: string;
  onLinkIssued?: () => void;
}

/**
 * Shared action buttons for an inspection request row.
 *
 * Reused by:
 *   - InspectionRequestHistoryModal  (modal lịch sử tìm kiếm)
 *   - ProductionLotDetailPage        (màn hình lịch sử chính — card list)
 *
 * Rendering rules (Action Matrix):
 *   PENDING           → "Ghi nhận kết quả" text button + "Cấp link" icon/button
 *   PASSED / FAILED   → Eye icon (Xem chi tiết)
 *   CANCELLED / other → null (no action)
 *
 * Both actions navigate to the SAME route handled by RecordInspectionResultPage:
 *   /production-lots/{lotId}/inspection-requests/{testRequestId}/results
 */
export const InspectionRequestActionButtons: React.FC<InspectionRequestActionButtonsProps> = ({
  testRequestId,
  lotId,
  status,
  testingUnitName = "Đơn vị kiểm nghiệm",
  testingUnitEmail = "",
  onLinkIssued,
}) => {
  const navigate = useNavigate();
  const [isIssueLinkOpen, setIsIssueLinkOpen] = useState(false);

  const handleNavigate = () => {
    navigate(
      `/production-lots/${lotId}/inspection-requests/${testRequestId}/results`,
    );
  };

  // PASSED / FAILED → Eye icon "Xem chi tiết"
  if (status === "PASSED" || status === "FAILED") {
    return (
      <Button
        size="icon-sm"
        variant="ghost"
        aria-label="Xem chi tiết"
        title="Xem chi tiết"
        className="text-muted-foreground hover:text-primary hover:bg-primary/10"
        onClick={handleNavigate}
      >
        <Eye className="h-4 w-4" />
      </Button>
    );
  }

  // PENDING → "Ghi nhận kết quả" + "Cấp link cho đơn vị"
  if (status === "PENDING") {
    return (
      <>
        <div className="flex items-center gap-1.5">
          <Button
            size="sm"
            variant="outline"
            className="text-xs font-semibold"
            title="Ghi nhận kết quả thủ công"
            onClick={handleNavigate}
          >
            Ghi nhận kết quả
          </Button>

          <Button
            size="sm"
            variant="outline"
            className="h-8 px-2.5 text-xs text-primary hover:bg-primary/10 border-primary/30"
            title="Cấp liên kết nhập kết quả cho đơn vị kiểm nghiệm"
            onClick={() => setIsIssueLinkOpen(true)}
          >
            <LinkIcon className="h-3.5 w-3.5 mr-1 text-primary" />
            Cấp link
          </Button>
        </div>

        <IssueInspectionResultLinkDialog
          requestId={testRequestId}
          testingUnitName={testingUnitName}
          defaultEmail={testingUnitEmail}
          isOpen={isIssueLinkOpen}
          onClose={() => setIsIssueLinkOpen(false)}
          onSuccess={() => {
            if (onLinkIssued) onLinkIssued();
          }}
        />
      </>
    );
  }

  // CANCELLED or any other status → no action
  return null;
};
