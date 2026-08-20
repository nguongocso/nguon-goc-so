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
// unused import removed
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
        "Lô sản xuất từ mã vừa quét chưa ở trạng thái đã thu hoạch hoặc đã sơ chế, không thể chọn sẵn.",
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

  useAutoGeolocation({
    onLocation: (selectedLatitude, selectedLongitude) => {
      handleLocationSelect(selectedLatitude, selectedLongitude);
      toast.success("Đã lấy vị trí hiện tại");
    },
    onError: (message) => {
      toast.error(`Không thể lấy vị trí: ${message}`);
    },
  });

  const checkFarmLogEligibility = async (lotId: string) => {
    const requestId = (eligibilityRequestRef.current += 1);
    setEligibilityStatus("checking");
    setEligibilityMessage("");
    setMissingActivities([]);

    try {
      const logs = await getAllFarmLogsByProductionLot(lotId);
      if (eligibilityRequestRef.current !== requestId) return;

      const loggedTypes = new Set(logs.map((log) => log.activityType));
      const missing = requiredFarmActivities.filter(
        (type) => !loggedTypes.has(type),
      );

      if (missing.length === 0) {
        setEligibilityStatus("eligible");
        setEligibilityMessage("Đã đủ nhật ký nông vụ để ghi nhận đóng gói");
      } else {
        setEligibilityStatus("ineligible");
        setEligibilityMessage(
          "Chưa đủ nhật ký nông vụ bắt buộc để đóng gói",
        );
        setMissingActivities(missing);
      }
    } catch {
      if (eligibilityRequestRef.current !== requestId) return;
      setEligibilityStatus("ineligible");
      setEligibilityMessage("Không thể kiểm tra nhật ký nông vụ");
    }
  };

  const handleLotSelect = (lotId: string) => {
    eligibilityRequestRef.current += 1;
    setSelectedLotId(lotId);
    setValue("productionLotId", lotId, { shouldValidate: true });
    void checkFarmLogEligibility(lotId);
  };

  const onSubmit = async (values: RecordPackagingFormValues) => {
    try {
      await recordPackagingEvent(values);
      toast.success("Ghi sự kiện đóng gói thành công!");
      navigate("/packaging-events");
    } catch (err: unknown) {
      const parsedError = getPackagingError(err);
      toast.error(parsedError.message);

      if (parsedError.missingActivities.length > 0) {
        setEligibilityStatus("ineligible");
        setEligibilityMessage(parsedError.message);
        setMissingActivities(parsedError.missingActivities);
      }
    }
  };

  return (
    <Card className="max-w-2xl mx-auto">
      <CardHeader>
        <CardTitle>Ghi sự kiện đóng gói</CardTitle>
        <CardDescription>
          Nhập thông tin đóng gói nông sản để lưu vào chuỗi cung ứng
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="productionLotId">Chọn lô sản xuất *</Label>
            <Select
              value={selectedLotId}
              onValueChange={(val) => handleLotSelect(val || "")}
              disabled={loadingLots}
            >
              <SelectTrigger id="productionLotId" className="w-full">
                <span>
                  {selectedLot
                    ? selectedLot.name
                    : "Chọn lô sản xuất"}
                </span>
              </SelectTrigger>
              <SelectContent>
                {productionLots.map((lot) => (
                  <SelectItem key={lot.id} value={lot.id}>
                    {lot.name} - {lot.productCategoryName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {errors.productionLotId && (
              <p className="text-sm text-red-500">
                {errors.productionLotId.message}
              </p>
            )}
          </div>

          {selectedLotId && (
            <LotValidationStatus
              isValid={validation?.valid ?? null}
              message={validation?.message ?? ""}
              loading={loading}
            />
          )}

          <FarmLogEligibilityAlert
            status={eligibilityStatus}
            message={eligibilityMessage}
            missingActivities={missingActivities}
          />

          <div className="space-y-2">
            <Label htmlFor="packagingSpecification">Quy cách đóng gói *</Label>
            <Input
              id="packagingSpecification"
              placeholder="Ví dụ: Thùng carton 10kg, Túi PE 1kg..."
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
            <Label>Vị trí đóng gói (click trên bản đồ)</Label>

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
