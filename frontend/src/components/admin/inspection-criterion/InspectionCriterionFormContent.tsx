import { useEffect, useMemo, useState } from "react";
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
  createInspectionCriterion,
  updateInspectionCriterion,
} from "@/api/inspectionCriterionApi";
import { getStandards } from "@/api/standardApi";
import type { Standard } from "@/types/standard";
import type { InspectionCriterion } from "@/types/inspectionCriterion";

/** Option trong dropdown Tiêu chuẩn tham chiếu: label = tên, value = id. */
interface StandardOption {
  id: string;
  name: string;
}

/**
 * Prefix cho option giữ lại giá trị referenceStandard cũ không khớp danh mục
 * tiêu chuẩn hiện tại (dữ liệu tự nhập trước khi chuyển sang dropdown), để khi
 * chỉnh sửa không làm mất giá trị đã lưu của chỉ tiêu.
 */
const LEGACY_STANDARD_OPTION_PREFIX = "__legacy_standard__:";

// Ràng buộc mirror từ backend InspectionCriterionCatalogRequest.
// Contract hiện tại của backend dùng trường `referenceStandard` (string):
// dropdown chọn theo ID tiêu chuẩn chất lượng rồi map sang tên tiêu chuẩn
// khi submit — không đổi tên field so với API contract.
const formSchema = z.object({
  name: z
    .string()
    .min(1, "Tên chỉ tiêu không được để trống")
    .max(150, "Tên chỉ tiêu tối đa 150 ký tự"),
  unit: z
    .string()
    .min(1, "Đơn vị tính không được để trống")
    .max(30, "Đơn vị tính tối đa 30 ký tự"),
  maxThreshold: z
    .number({ invalid_type_error: "Ngưỡng tối đa phải là số dương" })
    .positive("Ngưỡng tối đa phải là số dương"),
  standardId: z.string().min(1, "Vui lòng chọn Tiêu chuẩn chất lượng."),
});

type FormValues = z.infer<typeof formSchema>;

export interface InspectionCriterionFormContentProps {
  /** Chỉ tiêu đang chỉnh sửa (null/undefined = tạo mới). */
  criterion?: InspectionCriterion | null;
  /** Được gọi sau khi submit thành công (trước khi đóng modal / chuyển trang). */
  onSuccess: () => void;
  /** Được gọi khi người dùng bấm nút Hủy. */
  onCancel: () => void;
  /**
   * Chỉ báo form đang hiển thị (dùng bởi modal để tránh gọi API khi chưa mở).
   * Mặc định `true` cho trường hợp dùng trang.
   */
  open?: boolean;
}

/**
 * Form chung cho tạo/sửa chỉ tiêu kiểm nghiệm (không bao bởi Dialog).
 * Được dùng chung bởi:
 *  - Trang "Thêm mới chỉ tiêu kiểm nghiệm" (open mặc định = true)
 *  - Modal "Cập nhật chỉ tiêu kiểm nghiệm" (pass open={modalOpen})
 */
