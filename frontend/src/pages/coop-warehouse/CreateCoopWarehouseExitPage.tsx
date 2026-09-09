import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";

import { recordWarehouseExit } from "@/api/coopWarehouseApi";
import { getShipmentById } from "@/api/shipmentApi";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { LocationPicker } from "@/pages/packaging-event/components/LocationPicker";
import type { Shipment } from "@/types/shipment";
import {
  recordWarehouseExitSchema,
  type RecordWarehouseExitFormValues,
} from "@/utils/validators/coopWarehouseEventSchema";

export default function CreateCoopWarehouseExitPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const preselectedShipmentId = searchParams.get("shipmentId") ?? "";

  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [loadingShipment, setLoadingShipment] = useState(true);
  const [serverError, setServerError] = useState<string | null>(null);

  const getCurrentDatetimeString = () => {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  };

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<RecordWarehouseExitFormValues>({
    resolver: zodResolver(recordWarehouseExitSchema),
    defaultValues: {
      shipmentId: preselectedShipmentId,
      exitTime: getCurrentDatetimeString(),
      destination: "",
      notes: "",
      latitude: undefined,
      longitude: undefined,
    },
  });

  useEffect(() => {
    async function loadShipment() {
      if (!preselectedShipmentId) {
        setLoadingShipment(false);
        return;
      }
      try {
        setLoadingShipment(true);
        const res = await getShipmentById(preselectedShipmentId);
        if (res) {
          setShipment(res);
          setValue("shipmentId", res.id);
        }
      } catch (err) {
        console.error("Lỗi khi tải thông tin lô hàng:", err);
        setServerError("Không tìm thấy thông tin lô hàng");
      } finally {
        setLoadingShipment(false);
      }
    }
    loadShipment();
  }, [preselectedShipmentId, setValue]);

  const onSubmit = async (values: RecordWarehouseExitFormValues) => {
    try {
      setServerError(null);
      const res = await recordWarehouseExit(values);
      if (res && res.success) {
        if (res.data?.isStorageExceeded) {
          toast.warning(res.data.warningMessage || "Cảnh báo: Thời gian lưu kho vượt ngưỡng bảo quản");
        } else {
          toast.success("Ghi sự kiện xuất kho HTX thành công");
        }
        navigate(-1);
      } else {
        const msg = res?.message || "Không thể ghi sự kiện xuất kho HTX";
        setServerError(msg);
        toast.error(msg);
      }
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || "Có lỗi xảy ra khi ghi sự kiện xuất kho HTX";
      setServerError(msg);
      toast.error(msg);
    }
  };

  return (
    <div className="container mx-auto p-4 max-w-4xl">
      <Card className="border-emerald-200 shadow-sm">
        <CardHeader className="bg-emerald-50/50 rounded-t-lg border-b border-emerald-100">
          <CardTitle className="text-xl text-emerald-900 font-semibold">
            Ghi sự kiện xuất kho HTX
          </CardTitle>
          <p className="text-sm text-emerald-700 mt-1">
            Ghi nhận thời điểm lô hàng rời kho hợp tác xã để chuyển sang vận chuyển hoặc thu mua.
          </p>
        </CardHeader>

        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-6 pt-6">
            {serverError && (
              <div className="p-4 bg-red-50 border border-red-200 text-red-800 rounded-md text-sm">
                {serverError}
              </div>
            )}

            {/* Thông tin lô hàng */}
            <div className="p-4 bg-emerald-50/70 border border-emerald-200 rounded-md space-y-1.5">
              <Label className="font-semibold text-emerald-900 text-base">
                Lô hàng được chọn
              </Label>
              {loadingShipment ? (
                <p className="text-sm text-emerald-700">Đang tải thông tin lô hàng...</p>
              ) : shipment ? (
                <div className="text-sm text-emerald-900 space-y-1">
                  <p>
                    Tên lô hàng: <span className="font-semibold">{shipment.name}</span>
                  </p>
                  <p className="text-emerald-800 text-xs">
                    Mã ID: {shipment.id} | Số lượng: {shipment.totalQuantity} | Quy cách: {shipment.packagingInfo || "N/A"}
                  </p>
                </div>
              ) : (
                <p className="text-sm text-red-600">Chưa chọn lô hàng hợp lệ.</p>
              )}
            </div>

            {/* Thời điểm xuất kho & Nơi chuyển đến */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="exitTime" className="font-medium text-gray-700">
                  Thời điểm xuất kho <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="exitTime"
                  type="datetime-local"
                  {...register("exitTime")}
                />
                {errors.exitTime && (
                  <p className="text-sm text-red-600">
                    {errors.exitTime.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="destination" className="font-medium text-gray-700">
                  Nơi chuyển đến / Đơn vị tiếp nhận
                </Label>
                <Input
                  id="destination"
                  placeholder="VD: Xe vận chuyển Công ty Thu Mua Chè Việt"
                  {...register("destination")}
                />
                {errors.destination && (
                  <p className="text-sm text-red-600">
                    {errors.destination.message}
                  </p>
                )}
              </div>
            </div>

            {/* Chọn vị trí bản đồ */}
            <div className="space-y-2">
              <Label className="font-medium text-gray-700">
                Vị trí xuất kho (Click trên bản đồ)
              </Label>
              <div className="rounded-md border border-gray-200 overflow-hidden">
                <LocationPicker
                  onLocationSelect={(lat, lng) => {
                    setValue("latitude", lat);
                    setValue("longitude", lng);
                  }}
                  height="260px"
                />
              </div>
            </div>

            {/* Ghi chú */}
            <div className="space-y-2">
              <Label htmlFor="notes" className="font-medium text-gray-700">
                Ghi chú thêm
              </Label>
              <Textarea
                id="notes"
                rows={3}
                placeholder="Nhập ghi chú bổ sung khi xuất kho..."
                {...register("notes")}
              />
              {errors.notes && (
                <p className="text-sm text-red-600">{errors.notes.message}</p>
              )}
            </div>

            {/* Hộp lưu ý chân trang */}
            <div className="p-4 bg-amber-50 border border-amber-200 rounded-md text-amber-900 text-sm space-y-1">
              <p className="font-semibold">Sau khi ghi nhận thành công:</p>
              <p>• Hệ thống tự động tính thời gian lưu kho giữa thời điểm nhập và xuất.</p>
              <p>• Cảnh báo sẽ được phát ra nếu thời gian lưu kho vượt quá ngưỡng bảo quản của loại nông sản.</p>
              <p>• Sự kiện đã ghi không bị sửa trực tiếp; sai sót phải được đính chính.</p>
            </div>
          </CardContent>

          <CardFooter className="flex justify-end gap-3 border-t bg-gray-50/50 p-4 rounded-b-lg">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate(-1)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting || !shipment}
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
            >
              {isSubmitting ? "Đang xử lý..." : "Ghi sự kiện xuất kho"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
