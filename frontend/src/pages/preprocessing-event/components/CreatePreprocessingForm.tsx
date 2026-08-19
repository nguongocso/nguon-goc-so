import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  AlertTriangle,
  Camera,
  LoaderCircle,
  MapPin,
  Percent,
  Scale,
  Trash2,
  Wheat,
} from "lucide-react";
import { toast } from "sonner";

import { recordPreprocessingEvent } from "@/api/preprocessingApi";
import { getProductionLots } from "@/api/productionLotApi";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { LotValidationStatus } from "@/components/event-validation/LotValidationStatus";
import { useAutoGeolocation } from "@/hooks/useAutoGeolocation";
import { useLotValidation } from "@/hooks/useLotValidation";
import { LocationPicker } from "@/pages/packaging-event/components/LocationPicker";
import type { ProductionLot } from "@/types/productionLot";
import { getLocalDateString } from "@/utils/dateTime";
import {
  recordPreprocessingSchema,
  type RecordPreprocessingFormValues,
} from "@/utils/validators/preprocessingEventSchema";

import {
  fileToBase64,
  getPreprocessingErrorMessage,
  MAX_PREPROCESSING_IMAGES,
  MAX_PREPROCESSING_IMAGE_SIZE,
  toOptionalText,
} from "../preprocessingFormUtils";

