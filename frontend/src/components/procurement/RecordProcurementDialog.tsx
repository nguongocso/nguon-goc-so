import { zodResolver } from "@hookform/resolvers/zod";
import { BrowserQRCodeReader } from "@zxing/browser";
import {
  CheckCircle2,
  LoaderCircle,
  MapPin,
  Package,
  QrCode,
  RotateCcw,
  Send,
  Truck,
  X,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";

import { useAutoGeolocation } from "@/hooks/useAutoGeolocation";
import { LocationPicker } from "@/pages/packaging-event/components/LocationPicker";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
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
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
    value,
  );
}

export function RecordProcurementDialog({
  open,
  onOpenChange,
  initialShipmentId,
  onSuccess,
}: RecordProcurementDialogProps) {
  const { data, isLoading, error, submit, reset } = useProcurementEvent();

  // ── State cho quét mã QR (inline) ──────────────────────────────────────
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const controlsRef = useRef<{ stop: () => void } | null>(null);
  const [scannerActive, setScannerActive] = useState(false);
  const [scannerError, setScannerError] = useState("");

  // ── State cho tra cứu shipment ─────────────────────────────────────────
  const [isLookingUp, setIsLookingUp] = useState(false);
  const [resolvedShipment, setResolvedShipment] =
    useState<ShipmentSummary | null>(null);

  // ── Form ────────────────────────────────────────────────────────────────
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

  // ── Cập nhật shipmentId khi initialShipmentId thay đổi ────────────────
  useEffect(() => {
    if (initialShipmentId) {
      setValue("shipmentId", initialShipmentId, { shouldValidate: true });
    }
  }, [initialShipmentId, setValue]);

  // ── Dừng quét mã ──────────────────────────────────────────────────────
  const stopScanning = () => {
    controlsRef.current?.stop();
    controlsRef.current = null;
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    setScannerActive(false);
  };

  // ── Khởi động camera ──────────────────────────────────────────────────
  useEffect(() => {
    if (!scannerActive) return;

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
            facingMode: { ideal: "environment" },
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
  }, [scannerActive, setValue]);

  // ── Tra cứu lô hàng ──────────────────────────────────────────────────
  const handleResolveCode = async (code: string) => {
    const trimmed = code.trim();
    if (!trimmed) return;

    if (isUUID(trimmed)) {
      setValue("shipmentId", trimmed, { shouldValidate: true });
      setResolvedShipment(null);
      toast.success("Đã nhập mã lô hàng (UUID).");
      return;
    }

    setIsLookingUp(true);
    try {
      const result = await getShipmentByCode(trimmed);
      setValue("shipmentId", result.id, { shouldValidate: true });
      setResolvedShipment(result);
      toast.success(`Đã tìm thấy lô hàng: ${result.name}`);
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Không thể tra cứu lô hàng. Vui lòng thử lại.";
      toast.error(message);
      setResolvedShipment(null);
      setValue("shipmentId", "", { shouldValidate: false });
    } finally {
      setIsLookingUp(false);
    }
  };

  // ── Tự động lấy vị trí GPS ────────────────────────────────────────────
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

  // ── Submit ────────────────────────────────────────────────────────────
  const onSubmit = (values: ProcurementEventFormValues) => {
    void submit({
      shipmentId: values.shipmentId,
      receivedQuantity: values.receivedQuantity,
      notes: values.notes || undefined,
      latitude: rawLatitude ? values.latitude : undefined,
      longitude: rawLongitude ? values.longitude : undefined,
    });
  };

  // ── Reset ─────────────────────────────────────────────────────────────
  const handleReset = () => {
    reset();
    resetForm({
      shipmentId: initialShipmentId ?? "",
      notes: "",
    });
    setScannerError("");
    setResolvedShipment(null);
  };

  // ── Đóng dialog ──────────────────────────────────────────────────────
  const handleUserClose = (isOpen: boolean) => {
    if (!isOpen) {
      stopScanning();
      handleReset();
    }
    onOpenChange(isOpen);
  };

  // ── Khi ghi thành công ──────────────────────────────────────────────
  useEffect(() => {
    if (!data) return;
    onSuccess?.();
  }, [data, onSuccess]);

  return (
    <Dialog open={open} onOpenChange={handleUserClose}>
      <DialogContent className="sm:max-w-3xl max-h-[90vh] overflow-y-auto">
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

        {/* ─── SUCCESS STATE ─── */}
        {data ? (
          <div className="animate-in fade-in slide-in-from-bottom-4 duration-300">
            <Card className="border-emerald-200 bg-emerald-50">
              <CardContent className="pt-6 text-center">
                <div className="mx-auto mb-4 flex size-16 items-center justify-center rounded-full bg-emerald-100">
                  <CheckCircle2 className="size-8 text-emerald-600" />
                </div>
                <p className="text-lg font-bold text-emerald-800">
                  Ghi nhận thành công! 🎉
                </p>
                <p className="text-sm text-emerald-700">
                  Sự kiện thu mua đã được lưu vào hệ thống.
                </p>
                <div className="mt-4 grid grid-cols-2 gap-2 text-left text-sm">
                  <div className="rounded bg-white/60 p-2">
                    <p className="text-xs text-muted-foreground">Mã sự kiện</p>
                    <p className="font-mono text-xs">
                      {data.id.slice(0, 8)}...
                    </p>
                  </div>
                  <div className="rounded bg-white/60 p-2">
                    <p className="text-xs text-muted-foreground">Lô hàng</p>
                    <p className="font-medium">{data.eventData.shipmentName}</p>
                  </div>
                  <div className="rounded bg-white/60 p-2">
                    <p className="text-xs text-muted-foreground">Số lượng</p>
                    <p className="font-medium">
                      {data.eventData.receivedQuantity.toLocaleString("vi-VN")}
                    </p>
                  </div>
                  <div className="rounded bg-white/60 p-2">
                    <p className="text-xs text-muted-foreground">Người ghi</p>
                    <p className="font-medium">{data.recordedByName}</p>
                  </div>
                </div>
                <div className="mt-4 flex flex-col gap-2 sm:flex-row sm:justify-center">
                  <Button variant="outline" onClick={handleReset}>
                    <RotateCcw className="mr-2 size-4" />
                    Ghi tiếp
                  </Button>
                  <Button onClick={() => onOpenChange(false)}>Đóng</Button>
                </div>
              </CardContent>
            </Card>
          </div>
        ) : (
          /* ─── FORM ─── */
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

            {/* ── Grid 2 cột ── */}
            <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
              {/* Cột trái */}
              <div className="space-y-4">
                {/* Mã lô hàng */}
                <div className="space-y-2">
                  <Label
                    htmlFor="shipmentId"
                    className="flex items-center gap-1"
                  >
                    Mã lô hàng <span className="text-red-500">*</span>
                  </Label>
                  <div className="flex flex-col gap-2">
                    <div className="flex gap-2">
                      <Input
                        id="shipmentId"
                        placeholder="550e8400-e29b-41d4-a716-446655440000"
                        autoComplete="off"
                        disabled={isLoading}
                        className="flex-1"
                        {...register("shipmentId")}
                      />
                      <Button
                        type="button"
                        variant={scannerActive ? "destructive" : "outline"}
                        disabled={isLoading}
                        onClick={() => {
                          if (scannerActive) {
                            stopScanning();
                          } else {
                            setScannerError("");
                            setScannerActive(true);
                          }
                        }}
                        className="shrink-0"
                      >
                        {scannerActive ? (
                          <X className="mr-1 size-4" />
                        ) : (
                          <QrCode className="mr-1 size-4" />
                        )}
                        {scannerActive ? "Đóng" : "Quét QR"}
                      </Button>
                    </div>

                    {/* Camera inline */}
                    {scannerActive && (
                      <div className="relative overflow-hidden rounded-lg bg-black">
                        <video
                          ref={videoRef}
                          className="aspect-video w-full object-cover"
                          muted
                          playsInline
                        />
                        {scannerError && (
                          <div className="absolute inset-x-0 bottom-0 bg-black/70 px-3 py-2 text-center text-sm text-white">
                            {scannerError}
                          </div>
                        )}
                      </div>
                    )}

                    {isLookingUp && (
                      <p className="flex items-center gap-1.5 text-sm text-emerald-700">
                        <LoaderCircle className="size-3.5 animate-spin" />
                        Đang tra cứu lô hàng...
                      </p>
                    )}

                    {errors.shipmentId ? (
                      <p className="text-sm text-destructive">
                        {errors.shipmentId.message}
                      </p>
                    ) : (
                      !isLookingUp &&
                      !resolvedShipment && (
                        <p className="text-xs text-muted-foreground">
                          Quét QR bằng camera hoặc nhập mã truy xuất / UUID.
                        </p>
                      )
                    )}
                  </div>
                </div>

                {/* Số lượng thực nhận */}
                <div className="space-y-2">
                  <Label
                    htmlFor="receivedQuantity"
                    className="flex items-center gap-1"
                  >
                    Số lượng thực nhận <span className="text-red-500">*</span>
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
              </div>

              {/* Cột phải */}
              <div className="space-y-4">
                {/* Vị trí nhận hàng */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <Label className="flex items-center gap-1.5 text-sm font-medium">
                      <MapPin className="size-4 text-emerald-600" />
                      Vị trí nhận hàng
                    </Label>
                    {rawLatitude && rawLongitude && (
                      <Badge
                        variant="outline"
                        className="text-xs text-emerald-700"
                      >
                        Đã chọn
                      </Badge>
                    )}
                  </div>
                  <div className="rounded-lg border border-emerald-100 bg-emerald-50/20 p-2">
                    {/* ✅ Bỏ className trên LocationPicker, thêm wrapper div */}
                    <div className="rounded-md overflow-hidden">
                      <LocationPicker
                        onLocationSelect={(lat, lng) => {
                          setValue("latitude", lat, { shouldValidate: true });
                          setValue("longitude", lng, { shouldValidate: true });
                        }}
                        initialPosition={
                          rawLatitude && rawLongitude
                            ? {
                                lat: Number(rawLatitude),
                                lng: Number(rawLongitude),
                              }
                            : undefined
                        }
                        height="200px"
                      />
                    </div>
                  </div>
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
              </div>
            </div>

            {/* ── Thông tin lô hàng đã tra cứu (full width) ── */}
            {resolvedShipment && (
              <Card className="border-emerald-200 bg-emerald-50/50 shadow-sm">
                <CardContent className="p-3">
                  <div className="flex items-start gap-3">
                    <Package className="mt-0.5 size-5 text-emerald-600" />
                    <div className="flex-1">
                      <p className="font-semibold text-emerald-800">
                        {resolvedShipment.name}
                      </p>
                      <div className="mt-1 grid grid-cols-2 gap-1 text-xs text-emerald-700">
                        <span>
                          Lô sản xuất:{" "}
                          {resolvedShipment.productionLotName || "—"}
                        </span>
                        <span>
                          Sản lượng:{" "}
                          {resolvedShipment.totalQuantity?.toLocaleString(
                            "vi-VN",
                          ) || "—"}
                        </span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* ── Nút hành động ── */}
            <div className="flex flex-col-reverse gap-2 pt-1 sm:flex-row sm:justify-end">
              <Button
                type="button"
                variant="outline"
                disabled={isLoading}
                onClick={handleReset}
              >
                <RotateCcw className="mr-2 size-4" />
                Làm mới
              </Button>
              <Button
                type="submit"
                disabled={isLoading || !watch("shipmentId")}
                className="min-w-32"
              >
                {isLoading ? (
                  <>
                    <LoaderCircle className="mr-2 size-4 animate-spin" />
                    Đang ghi...
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
      </DialogContent>
    </Dialog>
  );
}
