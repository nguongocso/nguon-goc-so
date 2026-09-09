import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";

import { recordWarehouseEntry } from "@/api/coopWarehouseApi";
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
  recordWarehouseEntrySchema,
  type RecordWarehouseEntryFormValues,
} from "@/utils/validators/coopWarehouseEventSchema";

export default function CreateCoopWarehouseEntryPage() {
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
  } = useForm<RecordWarehouseEntryFormValues>({
    resolver: zodResolver(recordWarehouseEntrySchema),
    defaultValues: {
      productionLotId: preselectedLotId,
      entryTime: getCurrentDatetimeString(),
      warehouseName: "",
      storageCondition: "",
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
        const res = await getProductionLots({ status: "PACKAGED" });
        if (res && res.data) {
          setProductionLots(res.data.content || []);
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

  const onSubmit = async (values: RecordWarehouseEntryFormValues) => {
    try {
      setServerError(null);
      const res = await recordWarehouseEntry(values);
      if (res && res.success) {
        toast.success("Ghi sự kiện nhập kho HTX thành công");
        navigate("/production-lots");
      } else {
        const msg = res?.message || "Không thể ghi sự kiện nhập kho HTX";
        setServerError(msg);
        toast.error(msg);
      }
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || "Có lỗi xảy ra khi ghi sự kiện nhập kho HTX";
      setServerError(msg);
      toast.error(msg);
    }
  };

  return (
    <div className="container mx-auto p-4 max-w-4xl">
      <Card className="border-emerald-200 shadow-sm">
        <CardHeader className="bg-emerald-50/50 rounded-t-lg border-b border-emerald-100">
          <CardTitle className="text-xl text-emerald-900 font-semibold">
            Ghi sự kiện nhập kho HTX
          </CardTitle>
          <p className="text-sm text-emerald-700 mt-1">
            Ghi nhận thông tin thời điểm lô hàng đóng gói vừa vào kho lưu trữ của hợp tác xã.
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
                Chọn lô sản xuất (Đã đóng gói) <span className="text-red-500">*</span>
              </Label>
              <Select
                value={selectedLotId}
                onValueChange={(val) => setValue("productionLotId", val)}
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
                      {lot.name} ({lot.code || lot.id.substring(0, 8)})
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
                <p className="font-semibold">Lô sản xuất hợp lệ</p>
                <p className="mt-0.5">
                  Lô sản xuất <span className="font-medium">{selectedLot.name}</span> đã hoàn tất đóng gói và sẵn sàng ghi nhận nhập kho HTX.
                </p>
              </div>
            )}

            {/* Tên kho & Điều kiện bảo quản */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="warehouseName" className="font-medium text-gray-700">
                  Tên kho lưu trữ <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="warehouseName"
                  placeholder="VD: Kho lạnh HTX Nông nghiệp Số 1"
                  {...register("warehouseName")}
                />
                {errors.warehouseName && (
                  <p className="text-sm text-red-600">
                    {errors.warehouseName.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="entryTime" className="font-medium text-gray-700">
                  Thời điểm nhập kho <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="entryTime"
                  type="datetime-local"
                  {...register("entryTime")}
                />
                {errors.entryTime && (
                  <p className="text-sm text-red-600">
                    {errors.entryTime.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="storageCondition" className="font-medium text-gray-700">
                Điều kiện bảo quản
              </Label>
              <Input
                id="storageCondition"
                placeholder="VD: Nhiệt độ 4°C - 8°C, Độ ẩm 85%"
                {...register("storageCondition")}
              />
              {errors.storageCondition && (
                <p className="text-sm text-red-600">
                  {errors.storageCondition.message}
                </p>
              )}
            </div>

            {/* Chọn vị trí bản đồ */}
            <div className="space-y-2">
              <Label className="font-medium text-gray-700">
                Vị trí kho (Click trên bản đồ)
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
                placeholder="Nhập ghi chú bổ sung về lô hàng khi nhập kho..."
                {...register("notes")}
              />
              {errors.notes && (
                <p className="text-sm text-red-600">{errors.notes.message}</p>
              )}
            </div>

            {/* Hộp lưu ý chân trang */}
            <div className="p-4 bg-amber-50 border border-amber-200 rounded-md text-amber-900 text-sm space-y-1">
              <p className="font-semibold">Sau khi ghi nhận thành công:</p>
              <p>• Lô sản xuất được ghi nhận thời điểm bắt đầu lưu kho tại HTX.</p>
              <p>• Thời gian lưu kho sẽ tự động được tính toán khi ghi sự kiện xuất kho.</p>
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
              {isSubmitting ? "Đang xử lý..." : "Ghi sự kiện nhập kho"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
