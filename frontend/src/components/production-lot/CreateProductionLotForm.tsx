import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type {
  CreateProductionLotRequest,
  FarmAreaOption,
  ProductCategoryOption,
} from "@/types/productionLot";
import axios from "axios";
import { CheckCircle2, PackageOpen, Sprout } from "lucide-react";
import { useState, type FormEvent } from "react";
import { getLocalDateString } from "@/utils/dateTime";
import { selectAllOnFocus, preventMouseUpCollapse } from "@/utils/inputUtils";

interface CreateProductionLotFormProps {
  farmAreas: FarmAreaOption[];
  productCategories: ProductCategoryOption[];
  onCancel: () => void;
  onSubmit?: (payload: CreateProductionLotRequest) => Promise<void> | void;
}

interface FormErrors {
  name?: string;
  farmAreaId?: string;
  productCategoryId?: string;
  expectedQuantity?: string;
  plantingDate?: string;
}

interface ApiErrorResponse {
  message?: string;
  errors?: Record<string, string>;
}

const initialForm: CreateProductionLotRequest = {
  name: "",
  farmAreaId: null,
  productCategoryId: "",
  expectedQuantity: 0,
  expectedQuantityUnit: "kg",
  plantingDate: null,
};

const selectClassName =
  "h-10 w-full rounded-lg border border-input bg-white px-3 text-sm outline-none transition focus:border-emerald-600 focus:ring-3 focus:ring-emerald-100";

