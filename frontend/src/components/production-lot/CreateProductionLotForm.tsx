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
  ProductionLot,
} from "@/types/productionLot";
import axios from "axios";
import { CheckCircle2, PackageOpen, Sprout, Copy, X, Lock, Plus } from "lucide-react";
import { useState, type FormEvent, type ReactNode, useEffect } from "react";
import { getLocalDateString } from "@/utils/dateTime";
import { selectAllOnFocus, preventMouseUpCollapse } from "@/utils/inputUtils";

interface CreateProductionLotFormProps {
  farmAreas: FarmAreaOption[];
  productCategories: ProductCategoryOption[];
  onCancel: () => void;
  onSubmit?: (payload: CreateProductionLotRequest) => Promise<void> | void;
  /** Giá trị khởi tạo khác mặc định (dùng khi prefill từ lô mẫu NCL-02-CN-007). */
  initialValues?: CreateProductionLotRequest;
  /** Khóa vùng trồng + loại nông sản vì được kế thừa từ lô mẫu. */
  lockFarmAreaAndCategory?: boolean;
  /** Nhãn nút submit (mặc định "Tạo lô sản xuất"). */
  submitLabel?: string;
  /** Banner hiển thị phía trên form (ví dụ nguồn lô mẫu). */
  infoBanner?: ReactNode;
  /** Danh sách lô vụ trước thuộc cùng tổ chức (QTN-01). */
  previousLots?: ProductionLot[];
  /** Đang tải danh sách lô vụ trước. */
  isLoadingPreviousLots?: boolean;
  /** Callback khi người dùng chọn một lô vụ trước từ dropdown. */
  onSelectPreviousLot?: (lotId: string) => void;
  /** Callback khi người dùng hủy chế độ sao chép và quay lại text input. */
  onCancelCopy?: () => void;
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
  initialValues,
  lockFarmAreaAndCategory = false,
  submitLabel = "Tạo lô sản xuất",
  infoBanner,
  previousLots = [],
  isLoadingPreviousLots = false,
  onSelectPreviousLot,
  onCancelCopy,
}: CreateProductionLotFormProps) => {
  const [isCopyMode, setIsCopyMode] = useState(false);
  const [suggestedName, setSuggestedName] = useState<string | null>(null);
  const [selectedPreviousLotId, setSelectedPreviousLotId] = useState("");

  useEffect(() => {
    if (!isCopyMode) setSuggestedName(null);
  }, [isCopyMode]);

  const [form, setForm] = useState<CreateProductionLotRequest>(
    initialValues ?? initialForm,
  );
  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCreated, setIsCreated] = useState(false);
  const [submitError, setSubmitError] = useState("");

  const validate = () => {
    const nextErrors: FormErrors = {};

    if (!form.name.trim()) {
      nextErrors.name = "Tên lô không được để trống.";
    }

    if (!form.farmAreaId) {
      nextErrors.farmAreaId = "Vui lòng chọn vùng trồng.";
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
        <div className="flex items-center justify-between gap-4">
          <CardTitle className="flex items-center gap-2 text-lg font-bold">
            <PackageOpen className="size-5 text-emerald-700" />
            Thông tin lô sản xuất
          </CardTitle>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="inline-flex items-center gap-2"
            title={
              previousLots.length === 0
                ? 'Không có lô vụ trước để sao chép'
                : 'Sao chép từ lô vụ trước'
            }
            aria-label="Sao chép từ lô vụ trước"
            disabled={isLoadingPreviousLots || previousLots.length === 0}
            onClick={() => {
              if (previousLots.length > 0) {
                setIsCopyMode(true);
              }
            }}
          >
            <Copy className="size-4" />
            <span>Sao chép từ lô vụ trước</span>
          </Button>
        </div>
        <CardDescription>
          Các trường có dấu <span className="text-red-600">*</span> là bắt buộc.
        </CardDescription>
      </CardHeader>

      <form onSubmit={handleSubmit} noValidate>
        <CardContent className="space-y-7 px-6 py-6 sm:px-8">
          {infoBanner}
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <Label htmlFor="productionLotName">
                Tên lô sản xuất <span className="text-red-600">*</span>
              </Label>
            </div>
            {isCopyMode ? (
              <div className="relative">
                <select
                  id="productionLotName"
                  className={`${selectClassName} pr-10`}
                  value={selectedPreviousLotId}
                  onChange={(event) => {
                    const value = event.target.value;
                    setSelectedPreviousLotId(value);
                    if (value && onSelectPreviousLot) {
                      onSelectPreviousLot(value);
                      const lot = previousLots.find((l) => l.id === value);
                      if (lot) {
                        // Điền sẵn tên lô vụ tiếp theo vào input.
                        const baseName = lot.name.replace(/\s*[vV]ụ\s*\d+$/i, '').trim();
                        const match = lot.name.match(/\s*[vV]ụ\s*(\d+)$/i);
                        const nextNum = match ? (parseInt(match[1], 10) + 1).toString() : '2';
                        const newSuggested = `${baseName} vụ ${nextNum}`;
                        setForm((current) => ({
                          ...current,
                          name: newSuggested,
                        }));
                        setSuggestedName(newSuggested);
                        setErrors((current) => ({ ...current, name: undefined }));
                        // Quay lại text input ngay sau khi chọn để người dùng chỉnh sửa.
                        setIsCopyMode(false);
                        setSelectedPreviousLotId("");
                      }
                    }
                  }}
                  aria-invalid={Boolean(errors.name)}
                >
                  <option value="">Chọn lô vụ trước để sao chép</option>
                  {previousLots.map((lot) => (
                    <option key={lot.id} value={lot.id}>
                      {lot.name} · {lot.farmAreaName ?? "Chưa có vùng trồng"} ·{" "}
                      {lot.status}
                    </option>
                  ))}
                </select>
              </div>
            ) : (
              <div className="relative">
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
              </div>
            )}
            {suggestedName && (
              <p className="text-xs text-emerald-600">
                Gợi ý tên mới: <span className="font-semibold">{suggestedName}</span>
              </p>
            )}
            {isCopyMode && isLoadingPreviousLots && (
              <p className="text-xs text-slate-500">Đang tải danh sách lô vụ trước...</p>
            )}
            {!isCopyMode && previousLots.length === 0 && !isLoadingPreviousLots && (
              <p className="text-xs text-slate-400">Không có lô vụ trước để sao chép</p>
            )}
            {errors.name && (
              <p className="text-xs text-red-600">{errors.name}</p>
            )}
          </div>

          <div className="grid gap-6 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="farmAreaId">
                Vùng trồng <span className="text-red-600">*</span>
              </Label>
              {lockFarmAreaAndCategory ? (
                <div className="relative flex items-center rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700">
                  <span>
                    {farmAreas.find((a) => a.id === form.farmAreaId)?.name ??
                      '—'}
                  </span>
                  <span
                    title="Trường này được kế thừa từ lô mẫu và không thể thay đổi."
                    aria-label="Kế thừa từ lô mẫu"
                    className="ml-auto inline-flex items-center"
                  >
                    <Lock className="size-3.5 text-amber-500" />
                  </span>
                </div>
              ) : (
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
                  <option value="">Chọn vùng trồng</option>
                  {farmAreas.map((area) => (
                    <option key={area.id} value={area.id}>
                      {area.name}
                      {area.area ? ` · ${area.area} ha` : ""}
                    </option>
                  ))}
                </select>
              )}
              {errors.farmAreaId && (
                <p className="text-xs text-red-600">{errors.farmAreaId}</p>
              )}
              {lockFarmAreaAndCategory && (
                <p className="text-xs text-slate-500">
                  Vùng trồng được kế thừa từ lô mẫu, không thay đổi.
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="productCategoryId">
                Loại nông sản <span className="text-red-600">*</span>
              </Label>
              {lockFarmAreaAndCategory ? (
                <div className="relative flex items-center rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700">
                  <span>
                    {productCategories.find((c) => c.id === form.productCategoryId)?.name ??
                      '—'}
                  </span>
                  <span
                    title="Trường này được kế thừa từ lô mẫu và không thể thay đổi."
                    aria-label="Kế thừa từ lô mẫu"
                    className="ml-auto inline-flex items-center"
                  >
                    <Lock className="size-3.5 text-amber-500" />
                  </span>
                </div>
              ) : (
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
              )}
              {errors.productCategoryId && (
                <p className="text-xs text-red-600">
                  {errors.productCategoryId}
                </p>
              )}
              {lockFarmAreaAndCategory && (
                <p className="text-xs text-slate-500">
                  Loại nông sản được kế thừa từ lô mẫu, không thay đổi.
                </p>
              )}
            </div>
          </div>

          <section className="border-t border-slate-200 pt-6">
            <h3 className="mb-4 flex items-center gap-2 font-bold text-slate-900">
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
                <div className="flex items-center gap-2">
                  <Input
                    id="plantingDate"
                    type="date"
                    className="h-10 flex-1 [&::-webkit-calendar-picker-indicator]:ml-auto [&::-webkit-calendar-picker-indicator]:cursor-pointer"
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
                    className="h-10"
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
          <Button type="button" variant="outline" size="default" onClick={onCancel}>
            Hủy
          </Button>
          <Button
            type="submit"
            size="default"
            variant="create"
            disabled={isSubmitting}
          >
            <Plus className="size-4" />
            {isSubmitting ? "Đang tạo..." : submitLabel}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};

export default CreateProductionLotForm;
