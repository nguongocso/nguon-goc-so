import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { useNavigate, useLocation } from "react-router-dom";
import { getAllFarmLogsByProductionLot } from "@/api/farmLogApi";
import { getLocalDateString } from "@/utils/dateTime";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  recordPackagingSchema,
  type RecordPackagingFormValues,
} from "@/utils/validators/packagingEventSchema";
import type { ProductionLot } from "@/types/productionLot";
import type { FarmActivityType } from "@/types/farmLog";
import {
  getHarvestedProductionLots,
  recordPackagingEvent,
} from "@/api/packagingApi";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { LocationPicker } from "@/pages/packaging-event/components/LocationPicker";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/useAuth";
import {
  FarmLogEligibilityAlert,
  type FarmLogEligibilityStatus,
} from "@/pages/packaging-event/components/FarmLogEligibilityAlert";
import { useLotValidation } from "@/hooks/useLotValidation";
import { useAutoGeolocation } from "@/hooks/useAutoGeolocation";
import { LotValidationStatus } from "@/components/event-validation/LotValidationStatus";

const farmActivityTypes: FarmActivityType[] = [
  "PLANTING",
  "WATERING",
  "FERTILIZING",
  "PESTICIDE",
  "WEEDING",
  "HARVESTING",
  "OTHER",
];

const requiredFarmActivities: FarmActivityType[] = [
  "PLANTING",
  "FERTILIZING",
  "PESTICIDE",
  "HARVESTING",
];

interface PackagingErrorPayload {
  message?: string;
  data?: {
    missingActivities?: string[];
  };
}

const getPackagingError = (error: unknown) => {
  if (!isAxiosError<PackagingErrorPayload>(error)) {
    return {
      message: "Có lỗi xảy ra khi ghi sự kiện đóng gói",
      missingActivities: [] as FarmActivityType[],
      isNetworkError: true,
    };
  }

  const payload = error.response?.data;
  const message = payload?.message ?? "Có lỗi xảy ra khi ghi sự kiện đóng gói";
  const fromResponse = payload?.data?.missingActivities ?? [];
  const normalizedMessage = message.toUpperCase();
  const missingActivities = farmActivityTypes.filter(
    (activity) =>
      fromResponse.includes(activity) || normalizedMessage.includes(activity),
  );

  return {
    message,
    missingActivities,
    isNetworkError: !error.response,
  };
};

