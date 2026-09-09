import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import {
  LogIn,
  AlertCircle,
  CheckCircle2,
  Package,
  Building2,
  Calendar,
  MapPin,
  FileText,
} from "lucide-react";

import {
  getShipmentWarehouseStatus,
  recordWarehouseEntry,
} from "@/api/coopWarehouseApi";
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
  recordWarehouseEntrySchema,
  type RecordWarehouseEntryFormValues,
} from "@/utils/validators/coopWarehouseEventSchema";

interface SelectedShipmentStatus {
  shipment: Shipment;
  warehouseStatus: "IN_WAREHOUSE" | "NOT_IN_WAREHOUSE";
}

export default function CreateCoopWarehouseEntryPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Đọc danh sách shipmentIds từ query string (ví dụ: ?shipmentIds=id1,id2 hoặc ?shipmentId=id1)
  const queryParam = searchParams.get("shipmentIds") || searchParams.get("shipmentId") || "";
  const initialShipmentIds = queryParam ? queryParam.split(",").filter(Boolean) : [];

  const [selectedShipments, setSelectedShipments] = useState<SelectedShipmentStatus[]>([]);
  const [loadingShipments, setLoadingShipments] = useState(true);
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
  } = useForm<RecordWarehouseEntryFormValues>({
    resolver: zodResolver(recordWarehouseEntrySchema),
    defaultValues: {
      shipmentId: initialShipmentIds[0] || "",
      entryTime: getCurrentDatetimeString(),
      warehouseName: "",
      storageCondition: "",
      notes: "",
      latitude: undefined,
      longitude: undefined,
    },
  });

  // Tải thông tin lô hàng và kiểm tra trạng thái ngay lập tức khi chọn lô hàng
  useEffect(() => {
    async function loadAndValidateShipments() {
      if (initialShipmentIds.length === 0) {
        setLoadingShipments(false);
        return;
      }
      try {
        setLoadingShipments(true);
        setServerError(null);

        const items: SelectedShipmentStatus[] = [];
        for (const id of initialShipmentIds) {
          const [shipmentData, status] = await Promise.all([
            getShipmentById(id),
            getShipmentWarehouseStatus(id),
          ]);
          if (shipmentData) {
            items.push({
              shipment: shipmentData,
              warehouseStatus: status,
            });
          }
        }

        setSelectedShipments(items);
        if (items.length > 0) {
          setValue("shipmentId", items[0].shipment.id);
        }
      } catch (err) {
        console.error("Lỗi khi kiểm tra thông tin lô hàng:", err);
        setServerError("Không thể tải thông tin hoặc kiểm tra trạng thái lô hàng.");
      } finally {
        setLoadingShipments(false);
      }
    }
    loadAndValidateShipments();
  }, [queryParam, setValue]);

  // Kiểm tra có lô hàng nào vi phạm TC-04 (đã trong kho) hay không
  const invalidShipments = selectedShipments.filter(
    (item) => item.warehouseStatus === "IN_WAREHOUSE"
  );
  const hasValidationError = invalidShipments.length > 0;

  const onSubmit = async (values: RecordWarehouseEntryFormValues) => {
    if (selectedShipments.length === 0) {
      toast.error("Vui lòng chọn ít nhất một lô hàng hợp lệ.");
      return;
    }

    if (hasValidationError) {
      toast.error("Vui lòng loại bỏ các lô hàng đang ở trong kho trước khi nhập kho mới.");
      return;
    }

    try {
      setServerError(null);
      let successCount = 0;
      let failMessage = "";

      for (const item of selectedShipments) {
        const payload = {
          ...values,
          shipmentId: item.shipment.id,
        };
        const res = await recordWarehouseEntry(payload);
        if (res && res.success) {
          successCount++;
        } else {
          failMessage = res?.message || `Lỗi khi ghi sự kiện cho lô ${item.shipment.name}`;
          break;
        }
      }

      if (successCount === selectedShipments.length) {
        toast.success(`Ghi sự kiện nhập kho HTX thành công cho ${successCount} lô hàng!`);
        navigate(-1);
      } else {
        setServerError(failMessage || "Không thể ghi nhận nhập kho cho tất cả lô hàng.");
        toast.error(failMessage || "Có lỗi xảy ra khi ghi sự kiện nhập kho HTX.");
      }
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || "Có lỗi xảy ra khi ghi sự kiện nhập kho HTX.";
      setServerError(msg);
      toast.error(msg);
    }
  };

  return (
    <div className="container mx-auto p-4 max-w-4xl">
      <Card className="border-emerald-200 shadow-sm">
        <CardHeader className="bg-emerald-50/50 rounded-t-lg border-b border-emerald-100">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-emerald-100 text-emerald-800 rounded-lg">
              <LogIn className="h-5 w-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-emerald-900 font-semibold">
                Ghi sự kiện nhập kho HTX
              </CardTitle>
              <p className="text-sm text-emerald-700 mt-0.5">
                Ghi nhận thời điểm lô hàng rời xưởng đóng gói và nhập vào kho lưu trữ hợp tác xã.
              </p>
            </div>
          </div>
        </CardHeader>

        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-6 pt-6">
            {serverError && (
              <div className="p-4 bg-red-50 border border-red-200 text-red-800 rounded-md text-sm flex items-start gap-2">
                <AlertCircle className="h-5 w-5 text-red-600 shrink-0 mt-0.5" />
                <div>{serverError}</div>
              </div>
            )}

            {/* Thẻ danh sách lô hàng đã chọn */}
            <div className="space-y-3">
              <Label className="font-semibold text-gray-800 text-sm flex items-center gap-2">
                <Package className="h-4 w-4 text-emerald-600" />
                Danh sách lô hàng được chọn ({selectedShipments.length})
              </Label>

              {loadingShipments ? (
                <div className="p-4 bg-slate-50 border rounded-md text-sm text-slate-600 animate-pulse">
                  Đang tải và kiểm tra trạng thái lô hàng...
                </div>
              ) : selectedShipments.length === 0 ? (
                <div className="p-4 bg-amber-50 border border-amber-200 rounded-md text-amber-900 text-sm">
                  Chưa có lô hàng nào được chọn. Vui lòng quay lại danh sách lô hàng và chọn ít nhất 1 lô.
                </div>
              ) : (
                <div className="space-y-2">
                  {selectedShipments.map(({ shipment, warehouseStatus }) => {
                    const isInWarehouse = warehouseStatus === "IN_WAREHOUSE";
                    return (
                      <div
                        key={shipment.id}
                        className={`p-3.5 border rounded-lg flex items-center justify-between text-sm ${
                          isInWarehouse
                            ? "bg-red-50/80 border-red-200 text-red-900"
                            : "bg-emerald-50/50 border-emerald-200 text-emerald-900"
                        }`}
                      >
                        <div className="space-y-1">
                          <p className="font-semibold flex items-center gap-2">
                            {shipment.name}
                            <span className="text-xs font-normal text-slate-500">
                              (Mã: {shipment.id.substring(0, 8)}...)
                            </span>
                          </p>
                          <p className="text-xs text-slate-600">
                            Số lượng: <span className="font-medium">{shipment.totalQuantity}</span> | Quy cách:{" "}
                            <span className="font-medium">{shipment.packagingInfo || "—"}</span>
                          </p>
                        </div>

                        <div>
                          {isInWarehouse ? (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-800 border border-red-300">
                              <AlertCircle className="h-3.5 w-3.5" />
                              Đang trong kho (Vi phạm TC-04)
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-300">
                              <CheckCircle2 className="h-3.5 w-3.5" />
                              Sẵn sàng nhập kho
                            </span>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* KHU VỰC THÔNG BÁO LỖI / HỢP LỆ NGAY LÚC CHỌN (Instant Validation Banner) */}
              {hasValidationError && (
                <div className="p-4 bg-red-100/90 border-2 border-red-400 rounded-lg text-red-950 text-sm space-y-1">
                  <p className="font-bold flex items-center gap-2 text-base text-red-900">
                    <AlertCircle className="h-5 w-5 text-red-600 shrink-0" />
                    Phát hiện lỗi logic giao dịch kho (TC-04):
                  </p>
                  {invalidShipments.map(({ shipment }) => (
                    <p key={shipment.id} className="pl-7 text-red-800">
                      • Lô hàng <span className="font-semibold text-red-950">"{shipment.name}"</span> hiện đang ở trong kho HTX (chưa ghi nhận xuất kho). Không thể ghi nhập kho 2 lần liên tiếp.
                    </p>
                  ))}
                  <p className="pl-7 text-xs text-red-700 italic pt-1">
                    👉 Vui lòng ghi xuất kho cho lô hàng trên trước khi thực hiện ghi nhập kho mới.
                  </p>
                </div>
              )}

              {!hasValidationError && selectedShipments.length > 0 && (
                <div className="p-3.5 bg-emerald-100/80 border border-emerald-300 rounded-lg text-emerald-900 text-sm flex items-center gap-2.5">
                  <CheckCircle2 className="h-5 w-5 text-emerald-700 shrink-0" />
                  <div>
                    <span className="font-semibold">Tất cả {selectedShipments.length} lô hàng đều hợp lệ.</span> Bạn có thể hoàn tất thông tin phía dưới để ghi sự kiện nhập kho.
                  </div>
                </div>
              )}
            </div>

            {/* Tên kho & Thời điểm nhập kho */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="warehouseName" className="font-medium text-gray-700 flex items-center gap-1.5">
                  <Building2 className="h-4 w-4 text-slate-500" />
                  Tên kho lưu trữ <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="warehouseName"
                  placeholder="VD: Kho lạnh HTX Nông nghiệp Số 1"
                  {...register("warehouseName")}
                />
                {errors.warehouseName && (
                  <p className="text-sm text-red-600">{errors.warehouseName.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="entryTime" className="font-medium text-gray-700 flex items-center gap-1.5">
                  <Calendar className="h-4 w-4 text-slate-500" />
                  Thời điểm nhập kho <span className="text-red-500">*</span>
                </Label>
                <Input id="entryTime" type="datetime-local" {...register("entryTime")} />
                {errors.entryTime && (
                  <p className="text-sm text-red-600">{errors.entryTime.message}</p>
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
                <p className="text-sm text-red-600">{errors.storageCondition.message}</p>
              )}
            </div>

            {/* Vị trí bản đồ */}
            <div className="space-y-2">
              <Label className="font-medium text-gray-700 flex items-center gap-1.5">
                <MapPin className="h-4 w-4 text-slate-500" />
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
              <Label htmlFor="notes" className="font-medium text-gray-700 flex items-center gap-1.5">
                <FileText className="h-4 w-4 text-slate-500" />
                Ghi chú thêm
              </Label>
              <Textarea
                id="notes"
                rows={3}
                placeholder="Nhập ghi chú bổ sung khi nhập kho..."
                {...register("notes")}
              />
              {errors.notes && <p className="text-sm text-red-600">{errors.notes.message}</p>}
            </div>

            {/* Hộp lưu ý */}
            <div className="p-4 bg-amber-50 border border-amber-200 rounded-md text-amber-900 text-sm space-y-1">
              <p className="font-semibold">Lưu ý nghiệp vụ:</p>
              <p>• Thời gian lưu kho sẽ bắt đầu tính từ thời điểm nhập kho được ghi nhận ở đây.</p>
              <p>• Sự kiện sau khi tạo sẽ được liên kết trực tiếp vào chuỗi hash mã hóa của lô hàng.</p>
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
              disabled={isSubmitting || selectedShipments.length === 0 || hasValidationError}
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
            >
              <LogIn className="mr-1.5 h-4 w-4" />
              {isSubmitting ? "Đang xử lý..." : `Ghi nhập kho (${selectedShipments.length} lô)`}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
