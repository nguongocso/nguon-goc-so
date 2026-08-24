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
import { selectAllOnFocus, preventMouseUpCollapse } from "@/utils/inputUtils";
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
      inputQuantity: undefined,
      outputQuantity: undefined,
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
  };

  const handleLocationSelect = useCallback(
    (nextLat: number, nextLng: number) => {
      setValue("latitude", nextLat, {
        shouldDirty: true,
        shouldValidate: true,
      });
      setValue("longitude", nextLng, {
        shouldDirty: true,
        shouldValidate: true,
      });
    },
    [setValue],
  );

  useAutoGeolocation({
    onLocation: (nextLat, nextLng) => {
      handleLocationSelect(nextLat, nextLng);
      toast.success("Đã cập nhật vị trí hiện tại cho sự kiện sơ chế.");
    },
    onError: (message) => {
      toast.error(`Không thể lấy vị trí hiện tại: ${message}`);
    },
  });

  const handleImageChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || files.length === 0) return;

    const fileList = Array.from(files);
    if (imageFiles.length + fileList.length > MAX_PREPROCESSING_IMAGES) {
      toast.error(`Chỉ được chọn tối đa ${MAX_PREPROCESSING_IMAGES} ảnh thực địa.`);
      event.target.value = "";
      return;
    }

    for (const file of fileList) {
      if (!file.type.startsWith("image/")) {
        toast.error(`File "${file.name}" không phải định dạng ảnh.`);
        event.target.value = "";
        return;
      }
      if (file.size > MAX_PREPROCESSING_IMAGE_SIZE) {
        toast.error(`Ảnh "${file.name}" vượt quá kích thước 5 MB.`);
        event.target.value = "";
        return;
      }
    }

    const nextFiles = [...imageFiles, ...fileList];
    const newUrls = fileList.map((file) => URL.createObjectURL(file));

    previewUrlsRef.current = [...previewUrlsRef.current, ...newUrls];
    setImageFiles(nextFiles);
    setImagePreviews((prev) => [...prev, ...newUrls]);
    event.target.value = "";
  };

  const removeImage = (index: number) => {
    const targetUrl = imagePreviews[index];
    if (targetUrl) {
      URL.revokeObjectURL(targetUrl);
      previewUrlsRef.current = previewUrlsRef.current.filter(
        (url) => url !== targetUrl,
      );
    }
    setImageFiles((prev) => prev.filter((_, i) => i !== index));
    setImagePreviews((prev) => prev.filter((_, i) => i !== index));
  };

  const onSubmit = async (values: RecordPreprocessingFormValues) => {
    setServerError(null);

    if (validation && !validation.valid) {
      const msg = validation.message || "Lô sản xuất không hợp lệ.";
      setServerError(msg);
      toast.error(msg);
      return;
    }

    try {
      const base64Images = await Promise.all(
        imageFiles.map((file) => fileToBase64(file)),
      );

      await recordPreprocessingEvent({
        productionLotId: values.productionLotId,
        inputQuantity: values.inputQuantity,
        outputQuantity: values.outputQuantity,
        grade: toOptionalText(values.grade),
        processingMethod: toOptionalText(values.processingMethod),
        preprocessingDate: values.preprocessingDate,
        latitude: values.latitude,
        longitude: values.longitude,
        images: base64Images.length > 0 ? base64Images : undefined,
      });

      toast.success("Ghi nhận sự kiện sơ chế thành công!");
      navigate(`/production-lots/${values.productionLotId}`);
    } catch (error) {
      const message = getPreprocessingErrorMessage(
        error,
        "Không thể lưu sự kiện sơ chế. Vui lòng kiểm tra lại thông tin.",
      );
      setServerError(message);

      if (message.includes("Sản lượng sau sơ chế")) {
        setError("outputQuantity", { type: "server", message });
      } else if (message.includes("Sản lượng ban đầu")) {
        setError("inputQuantity", { type: "server", message });
      }

      toast.error(message);
    }
  };

  return (
    <Card className="mx-auto max-w-4xl border-emerald-100 shadow-sm">
      <CardHeader className="border-b border-emerald-50 bg-emerald-50/40">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-lg bg-emerald-100 text-emerald-800">
            <Wheat className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl font-bold text-emerald-900">
              Ghi sự kiện sơ chế
            </CardTitle>
            <CardDescription className="text-emerald-700">
              Ghi nhận phân loại, hao hụt và xử lý sơ bộ nông sản sau thu hoạch
            </CardDescription>
          </div>
        </div>
      </CardHeader>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <CardContent className="space-y-6 pt-6">
          {serverError && (
            <Alert variant="destructive">
              <AlertTriangle className="size-4" />
              <AlertDescription>{serverError}</AlertDescription>
            </Alert>
          )}

          {lotsError && (
            <Alert variant="destructive">
              <AlertTriangle className="size-4" />
              <AlertDescription className="flex items-center justify-between gap-2">
                <span>{lotsError}</span>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => void loadLots()}
                >
                  Tải lại
                </Button>
              </AlertDescription>
            </Alert>
          )}

          <section className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="productionLotId" className="font-semibold text-emerald-900">
                Chọn lô sản xuất (Đã thu hoạch) <span className="text-red-500">*</span>
              </Label>
              <Select
                value={selectedLotId}
                onValueChange={handleLotChange}
                disabled={loadingLots || isSubmitting}
              >
                <SelectTrigger id="productionLotId" className="w-full">
                  <span>
                    {selectedLot
                      ? selectedLot.name
                      : loadingLots
                        ? "Đang tải danh sách lô..."
                        : "Chọn lô sản xuất"}
                  </span>
                </SelectTrigger>
                <SelectContent>
                  {productionLots.map((lot) => (
                    <SelectItem key={lot.id} value={lot.id}>
                      {lot.name}
                      {lot.productCategoryName ? ` - ${lot.productCategoryName}` : ""}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              {errors.productionLotId && (
                <p className="text-sm font-medium text-red-500">
                  {errors.productionLotId.message}
                </p>
              )}
            </div>

            {selectedLotId && (
              <LotValidationStatus
                isValid={validation?.valid ?? null}
                message={validation?.message ?? validationError ?? ""}
                loading={validationLoading}
              />
            )}
          </section>

          <section className="space-y-4 rounded-lg border border-emerald-100 bg-emerald-50/20 p-4">
            <h2 className="flex items-center gap-2 font-semibold text-emerald-800">
              <Scale className="size-4" /> Khối lượng & Phân loại
            </h2>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="inputQuantity">
                  Khối lượng đưa vào sơ chế (kg) <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="inputQuantity"
                  type="number"
                  step="0.01"
                  min="0.01"
                  placeholder="VD: 1000"
                  disabled={isSubmitting}
                  {...register("inputQuantity", { valueAsNumber: true })}
                  onFocus={selectAllOnFocus}
                  onMouseUp={preventMouseUpCollapse}
                />
                {errors.inputQuantity && (
                  <p className="text-sm text-red-500">{errors.inputQuantity.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="outputQuantity">
                  Khối lượng thu được sau sơ chế (kg) <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="outputQuantity"
                  type="number"
                  step="0.01"
                  min="0.01"
                  placeholder="VD: 950"
                  disabled={isSubmitting}
                  {...register("outputQuantity", { valueAsNumber: true })}
                  onFocus={selectAllOnFocus}
                  onMouseUp={preventMouseUpCollapse}
                />
                {errors.outputQuantity && (
                  <p className="text-sm text-red-500">{errors.outputQuantity.message}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="rounded-lg border border-emerald-200 bg-white p-3 shadow-xs">
                <div className="flex items-center justify-between text-sm">
                  <span className="flex items-center gap-1.5 text-muted-foreground">
                    <Percent className="size-4 text-emerald-700" /> Tỷ lệ hao hụt dự kiến:
                  </span>
                  <span className="font-bold text-emerald-900">
                    {lossRate !== null ? `${lossRate}%` : "--"}
                  </span>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="grade">Phân loại phẩm cấp (Grade)</Label>
                <Input
                  id="grade"
                  placeholder="VD: Loại 1, Hạng A, Xuất khẩu..."
                  disabled={isSubmitting}
                  {...register("grade")}
                />
                {errors.grade && (
                  <p className="text-sm text-red-500">{errors.grade.message}</p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="processingMethod">Phương pháp / Quy trình sơ chế</Label>
              <Textarea
                id="processingMethod"
                rows={3}
                placeholder="VD: Rửa sạch, sấy lạnh ở 45 độ C trong 8 giờ, phân loại kích thước bằng sàng..."
                disabled={isSubmitting}
                {...register("processingMethod")}
              />
              {errors.processingMethod && (
                <p className="text-sm text-red-500">{errors.processingMethod.message}</p>
              )}
            </div>
          </section>

          <section className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="preprocessingDate" className="font-semibold text-emerald-900">
                Ngày thực hiện sơ chế <span className="text-red-500">*</span>
              </Label>
              <Input
                id="preprocessingDate"
                type="date"
                max={getLocalDateString()}
                disabled={isSubmitting}
                {...register("preprocessingDate")}
              />
              {errors.preprocessingDate && (
                <p className="text-sm text-red-500">{errors.preprocessingDate.message}</p>
              )}
            </div>

            <div className="space-y-3 rounded-lg border border-emerald-100 bg-emerald-50/20 p-4">
              <Label className="flex items-center gap-2 font-semibold text-emerald-800">
                <MapPin className="size-4" /> Vị trí sơ chế (Click trên bản đồ)
              </Label>

              <LocationPicker
                onLocationSelect={handleLocationSelect}
                initialPosition={currentPosition}
                height="280px"
              />
            </div>
          </section>

          <section className="space-y-3 rounded-lg border border-emerald-100 bg-emerald-50/20 p-4">
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