export function CreatePackagingForm() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const [productionLots, setProductionLots] = useState<ProductionLot[]>([]);
  const [loadingLots, setLoadingLots] = useState(true);
  const [selectedLotId, setSelectedLotId] = useState("");
  const [eligibilityStatus, setEligibilityStatus] =
    useState<FarmLogEligibilityStatus>("unselected");
  const [eligibilityMessage, setEligibilityMessage] = useState("");
  const [missingActivities, setMissingActivities] = useState<
    FarmActivityType[]
  >([]);
  const eligibilityRequestRef = useRef(0);

  const { validation, loading } = useLotValidation(selectedLotId, "PACKAGING");

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RecordPackagingFormValues>({
    resolver: zodResolver(recordPackagingSchema),
    defaultValues: {
      productionLotId: "",
      packagingSpecification: "",
      packagingDate: getLocalDateString(),
      latitude: 0,
      longitude: 0,
    },
  });

  const lat = watch("latitude");
  const lng = watch("longitude");

  const currentPosition =
    typeof lat === "number" &&
    Number.isFinite(lat) &&
    typeof lng === "number" &&
    Number.isFinite(lng) &&
    !(lat === 0 && lng === 0)
      ? {
          lat,
          lng,
        }
      : undefined;

  const selectedLot = productionLots.find((lot) => lot.id === selectedLotId);

  useEffect(() => {
    const fetchLots = async () => {
      try {
        const data = await getHarvestedProductionLots();
        setProductionLots(data);
      } catch {
        toast.error("Không thể tải danh sách lô sản xuất");
      } finally {
        setLoadingLots(false);
      }
    };
    fetchLots();
  }, []);

  // Điền sẵn lô sản xuất khi được điều hướng từ trang "Quét mã ghi sự kiện
  // nhanh" (state.productionLotId lấy từ ScanLookupResponse). Chỉ áp dụng
  // khi lô đó thực sự có trong danh sách lô đã thu hoạch tải được ở trên;
  // nếu không, báo rõ lý do thay vì set một giá trị không khớp dropdown.
  useEffect(() => {
    if (loadingLots || selectedLotId) return;

    const prefilledLotId = (
      location.state as { productionLotId?: string } | null
    )?.productionLotId;
    if (!prefilledLotId) return;

    const matchedLot = productionLots.find((lot) => lot.id === prefilledLotId);
    if (!matchedLot) {
      toast.error(
        "Lô sản xuất từ mã vừa quét chưa ở trạng thái đã thu hoạch, không thể chọn sẵn.",
      );
      return;
    }

    eligibilityRequestRef.current += 1;
    setSelectedLotId(prefilledLotId);
    setValue("productionLotId", prefilledLotId, { shouldValidate: true });
    void checkFarmLogEligibility(prefilledLotId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadingLots, productionLots]);

  const handleLocationSelect = (
    selectedLatitude: number,
    selectedLongitude: number,
  ) => {
    setValue("latitude", selectedLatitude, {
      shouldValidate: true,
      shouldDirty: true,
    });
    setValue("longitude", selectedLongitude, {
      shouldValidate: true,
      shouldDirty: true,
    });
  };

  const { locationLoading, fetchLocation } = useAutoGeolocation({
    onLocation: (selectedLatitude, selectedLongitude) => {
      handleLocationSelect(selectedLatitude, selectedLongitude);
      toast.success("Đã lấy vị trí hiện tại");
    },
    onError: (message) => {
      toast.error(`Không thể lấy vị trí: ${message}`);
    },
  });

  const checkFarmLogEligibility = async (productionLotId: string) => {
    const requestId = ++eligibilityRequestRef.current;

    setEligibilityStatus("checking");
    setEligibilityMessage("");
    setMissingActivities([]);

    try {
      const logs = await getAllFarmLogsByProductionLot(productionLotId);
      if (requestId !== eligibilityRequestRef.current) return;

      const recordedActivities = new Set(logs.map((log) => log.activityType));
      const missing = requiredFarmActivities.filter(
        (activity) => !recordedActivities.has(activity),
      );

      if (missing.length > 0) {
        setEligibilityStatus("ineligible");
        setEligibilityMessage(
          "Lô sản xuất còn thiếu nhật ký bắt buộc. Vui lòng bổ sung trước khi đóng gói.",
        );
        setMissingActivities(missing);
        return;
      }

      setEligibilityStatus("eligible");
      setEligibilityMessage(
        "Lô đã có đủ nhật ký gieo trồng, bón phân, phun thuốc và thu hoạch.",
      );
    } catch (error: unknown) {
      if (requestId !== eligibilityRequestRef.current) return;

      const details = getPackagingError(error);
      setEligibilityStatus("error");
      setEligibilityMessage(
        details.isNetworkError
          ? "Không thể kết nối để kiểm tra nhật ký. Vui lòng thử lại."
          : details.message,
      );
    }
  };

  const onSubmit = async (values: RecordPackagingFormValues) => {
    if (eligibilityStatus !== "eligible") {
      toast.error("Cần kiểm tra đủ nhật ký trước khi đóng gói");
      await checkFarmLogEligibility(values.productionLotId);
      return;
    }

    try {
      await recordPackagingEvent({
        productionLotId: values.productionLotId,
        packagingSpecification: values.packagingSpecification,
        packagingDate: values.packagingDate,
        latitude: values.latitude || undefined,
        longitude: values.longitude || undefined,
      });
      setEligibilityStatus("eligible");
      toast.success("Ghi sự kiện đóng gói thành công");
      navigate("/production-lots");
    } catch (error: unknown) {
      const details = getPackagingError(error);
      const missingLogError =
        details.missingActivities.length > 0 ||
        /thiếu.*nhật ký|nhật ký.*(?:chưa|không).*đầy đủ|không đủ điều kiện đóng gói/i.test(
          details.message,
        );

      if (missingLogError) {
        setEligibilityStatus("ineligible");
        setEligibilityMessage(details.message);
        setMissingActivities(details.missingActivities);
      } else if (details.isNetworkError) {
        setEligibilityStatus("error");
        setEligibilityMessage(
          "Không thể kết nối để kiểm tra nhật ký. Vui lòng thử lại.",
        );
      } else {
        setEligibilityStatus("error");
        setEligibilityMessage(details.message);
      }

      toast.error(details.message);
    }
  };

  if (loadingLots) return <div className="p-8 text-center">Đang tải...</div>;

  return (
    <Card className="max-w-4xl mx-auto">
      <CardHeader>
        <CardTitle>Ghi sự kiện đóng gói</CardTitle>
        <CardDescription>
          Nhập thông tin đóng gói cho lô sản xuất đã thu hoạch.
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="productionLotId">Lô sản xuất *</Label>
            <Select
              value={selectedLotId || ""}
              onValueChange={(val) => {
                const nextLotId = val || "";
                eligibilityRequestRef.current += 1;
                setSelectedLotId(nextLotId);
                setValue("productionLotId", nextLotId, {
                  shouldValidate: true,
                });
                setEligibilityMessage("");
                setMissingActivities([]);

                if (nextLotId) {
                  void checkFarmLogEligibility(nextLotId);
                } else {
                  setEligibilityStatus("unselected");
                }
              }}
            >
              <SelectTrigger>
                <span>
                  {selectedLotId
                    ? productionLots.find((lot) => lot.id === selectedLotId)
                        ?.name
                    : "Chọn lô đã thu hoạch"}
                </span>
              </SelectTrigger>
              <SelectContent>
                {productionLots.map((lot) => (
                  <SelectItem key={lot.id} value={lot.id}>
                    {lot.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <LotValidationStatus
              isValid={validation?.valid ?? null}
              message={validation?.message || ""}
              loading={loading}
              className="mt-2"
            />
            {errors.productionLotId && (
              <p className="text-sm text-red-500">
                {errors.productionLotId.message}
              </p>
            )}
          </div>

          <FarmLogEligibilityAlert
            status={eligibilityStatus}
            productionLotName={selectedLot?.name}
            missingActivities={missingActivities}
            message={eligibilityMessage || undefined}
            actionLabel={
              user?.roleCode === "VT-02"
                ? "Xem lịch sử nhật ký"
                : "Ghi bổ sung nhật ký"
            }
            onAction={
              eligibilityStatus === "ineligible" && selectedLotId
                ? () =>
                    navigate(
                      user?.roleCode === "VT-02"
                        ? `/production-lots/${selectedLotId}/farm-logs`
                        : `/farm-logs/create?productionLotId=${encodeURIComponent(selectedLotId)}`,
                    )
                : undefined
            }
            onRetry={
              eligibilityStatus === "error" ||
              eligibilityStatus === "ineligible"
                ? () =>
                    selectedLotId
                      ? void checkFarmLogEligibility(selectedLotId)
                      : setEligibilityStatus("unselected")
                : undefined
            }
          />

          <div className="space-y-2">
            <Label htmlFor="packagingSpecification">Quy cách đóng gói *</Label>
            <Input
              id="packagingSpecification"
              {...register("packagingSpecification")}
            />
            {errors.packagingSpecification && (
              <p className="text-sm text-red-500">
                {errors.packagingSpecification.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="packagingDate">Ngày đóng gói *</Label>
            <Input
              id="packagingDate"
              type="date"
              {...register("packagingDate")}
              max={getLocalDateString()}
            />
            {errors.packagingDate && (
              <p className="text-sm text-red-500">
                {errors.packagingDate.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between gap-3">
              <Label>Vị trí đóng gói (click trên bản đồ)</Label>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={locationLoading || isSubmitting}
                onClick={() => fetchLocation()}
              >
                {locationLoading
                  ? "Đang lấy vị trí..."
                  : "Lấy vị trí hiện tại"}
              </Button>
            </div>

            <div className="flex gap-2">
              <Input
                value={currentPosition?.lat ?? ""}
                disabled
                placeholder="Vĩ độ"
              />
              <Input
                value={currentPosition?.lng ?? ""}
                disabled
                placeholder="Kinh độ"
              />
            </div>

            <LocationPicker
              onLocationSelect={handleLocationSelect}
              initialPosition={currentPosition}
              height="300px"
            />
          </div>
        </CardContent>
        <CardFooter className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={() => navigate(-1)}>
            Hủy
          </Button>
          {/* CHANGED: thêm variant="create" */}
          <Button
            type="submit"
            disabled={
              isSubmitting || !selectedLotId || eligibilityStatus !== "eligible" || !validation?.valid
            }
            variant="create"
          >
            {isSubmitting ? "Đang ghi..." : "Ghi sự kiện"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}