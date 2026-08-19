import { useState } from "react";
import { isAxiosError } from "axios";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";

import { recordTransportEvent } from "@/api/transportEventApi";
import { useOfflineSync } from "@/hooks/useOfflineSync";
import { addOfflineEvent } from "@/services/offlineQueue";
import { ChainEventType } from "@/enums/chainEventType";
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
  transportEventSchema,
  type TransportEventFormValues,
} from "@/utils/validators/transportEventSchema";

import { ScanCodeField } from "./ScanCodeField";
import { Camera } from "lucide-react";

const MAX_IMAGES = 5;

function getCurrentDateTimeLocal() {
  const now = new Date();
  const timezoneOffset = now.getTimezoneOffset() * 60_000;
  return new Date(now.getTime() - timezoneOffset).toISOString().slice(0, 16);
}

const fileToBase64 = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = (error) => reject(error);
  });

export function TransportEventForm() {
  const navigate = useNavigate();
  const location = useLocation();
  const prefilledCode =
    (location.state as { codeValue?: string } | null)?.codeValue ?? "";

  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<TransportEventFormValues>({
    resolver: zodResolver(transportEventSchema),
    defaultValues: {
      codeValue: prefilledCode,
      fromLocation: "",
      toLocation: "",
      transportTime: getCurrentDateTimeLocal(),
    },
  });

  const codeValue = watch("codeValue");
  const { isOnline } = useOfflineSync();

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    const fileArray = Array.from(files);
    if (imageFiles.length + fileArray.length > MAX_IMAGES) {
      toast.error(`Chỉ được chọn tối đa ${MAX_IMAGES} ảnh`);
      return;
    }
    setImageFiles((prev) => [...prev, ...fileArray]);
    const newPreviews = fileArray.map((f) => URL.createObjectURL(f));
    setImagePreviews((prev) => [...prev, ...newPreviews]);
  };

  const removeImage = (index: number) => {
    setImageFiles((prev) => prev.filter((_, i) => i !== index));
    setImagePreviews((prev) => prev.filter((_, i) => i !== index));
  };

  const saveOffline = (values: TransportEventFormValues) => {
    const validationError = addOfflineEvent({
      eventType: ChainEventType.TRANSPORT,
      recordedAt: values.transportTime,
      latitude: 0,
      longitude: 0,
      images: imagePreviews,
      deviceSource: "WEB",
      codeValue: values.codeValue,
      eventData: {
        codeValue: values.codeValue,
        fromLocation: values.fromLocation,
        toLocation: values.toLocation,
        transportTime: values.transportTime,
      },
    });

    if (validationError) {
      toast.error(`Không thể lưu tạm: ${validationError}`);
      return false;
    }

    toast.info("Không có kết nối mạng. Sự kiện đã được lưu tạm và sẽ đồng bộ khi có mạng.");
    reset({
      codeValue: "",
      fromLocation: "",
      toLocation: "",
      transportTime: getCurrentDateTimeLocal(),
    });
    setImageFiles([]);
    setImagePreviews([]);
    return true;
  };

  const onSubmit = async (values: TransportEventFormValues) => {
    try {
      if (!isOnline) {
        saveOffline(values);
        return;
      }

      // Convert images to base64
      let base64Images: string[] = [];
      try {
        base64Images = await Promise.all(imageFiles.map(fileToBase64));
      } catch {
        toast.error("Không thể xử lý ảnh. Vui lòng thử lại.");
        return;
      }

      // Send to backend with images
      await recordTransportEvent({
        ...values,
        images: base64Images.length > 0 ? base64Images : undefined,
      } as any);

      toast.success("Ghi sự kiện vận chuyển thành công.");

      reset({
        codeValue: "",
        fromLocation: "",
        toLocation: "",
        transportTime: getCurrentDateTimeLocal(),
      });
      setImageFiles([]);
      setImagePreviews([]);
    } catch (error: unknown) {
      const isNetworkError =
        !isAxiosError(error) ||
        (error as { code?: string }).code === "ERR_NETWORK" ||
        (error as { message?: string })?.message?.includes("Network") ||
        !(error as { response?: unknown }).response;

      if (isNetworkError) {
        saveOffline(values);
        return;
      }

      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message ??
          (error.response
            ? "Không thể ghi sự kiện vận chuyển."
            : "Không thể kết nối đến máy chủ. Vui lòng kiểm tra backend.")
        : "Đã xảy ra lỗi khi ghi sự kiện vận chuyển.";

      toast.error(message);
    }
  };

  return (
    <Card className="mx-auto max-w-3xl">
      <CardHeader>
        <CardTitle>Ghi sự kiện vận chuyển</CardTitle>
        <CardDescription>
          Quét mã truy xuất của lô hàng, sau đó nhập thông tin chuyến vận
          chuyển thực tế.
        </CardDescription>
      </CardHeader>

      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-6">
          <ScanCodeField
            value={codeValue}
            onChange={(value) =>
              setValue("codeValue", value, {
                shouldValidate: true,
              })
            }
            error={errors.codeValue?.message}
          />

          <div className="grid gap-6 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="fromLocation">Điểm đi *</Label>
              <Input
                id="fromLocation"
                placeholder="Ví dụ: Xã Long Cốc, huyện Tân Sơn, Phú Thọ"
                {...register("fromLocation")}
              />
              {errors.fromLocation && (
                <p className="text-sm text-destructive">
                  {errors.fromLocation.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="toLocation">Điểm đến *</Label>
              <Input
                id="toLocation"
                placeholder="Ví dụ: Kho trung chuyển Việt Trì, Phú Thọ"
                {...register("toLocation")}
              />
              {errors.toLocation && (
                <p className="text-sm text-destructive">
                  {errors.toLocation.message}
                </p>
              )}
            </div>
          </div>

          <div className="max-w-sm space-y-2">
            <Label htmlFor="transportTime">Thời gian vận chuyển *</Label>
            <Input
              id="transportTime"
              type="datetime-local"
              {...register("transportTime")}
            />
            {errors.transportTime && (
              <p className="text-sm text-destructive">
                {errors.transportTime.message}
              </p>
            )}
          </div>

          {/* Image Upload */}
          <div className="space-y-2">
            <Label>Hình ảnh thực địa (tối đa {MAX_IMAGES})</Label>
            <div className="flex items-center gap-2 flex-wrap">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => document.getElementById('transport-image-input')?.click()}
                disabled={isSubmitting || imageFiles.length >= MAX_IMAGES}
              >
                <Camera className="h-4 w-4 mr-1" />
                Chọn ảnh
              </Button>
              <span className="text-sm text-muted-foreground">
                {imageFiles.length}/{MAX_IMAGES}
              </span>
              <input
                id="transport-image-input"
                type="file"
                accept="image/*"
                multiple
                className="hidden"
                onChange={handleImageChange}
                disabled={isSubmitting}
              />
            </div>
            {imagePreviews.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-2">
                {imagePreviews.map((src, idx) => (
                  <div key={idx} className="relative w-16 h-16 rounded border overflow-hidden">
                    <img src={src} alt={`preview-${idx}`} className="w-full h-full object-cover" />
                    <button
                      type="button"
                      className="absolute -top-1 -right-1 bg-red-500 text-white rounded-full w-5 h-5 flex items-center justify-center text-xs hover:bg-red-600"
                      onClick={() => removeImage(idx)}
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </CardContent>

        <CardFooter className="flex justify-end gap-3">
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() => navigate(-1)}
            className="border-emerald-200 text-emerald-700 hover:bg-emerald-50"
          >
            Hủy
          </Button>

          <Button type="submit" size="sm" disabled={isSubmitting} variant="create">
            {isSubmitting ? "Đang ghi..." : "Ghi sự kiện vận chuyển"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}