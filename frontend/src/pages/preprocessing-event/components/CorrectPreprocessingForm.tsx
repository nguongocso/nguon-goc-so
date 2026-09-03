import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import {
  AlertTriangle,
  FilePenLine,
  LoaderCircle,
  MapPin,
  Percent,
  Scale,
} from "lucide-react";
import { toast } from "sonner";

import { correctPreprocessingEvent } from "@/api/preprocessingApi";
import { Alert, AlertDescription } from "@/components/ui/alert";
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
import { Textarea } from "@/components/ui/textarea";
import { useAutoGeolocation } from "@/hooks/useAutoGeolocation";
import { LocationPicker } from "@/pages/packaging-event/components/LocationPicker";
import type { PreprocessingEventResponse } from "@/types/preprocessing";
import { getLocalDateString } from "@/utils/dateTime";
import {
  correctPreprocessingSchema,
  type CorrectPreprocessingFormValues,
} from "@/utils/validators/preprocessingEventSchema";

import {
  getPreprocessingErrorMessage,
  toOptionalText,
} from "../preprocessingFormUtils";

interface CorrectionLocationState {
  preprocessingEvent?: PreprocessingEventResponse;
}

export function CorrectPreprocessingForm() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const sourceEvent = (location.state as CorrectionLocationState | null)
    ?.preprocessingEvent;
  const sourceData = sourceEvent?.eventData;
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CorrectPreprocessingFormValues>({
    resolver: zodResolver(correctPreprocessingSchema),
    defaultValues: {
      inputQuantity: sourceData?.inputQuantity ?? 0,
      outputQuantity: sourceData?.outputQuantity ?? 0,
      grade: sourceData?.grade ?? "",
      processingMethod: sourceData?.processingMethod ?? "",
      preprocessingDate:
        sourceData?.preprocessingDate ?? getLocalDateString(),
      correctionReason: "",
      latitude: sourceEvent?.latitude ?? undefined,
      longitude: sourceEvent?.longitude ?? undefined,
    },
  });

  const inputQuantity = watch("inputQuantity");
  const outputQuantity = watch("outputQuantity");
  const latitude = watch("latitude");
  const longitude = watch("longitude");
  const currentPosition =
    typeof latitude === "number" &&
    Number.isFinite(latitude) &&
    typeof longitude === "number" &&
    Number.isFinite(longitude)
      ? { lat: latitude, lng: longitude }
      : undefined;

  const lossRate = useMemo(() => {
    if (
      !Number.isFinite(inputQuantity) ||
      inputQuantity <= 0 ||
      !Number.isFinite(outputQuantity) ||
      outputQuantity < 0 ||
      outputQuantity > inputQuantity
    ) {
      return null;
    }

    return Math.round(
      ((inputQuantity - outputQuantity) / inputQuantity) * 10_000,
    ) / 100;
  }, [inputQuantity, outputQuantity]);

  const handleLocationSelect = (
    selectedLatitude: number,
    selectedLongitude: number,
  ) => {
    setValue("latitude", selectedLatitude, {
      shouldDirty: true,
      shouldValidate: true,
    });
    setValue("longitude", selectedLongitude, {
      shouldDirty: true,
      shouldValidate: true,
    });
  };

  useAutoGeolocation({
    onLocation: (lat, lng) => {
      handleLocationSelect(lat, lng);
    },
  });

  const onSubmit = async (values: CorrectPreprocessingFormValues) => {
    if (!id) {
      setServerError("Thiếu ID sự kiện sơ chế gốc.");
      return;
    }

    setServerError(null);

    try {
      await correctPreprocessingEvent(id, {
        inputQuantity: values.inputQuantity,
        outputQuantity: values.outputQuantity,
        grade: toOptionalText(values.grade),
        processingMethod: toOptionalText(values.processingMethod),
        preprocessingDate: values.preprocessingDate,
        correctionReason: values.correctionReason.trim(),
        latitude: values.latitude,
        longitude: values.longitude,
      });

      toast.success("Đính chính sự kiện sơ chế thành công.");
      navigate(-1);
    } catch (error) {
      const message = getPreprocessingErrorMessage(
        error,
        "Không thể đính chính sự kiện sơ chế. Vui lòng thử lại.",
      );
      setServerError(message);
      toast.error(message);
    }
  };

  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-amber-800">
          <FilePenLine className="size-5" />
          Đính chính sự kiện sơ chế
        </CardTitle>
        <CardDescription>
          Hệ thống tạo một sự kiện đính chính mới; dữ liệu sự kiện gốc vẫn được
          giữ nguyên trong dòng thời gian.
        </CardDescription>
      </CardHeader>

      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <CardContent className="space-y-6">
          {serverError && (
            <Alert variant="destructive" aria-live="assertive">
              <AlertTriangle className="size-4" />
              <AlertDescription>{serverError}</AlertDescription>
            </Alert>
          )}

          {sourceData ? (
            <div className="grid gap-3 rounded-xl border border-emerald-100 bg-emerald-50 p-4 text-sm sm:grid-cols-2">
              <div>
                <span className="text-muted-foreground">Lô sản xuất</span>
                <p className="font-medium text-emerald-900">
                  {sourceData.productionLotName}
                </p>
              </div>
              <div>
                <span className="text-muted-foreground">Sự kiện gốc</span>
                <p className="break-all font-mono text-xs text-emerald-900">
                  {sourceEvent?.id}
                </p>
              </div>
            </div>
          ) : (
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              Không tải sẵn được dữ liệu sự kiện gốc. Bạn vẫn có thể nhập đầy
              đủ thông tin đính chính; backend sẽ kiểm tra ID và quyền truy cập.
            </div>
          )}

          <section className="space-y-4" aria-labelledby="correction-quantity-heading">
            <div>
              <h2
                id="correction-quantity-heading"
                className="flex items-center gap-2 font-semibold text-amber-800"
              >
                <Scale className="size-4" /> Khối lượng đính chính
              </h2>
              <p className="text-sm text-muted-foreground">
                Nhập lại giá trị chính xác của lần sơ chế cần đính chính.
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="correctionInputQuantity">
                  Khối lượng đưa vào (kg) <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="correctionInputQuantity"
                  type="number"
                  min="0.01"
                  step="0.01"
                  inputMode="decimal"
                  aria-invalid={Boolean(errors.inputQuantity)}
                  {...register("inputQuantity", { valueAsNumber: true })}
                />
                {errors.inputQuantity && (
                  <p className="text-sm text-red-500" role="alert">
                    {errors.inputQuantity.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="correctionOutputQuantity">
                  Khối lượng sau sơ chế (kg) <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="correctionOutputQuantity"
                  type="number"
                  min="0"
                  step="0.01"
                  inputMode="decimal"
                  aria-invalid={Boolean(errors.outputQuantity)}
                  {...register("outputQuantity", { valueAsNumber: true })}
                />
                {errors.outputQuantity && (
                  <p className="text-sm text-red-500" role="alert">
                    {errors.outputQuantity.message}
                  </p>
                )}
              </div>
            </div>

            <div className="flex items-center gap-3 rounded-xl border border-amber-100 bg-amber-50 p-4">
              <Percent className="size-5 text-amber-700" />
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  Tỷ lệ hao hụt sau đính chính
                </p>
                <p className="text-xl font-semibold text-amber-800" aria-live="polite">
                  {lossRate === null ? "—" : `${lossRate.toLocaleString("vi-VN")}%`}
                </p>
              </div>
            </div>
          </section>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="correctionGrade">Hạng phân loại</Label>
              <Input
                id="correctionGrade"
                maxLength={100}
                placeholder="VD: Hạng A, Loại 1"
                {...register("grade")}
              />
              {errors.grade && (
                <p className="text-sm text-red-500" role="alert">
                  {errors.grade.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="correctionPreprocessingDate">
                Ngày sơ chế <span className="text-red-500">*</span>
              </Label>
              <Input
                id="correctionPreprocessingDate"
                type="date"
                max={getLocalDateString()}
                aria-invalid={Boolean(errors.preprocessingDate)}
                {...register("preprocessingDate")}
              />
              {errors.preprocessingDate && (
                <p className="text-sm text-red-500" role="alert">
                  {errors.preprocessingDate.message}
                </p>
              )}
            </div>

            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="correctionProcessingMethod">Phương pháp sơ chế</Label>
              <Textarea
                id="correctionProcessingMethod"
                rows={4}
                maxLength={500}
                {...register("processingMethod")}
              />
              {errors.processingMethod && (
                <p className="text-sm text-red-500" role="alert">
                  {errors.processingMethod.message}
                </p>
              )}
            </div>

            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="correctionReason">
                Lý do đính chính <span className="text-red-500">*</span>
              </Label>
              <Textarea
                id="correctionReason"
                rows={3}
                maxLength={500}
                placeholder="Nêu rõ thông tin sai và căn cứ điều chỉnh..."
                aria-invalid={Boolean(errors.correctionReason)}
                {...register("correctionReason")}
              />
              <div className="flex justify-between gap-3 text-xs text-muted-foreground">
                <span className={errors.correctionReason ? "text-red-500" : ""}>
                  {errors.correctionReason?.message}
                </span>
                <span>{watch("correctionReason")?.length ?? 0}/500</span>
              </div>
            </div>
          </div>

          <section className="space-y-3" aria-labelledby="correction-location-heading">
            <div>
              <h2
                id="correction-location-heading"
                className="flex items-center gap-2 font-semibold text-amber-800"
              >
                <MapPin className="size-4" /> Vị trí đính chính
              </h2>
              <p className="text-sm text-muted-foreground">
                Chọn lại trên bản đồ nếu vị trí trong sự kiện gốc chưa chính xác.
              </p>
            </div>

            <LocationPicker
              onLocationSelect={handleLocationSelect}
              initialPosition={currentPosition}
              height="280px"
            />
          </section>
        </CardContent>

        <CardFooter className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button type="button" variant="outline" onClick={() => navigate(-1)} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button type="submit" variant="edit" disabled={isSubmitting || !id}>
            {isSubmitting && <LoaderCircle className="mr-2 size-4 animate-spin" />}
            {isSubmitting ? "Đang đính chính..." : "Tạo sự kiện đính chính"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
