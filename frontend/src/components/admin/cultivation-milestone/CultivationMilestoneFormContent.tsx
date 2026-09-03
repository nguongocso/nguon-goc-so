import { useEffect } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { toast } from "sonner";
import {
  createCultivationMilestone,
  updateCultivationMilestone,
} from "@/api/cultivationMilestoneApi";
import type { CultivationMilestone } from "@/types/cultivationMilestone";

const ACTIVITY_TYPE_OPTIONS = [
  { value: "PLANTING", label: "Gieo trồng" },
  { value: "WATERING", label: "Tưới nước" },
  { value: "FERTILIZING", label: "Bón phân" },
  { value: "PESTICIDE", label: "Phun thuốc" },
  { value: "WEEDING", label: "Làm cỏ" },
  { value: "HARVESTING", label: "Thu hoạch" },
  { value: "OTHER", label: "Khác" },
];

const formSchema = z.object({
  name: z
    .string()
    .min(1, "Tên mốc canh tác không được để trống")
    .max(150, "Tên mốc canh tác tối đa 150 ký tự"),
  description: z
    .string()
    .max(500, "Mô tả tối đa 500 ký tự")
    .optional()
    .or(z.literal("")),
  activityType: z.string().min(1, "Vui lòng chọn loại hoạt động"),
  expectedDaysFromPlanting: z
    .number({ invalid_type_error: "Số ngày phải là số nguyên dương" })
    .int("Số ngày phải là số nguyên")
    .positive("Số ngày phải lớn hơn 0")
    .optional()
    .nullable(),
});

type FormValues = z.infer<typeof formSchema>;

export interface CultivationMilestoneFormContentProps {
  milestone?: CultivationMilestone | null;
  onSuccess: () => void;
  onCancel: () => void;
  open?: boolean;
}

export const CultivationMilestoneFormContent = ({
  milestone,
  onSuccess,
  onCancel,
  open = true,
}: CultivationMilestoneFormContentProps) => {
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
      description: "",
      activityType: "",
      expectedDaysFromPlanting: null,
    },
  });

  useEffect(() => {
    if (!open) return;
    if (milestone) {
      reset({
        name: milestone.name,
        description: milestone.description ?? "",
        activityType: milestone.activityType,
        expectedDaysFromPlanting: milestone.expectedDaysFromPlanting ?? null,
      });
    } else {
      reset({ name: "", description: "", activityType: "", expectedDaysFromPlanting: null });
    }
  }, [milestone, open, reset]);

  const onSubmit = async (values: FormValues) => {
    try {
      if (milestone) {
        await updateCultivationMilestone(milestone.id, {
          name: values.name.trim(),
          description: values.description?.trim() || undefined,
          activityType: values.activityType,
          expectedDaysFromPlanting: values.expectedDaysFromPlanting ?? undefined,
        });
        toast.success("Cập nhật mốc canh tác thành công");
      } else {
        await createCultivationMilestone({
          name: values.name.trim(),
          description: values.description?.trim() || undefined,
          activityType: values.activityType,
          expectedDaysFromPlanting: values.expectedDaysFromPlanting ?? undefined,
        });
        toast.success("Thêm mới mốc canh tác thành công");
      }
      onSuccess();
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Có lỗi xảy ra");
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      {/* Loại hoạt động */}
      <div className="flex flex-col gap-1.5">
        <Label className="text-sm font-medium">
          Loại hoạt động <span className="text-red-500">*</span>
        </Label>
        <Controller
          name="activityType"
          control={control}
          render={({ field }) => (
            <Select value={field.value} onValueChange={field.onChange} disabled={isSubmitting}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Chọn loại hoạt động" />
              </SelectTrigger>
              <SelectContent>
                {ACTIVITY_TYPE_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        />
        {errors.activityType && (
          <p className="text-sm text-red-500">{errors.activityType.message}</p>
        )}
      </div>

      {/* Tên mốc */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name" className="text-sm font-medium">
          Tên mốc canh tác <span className="text-red-500">*</span>
        </Label>
        <Input
          id="name"
          {...register("name")}
          placeholder="VD: Bón phân đợt 1"
        />
        {errors.name && (
          <p className="text-sm text-red-500">{errors.name.message}</p>
        )}
        <p className="text-xs text-muted-foreground">
          Tên mốc không được trùng trong cùng một loại hoạt động.
        </p>
      </div>

      {/* Mô tả */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description" className="text-sm font-medium">
          Mô tả
        </Label>
        <Input
          id="description"
          {...register("description")}
          placeholder="Mô tả ngắn về mốc canh tác"
        />
        {errors.description && (
          <p className="text-sm text-red-500">{errors.description.message}</p>
        )}
      </div>

      {/* Số ngày dự kiến từ ngày gieo trồng */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="expectedDaysFromPlanting" className="text-sm font-medium">
          Số ngày dự kiến từ ngày gieo trồng
        </Label>
        <Input
          id="expectedDaysFromPlanting"
          type="number"
          min="1"
          placeholder="VD: 30"
          {...register("expectedDaysFromPlanting", {
            setValueAs: (value: unknown) =>
              value === "" || value === null || value === undefined
                ? null
                : Number(value),
          })}
        />
        {errors.expectedDaysFromPlanting && (
          <p className="text-sm text-red-500">
            {errors.expectedDaysFromPlanting.message}
          </p>
        )}
        <p className="text-xs text-muted-foreground">
          Thông tin tham khảo,用于 hiển thị trên giao diện.
        </p>
      </div>

      {/* Nút hành động */}
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Hủy
        </Button>
        <Button type="submit" variant="create" disabled={isSubmitting}>
          <Plus className="h-4 w-4 mr-1.5" />
          {isSubmitting ? "Đang lưu..." : milestone ? "Cập nhật" : "Thêm mới"}
        </Button>
      </div>
    </form>
  );
};
