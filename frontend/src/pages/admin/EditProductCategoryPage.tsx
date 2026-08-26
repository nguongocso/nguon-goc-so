import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Package, Save, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { HelpButton } from "@/components/help/HelpButton";
import { getProductCategories, updateProductCategory } from "@/api/productCategoryApi";
import type { ProductCategory } from "@/types/productCategory";

const emptyToUndefined = {
  setValueAs: (value: unknown) =>
    value === "" || value === null || value === undefined ? undefined : Number(value),
};

const formSchema = z.object({
  name: z.string().min(1, "Tên không được để trống").max(255),
  group: z.string().min(1, "Nhóm hàng không được để trống").max(100),
  description: z.string().optional(),
  isActive: z.boolean().optional(),
  tempMin: z.number().optional(),
  tempMax: z.number().optional(),
  humidityMin: z
    .number()
    .min(0, "Độ ẩm tối thiểu phải từ 0 đến 100%")
    .max(100, "Độ ẩm tối thiểu phải từ 0 đến 100%")
    .optional(),
  humidityMax: z
    .number()
    .min(0, "Độ ẩm tối đa phải từ 0 đến 100%")
    .max(100, "Độ ẩm tối đa phải từ 0 đến 100%")
    .optional(),
});

type FormValues = z.infer<typeof formSchema>;

export const EditProductCategoryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState<ProductCategory | null>(null);

  const {
    register,
    handleSubmit,
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
      tempMin: undefined,
      tempMax: undefined,
      humidityMin: undefined,
      humidityMax: undefined,
    },
  });

  const isActiveValue = watch("isActive");

  useEffect(() => {
    const fetchCategory = async () => {
      if (!id) return;
      try {
        setLoading(true);
        const categories = await getProductCategories();
        const found = categories.find((c) => c.id === id);
        if (found) {
          setCategory(found);
          setValue("name", found.name);
          setValue("group", found.group);
          setValue("description", found.description || "");
          setValue("isActive", found.isActive);
          setValue("tempMin", found.tempMin ?? undefined);
          setValue("tempMax", found.tempMax ?? undefined);
          setValue("humidityMin", found.humidityMin ?? undefined);
          setValue("humidityMax", found.humidityMax ?? undefined);
        } else {
          toast.error("Không tìm thấy loại nông sản");
          navigate("/admin/product-categories");
        }
      } catch (error) {
        toast.error("Không thể tải thông tin loại nông sản");
      } finally {
        setLoading(false);
      }
    };

    fetchCategory();
  }, [id, setValue, navigate]);

  const onSubmit = async (values: FormValues) => {
    if (!id) return;
    if (values.tempMin !== undefined && values.tempMax !== undefined && values.tempMin > values.tempMax) {
      toast.error("Nhiệt độ tối thiểu không được lớn hơn nhiệt độ tối đa");
      return;
    }
    if (values.humidityMin !== undefined && values.humidityMax !== undefined && values.humidityMin > values.humidityMax) {
      toast.error("Độ ẩm tối thiểu không được lớn hơn độ ẩm tối đa");
      return;
    }
    try {
      await updateProductCategory(id, {
        name: values.name,
        group: values.group,
        description: values.description || undefined,
        isActive: values.isActive ?? false,
        tempMin: values.tempMin,
        tempMax: values.tempMax,
        humidityMin: values.humidityMin,
        humidityMax: values.humidityMax,
      });
      toast.success("Cập nhật loại nông sản thành công");
      navigate("/admin/product-categories");
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Có lỗi xảy ra khi cập nhật");
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
        Đang tải thông tin loại nông sản...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Package className="size-6 text-emerald-600" />
            Cập nhật loại nông sản
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Chỉnh sửa thông tin loại nông sản và ngưỡng bảo quản: <span className="font-semibold text-slate-900">{category?.name}</span>
          </p>
        </div>
        <HelpButton screenKey="admin-product-categories" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <CardTitle className="text-lg font-semibold text-slate-900">
            Thông tin loại nông sản
          </CardTitle>
          <CardDescription>
            Cập nhật các thông tin cơ bản và điều chỉnh ngưỡng môi trường bảo quản.
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-5 pt-6">
            <div className="space-y-1.5">
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

            <div className="space-y-1.5">
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

            <div className="space-y-1.5">
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

            <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-4 space-y-4">
              <div>
                <h4 className="text-sm font-semibold text-slate-900">
                  Ngưỡng bảo quản (điều kiện vận chuyển)
                </h4>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Thiết lập giới hạn an toàn để hệ thống tự động cảnh báo khi vượt ngưỡng.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className="text-xs font-medium text-slate-700">
                    Nhiệt độ tối thiểu (°C)
                  </Label>
                  <Input
                    type="number"
                    step="0.1"
                    placeholder="Tối thiểu"
                    {...register("tempMin", emptyToUndefined)}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label className="text-xs font-medium text-slate-700">
                    Nhiệt độ tối đa (°C)
                  </Label>
                  <Input
                    type="number"
                    step="0.1"
                    placeholder="Tối đa"
                    {...register("tempMax", emptyToUndefined)}
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className="text-xs font-medium text-slate-700">
                    Độ ẩm tối thiểu (%)
                  </Label>
                  <Input
                    type="number"
                    step="0.1"
                    placeholder="Tối thiểu (0-100)"
                    {...register("humidityMin", emptyToUndefined)}
                  />
                  {errors.humidityMin && (
                    <p className="text-sm text-red-500">{errors.humidityMin.message}</p>
                  )}
                </div>
                <div className="space-y-1.5">
                  <Label className="text-xs font-medium text-slate-700">
                    Độ ẩm tối đa (%)
                  </Label>
                  <Input
                    type="number"
                    step="0.1"
                    placeholder="Tối đa (0-100)"
                    {...register("humidityMax", emptyToUndefined)}
                  />
                  {errors.humidityMax && (
                    <p className="text-sm text-red-500">{errors.humidityMax.message}</p>
                  )}
                </div>
              </div>
            </div>

            <div className="flex items-center space-x-2 pt-1">
              <Checkbox
                id="isActive"
                checked={isActiveValue}
                onCheckedChange={(checked) => setValue("isActive", Boolean(checked))}
              />
              <Label
                htmlFor="isActive"
                className="text-sm font-medium leading-none cursor-pointer"
              >
                Đang hoạt động
              </Label>
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate("/admin/product-categories")}
                disabled={isSubmitting}
              >
                Hủy
              </Button>
              <Button type="submit" variant="create" disabled={isSubmitting}>
                <Save className="h-4 w-4 mr-1.5" />
                {isSubmitting ? "Đang lưu..." : "Cập nhật"}
              </Button>
            </div>
          </CardContent>
        </form>
      </Card>
    </div>
  );
};

export default EditProductCategoryPage;
