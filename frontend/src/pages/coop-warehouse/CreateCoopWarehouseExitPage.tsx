import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";

import { recordWarehouseExit } from "@/api/coopWarehouseApi";
import { getProductionLots } from "@/api/productionLotApi";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { LocationPicker } from "@/pages/packaging-event/components/LocationPicker";
import type { ProductionLot } from "@/types/productionLot";
import {
  recordWarehouseExitSchema,
  type RecordWarehouseExitFormValues,
} from "@/utils/validators/coopWarehouseEventSchema";

export default function CreateCoopWarehouseExitPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const preselectedLotId = searchParams.get("productionLotId") ?? "";

  const [productionLots, setProductionLots] = useState<ProductionLot[]>([]);
  const [loadingLots, setLoadingLots] = useState(true);
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
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RecordWarehouseExitFormValues>({
    resolver: zodResolver(recordWarehouseExitSchema),
    defaultValues: {
      productionLotId: preselectedLotId,
      exitTime: getCurrentDatetimeString(),
      destination: "",
      notes: "",
      latitude: undefined,
      longitude: undefined,
    },
  });

  const selectedLotId = watch("productionLotId");
  const selectedLot = productionLots.find((l) => l.id === selectedLotId);

  useEffect(() => {
    async function loadLots() {
      try {
        setLoadingLots(true);
        const res = await getProductionLots();
        if (res) {
          setProductionLots(res);
          if (preselectedLotId) {
            setValue("productionLotId", preselectedLotId);
          }
        }
      } catch (err) {
        console.error("Lỗi khi tải danh sách lô sản xuất:", err);
      } finally {
        setLoadingLots(false);
      }
    }
    loadLots();
  }, [preselectedLotId, setValue]);

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
        navigate("/production-lots");
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

            {/* Chọn lô sản xuất */}
            <div className="space-y-2">
              <Label className="font-medium text-gray-700">
                Chọn lô sản xuất <span className="text-red-500">*</span>
              </Label>
              <Select
                value={selectedLotId || undefined}
                onValueChange={(val) => setValue("productionLotId", val ?? "")}
                disabled={loadingLots}
              >
                <SelectTrigger className="w-full">
                  <SelectValue
                    placeholder={
                      loadingLots
                        ? "Đang tải danh sách lô sản xuất..."
                        : "Chọn lô sản xuất"
                    }
                  />
                </SelectTrigger>
                <SelectContent>
                  {productionLots.map((lot) => (
                    <SelectItem key={lot.id} value={lot.id}>
                      {lot.name} ({lot.code || (lot.id ? lot.id.substring(0, 8) : "")})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.productionLotId && (
                <p className="text-sm text-red-600">
                  {errors.productionLotId.message}
                </p>
              )}
            </div>

            {/* Banner trạng thái hợp lệ */}
            {selectedLot && (
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-md text-emerald-800 text-sm">
                <p className="font-semibold">Lô sản xuất sẵn sàng xuất kho</p>
                <p className="mt-0.5">
                  Lô sản xuất <span className="font-medium">{selectedLot.name}</span> sẽ được ghi nhận thời điểm rời kho và tính thời gian lưu kho.
                </p>
              </div>
            )}

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
              disabled={isSubmitting}
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
