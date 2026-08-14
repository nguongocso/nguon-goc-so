import { zodResolver } from "@hookform/resolvers/zod";
import { BrowserQRCodeReader } from "@zxing/browser";
import {
  LoaderCircle,
  MapPin,
  QrCode,
  RotateCcw,
  Send,
  Truck,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";

import { useAutoGeolocation } from "@/hooks/useAutoGeolocation";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
} from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { getShipmentByCode } from "@/api/shipmentApi";
import type { ShipmentSummary } from "@/types/shipment";
import { useProcurementEvent } from "@/hooks/useProcurementEvent";
import {
  procurementEventSchema,
  type ProcurementEventFormValues,
} from "@/utils/procurementEventSchema";

interface RecordProcurementDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialShipmentId?: string;
  onSuccess?: () => void;
}

/** Kiểm tra chuỗi có dạng UUID v4 hay không */
function isUUID(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
}

export function RecordProcurementDialog({
  open,
  onOpenChange,
  initialShipmentId,
  onSuccess,
}: RecordProcurementDialogProps) {
  const { data, isLoading, error, submit, reset } = useProcurementEvent();

  // ── State cho hộp thoại quét mã QR ──────────────────────────────────────
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const controlsRef = useRef<{ stop: () => void } | null>(null);
  const [scannerOpen, setScannerOpen] = useState(false);
  const [scannerError, setScannerError] = useState("");

  // ── State cho tra cứu shipment qua mã truy xuất ─────────────────────────
  const [isLookingUp, setIsLookingUp] = useState(false);
  const [resolvedShipment, setResolvedShipment] = useState<ShipmentSummary | null>(null);

  // ── Form với react‑hook‑form + zod ──────────────────────────────────────
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset: resetForm,
    formState: { errors },
  } = useForm<ProcurementEventFormValues>({
    resolver: zodResolver(procurementEventSchema),
    defaultValues: {
      shipmentId: initialShipmentId ?? "",
      notes: "",
    },
  });

  const notesValue = watch("notes") ?? "";
  const rawLatitude = watch("latitude");
  const rawLongitude = watch("longitude");

  // ── Cập nhật shipmentId khi initialShipmentId thay đổi ──────────────────
  useEffect(() => {
    if (initialShipmentId) {
      setValue("shipmentId", initialShipmentId, { shouldValidate: true });
    }
  }, [initialShipmentId, setValue]);

  // ── Dừng quét mã ────────────────────────────────────────────────────────
  const stopScanning = () => {
    controlsRef.current?.stop();
    controlsRef.current = null;

    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;

    setScannerOpen(false);
  };

  // ── Khởi động camera khi mở hộp thoại quét ──────────────────────────────
  useEffect(() => {
    if (!scannerOpen) return;

    let isActive = true;
    const codeReader = new BrowserQRCodeReader();

    const startScanning = async () => {
      try {
        await new Promise((resolve) => window.setTimeout(resolve, 150));

        const video = videoRef.current;

        if (!video) {
          throw new Error("Không tìm thấy vùng hiển thị camera.");
        }

        const stream = await navigator.mediaDevices.getUserMedia({
          audio: false,
          video: {
            facingMode: {
              ideal: "environment",
            },
          },
        });

        if (!isActive) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }

        streamRef.current = stream;
        video.srcObject = stream;

        const controls = await codeReader.decodeFromVideoElement(
          video,
          (result) => {
            if (!result || !isActive) return;

            const scannedText = result.getText().trim();
            stopScanning();
            void handleResolveCode(scannedText);
          },
        );

        if (!isActive) {
          controls.stop();
          return;
        }

        controlsRef.current = controls;
      } catch (scanError: unknown) {
        if (!isActive) return;

        if (
          scanError instanceof DOMException &&
          scanError.name === "NotAllowedError"
        ) {
          setScannerError(
            "Bạn chưa cho phép dùng camera. Hãy cấp quyền camera rồi thử lại.",
          );
          return;
        }

        if (
          scanError instanceof DOMException &&
          scanError.name === "NotReadableError"
        ) {
          setScannerError(
            "Camera đang được ứng dụng khác sử dụng. Hãy đóng ứng dụng đó rồi thử lại.",
          );
          return;
        }

        setScannerError(
          "Không thể mở camera. Hãy kiểm tra camera hoặc nhập mã thủ công.",
        );
      }
    };

    void startScanning();

    return () => {
      isActive = false;
      controlsRef.current?.stop();
      controlsRef.current = null;

      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    };
  }, [scannerOpen, setValue]);

  // ── Tự động lấy vị trí GPS từ trình duyệt ───────────────────────────────
  // ── Tra cứu lô hàng từ mã truy xuất hoặc UUID ──────────────────────────
  const handleResolveCode = async (code: string) => {
    const trimmed = code.trim();
    if (!trimmed) return;

    // Nếu đã là UUID, dùng trực tiếp
    if (isUUID(trimmed)) {
      setValue("shipmentId", trimmed, { shouldValidate: true });
      setResolvedShipment(null);
      toast.success("Đã nhập mã lô hàng (UUID).");
      return;
    }

    // Ngược lại, tra cứu qua API
    setIsLookingUp(true);
    try {
      const result = await getShipmentByCode(trimmed);
      setValue("shipmentId", result.id, { shouldValidate: true });
      setResolvedShipment(result);
      toast.success(`Đã tìm thấy lô hàng: ${result.name}`);
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ||
        "Không thể tra cứu lô hàng. Vui lòng thử lại.";
      toast.error(message);
      setResolvedShipment(null);
      setValue("shipmentId", "", { shouldValidate: false });
    } finally {
      setIsLookingUp(false);
    }
  };

  // ── Tự động lấy vị trí GPS từ trình duyệt ───────────────────────────────
  // Tự động lấy vị trí ngay khi mở dialog (nếu đã/được cấp quyền), và tự động
  // lấy lại ngay khi người dùng cấp quyền GPS trong lúc dialog đang mở.
  // `handleGetCurrentLocation` vẫn dùng cho nút "Lấy vị trí hiện tại" để người
  // dùng chủ động lấy lại vị trí mới nhất.
  const { locationLoading: isLocating, fetchLocation: handleGetCurrentLocation } =
    useAutoGeolocation({
      enabled: open,
      onLocation: (latitude, longitude) => {
        setValue("latitude", latitude, { shouldValidate: true });
        setValue("longitude", longitude, { shouldValidate: true });
        toast.success("Đã lấy vị trí hiện tại.");
      },
      onError: (message) => {
        const isPermissionDenied = message.toLowerCase().includes("denied");
        toast.error(
          isPermissionDenied
            ? "Bạn chưa cấp quyền truy cập vị trí. Vui lòng bật quyền vị trí rồi thử lại."
            : "Không thể lấy vị trí hiện tại. Vui lòng thử lại.",
        );
      },
    });

  // ── Xử lý submit: chuyển dữ liệu hợp lệ sang hook useProcurementEvent ──
  const onSubmit = (values: ProcurementEventFormValues) => {
    void submit({
      shipmentId: values.shipmentId,
      receivedQuantity: values.receivedQuantity,
      notes: values.notes || undefined,
      latitude: rawLatitude ? values.latitude : undefined,
      longitude: rawLongitude ? values.longitude : undefined,
    });
  };

  // ── Làm mới toàn bộ form + reset trạng thái hook ─────────────────────────
  const handleReset = () => {
    reset();
    resetForm({
      shipmentId: initialShipmentId ?? "",
      notes: "",
    });
    setScannerError("");
    setResolvedShipment(null);
  };

  // ── Đóng dialog và reset trạng thái (khi người dùng tự đóng) ────────────
  const handleUserClose = (isOpen: boolean) => {
    if (!isOpen) {
      handleReset();
    }
    onOpenChange(isOpen);
  };

  // ── Khi ghi thành công, đóng dialog sau 1.5s ────────────────────────────
  useEffect(() => {
    if (!data) return;

    const timer = window.setTimeout(() => {
      handleReset();
      onOpenChange(false);
      onSuccess?.();
    }, 1500);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  return (
    <>
      <Dialog open={open} onOpenChange={handleUserClose}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-xl">
              <Truck className="size-5 text-emerald-700" />
              Ghi sự kiện thu mua
            </DialogTitle>
            <DialogDescription>
              Ghi nhận lô hàng khi doanh nghiệp thu mua nhận hàng, bổ sung mắt
              xích cho hành trình truy xuất nguồn gốc.
            </DialogDescription>
          </DialogHeader>

          <div className="max-h-[65vh] overflow-y-auto pr-1">
            {/* Form chỉ hiển thị khi chưa ghi thành công. */}
            {!data && (
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
                {/* Lỗi từ backend */}
                {error && (
                  <div
                    role="alert"
                    className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
                  >
                    {error}
                  </div>
                )}

                {/* Mã lô hàng */}
                <div className="space-y-2">
                  <Label htmlFor="shipmentId">Mã lô hàng *</Label>
                  <div className="flex flex-col gap-2 sm:flex-row">
                    <Input
                      id="shipmentId"
                      placeholder="550e8400-e29b-41d4-a716-446655440000"
                      autoComplete="off"
                      disabled={isLoading}
                      {...register("shipmentId")}
                    />
                    <Button
                      type="button"
                      variant="outline"
                      disabled={isLoading}
                      onClick={() => {
                        setScannerError("");
                        setScannerOpen(true);
                      }}
                      className="shrink-0"
                    >
                      <QrCode className="mr-2 size-4" />
                      Quét mã QR
                    </Button>
                  </div>
                  {isLookingUp && (
                    <p className="flex items-center gap-1.5 text-sm text-emerald-700">
                      <LoaderCircle className="size-3.5 animate-spin" />
                      Đang tra cứu lô hàng...
                    </p>
                  )}
                  {resolvedShipment && (
                    <div className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
                      <p>
                        <span className="font-semibold">Lô hàng:</span>{" "}
                        {resolvedShipment.name}
                      </p>
                      {resolvedShipment.productionLotName && (
                        <p className="text-xs text-emerald-600">
                          Lô sản xuất: {resolvedShipment.productionLotName}
                        </p>
                      )}
                      {resolvedShipment.totalQuantity != null && (
                        <p className="text-xs text-emerald-600">
                          Sản lượng:{" "}
                          {resolvedShipment.totalQuantity.toLocaleString(
                            "vi-VN",
                          )}
                        </p>
                      )}
                    </div>
                  )}
                  {errors.shipmentId ? (
                    <p className="text-sm text-destructive">
                      {errors.shipmentId.message}
                    </p>
                  ) : (
                    !isLookingUp &&
                    !resolvedShipment && (
                      <p className="text-xs text-muted-foreground">
                        Quét QR bằng camera hoặc nhập mã lô hàng thủ công (mã
                        truy xuất hoặc UUID).
                      </p>
                    )
                  )}
                </div>

                {/* Số lượng thực nhận */}
                <div className="space-y-2">
                  <Label htmlFor="receivedQuantity">
                    Số lượng thực nhận *
                  </Label>
                  <Input
                    id="receivedQuantity"
                    type="number"
                    min={0}
                    step="any"
                    placeholder="Ví dụ: 1000"
                    disabled={isLoading}
                    {...register("receivedQuantity")}
                  />
                  {errors.receivedQuantity && (
                    <p className="text-sm text-destructive">
                      {errors.receivedQuantity.message}
                    </p>
                  )}
                </div>

                {/* Vị trí GPS */}
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <Label className="flex items-center gap-1.5">
                      <MapPin className="size-4 text-emerald-700" />
                      Vị trí nhận hàng
                    </Label>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={isLoading || isLocating}
                      onClick={() => handleGetCurrentLocation()}
                    >
                      <MapPin className="mr-1.5 size-4" />
                      {isLocating ? "Đang lấy vị trí..." : "Lấy vị trí hiện tại"}
                    </Button>
                  </div>

                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="space-y-2">
                      <Label
                        htmlFor="latitude"
                        className="text-xs text-slate-500"
                      >
                        Vĩ độ
                      </Label>
                      <Input
                        id="latitude"
                        type="number"
                        step="any"
                        placeholder="Ví dụ: 21.0278"
                        disabled={isLoading}
                        {...register("latitude")}
                      />
                      {errors.latitude && (
                        <p className="text-sm text-destructive">
                          {errors.latitude.message}
                        </p>
                      )}
                    </div>
                    <div className="space-y-2">
                      <Label
                        htmlFor="longitude"
                        className="text-xs text-slate-500"
                      >
                        Kinh độ
                      </Label>
                      <Input
                        id="longitude"
                        type="number"
                        step="any"
                        placeholder="Ví dụ: 105.8342"
                        disabled={isLoading}
                        {...register("longitude")}
                      />
                      {errors.longitude && (
                        <p className="text-sm text-destructive">
                          {errors.longitude.message}
                        </p>
                      )}
                    </div>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Tọa độ không bắt buộc. Nếu bạn cho phép trình duyệt, hệ
                    thống sẽ tự động điền vị trí hiện tại.
                  </p>
                </div>

                {/* Ghi chú */}
                <div className="space-y-2">
                  <Label htmlFor="notes">Ghi chú</Label>
                  <Textarea
                    id="notes"
                    rows={3}
                    placeholder="Mô tả tình trạng nhận hàng, chất lượng, bao bì..."
                    disabled={isLoading}
                    {...register("notes")}
                  />
                  <div className="flex items-center justify-between">
                    <p className="text-xs text-muted-foreground">
                      Không bắt buộc, tối đa 500 ký tự.
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {notesValue.length}/500
                    </p>
                  </div>
                  {errors.notes && (
                    <p className="text-sm text-destructive">
                      {errors.notes.message}
                    </p>
                  )}
                </div>

                {/* Nút hành động */}
                <div className="flex flex-col gap-2 pt-1 sm:flex-row sm:justify-end">
                  <Button
                    type="button"
                    variant="outline"
                    disabled={isLoading}
                    onClick={handleReset}
                    className="sm:order-first"
                  >
                    <RotateCcw className="mr-2 size-4" />
                    Làm mới
                  </Button>
                  <Button
                    type="submit"
                    disabled={isLoading}
                    className="flex-1 sm:flex-none"
                  >
                    {isLoading ? (
                      <>
                        <LoaderCircle className="mr-2 size-4 animate-spin" />
                        Đang ghi nhận...
                      </>
                    ) : (
                      <>
                        <Send className="mr-2 size-4" />
                        Ghi nhận
                      </>
                    )}
                  </Button>
                </div>
              </form>
            )}

            {/* Kết quả sau khi ghi thành công */}
            {data && (
              <Card className="border-emerald-200 bg-emerald-50">
                <CardContent className="pt-6">
                  <p className="font-semibold text-emerald-800">
                    Ghi nhận thành công!
                  </p>
                  <div className="mt-3 space-y-1.5 rounded-lg bg-white/60 p-3 text-sm text-emerald-800">
                    <p>
                      Mã sự kiện:{" "}
                      <span className="font-mono text-xs">{data.id}</span>
                    </p>
                    <p>
                      Lô hàng:{" "}
                      <span className="font-semibold">
                        {data.eventData.shipmentName}
                      </span>
                    </p>
                    <p>
                      Số lượng thực nhận:{" "}
                      <span className="font-semibold">
                        {data.eventData.receivedQuantity.toLocaleString(
                          "vi-VN",
                        )}
                      </span>
                    </p>
                    <p>
                      Người ghi:{" "}
                      <span className="font-semibold">
                        {data.recordedByName}
                      </span>
                    </p>
                    <p>
                      Thời gian:{" "}
                      <span className="font-semibold">
                        {new Date(data.recordedAt).toLocaleString("vi-VN")}
                      </span>
                    </p>
                  </div>
                </CardContent>
              </Card>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* Hộp thoại quét mã QR bằng camera */}
      <Dialog
        open={scannerOpen}
        onOpenChange={(open) => {
          if (open) {
            setScannerOpen(true);
          } else {
            stopScanning();
          }
        }}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <QrCode className="size-5" />
              Quét mã lô hàng
            </DialogTitle>
            <DialogDescription>
              Đưa mã QR trên bao bì vào trong khung hình để hệ thống tự điền mã
              lô hàng.
            </DialogDescription>
          </DialogHeader>

          <div className="overflow-hidden rounded-lg bg-black">
            <video
              ref={videoRef}
              className="aspect-video w-full object-cover"
              muted
              playsInline
            />
          </div>

          {scannerError && (
            <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {scannerError}
            </div>
          )}

          <Button type="button" variant="outline" onClick={stopScanning}>
            Hủy quét
          </Button>
        </DialogContent>
      </Dialog>
    </>
  );
}