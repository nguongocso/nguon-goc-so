import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { toast } from "sonner";
import {
  createProductCategory,
  updateProductCategory,
} from "@/api/productCategoryApi";
import type { ProductCategory } from "@/types/productCategory";

const formSchema = z.object({
  name: z.string().min(1, "Tên không được để trống").max(255),
  group: z.string().min(1, "Nhóm hàng không được để trống").max(100),
  description: z.string().optional(),
  isActive: z.boolean().optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  category?: ProductCategory | null;
}

export const ProductCategoryForm = ({
  open,
  onClose,
  onSuccess,
  category,
}: Props) => {
  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
      group: "",
      description: "",
      isActive: true,
    },
  });

  const isActiveValue = watch("isActive");

  useEffect(() => {
    if (category) {
      setValue("name", category.name);
      setValue("group", category.group);
      setValue("description", category.description || "");
      setValue("isActive", category.isActive);
    } else {
      reset();
    }
  }, [category, setValue, reset]);

  const onSubmit = async (values: FormValues) => {
    try {
      if (category) {
        await updateProductCategory(category.id, {
          name: values.name,
          group: values.group,
          description: values.description || undefined,
          isActive: values.isActive || false,
        });
        toast.success("Cập nhật loại nông sản thành công");
      } else {
        await createProductCategory({
          name: values.name,
          group: values.group,
          description: values.description || undefined,
        });
        toast.success("Thêm mới loại nông sản thành công");
      }
      onSuccess();
      onClose();
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Có lỗi xảy ra");
    }
  };

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {category ? "Cập nhật loại nông sản" : "Thêm mới loại nông sản"}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {/* Tên */}
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="name" className="text-sm font-medium">
              Tên loại nông sản <span className="text-red-500">*</span>
            </Label>
            <Input
              id="name"
              {...register("name")}
              placeholder="VD: Xoài Cát Chu"
            />
            {errors.name && (
              <p className="text-sm text-red-500">{errors.name.message}</p>
            )}
          </div>

          {/* Nhóm hàng */}
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="group" className="text-sm font-medium">
              Nhóm hàng <span className="text-red-500">*</span>
            </Label>
            <Input
              id="group"
              {...register("group")}
              placeholder="VD: Cây ăn quả"
            />
            {errors.group && (
              <p className="text-sm text-red-500">{errors.group.message}</p>
            )}
          </div>

          {/* Mô tả */}
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="description" className="text-sm font-medium">
              Mô tả
            </Label>
            <Textarea
              id="description"
              {...register("description")}
              placeholder="Mô tả chi tiết..."
              rows={3}
            />
          </div>

          {/* Trạng thái hoạt động (chỉ hiển thị khi sửa) */}
          {category && (
            <div className="flex items-center gap-2 pt-1">
              <Checkbox
                id="isActive"
                checked={isActiveValue}
                onCheckedChange={(checked) =>
                  setValue("isActive", checked === true)
                }
              />
              <Label
                htmlFor="isActive"
                className="cursor-pointer text-sm font-medium"
              >
                Đang hoạt động
              </Label>
            </div>
          )}

          {/* Nút hành động */}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting
                ? "Đang lưu..."
                : category
                ? "Cập nhật"
                : "Thêm mới"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};