import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import {
  LogOut,
  AlertCircle,
  CheckCircle2,
  Package,
  Calendar,
  MapPin,
  FileText,
  Truck,
  Building2,
} from "lucide-react";

import {
  getShipmentWarehouseStatus,
  recordWarehouseExit,
} from "@/api/coopWarehouseApi";
import { getProductionLotById } from "@/api/productionLotApi";
import { getShipmentById, getShipmentsByProductionLot } from "@/api/shipmentApi";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardFooter,
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
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { ListPageHeader } from "@/components/common/ListPageHeader";
import { HelpButton } from "@/components/help/HelpButton";

interface ShipmentStatusItem {
  shipment: Shipment;
  warehouseStatus: "IN_WAREHOUSE" | "NOT_IN_WAREHOUSE";
}

export default function CreateCoopWarehouseExitPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const productionLotId = searchParams.get("productionLotId") || "";
  const queryParam = searchParams.get("shipmentIds") || searchParams.get("shipmentId") || "";
  const initialShipmentIds = queryParam ? queryParam.split(",").filter(Boolean) : [];

  const [allAvailableShipments, setAllAvailableShipments] = useState<ShipmentStatusItem[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>(initialShipmentIds);
  const [loadingShipments, setLoadingShipments] = useState(true);
  const [serverError, setServerError] = useState<string | null>(null);
  const [lotName, setLotName] = useState<string | null>(null);

  useEffect(() => {
    if (!productionLotId) return;
    getProductionLotById(productionLotId)
      .then((lot) => setLotName(lot.name || lot.code || null))
      .catch(() => setLotName(null));
  }, [productionLotId]);

  useSetBreadcrumb([
    { label: "Tổng quan", href: "/dashboard" },
    { label: "Lô sản xuất", href: "/production-lots" },
    ...(productionLotId
      ? [
          {
            label: lotName || "Chi tiết lô sản xuất",
            href: `/production-lots/${productionLotId}`,
          },
        ]
      : []),
    { label: "Ghi sự kiện xuất kho HTX" },
  ]);

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
      shipmentId: initialShipmentIds[0] || "",
      exitTime: getCurrentDatetimeString(),
      destination: "",
      notes: "",
      latitude: undefined,
      longitude: undefined,
    },
  });

  // Tải tất cả lô hàng khả dụng & kiểm tra trạng thái ngay lập tức khi chọn lô hàng
  useEffect(() => {
    async function loadShipmentsAndStatuses() {
      try {
        setLoadingShipments(true);
        setServerError(null);

        let list: Shipment[] = [];
        if (productionLotId) {
          list = await getShipmentsByProductionLot(productionLotId);
        } else if (initialShipmentIds.length > 0) {
          const loaded = await Promise.all(
            initialShipmentIds.map((id) => getShipmentById(id).catch(() => null))
          );
          list = loaded.filter((s): s is Shipment => s !== null);
        }

        // Tải trạng thái kho từng lô hàng
        const itemsWithStatus = await Promise.all(
          list.map(async (s) => {
            const status = await getShipmentWarehouseStatus(s.id);
            return { shipment: s, warehouseStatus: status };
          })
        );

        setAllAvailableShipments(itemsWithStatus);

        // Mặc định chọn tất cả nếu từ URL truyền sang, hoặc chọn lô đầu tiên nếu chưa chọn
        if (initialShipmentIds.length > 0) {
          setSelectedIds(initialShipmentIds);
          setValue("shipmentId", initialShipmentIds[0]);
        } else if (itemsWithStatus.length > 0) {
          setSelectedIds([itemsWithStatus[0].shipment.id]);
          setValue("shipmentId", itemsWithStatus[0].shipment.id);
        }
      } catch (err) {
        console.error("Lỗi khi tải danh sách lô hàng:", err);
        setServerError("Không thể tải thông tin hoặc kiểm tra trạng thái lô hàng.");
      } finally {
        setLoadingShipments(false);
      }
    }
    loadShipmentsAndStatuses();
  }, [productionLotId, queryParam, setValue]);

  const selectedItems = allAvailableShipments.filter((item) =>
    selectedIds.includes(item.shipment.id)
  );

  // KIỂM TRA LỖI VI PHẠM TC-02 (Lô chưa có sự kiện nhập kho)
  const invalidItems = selectedItems.filter(
    (item) => item.warehouseStatus === "NOT_IN_WAREHOUSE"
  );
  const hasValidationError = invalidItems.length > 0;

  const onSubmit = async (values: RecordWarehouseExitFormValues) => {
    if (selectedItems.length === 0) {
      toast.error("Vui lòng chọn ít nhất một lô hàng để ghi sự kiện.");
      return;
    }

    if (hasValidationError) {
      toast.error("Vui lòng nhập kho cho các lô hàng chưa ở trong kho trước khi ghi xuất kho (TC-02).");
      return;
    }

    try {
      setServerError(null);
      let successCount = 0;
      let failMessage = "";
      let hasWarning = false;

      for (const item of selectedItems) {
        const formattedExitTime = values.exitTime && values.exitTime.length === 16 ? `${values.exitTime}:00` : values.exitTime;
        const payload = {
          ...values,
          exitTime: formattedExitTime,
          shipmentId: item.shipment.id,
        };
        const res = await recordWarehouseExit(payload);
        if (res && res.success) {
          successCount++;
          if (res.data?.isStorageExceeded) {
            hasWarning = true;
          }
        } else {
          failMessage = res?.message || `Lỗi khi ghi xuất kho cho lô ${item.shipment.name}`;
          break;
        }
      }

      if (successCount === selectedItems.length) {
        if (hasWarning) {
          toast.warning(`Ghi xuất kho HTX thành công cho ${successCount} lô hàng (Cảnh báo: Có lô vượt quá ngưỡng bảo quản).`);
        } else {
          toast.success(`Ghi sự kiện xuất kho HTX thành công cho ${successCount} lô hàng!`);
        }
        navigate(-1);
      } else {
        setServerError(failMessage || "Không thể ghi nhận xuất kho cho tất cả lô hàng.");
        toast.error(failMessage || "Có lỗi xảy ra khi ghi sự kiện xuất kho HTX.");
      }
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || "Có lỗi xảy ra khi ghi sự kiện xuất kho HTX.";
      setServerError(msg);
      toast.error(msg);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header chuẩn dự án */}
      <ListPageHeader
        icon={LogOut}
        title="Ghi sự kiện xuất kho HTX"
        description="Ghi nhận thời điểm các lô hàng rời kho hợp tác xã để chuyển đi thu mua hoặc vận chuyển."
        iconBoxClassName="text-amber-600 bg-amber-50"
        actions={<HelpButton screenKey="coop-warehouse-exit" />}
      />

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-6 pt-6">
            {serverError && (
              <div className="p-4 bg-red-50 border border-red-200 text-red-800 rounded-lg text-sm flex items-start gap-2">
                <AlertCircle className="h-5 w-5 text-red-600 shrink-0 mt-0.5" />
                <div>{serverError}</div>
              </div>
            )}

            {/* MỤC 1: DANH SÁCH LÔ HÀNG ĐÃ CHỌN (ĐÃ CHỌN BÊN NGOÀI) */}
            <div className="space-y-3">
              <div className="flex items-center justify-between pb-1 border-b border-slate-100">
                <Label className="font-semibold text-slate-900 text-sm flex items-center gap-2">
                  <Package className="h-4 w-4 text-amber-600" />
                  1. Danh sách lô hàng thực hiện xuất kho ({selectedItems.length} lô)
                </Label>
              </div>

              {loadingShipments ? (
                <div className="p-4 bg-slate-50 border rounded-lg text-sm text-slate-600 animate-pulse">
                  Đang tải thông tin các lô hàng...
                </div>
              ) : selectedItems.length === 0 ? (
                <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg text-amber-900 text-sm">
                  Chưa có lô hàng nào được chọn. Vui lòng quay lại danh sách để chọn lô hàng.
                </div>
              ) : (
                <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                  {selectedItems.map(({ shipment, warehouseStatus }) => {
                    const isNotInWarehouse = warehouseStatus === "NOT_IN_WAREHOUSE";

                    return (
                      <div
                        key={shipment.id}
                        className={`p-3.5 border rounded-lg flex items-center justify-between text-sm ${
                          isNotInWarehouse
                            ? "bg-red-50/70 border-red-200 text-red-950"
                            : "bg-amber-50/50 border-amber-200 text-amber-950"
                        }`}
                      >
                        <div className="space-y-0.5">
                          <p className="font-semibold text-slate-900">
                            {shipment.name}
                          </p>
                          <p className="text-xs text-slate-600">
                            Số lượng: <span className="font-medium">{shipment.totalQuantity}</span> | Quy cách:{" "}
                            <span className="font-medium">{shipment.packagingInfo || "—"}</span>
                          </p>
                        </div>

                        <div>
                          {isNotInWarehouse ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-800 border border-red-300">
                              <AlertCircle className="h-3.5 w-3.5" />
                              Chưa có sự kiện nhập kho (TC-02)
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-300">
                              <CheckCircle2 className="h-3.5 w-3.5" />
                              Đang trong kho (Đã nhập)
                            </span>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* BÁO LỖI TỨC THÌ KHI CHỌN VÀO LÔ KHÔNG HỢP LỆ (TC-02 Instant Validation Alert) */}
              {hasValidationError && (
                <div className="p-4 bg-red-100/90 border-2 border-red-400 rounded-lg text-red-950 text-sm space-y-1.5 shadow-sm">
                  <p className="font-bold flex items-center gap-2 text-base text-red-900">
                    <AlertCircle className="h-5 w-5 text-red-600 shrink-0" />
                    Cảnh báo vi phạm ràng buộc nghiệp vụ (TC-02):
                  </p>
                  {invalidItems.map(({ shipment }) => (
                    <p key={shipment.id} className="pl-7 text-red-900 font-medium">
                      • Lô hàng <span className="font-bold text-red-950">"{shipment.name}"</span> chưa được ghi nhận nhập kho HTX. Hệ thống yêu cầu ghi nhập kho trước khi xuất kho.
                    </p>
                  ))}
                </div>
              )}
            </div>

            {/* MỤC 2: THỜI ĐIỂM & ĐƠN VỊ TIẾP NHẬN */}
            <div className="space-y-4 pt-2">
              <div className="pb-1 border-b border-slate-100">
                <Label className="font-semibold text-slate-900 text-sm flex items-center gap-2">
                  <Building2 className="h-4 w-4 text-amber-600" />
                  2. Thời điểm xuất kho & Nơi chuyển đến
                </Label>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="exitTime" className="font-medium text-slate-700 flex items-center gap-1.5">
                    <Calendar className="h-4 w-4 text-slate-400" />
                    Thời điểm xuất kho <span className="text-red-500">*</span>
                  </Label>
                  <Input id="exitTime" type="datetime-local" {...register("exitTime")} />
                  {errors.exitTime && <p className="text-sm text-red-600">{errors.exitTime.message}</p>}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="destination" className="font-medium text-slate-700 flex items-center gap-1.5">
                    <Truck className="h-4 w-4 text-slate-400" />
                    Nơi chuyển đến / Đơn vị tiếp nhận
                  </Label>
                  <Input
                    id="destination"
                    placeholder="VD: Xe vận chuyển Công ty Thu Mua Chè Việt"
                    {...register("destination")}
                  />
                  {errors.destination && (
                    <p className="text-sm text-red-600">{errors.destination.message}</p>
                  )}
                </div>
              </div>
            </div>

            {/* MỤC 3: VỊ TRÍ BẢN ĐỒ */}
            <div className="space-y-2 pt-2">
              <div className="pb-1 border-b border-slate-100">
                <Label className="font-semibold text-slate-900 text-sm flex items-center gap-2">
                  <MapPin className="h-4 w-4 text-amber-600" />
                  3. Vị trí xuất kho (Click chọn trên bản đồ)
                </Label>
              </div>
              <div className="rounded-lg border border-slate-200 overflow-hidden">
                <LocationPicker
                  onLocationSelect={(lat, lng) => {
                    setValue("latitude", lat);
                    setValue("longitude", lng);
                  }}
                  height="260px"
                />
              </div>
            </div>

            {/* MỤC 4: GHI CHÚ BỔ SUNG */}
            <div className="space-y-2 pt-2">
              <div className="pb-1 border-b border-slate-100">
                <Label htmlFor="notes" className="font-semibold text-slate-900 text-sm flex items-center gap-2">
                  <FileText className="h-4 w-4 text-amber-600" />
                  4. Ghi chú bổ sung
                </Label>
              </div>
              <Textarea
                id="notes"
                rows={3}
                placeholder="Nhập ghi chú bổ sung khi xuất kho..."
                {...register("notes")}
              />
              {errors.notes && <p className="text-sm text-red-600">{errors.notes.message}</p>}
            </div>

            {/* Hộp lưu ý */}
            <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg text-amber-900 text-sm space-y-1">
              <p className="font-semibold">Lưu ý nghiệp vụ:</p>
              <p>• Hệ thống sẽ tính tổng thời gian lưu kho từ thời điểm nhập kho gần nhất đến thời điểm xuất kho này.</p>
              <p>• Nếu thời gian lưu kho vượt quá thời gian bảo quản tối đa của loại nông sản, cảnh báo màu cam sẽ tự động phát ra (TC-03).</p>
            </div>
          </CardContent>

          <CardFooter className="flex justify-end gap-3 border-t border-slate-100 bg-slate-50/50 p-4 rounded-b-xl">
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
              disabled={isSubmitting || selectedItems.length === 0 || hasValidationError}
              className="bg-amber-600 hover:bg-amber-700 text-white"
            >
              <LogOut className="mr-1.5 h-4 w-4" />
              {isSubmitting ? "Đang xử lý..." : `Ghi xuất kho (${selectedItems.length} lô)`}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}