export function CreatePreprocessingForm() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const preselectedLotId = searchParams.get("productionLotId") ?? "";

  const [productionLots, setProductionLots] = useState<ProductionLot[]>([]);
  const [loadingLots, setLoadingLots] = useState(true);
  const [lotsError, setLotsError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);
  const previewUrlsRef = useRef<string[]>([]);

  const {
    register,
    handleSubmit,
    setError,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RecordPreprocessingFormValues>({
    resolver: zodResolver(recordPreprocessingSchema),
    defaultValues: {
      productionLotId: "",
      inputQuantity: 0,
      outputQuantity: 0,
      grade: "",
      processingMethod: "",
      preprocessingDate: getLocalDateString(),
      latitude: undefined,
      longitude: undefined,
    },
  });

  const selectedLotId = watch("productionLotId");
  const inputQuantity = watch("inputQuantity");
  const outputQuantity = watch("outputQuantity");
  const latitude = watch("latitude");
  const longitude = watch("longitude");
  const selectedLot = productionLots.find((lot) => lot.id === selectedLotId);

  const { validation, loading: validationLoading, error: validationError } =
    useLotValidation(selectedLotId, "PREPROCESSING");

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

  const loadLots = useCallback(async () => {
    setLoadingLots(true);
    setLotsError(null);

    try {
      const lots = await getProductionLots("HARVESTED");
      setProductionLots(lots.filter((lot) => lot.status === "HARVESTED"));
    } catch (error) {
      setProductionLots([]);
      setLotsError(
        getPreprocessingErrorMessage(
          error,
          "Không thể tải danh sách lô đã thu hoạch.",
        ),
      );
    } finally {
      setLoadingLots(false);
    }
  }, []);

  useEffect(() => {
    void loadLots();
  }, [loadLots]);

  useEffect(() => {
    if (loadingLots || !preselectedLotId || selectedLotId) return;

    const lot = productionLots.find((item) => item.id === preselectedLotId);
    if (!lot) {
      setLotsError(
        "Lô được chọn không còn ở trạng thái đã thu hoạch. Hãy chọn một lô hợp lệ.",
      );
      return;
    }

    setValue("productionLotId", lot.id, { shouldValidate: true });
    if (lot.actualQuantity && lot.actualQuantity > 0) {
      setValue("inputQuantity", lot.actualQuantity, { shouldValidate: true });
    }
  }, [
    loadingLots,
    preselectedLotId,
    productionLots,
    selectedLotId,
    setValue,
  ]);

  useEffect(
    () => () => {
      previewUrlsRef.current.forEach((url) => URL.revokeObjectURL(url));
    },
    [],
  );

  const handleLotChange = (lotId: string | null) => {
    const nextLotId = lotId ?? "";
    const lot = productionLots.find((item) => item.id === nextLotId);
    setServerError(null);
    setLotsError(null);
    setValue("productionLotId", nextLotId, {
      shouldDirty: true,
      shouldValidate: true,
    });

    if (lot?.actualQuantity && lot.actualQuantity > 0) {
      setValue("inputQuantity", lot.actualQuantity, {
        shouldDirty: true,
        shouldValidate: true,
      });
    }

    if (lot?.harvestDate) {
      const today = getLocalDateString();
      setValue(
        "preprocessingDate",
        lot.harvestDate > today ? today : lot.harvestDate,
        { shouldValidate: true },
      );
    }
  };

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
    onLocation: (selectedLatitude, selectedLongitude) => {
      handleLocationSelect(selectedLatitude, selectedLongitude);
      toast.success("Đã lấy vị trí hiện tại");
    },
    onError: (message) => {
      toast.error(`Không thể lấy vị trí: ${message}`);
    },
  });

  const handleImageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(event.target.files ?? []);
    event.target.value = "";

    const validFiles = selectedFiles.filter((file) => {
      if (!file.type.startsWith("image/")) {
        toast.error(`“${file.name}” không phải là tệp ảnh.`);
        return false;
      }

      if (file.size > MAX_PREPROCESSING_IMAGE_SIZE) {
        toast.error(`“${file.name}” vượt quá dung lượng 5 MB.`);
        return false;
      }

      return true;
    });

    const availableSlots = MAX_PREPROCESSING_IMAGES - imageFiles.length;
    if (validFiles.length > availableSlots) {
      toast.error(`Chỉ được chọn tối đa ${MAX_PREPROCESSING_IMAGES} ảnh.`);
    }

    const acceptedFiles = validFiles.slice(0, availableSlots);
    const nextUrls = acceptedFiles.map((file) => URL.createObjectURL(file));
    previewUrlsRef.current = [...previewUrlsRef.current, ...nextUrls];
    setImageFiles((current) => [...current, ...acceptedFiles]);
    setImagePreviews((current) => [...current, ...nextUrls]);
  };

  const removeImage = (index: number) => {
    const url = imagePreviews[index];
    if (url) URL.revokeObjectURL(url);
    previewUrlsRef.current = previewUrlsRef.current.filter(
      (previewUrl) => previewUrl !== url,
    );
    setImageFiles((current) => current.filter((_, itemIndex) => itemIndex !== index));
    setImagePreviews((current) =>
      current.filter((_, itemIndex) => itemIndex !== index),
    );
  };

  const onSubmit = async (values: RecordPreprocessingFormValues) => {
    setServerError(null);

    if (!selectedLot) {
      setError("productionLotId", {
        type: "manual",
        message: "Vui lòng chọn lô sản xuất",
      });
      return;
    }

    if (
      selectedLot.harvestDate &&
      values.preprocessingDate < selectedLot.harvestDate
    ) {
      setError("preprocessingDate", {
        type: "manual",
        message: "Ngày sơ chế phải sau hoặc bằng ngày thu hoạch của lô",
      });
      return;
    }

    if (!validation?.valid) {
      setServerError(
        validation?.message ||
          validationError ||
          "Lô sản xuất chưa đủ điều kiện ghi sự kiện sơ chế.",
      );
      return;
    }

    try {
      const images = await Promise.all(imageFiles.map(fileToBase64));
      await recordPreprocessingEvent({
        productionLotId: values.productionLotId,
        inputQuantity: values.inputQuantity,
        outputQuantity: values.outputQuantity,
        grade: toOptionalText(values.grade),
        processingMethod: toOptionalText(values.processingMethod),
        preprocessingDate: values.preprocessingDate,
        images: images.length > 0 ? images : undefined,
        latitude: values.latitude,
        longitude: values.longitude,
        deviceSource: "WEB",
      });

      toast.success(`Đã ghi sự kiện sơ chế cho lô “${selectedLot.name}”.`);
      navigate(`/production-lots/${selectedLot.id}`, { replace: true });
    } catch (error) {
      const message = getPreprocessingErrorMessage(
        error,
        "Không thể ghi sự kiện sơ chế. Vui lòng kiểm tra lại thông tin.",
      );
      setServerError(message);
      toast.error(message);
    }
  };

  return (
    <Card className="mx-auto max-w-5xl border-emerald-100 shadow-sm">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-emerald-800">
          <Wheat className="size-5" />
          Ghi sự kiện sơ chế và phân loại
        </CardTitle>
        <CardDescription>
          Ghi nhận khối lượng trước và sau sơ chế, phẩm cấp, phương pháp thực
          hiện và bằng chứng thực địa.
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

          <section className="space-y-4" aria-labelledby="preprocessing-lot-heading">
            <div>
              <h2
                id="preprocessing-lot-heading"
                className="font-semibold text-emerald-800"
              >
                Lô sản xuất
              </h2>
              <p className="text-sm text-muted-foreground">
                Chỉ hiển thị các lô đang ở trạng thái đã thu hoạch.
              </p>
            </div>

            {loadingLots ? (
              <div className="flex items-center gap-2 rounded-lg border p-4 text-sm text-muted-foreground">
                <LoaderCircle className="size-4 animate-spin text-emerald-600" />
                Đang tải danh sách lô...
              </div>
            ) : lotsError && productionLots.length === 0 ? (
              <div className="space-y-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                <p>{lotsError}</p>
                <Button type="button" size="sm" variant="outline" onClick={() => void loadLots()}>
                  Thử lại
                </Button>
              </div>
            ) : productionLots.length === 0 ? (
              <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
                Chưa có lô sản xuất nào đã thu hoạch để ghi sơ chế.
              </div>
            ) : (
              <div className="space-y-2">
                {lotsError && (
                  <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
                    {lotsError}
                  </div>
                )}
                <Label htmlFor="productionLotId">
                  Lô sản xuất <span className="text-red-500">*</span>
                </Label>
                <Select value={selectedLotId} onValueChange={handleLotChange}>
                <SelectTrigger
                  id="productionLotId"
                  className="w-full"
                  aria-invalid={Boolean(errors.productionLotId)}
                >
                    <span>
                      {selectedLot?.name ?? "Chọn lô đã thu hoạch"}
                    </span>
                  </SelectTrigger>
                  <SelectContent>
                    {productionLots.map((lot) => (
                      <SelectItem key={lot.id} value={lot.id}>
                        {lot.name} — {lot.actualQuantity?.toLocaleString("vi-VN") ?? "—"} kg
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.productionLotId && (
                  <p className="text-sm text-red-500" role="alert">
                    {errors.productionLotId.message}
                  </p>
                )}
                <LotValidationStatus
                  isValid={validation?.valid ?? null}
                  message={validationError || validation?.message || ""}
                  loading={validationLoading}
                  className="mt-2"
                />
              </div>
            )}
          </section>

          <section className="space-y-4" aria-labelledby="preprocessing-quantity-heading">
            <div>
              <h2
                id="preprocessing-quantity-heading"
                className="flex items-center gap-2 font-semibold text-emerald-800"
              >
                <Scale className="size-4" /> Khối lượng và hao hụt
              </h2>
              <p className="text-sm text-muted-foreground">
                Tỷ lệ hao hụt được tính tự động từ hai khối lượng bên dưới.
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="inputQuantity">
                  Khối lượng đưa vào (kg) <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="inputQuantity"
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
                <Label htmlFor="outputQuantity">
                  Khối lượng sau sơ chế (kg) <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="outputQuantity"
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

            <div className="flex items-center gap-3 rounded-xl border border-emerald-100 bg-emerald-50 p-4">
              <Percent className="size-5 text-emerald-700" />
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  Tỷ lệ hao hụt dự kiến
                </p>
                <p className="text-xl font-semibold text-emerald-800" aria-live="polite">
                  {lossRate === null ? "—" : `${lossRate.toLocaleString("vi-VN")}%`}
                </p>
              </div>
            </div>
          </section>

          <section className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="grade">Hạng phân loại</Label>
              <Input
                id="grade"
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
              <Label htmlFor="preprocessingDate">
                Ngày sơ chế <span className="text-red-500">*</span>
              </Label>
              <Input
                id="preprocessingDate"
                type="date"
                min={selectedLot?.harvestDate || undefined}
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
              <Label htmlFor="processingMethod">Phương pháp sơ chế</Label>
              <Textarea
                id="processingMethod"
                rows={4}
                maxLength={500}
                placeholder="Mô tả các bước rửa, làm sạch, sấy, loại bỏ sản phẩm không đạt..."
                {...register("processingMethod")}
              />
              <div className="flex justify-between gap-3 text-xs text-muted-foreground">
                <span>{errors.processingMethod?.message}</span>
                <span>{watch("processingMethod")?.length ?? 0}/500</span>
              </div>
            </div>
          </section>

          <section className="space-y-3" aria-labelledby="preprocessing-location-heading">
            <div>
              <h2
                id="preprocessing-location-heading"
                className="flex items-center gap-2 font-semibold text-emerald-800"
              >
                <MapPin className="size-4" /> Vị trí sơ chế
              </h2>
              <p className="text-sm text-muted-foreground">
                Không bắt buộc; chọn vị trí trên bản đồ.
              </p>
            </div>

            <LocationPicker
              onLocationSelect={handleLocationSelect}
              initialPosition={currentPosition}
              height="280px"
            />
          </section>

          <section className="space-y-3" aria-labelledby="preprocessing-images-heading">
            <div>
              <h2
                id="preprocessing-images-heading"
                className="flex items-center gap-2 font-semibold text-emerald-800"
              >
                <Camera className="size-4" /> Hình ảnh thực địa
              </h2>
              <p className="text-sm text-muted-foreground">
                Tối đa {MAX_PREPROCESSING_IMAGES} ảnh, không quá 5 MB mỗi ảnh.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <Button
                type="button"
                size="sm"
                variant="outline"
                disabled={isSubmitting || imageFiles.length >= MAX_PREPROCESSING_IMAGES}
                onClick={() => document.getElementById("preprocessing-images")?.click()}
              >
                <Camera className="mr-1 size-4" /> Chọn ảnh
              </Button>
              <span className="text-sm text-muted-foreground">
                {imageFiles.length}/{MAX_PREPROCESSING_IMAGES}
              </span>
              <input
                id="preprocessing-images"
                type="file"
                accept="image/*"
                multiple
                className="sr-only"
                onChange={handleImageChange}
                disabled={isSubmitting}
              />
            </div>

            {imagePreviews.length > 0 && (
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5">
                {imagePreviews.map((preview, index) => (
                  <div key={preview} className="group relative overflow-hidden rounded-lg border bg-muted">
                    <img
                      src={preview}
                      alt={`Ảnh sơ chế ${index + 1}`}
                      className="aspect-square w-full object-cover"
                    />
                    <Button
                      type="button"
                      size="icon"
                      variant="destructive"
                      className="absolute right-1 top-1 size-8"
                      aria-label={`Xóa ảnh ${index + 1}`}
                      onClick={() => removeImage(index)}
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </section>

          <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
            <p className="font-medium">Sau khi ghi nhận thành công:</p>
            <ul className="mt-1 list-disc space-y-1 pl-5">
              <li>Lô chuyển từ “Đã thu hoạch” sang “Đã sơ chế”.</li>
              <li>Sản lượng thực tế được cập nhật bằng khối lượng sau sơ chế.</li>
              <li>Sự kiện đã ghi không bị sửa trực tiếp; sai sót phải được đính chính.</li>
            </ul>
          </div>
        </CardContent>

        <CardFooter className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button type="button" variant="outline" onClick={() => navigate(-1)} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button
            type="submit"
            variant="create"
            disabled={
              isSubmitting ||
              loadingLots ||
              validationLoading ||
              !selectedLotId ||
              !validation?.valid
            }
          >
            {isSubmitting && <LoaderCircle className="mr-2 size-4 animate-spin" />}
            {isSubmitting ? "Đang ghi nhận..." : "Ghi sự kiện sơ chế"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
