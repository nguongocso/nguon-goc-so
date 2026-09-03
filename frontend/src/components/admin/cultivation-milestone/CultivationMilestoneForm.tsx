import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { CultivationMilestoneFormContent } from "./CultivationMilestoneFormContent";
import type { CultivationMilestone } from "@/types/cultivationMilestone";

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  milestone?: CultivationMilestone | null;
}

export const CultivationMilestoneForm = ({
  open,
  onClose,
  onSuccess,
  milestone,
}: Props) => {
  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {milestone
              ? "Cập nhật mốc canh tác"
              : "Thêm mới mốc canh tác"}
          </DialogTitle>
        </DialogHeader>
        <CultivationMilestoneFormContent
          open={open}
          milestone={milestone}
          onSuccess={() => {
            onSuccess();
            onClose();
          }}
          onCancel={onClose}
        />
      </DialogContent>
    </Dialog>
  );
};
