import React, { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Camera, Loader2, MapPin } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
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
  SelectValue,
} from "@/components/ui/select";

import { recordMobileEvent } from "@/api/chainEventApi";
import {
  ChainEventType,
  ChainEventTypeLabel,
} from "@/enums/chainEventType";
import { useAutoGeolocation } from "@/hooks/useAutoGeolocation";
import { useOfflineSync } from "@/hooks/useOfflineSync";
import { addOfflineEvent } from "@/services/offlineQueue";
import type { ProductionLot } from "@/types/productionLot";
import {
  getLocalDateString,
  getLocalDateTimeString,
  isoToLocalDateTimeInputValue,
} from "@/utils/dateTime";
import {
  mobileEventSchema,
  type MobileEventFormValues,
} from "@/utils/validators";

interface Props {
  lots: ProductionLot[];
  onSuccess?: () => void;
}

const MAX_IMAGES = 5;

type MobileEventPayload =
  Parameters<typeof recordMobileEvent>[0] &
  Parameters<typeof addOfflineEvent>[0];

const getInitialFormValues = (): MobileEventFormValues => ({
  productionLotId: "",
  eventType: ChainEventType.HARVEST,
  recordedAt: new Date(getLocalDateTimeString()).toISOString(),
  latitude: 0,
  longitude: 0,
  images: [],
  harvestDate: getLocalDateString(),
  packagingDate: getLocalDateString(),
  quantity: 0,
  packagingSpecification: "",
});

const fileToBase64 = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = () => {
      if (typeof reader.result === "string") {
        resolve(reader.result);
        return;
      }

      reject(new Error("Không thể đọc dữ liệu ảnh"));
    };

    reader.onerror = () => {
      reject(reader.error ?? new Error("Không thể đọc tệp ảnh"));
    };

    reader.readAsDataURL(file);
  });