export const InspectionCriterionFormContent = ({
  criterion,
  onSuccess,
  onCancel,
  open = true,
}: InspectionCriterionFormContentProps) => {
  // Danh sách tiêu chuẩn chất lượng lấy từ GET /api/v1/standards (API có sẵn)
  const [standards, setStandards] = useState<Standard[]>([]);
  const [standardsLoading, setStandardsLoading] = useState(false);
  const [standardsError, setStandardsError] = useState<string | null>(null);
  const [standardsRetryToken, setStandardsRetryToken] = useState(0);

  const {
    register,
    control,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
      unit: "",
      maxThreshold: undefined,
      standardId: "",
    },
  });

  // Load danh mục tiêu chuẩn chất lượng khi form hiển thị (tránh gọi API khi
  // modal chưa mở)
  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setStandardsLoading(true);
    setStandardsError(null);
    getStandards({ isActive: true, page: 0, size: 100 })
      .then((data) => {
        if (!cancelled) setStandards(data.items || []);
      })
      .catch((error: any) => {
        if (cancelled) return;
        setStandards([]);
        setStandardsError(
          error.response?.data?.message || "Không thể tải danh sách tiêu chuẩn"
        );
      })
      .finally(() => {
        if (!cancelled) setStandardsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [open, standardsRetryToken]);

  // Options dropdown: label hiển thị tên tiêu chuẩn, value là ID tiêu chuẩn.
  // Với chỉ tiêu cũ có referenceStandard không khớp danh mục hiện tại,
  // giữ thêm option gốc để chỉnh sửa mà không mất dữ liệu đã lưu.
  const standardOptions = useMemo<StandardOption[]>(() => {
    const options: StandardOption[] = standards.map((standard) => ({
      id: standard.id,
      name: standard.name,
    }));
    const storedRef = criterion?.referenceStandard?.trim();
    if (
      storedRef &&
      !options.some(
        (option) => option.name.trim().toLowerCase() === storedRef.toLowerCase()
      )
    ) {
      options.unshift({
        id: `${LEGACY_STANDARD_OPTION_PREFIX}${storedRef}`,
        name: storedRef,
      });
    }
    return options;
  }, [standards, criterion]);

  // Reset form mỗi lần criterion thay đổi (tạo mới hoặc đổi chỉ tiêu đang sửa)
  useEffect(() => {
    if (!open) return;
    if (criterion) {
      reset({
        name: criterion.name,
        unit: criterion.unit,
        maxThreshold: criterion.maxThreshold,
        standardId: "",
      });
    } else {
      reset({ name: "", unit: "", maxThreshold: undefined, standardId: "" });
    }
  }, [criterion, open, reset]);

  // Ở chế độ chỉnh sửa, tự động chọn đúng tiêu chuẩn đã lưu trên chỉ tiêu
  // (chờ load xong danh sách để tránh nháy giá trị rỗng). Chỉ set lại
  // standardId, không reset toàn bộ form để không mất dữ liệu đang nhập.
  useEffect(() => {
    if (!open || !criterion || standardsLoading) return;
    const storedRef = criterion.referenceStandard?.trim().toLowerCase();
    const matched = storedRef
      ? standardOptions.find(
          (option) => option.name.trim().toLowerCase() === storedRef
        )
      : undefined;
    setValue("standardId", matched?.id ?? "");
  }, [open, criterion, standardsLoading, standardOptions, setValue]);

  // Tiêu chuẩn tham chiếu là bắt buộc: không cho submit khi chưa load được
  // danh sách, đang lỗi hoặc danh mục trống
  const standardNotReady =
    standardsLoading || !!standardsError || standardOptions.length === 0;

  const onSubmit = async (values: FormValues) => {
    const selectedStandard = standardOptions.find(
      (option) => option.id === values.standardId
    );
    if (!selectedStandard) return;
    try {
      if (criterion) {
        await updateInspectionCriterion(criterion.id, {
          name: values.name.trim(),
          unit: values.unit.trim(),
          maxThreshold: values.maxThreshold,
          referenceStandard: selectedStandard.name,
        });
        toast.success("Cập nhật chỉ tiêu kiểm nghiệm thành công");
      } else {
        await createInspectionCriterion({
          name: values.name.trim(),
          unit: values.unit.trim(),
          maxThreshold: values.maxThreshold,
          referenceStandard: selectedStandard.name,
        });
        toast.success("Thêm mới chỉ tiêu kiểm nghiệm thành công");
      }
      onSuccess();
    } catch (error: any) {
      // Lỗi nghiệp vụ từ backend (409 trùng tên + tiêu chuẩn, 403 quyền...)
      toast.error(error.response?.data?.message || "Có lỗi xảy ra");
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">

      {/* Tiêu chuẩn tham chiếu — dropdown từ danh mục Tiêu chuẩn chất lượng */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="standardId" className="text-sm font-medium">
          Tiêu chuẩn chất lượng <span className="text-red-500">*</span>
        </Label>
        <Controller
            name="standardId"
            control={control}
            render={({ field }) => {
              const selectedOption = standardOptions.find(
                  (option) => option.id === field.value
              );
              return (
                  <Select
                      value={field.value || ""}
                      onValueChange={field.onChange}
                      disabled={isSubmitting || standardNotReady}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue
                          placeholder={
                            standardsLoading
                                ? "Đang tải tiêu chuẩn..."
                                : standardsError
                                    ? "Không tải được tiêu chuẩn"
                                    : standardOptions.length === 0
                                        ? "Chưa có tiêu chuẩn chất lượng"
                                        : "Chọn tiêu chuẩn chất lượng"
                          }
                      >
                        {selectedOption?.name}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent className="min-w-[300px] max-h-[200px]">
                      {standardOptions.map((option) => (
                          <SelectItem key={option.id} value={option.id}>
                            {option.name}
                          </SelectItem>
                      ))}
                      {standardOptions.length === 0 && (
                          <div className="px-3 py-2 text-sm text-muted-foreground">
                            Chưa có tiêu chuẩn chất lượng
                          </div>
                      )}
                    </SelectContent>
                  </Select>
              );
            }}
        />
        {errors.standardId && (
            <p className="text-sm text-red-500">
              {errors.standardId.message}
            </p>
        )}
        {standardsError && (
            <p className="text-sm text-red-500">
              {standardsError}{" "}
              <button
                  type="button"
                  onClick={() => setStandardsRetryToken((token) => token + 1)}
                  className="font-medium underline hover:no-underline"
              >
                Thử lại
              </button>
            </p>
        )}
      </div>

      {/* Tên chỉ tiêu */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name" className="text-sm font-medium">
          Tên chỉ tiêu <span className="text-red-500">*</span>
        </Label>
        <Input
          id="name"
          {...register("name")}
          placeholder="VD: Dư lượng thuốc BVTV nhóm Lân hữu cơ"
        />
        {errors.name && (
          <p className="text-sm text-red-500">{errors.name.message}</p>
        )}
        <p className="text-xs text-muted-foreground">
          Tên chỉ tiêu không được trùng trong cùng một Tiêu chuẩn chất lượng.
        </p>
      </div>
      {/* Đơn vị đo */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="unit" className="text-sm font-medium">
          Đơn vị đo <span className="text-red-500">*</span>
        </Label>
        <Input id="unit" {...register("unit")} placeholder="VD: mg/kg" />
        {errors.unit && (
          <p className="text-sm text-red-500">{errors.unit.message}</p>
        )}
      </div>

      {/* Ngưỡng tối đa */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="maxThreshold" className="text-sm font-medium">
          Ngưỡng tối đa <span className="text-red-500">*</span>
        </Label>
        <Input
          id="maxThreshold"
          type="number"
          step="0.0001"
          min="0"
          placeholder="VD: 0.5"
          {...register("maxThreshold", {
            setValueAs: (value: unknown) =>
              value === "" || value === null || value === undefined
                ? undefined
                : Number(value),
          })}
        />
        {errors.maxThreshold && (
          <p className="text-sm text-red-500">
            {errors.maxThreshold.message}
          </p>
        )}
        <p className="text-xs text-muted-foreground">
          Kết quả kiểm nghiệm vượt ngưỡng này được coi là không đạt.
        </p>
      </div>

      {/* Nút hành động */}
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Hủy
        </Button>
        <Button
          type="submit"
          variant="create"
          disabled={isSubmitting || standardNotReady}
        >
          <Plus className="h-4 w-4 mr-1.5" />
          {isSubmitting ? "Đang lưu..." : criterion ? "Cập nhật" : "Thêm mới"}
        </Button>
      </div>
    </form>
  );
};
