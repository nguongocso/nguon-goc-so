import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Plus, Save } from "lucide-react";
import {
  inspectionCriterionSchema,
  type InspectionCriterionFormValues,
} from "@/utils/validators";
import type { InspectionCriterion } from "@/types/standard";

interface CriterionFormProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (values: InspectionCriterionFormValues) => Promise<void>;
  initialData?: InspectionCriterion | null;
  isLoading?: boolean;
}

export const CriterionForm: React.FC<CriterionFormProps> = ({
  open,
  onClose,
  onSubmit,
  initialData,
  isLoading = false,
}) => {
  const isEdit = !!initialData;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<InspectionCriterionFormValues>({
    resolver: zodResolver(inspectionCriterionSchema),
    defaultValues: {
      criterionCode: "",
      criterionName: "",
      note: "",
    },
  });

  useEffect(() => {
    if (open) {
      if (initialData) {
        reset({
          criterionCode: initialData.code,
          criterionName: initialData.name,
          note: initialData.note || "",
        });
      } else {
        reset({ criterionCode: "", criterionName: "", note: "" });
      }
    }
  }, [open, initialData, reset]);

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? "Cập nhật tiêu chí" : "Thêm mới tiêu chí"}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="grid gap-x-8 gap-y-6 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="criterionCode">Mã tiêu chí *</Label>
              <Input
                id="criterionCode"
                className="h-11 w-full text-base"
                {...register("criterionCode")}
                placeholder="VD: HEAVY_METAL"
                disabled={isLoading}
                aria-invalid={!!errors.criterionCode}
              />
              {errors.criterionCode && (
                <p className="text-sm text-red-500">
                  {errors.criterionCode.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="criterionName">Tên tiêu chí *</Label>
              <Input
                id="criterionName"
                className="h-11 w-full text-base"
                {...register("criterionName")}
                placeholder="VD: Kim loại nặng"
                disabled={isLoading}
                aria-invalid={!!errors.criterionName}
              />
              {errors.criterionName && (
                <p className="text-sm text-red-500">
                  {errors.criterionName.message}
                </p>
              )}
            </div>

            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="criterionNote">Ghi chú</Label>
              <Textarea
                id="criterionNote"
                {...register("note")}
                placeholder="Nhập ghi chú cho tiêu chí..."
                disabled={isLoading}
                rows={4}
              />
              {errors.note && (
                <p className="text-sm text-red-500">{errors.note.message}</p>
              )}
            </div>
          </div>

          <DialogFooter className="mt-6">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isLoading}
            >
              Hủy
            </Button>
            <Button type="submit" variant="create" disabled={isLoading}>
              {isLoading ? (
                "Đang lưu..."
              ) : isEdit ? (
                <>
                  <Save className="h-4 w-4 mr-1" />
                  Cập nhật
                </>
              ) : (
                <>
                  <Plus className="h-4 w-4 mr-1" />
                  Thêm mới
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};