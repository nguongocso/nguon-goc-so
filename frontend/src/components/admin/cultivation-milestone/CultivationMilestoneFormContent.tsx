import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
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
import { getProductCategories } from "@/api/productCategoryApi";
import { getActiveStandards } from "@/api/standardApi";
import type { CultivationMilestone } from "@/types/cultivationMilestone";
import type { ProductCategory } from "@/types/productCategory";
import type { Standard } from "@/types/standard";

const ALL_SCOPE = "__all__";

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
    .number({ invalid_type_error: "Số ngày phải là số nguyên" })
    .int("Số ngày phải là số nguyên")
    .min(0, "Số ngày phải lớn hơn hoặc bằng 0")
    .optional()
    .nullable(),
  productCategoryId: z.string().nullable(),
  standardId: z.string().nullable(),
});

type FormValues = z.infer<typeof formSchema>;

export interface CultivationMilestoneFormContentProps {
  milestone?: CultivationMilestone | null;
  onSuccess: () => void;
  onCancel: () => void;
  open?: boolean;
  isMandatory: boolean;
  onMandatoryChange: (value: boolean) => void;
}

export const CultivationMilestoneFormContent = ({
  milestone,
  onSuccess,
  onCancel,
  open = true,
  isMandatory,
  onMandatoryChange,
}: CultivationMilestoneFormContentProps) => {
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [standards, setStandards] = useState<Standard[]>([]);
  const [scopeLoading, setScopeLoading] = useState(false);

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
      productCategoryId: ALL_SCOPE,
      standardId: ALL_SCOPE,
    },
  });

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setScopeLoading(true);
    void getProductCategories({})
      .then((data) => {
        if (!cancelled) setCategories(data || []);
      })
      .catch(() => {
        if (!cancelled) setCategories([]);
      })
      .finally(() => {
        if (!cancelled) setScopeLoading(false);
      });
    getActiveStandards()
      .then((data) => {
        if (!cancelled) setStandards(data || []);
      })
      .catch(() => {
        if (!cancelled) setStandards([]);
      });
    return () => {
      cancelled = true;
    };
  }, [open]);

  useEffect(() => {
    if (!open) return;
    if (milestone) {
      reset({
        name: milestone.name,
        description: milestone.description ?? "",
        activityType: milestone.activityType,
        expectedDaysFromPlanting: milestone.expectedDaysFromPlanting,
        productCategoryId: milestone.productCategoryId ?? ALL_SCOPE,
        standardId: milestone.standardId ?? ALL_SCOPE,
      });
    } else {
      reset({
        name: "",
        description: "",
        activityType: "",
        expectedDaysFromPlanting: null,
        productCategoryId: ALL_SCOPE,
        standardId: ALL_SCOPE,
      });
    }
  }, [milestone, open, reset]);

  const toPayload = (values: FormValues) => ({
    name: values.name.trim(),
    description: values.description?.trim() || undefined,
    activityType: values.activityType,
    expectedDaysFromPlanting: values.expectedDaysFromPlanting ?? undefined,
    productCategoryId:
      values.productCategoryId === ALL_SCOPE
        ? null
        : values.productCategoryId || null,
    standardId:
      values.standardId === ALL_SCOPE ? null : values.standardId || null,
    isMandatory,
  });

  const onSubmit = async (values: FormValues) => {
    try {
      if (milestone) {
        await updateCultivationMilestone(milestone.id, toPayload(values));
        toast.success("Cập nhật mốc canh tác thành công");
      } else {
        await createCultivationMilestone(toPayload(values));
        toast.success("Thêm mới mốc canh tác thành công");
      }
      onSuccess();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          (error.response?.data?.message || "Có lỗi xảy ra")
      );
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {/* Nhóm 1 — Thông tin mốc */}
      <Card size="sm" className="rounded-none border-emerald-100 bg-transparent p-0">
        <CardHeader className="border-b border-emerald-100 bg-emerald-50/60 px-5 py-3">
          <CardTitle className="text-base font-semibold text-emerald-950">
            Thông tin mốc
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 gap-4 px-5 pt-4 pb-4 md:grid-cols-2">
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
              Tên mốc không được trùng trong cùng một loại nông sản và tiêu
              chuẩn.
            </p>
          </div>

          {/* Loại hoạt động */}
          <div className="flex flex-col gap-1.5">
            <Label className="text-sm font-medium">
              Loại hoạt động <span className="text-red-500">*</span>
            </Label>
            <Controller
              name="activityType"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={isSubmitting}
                  items={ACTIVITY_TYPE_OPTIONS}
                >
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
              <p className="text-sm text-red-500">
                {errors.activityType.message}
              </p>
            )}
            <p className="text-xs text-muted-foreground">
              Dùng để đối chiếu với nhật ký canh tác khi kiểm tra đóng gói.
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
              <p className="text-sm text-red-500">
                {errors.description.message}
              </p>
            )}
          </div>

          {/* Thời điểm dự kiến */}
          <div className="flex flex-col gap-1.5">
            <Label
              htmlFor="expectedDaysFromPlanting"
              className="text-sm font-medium"
            >
              Thời điểm dự kiến
            </Label>
            <Input
              id="expectedDaysFromPlanting"
              type="number"
              min="0"
              placeholder="Bao nhiêu ngày sau khi gieo trồng"
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
          </div>
        </CardContent>
      </Card>

      {/* Nhóm 2 — Phạm vi áp dụng */}
      <Card size="sm" className="rounded-none border-blue-100 bg-transparent p-0">
        <CardHeader className="flex flex-row items-center justify-between gap-4 border-b border-blue-100 bg-blue-50/60 px-5 py-3">
          <CardTitle className="text-base font-semibold text-blue-950">
            Phạm vi áp dụng
          </CardTitle>
          <div className="flex items-center gap-2">
            <Label className="text-sm font-medium">Bắt buộc</Label>
            <Switch
              checked={isMandatory}
              onCheckedChange={onMandatoryChange}
            />
          </div>
        </CardHeader>
        <CardContent className="grid grid-cols-1 gap-4 px-5 pt-4 pb-4 md:grid-cols-2">
          {/* Loại nông sản áp dụng */}
          <div className="flex flex-col gap-1.5">
            <Label className="text-sm font-medium">
              Loại nông sản áp dụng
            </Label>
            <Controller
              name="productCategoryId"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value ?? ALL_SCOPE}
                  onValueChange={field.onChange}
                  disabled={scopeLoading || isSubmitting}
                  items={[
                    { value: ALL_SCOPE, label: "Tất cả loại nông sản" },
                    ...categories.map((c) => ({ value: c.id, label: c.name })),
                  ]}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Chọn loại nông sản" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={ALL_SCOPE}>
                      Tất cả loại nông sản
                    </SelectItem>
                    {categories.map((c) => (
                      <SelectItem key={c.id} value={c.id}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            <p className="text-xs text-muted-foreground">
              Chọn "Tất cả" nếu mốc áp dụng cho mọi loại nông sản.
            </p>
          </div>

          {/* Tiêu chuẩn áp dụng */}
          <div className="flex flex-col gap-1.5">
            <Label className="text-sm font-medium">Tiêu chuẩn áp dụng</Label>
            <Controller
              name="standardId"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value ?? ALL_SCOPE}
                  onValueChange={field.onChange}
                  disabled={scopeLoading || isSubmitting}
                  items={[
                    { value: ALL_SCOPE, label: "Tất cả tiêu chuẩn" },
                    ...standards.map((s) => ({ value: s.id, label: s.name })),
                  ]}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Chọn tiêu chuẩn" />
                  </SelectTrigger>
                  <SelectContent className="max-h-[220px]">
                    <SelectItem value={ALL_SCOPE}>Tất cả tiêu chuẩn</SelectItem>
                    {standards.map((s) => (
                      <SelectItem key={s.id} value={s.id}>
                        {s.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            <p className="text-xs text-muted-foreground">
              Chọn "Tất cả" nếu mốc áp dụng cho mọi tiêu chuẩn của lô.
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Nút hành động */}
      <div className="flex justify-end gap-2 pt-1">
        <Button
          type="button"
          variant="outline"
          className="h-10"
          onClick={onCancel}
        >
          Hủy
        </Button>
        <Button
          type="submit"
          variant="create"
          className="h-10"
          disabled={isSubmitting}
        >
          <Plus className="h-4 w-4 mr-1.5" />
          {isSubmitting ? "Đang lưu..." : milestone ? "Cập nhật" : "Thêm mới"}
        </Button>
      </div>
    </form>
  );
};