export const RecordMobileEventForm: React.FC<Props> = ({
  lots,
  onSuccess,
}) => {
  const { isOnline } = useOfflineSync();

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);

  const {
    control,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<MobileEventFormValues>({
    resolver: zodResolver(mobileEventSchema),
    defaultValues: getInitialFormValues(),
  });

  const eventType = watch("eventType");

  const { locationLoading, fetchLocation: getLocation } =
    useAutoGeolocation({
      onLocation: (latitude, longitude) => {
        setValue("latitude", latitude, {
          shouldValidate: true,
          shouldDirty: true,
        });

        setValue("longitude", longitude, {
          shouldValidate: true,
          shouldDirty: true,
        });

        toast.success("Đã lấy vị trí GPS");
      },

      onError: (message) => {
        toast.error(`Không thể lấy vị trí: ${message}`);
      },
    });

  useEffect(() => {
    return () => {
      imagePreviews.forEach((preview) => {
        URL.revokeObjectURL(preview);
      });
    };
  }, [imagePreviews]);

  const clearImages = () => {
    imagePreviews.forEach((preview) => {
      URL.revokeObjectURL(preview);
    });

    setImageFiles([]);
    setImagePreviews([]);

    setValue("images", [], {
      shouldValidate: true,
      shouldDirty: true,
    });
  };

  const resetForm = () => {
    clearImages();
    reset(getInitialFormValues());
  };

  const handleImageChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const selectedFiles = event.target.files;

    if (!selectedFiles) {
      return;
    }

    const newFiles = Array.from(selectedFiles);

    if (imageFiles.length + newFiles.length > MAX_IMAGES) {
      toast.error(`Chỉ được chọn tối đa ${MAX_IMAGES} ảnh`);
      event.target.value = "";
      return;
    }

    const newPreviews = newFiles.map((file) =>
      URL.createObjectURL(file),
    );

    const updatedFiles = [...imageFiles, ...newFiles];
    const updatedPreviews = [...imagePreviews, ...newPreviews];

    setImageFiles(updatedFiles);
    setImagePreviews(updatedPreviews);

    setValue("images", updatedPreviews, {
      shouldValidate: true,
      shouldDirty: true,
    });

    event.target.value = "";
  };

  const removeImage = (index: number) => {
    const previewToRemove = imagePreviews[index];

    if (previewToRemove) {
      URL.revokeObjectURL(previewToRemove);
    }

    const updatedFiles = imageFiles.filter(
      (_, fileIndex) => fileIndex !== index,
    );

    const updatedPreviews = imagePreviews.filter(
      (_, previewIndex) => previewIndex !== index,
    );

    setImageFiles(updatedFiles);
    setImagePreviews(updatedPreviews);

    setValue("images", updatedPreviews, {
      shouldValidate: true,
      shouldDirty: true,
    });
  };

  const onSubmit = async (data: MobileEventFormValues) => {
    setIsSubmitting(true);

    let payload: MobileEventPayload | null = null;

    try {
      let base64Images: string[];

      try {
        base64Images = await Promise.all(
          imageFiles.map(fileToBase64),
        );
      } catch {
        toast.error(
          "Không thể xử lý ảnh. Vui lòng chọn lại ảnh và thử lại.",
        );
        return;
      }

      let eventData: Record<string, unknown> = {};

      if (data.eventType === ChainEventType.HARVEST) {
        eventData = {
          quantity: data.quantity,
          harvestDate: data.harvestDate,
        };
      }

      if (data.eventType === ChainEventType.PACKAGING) {
        eventData = {
          packagingSpecification:
            data.packagingSpecification,
          packagingDate: data.packagingDate,
        };
      }

      payload = {
        productionLotId: data.productionLotId,
        eventType: data.eventType,
        recordedAt: data.recordedAt,
        latitude: data.latitude,
        longitude: data.longitude,
        images: base64Images,
        deviceSource: "MOBILE",
        eventData,
      };

      if (!isOnline) {
        const validationError = addOfflineEvent(payload);

        if (validationError) {
          toast.error(
            `Không thể lưu tạm: ${validationError}`,
          );
          return;
        }

        toast.info(
          "Không có kết nối. Sự kiện đã được lưu tạm và sẽ đồng bộ sau.",
        );

        resetForm();
        onSuccess?.();
        return;
      }

      await recordMobileEvent(payload);

      toast.success("Ghi sự kiện thành công!");
      resetForm();
      onSuccess?.();
    } catch (error: any) {
      const isNetworkError =
        error?.code === "ERR_NETWORK" ||
        error?.message?.includes("Network");

      if (payload && isNetworkError) {
        const validationError = addOfflineEvent(payload);

        if (validationError) {
          toast.error(
            `Không thể lưu tạm: ${validationError}`,
          );
          return;
        }

        toast.info(
          "Lỗi mạng. Sự kiện đã được lưu tạm và sẽ đồng bộ sau.",
        );

        resetForm();
        onSuccess?.();
        return;
      }

      toast.error(
        error?.response?.data?.message ||
          "Ghi sự kiện thất bại",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredLots = lots.filter(
    (lot) =>
      lot.status === "APPROVED" ||
      lot.status === "HARVESTED",
  );

  return (
    <Card className="mx-auto max-w-md">
      <CardHeader>
        <CardTitle>Ghi sự kiện ngoài đồng</CardTitle>

        <CardDescription>
          Nhập thông tin thu hoạch hoặc đóng gói kèm ảnh
          thực địa
        </CardDescription>
      </CardHeader>

      <CardContent>
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="space-y-4"
        >
          <div className="space-y-2">
            <Label>Lô sản xuất *</Label>

            <Controller
              name="productionLotId"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={isSubmitting}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn lô sản xuất">
                      {field.value
                        ? lots.find(
                            (lot) =>
                              lot.id === field.value,
                          )?.name
                        : null}
                    </SelectValue>
                  </SelectTrigger>

                  <SelectContent className="min-w-[220px] w-auto">
                    {filteredLots.map((lot) => (
                      <SelectItem
                        key={lot.id}
                        value={lot.id}
                      >
                        {lot.name} ({lot.status})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />

            {errors.productionLotId && (
              <p className="text-sm text-red-500">
                {errors.productionLotId.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Loại sự kiện *</Label>

            <Controller
              name="eventType"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={(value) => {
                    field.onChange(value);

                    if (
                      value === ChainEventType.HARVEST
                    ) {
                      setValue(
                        "harvestDate",
                        getLocalDateString(),
                      );

                      setValue("quantity", 0);
                    }

                    if (
                      value === ChainEventType.PACKAGING
                    ) {
                      setValue(
                        "packagingDate",
                        getLocalDateString(),
                      );

                      setValue(
                        "packagingSpecification",
                        "",
                      );
                    }
                  }}
                  disabled={isSubmitting}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn loại">
                      {field.value
                        ? ChainEventTypeLabel[
                            field.value as ChainEventType
                          ]
                        : null}
                    </SelectValue>
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem
                      value={ChainEventType.HARVEST}
                    >
                      {
                        ChainEventTypeLabel[
                          ChainEventType.HARVEST
                        ]
                      }
                    </SelectItem>

                    <SelectItem
                      value={ChainEventType.PACKAGING}
                    >
                      {
                        ChainEventTypeLabel[
                          ChainEventType.PACKAGING
                        ]
                      }
                    </SelectItem>
                  </SelectContent>
                </Select>
              )}
            />

            {errors.eventType && (
              <p className="text-sm text-red-500">
                {errors.eventType.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Thời điểm *</Label>

            <Controller
              name="recordedAt"
              control={control}
              render={({ field }) => (
                <Input
                  type="datetime-local"
                  value={isoToLocalDateTimeInputValue(
                    field.value,
                  )}
                  onChange={(event) => {
                    const value = event.target.value;

                    if (!value) {
                      return;
                    }

                    field.onChange(
                      new Date(value).toISOString(),
                    );
                  }}
                  disabled={isSubmitting}
                />
              )}
            />

            {errors.recordedAt && (
              <p className="text-sm text-red-500">
                {errors.recordedAt.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <Label>Vị trí GPS *</Label>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => getLocation()}
                disabled={
                  locationLoading || isSubmitting
                }
              >
                {locationLoading ? (
                  <Loader2 className="mr-1 size-4 animate-spin" />
                ) : (
                  <MapPin className="mr-1 size-4" />
                )}

                {locationLoading
                  ? "Đang lấy vị trí"
                  : "Lấy vị trí"}
              </Button>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <Controller
                name="latitude"
                control={control}
                render={({ field }) => (
                  <Input
                    type="number"
                    step="any"
                    placeholder="Vĩ độ"
                    value={field.value ?? ""}
                    onChange={(event) => {
                      const value =
                        event.target.value;

                      field.onChange(
                        value === ""
                          ? 0
                          : Number(value),
                      );
                    }}
                    onBlur={field.onBlur}
                    name={field.name}
                    ref={field.ref}
                    disabled={
                      isSubmitting || locationLoading
                    }
                  />
                )}
              />

              <Controller
                name="longitude"
                control={control}
                render={({ field }) => (
                  <Input
                    type="number"
                    step="any"
                    placeholder="Kinh độ"
                    value={field.value ?? ""}
                    onChange={(event) => {
                      const value =
                        event.target.value;

                      field.onChange(
                        value === ""
                          ? 0
                          : Number(value),
                      );
                    }}
                    onBlur={field.onBlur}
                    name={field.name}
                    ref={field.ref}
                    disabled={
                      isSubmitting || locationLoading
                    }
                  />
                )}
              />
            </div>

            {(errors.latitude ||
              errors.longitude) && (
              <p className="text-sm text-red-500">
                {errors.latitude?.message ||
                  errors.longitude?.message}
              </p>
            )}
          </div>

          {eventType === ChainEventType.HARVEST && (
            <>
              <div className="space-y-2">
                <Label>Sản lượng (kg) *</Label>

                <Controller
                  name="quantity"
                  control={control}
                  render={({ field }) => (
                    <Input
                      type="number"
                      min="0"
                      step="0.01"
                      placeholder="Nhập sản lượng"
                      value={field.value ?? ""}
                      onChange={(event) => {
                        const value =
                          event.target.value;

                        field.onChange(
                          value === ""
                            ? 0
                            : Number(value),
                        );
                      }}
                      onBlur={field.onBlur}
                      name={field.name}
                      ref={field.ref}
                      disabled={isSubmitting}
                    />
                  )}
                />

                {errors.quantity && (
                  <p className="text-sm text-red-500">
                    {errors.quantity.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label>Ngày thu hoạch *</Label>

                <Controller
                  name="harvestDate"
                  control={control}
                  render={({ field }) => (
                    <Input
                      type="date"
                      {...field}
                      disabled={isSubmitting}
                    />
                  )}
                />

                {errors.harvestDate && (
                  <p className="text-sm text-red-500">
                    {errors.harvestDate.message}
                  </p>
                )}
              </div>
            </>
          )}

          {eventType ===
            ChainEventType.PACKAGING && (
            <>
              <div className="space-y-2">
                <Label>Quy cách đóng gói *</Label>

                <Controller
                  name="packagingSpecification"
                  control={control}
                  render={({ field }) => (
                    <Input
                      placeholder="Ví dụ: 10kg/bao"
                      {...field}
                      disabled={isSubmitting}
                    />
                  )}
                />

                {errors.packagingSpecification && (
                  <p className="text-sm text-red-500">
                    {
                      errors.packagingSpecification
                        .message
                    }
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label>Ngày đóng gói *</Label>

                <Controller
                  name="packagingDate"
                  control={control}
                  render={({ field }) => (
                    <Input
                      type="date"
                      {...field}
                      disabled={isSubmitting}
                    />
                  )}
                />

                {errors.packagingDate && (
                  <p className="text-sm text-red-500">
                    {errors.packagingDate.message}
                  </p>
                )}
              </div>
            </>
          )}

          <div className="space-y-2">
            <Label>
              Hình ảnh thực địa * (tối thiểu 1)
            </Label>

            <div className="flex flex-wrap items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => {
                  document
                    .getElementById(
                      "mobile-event-image-input",
                    )
                    ?.click();
                }}
                disabled={
                  isSubmitting ||
                  imageFiles.length >= MAX_IMAGES
                }
              >
                <Camera className="mr-1 size-4" />
                Chọn ảnh
              </Button>

              <span className="text-sm text-muted-foreground">
                {imageFiles.length}/{MAX_IMAGES}
              </span>

              <input
                id="mobile-event-image-input"
                type="file"
                accept="image/*"
                multiple
                className="hidden"
                onChange={handleImageChange}
                disabled={isSubmitting}
              />
            </div>

            {imagePreviews.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-2">
                {imagePreviews.map(
                  (preview, index) => (
                    <div
                      key={preview}
                      className="relative size-16 overflow-hidden rounded border"
                    >
                      <img
                        src={preview}
                        alt={`Ảnh thực địa ${index + 1}`}
                        className="size-full object-cover"
                      />

                      <button
                        type="button"
                        className="absolute -top-1 -right-1 flex size-5 items-center justify-center rounded-full bg-red-500 text-xs text-white hover:bg-red-600"
                        onClick={() =>
                          removeImage(index)
                        }
                        aria-label={`Xóa ảnh ${index + 1}`}
                      >
                        ×
                      </button>
                    </div>
                  ),
                )}
              </div>
            )}

            {errors.images && (
              <p className="text-sm text-red-500">
                {errors.images.message}
              </p>
            )}
          </div>

          <Button
            type="submit"
            className="w-full"
            disabled={
              isSubmitting || locationLoading
            }
          >
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 size-4 animate-spin" />
                Đang ghi...
              </>
            ) : (
              "Ghi sự kiện"
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
};