const CreateProductionLotForm = ({
  farmAreas,
  productCategories,
  onCancel,
  onSubmit,
}: CreateProductionLotFormProps) => {
  const [form, setForm] = useState<CreateProductionLotRequest>(initialForm);
  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCreated, setIsCreated] = useState(false);
  const [submitError, setSubmitError] = useState("");

  const validate = () => {
    const nextErrors: FormErrors = {};

    if (!form.name.trim()) {
      nextErrors.name = "Tên lô không được để trống.";
    }

    if (!form.productCategoryId) {
      nextErrors.productCategoryId = "Vui lòng chọn loại nông sản.";
    }

    if (!Number.isFinite(form.expectedQuantity) || form.expectedQuantity <= 0) {
      nextErrors.expectedQuantity = "Sản lượng dự kiến phải lớn hơn 0.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsCreated(false);
    setSubmitError("");

    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await onSubmit?.({
        ...form,
        name: form.name.trim(),
      });
      setIsCreated(true);
    } catch (error: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        const responseData = error.response?.data;
        const backendErrors = responseData?.errors;

        if (backendErrors) {
          setErrors((current) => ({
            ...current,
            name: backendErrors.name,
            farmAreaId: backendErrors.farmAreaId,
            productCategoryId: backendErrors.productCategoryId,
            expectedQuantity: backendErrors.expectedQuantity,
            plantingDate: backendErrors.plantingDate,
          }));
        }

        setSubmitError(
          responseData?.message ||
            "Không thể tạo lô sản xuất. Vui lòng kiểm tra lại dữ liệu.",
        );
      } else {
        setSubmitError("Không thể kết nối đến máy chủ. Vui lòng thử lại.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card className="border-slate-200 bg-white shadow-sm">
      <CardHeader className="border-b border-slate-100 px-6 pb-5 sm:px-8">
        <CardTitle className="flex items-center gap-2 text-lg font-bold">
          <PackageOpen className="size-5 text-emerald-700" />
          Thông tin lô sản xuất
        </CardTitle>
        <CardDescription>
          Các trường có dấu <span className="text-red-600">*</span> là bắt buộc.
        </CardDescription>
      </CardHeader>

      <form onSubmit={handleSubmit} noValidate>
        <CardContent className="space-y-7 px-6 py-6 sm:px-8">
          <div className="space-y-2">
            <Label htmlFor="productionLotName">
              Tên lô sản xuất <span className="text-red-600">*</span>
            </Label>
            <Input
              id="productionLotName"
              value={form.name}
              onChange={(event) => {
                setForm((current) => ({
                  ...current,
                  name: event.target.value,
                }));
                setErrors((current) => ({ ...current, name: undefined }));
              }}
              aria-invalid={Boolean(errors.name)}
              placeholder="Ví dụ: Lô xoài Cát Chu xuất khẩu đợt 1 - 2026"
            />
            {errors.name && (
              <p className="text-xs text-red-600">{errors.name}</p>
            )}
          </div>

          <div className="grid gap-6 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="farmAreaId">
                Vùng trồng
                <span className="font-normal text-slate-400">
                  (không bắt buộc)
                </span>
              </Label>
              <select
                id="farmAreaId"
                className={selectClassName}
                value={form.farmAreaId ?? ""}
                onChange={(event) => {
                  setForm((current) => ({
                    ...current,
                    farmAreaId: event.target.value || null,
                  }));
                  setErrors((current) => ({
                    ...current,
                    farmAreaId: undefined,
                  }));
                }}
                aria-invalid={Boolean(errors.farmAreaId)}
              >
                <option value="">Không chọn vùng trồng</option>
                {farmAreas.map((area) => (
                  <option key={area.id} value={area.id}>
                    {area.name}
                    {area.area ? ` · ${area.area} ha` : ""}
                  </option>
                ))}
              </select>
              <p className="text-xs leading-5 text-slate-500">
                Có thể bổ sung vùng trồng trước khi gửi lô sang bước chờ duyệt.
              </p>
              {errors.farmAreaId && (
                <p className="text-xs text-red-600">{errors.farmAreaId}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="productCategoryId">
                Loại nông sản <span className="text-red-600">*</span>
              </Label>
              <select
                id="productCategoryId"
                className={selectClassName}
                value={form.productCategoryId}
                onChange={(event) => {
                  setForm((current) => ({
                    ...current,
                    productCategoryId: event.target.value,
                  }));
                  setErrors((current) => ({
                    ...current,
                    productCategoryId: undefined,
                  }));
                }}
                aria-invalid={Boolean(errors.productCategoryId)}
              >
                <option value="">Chọn loại nông sản</option>
                {productCategories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
              {errors.productCategoryId && (
                <p className="text-xs text-red-600">
                  {errors.productCategoryId}
                </p>
              )}
            </div>
          </div>

          <section className="border-t border-slate-100 pt-7">
            <h3 className="mb-5 flex items-center gap-2 font-bold">
              <Sprout className="size-4 text-emerald-700" />
              Kế hoạch sản xuất
            </h3>

            <div className="grid gap-6 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="expectedQuantity">
                  Sản lượng dự kiến <span className="text-red-600">*</span>
                </Label>
                <div className="relative">
                  <Input
                    id="expectedQuantity"
                    type="number"
                    min="0.01"
                    step="0.01"
                    className="pr-14"
                    value={form.expectedQuantity || ""}
                    onFocus={selectAllOnFocus}
                    onMouseUp={preventMouseUpCollapse}
                    onChange={(event) => {
                      setForm((current) => ({
                        ...current,
                        expectedQuantity: Number(event.target.value),
                      }));
                      setErrors((current) => ({
                        ...current,
                        expectedQuantity: undefined,
                      }));
                    }}
                    aria-invalid={Boolean(errors.expectedQuantity)}
                    placeholder="0"
                  />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 rounded bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">
                    kg
                  </span>
                </div>
                {errors.expectedQuantity && (
                  <p className="text-xs text-red-600">
                    {errors.expectedQuantity}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="plantingDate">
                  Ngày gieo trồng
                  <span className="font-normal text-slate-400">
                    (không bắt buộc)
                  </span>
                </Label>
                <div className="flex gap-2">
                  <Input
                    id="plantingDate"
                    type="date"
                    className="flex-1 [&::-webkit-calendar-picker-indicator]:ml-auto [&::-webkit-calendar-picker-indicator]:cursor-pointer"
                    value={form.plantingDate ?? ""}
                    onChange={(event) => {
                      setForm((current) => ({
                        ...current,
                        plantingDate: event.target.value || null,
                      }));
                      setErrors((current) => ({
                        ...current,
                        plantingDate: undefined,
                      }));
                    }}
                    aria-invalid={Boolean(errors.plantingDate)}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      const today = getLocalDateString();
                      setForm((current) => ({
                        ...current,
                        plantingDate: today,
                      }));
                      setErrors((current) => ({
                        ...current,
                        plantingDate: undefined,
                      }));
                    }}
                  >
                    Hôm nay
                  </Button>
                </div>
                {errors.plantingDate && (
                  <p className="text-xs text-red-600">{errors.plantingDate}</p>
                )}
              </div>
            </div>
          </section>

          {submitError && (
            <div
              role="alert"
              className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"
            >
              {submitError}
            </div>
          )}

          {isCreated && (
            <div
              role="status"
              className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800"
            >
              <CheckCircle2 className="mt-0.5 size-5 shrink-0" />
              <p>
                <strong>Tạo lô sản xuất thành công.</strong> Lô được lưu ở trạng
                thái Nháp.
              </p>
            </div>
          )}
        </CardContent>

        <CardFooter className="justify-end gap-3 px-6 py-5 sm:px-8">
          <Button type="button" variant="outline" size="lg" onClick={onCancel}>
            Hủy
          </Button>
          <Button
            type="submit"
            size="lg"
            className="bg-emerald-700 text-white hover:bg-emerald-800"
            disabled={isSubmitting}
            variant="create"
          >
            {isSubmitting ? "Đang tạo..." : "Tạo lô sản xuất"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};

export default CreateProductionLotForm;
