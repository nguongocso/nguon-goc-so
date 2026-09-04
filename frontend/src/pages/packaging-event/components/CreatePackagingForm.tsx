import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { useNavigate, useLocation, useSearchParams } from "react-router-dom";
import { getPackagingEligibility } from "@/api/cultivationMilestoneApi";
import { getProductionLotById } from "@/api/productionLotApi";
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
import { recordPackagingEvent } from "@/api/packagingApi";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { AlertTriangle, LoaderCircle, PackageSearch } from "lucide-react";
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
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";

const getPackagingError = (error: unknown) => {
  if (!isAxiosError<{ message?: string }>(error)) {
    return {
      message: "Có lỗi xảy ra khi ghi sự kiện đóng gói",
      isNetworkError: true,
    };
  }

  const message =
    error.response?.data?.message ?? "Có lỗi xảy ra khi ghi sự kiện đóng gói";

  return {
    message,
    isNetworkError: !error.response,
  };
};

export function CreatePackagingForm() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { user } = useAuth();

  const sourceLotId =
    (location.state as { productionLotId?: string } | null)?.productionLotId ??
    searchParams.get("productionLotId") ??
    "";

  const [lot, setLot] = useState<ProductionLot | null>(null);
  const [loadingLot, setLoadingLot] = useState(sourceLotId ? true : false);
  const [lotLoadError, setLotLoadError] = useState<string | null>(null);
  const [eligibilityStatus, setEligibilityStatus] =
    useState<FarmLogEligibilityStatus>("unselected");
  const [eligibilityMessage, setEligibilityMessage] = useState("");
  const [missingMilestones, setMissingMilestones] = useState<string[]>([]);
  const eligibilityRequestRef = useRef(0);

  useSetBreadcrumb([
    { label: "Tổng quan", href: "/dashboard" },
    { label: "Lô sản xuất", href: "/production-lots" },
    ...(sourceLotId
      ? [
          {
            label: lot?.name || "Chi tiết lô sản xuất",
            href: `/production-lots/${sourceLotId}`,
          },
        ]
      : []),
    { label: "Ghi đóng gói" },
  ]);

  const { validation, loading } = useLotValidation(sourceLotId, "PACKAGING");

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RecordPackagingFormValues>({
    resolver: zodResolver(recordPackagingSchema),
    defaultValues: {
      productionLotId: sourceLotId,
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

  useEffect(() => {
    if (!sourceLotId) return;

    const fetchLot = async () => {
      setLoadingLot(true);
      setLotLoadError(null);
      setEligibilityStatus("checking");
      try {
        const data = await getProductionLotById(sourceLotId);
        setLot(data);
        eligibilityRequestRef.current += 1;
        void checkFarmLogEligibility(sourceLotId);
      } catch {
        setLot(null);
        setLotLoadError(
          "Không thể tải thông tin lô sản xuất đã chọn. Vui lòng quay lại và thử lại.",
        );
      } finally {
        setLoadingLot(false);
      }
    };

    void fetchLot();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sourceLotId]);

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

  // NCL-09-CN-011: điều kiện đóng gói do backend quyết định theo mốc canh tác
  // bắt buộc (loại nông sản + tiêu chuẩn của lô) — frontend chỉ hiển thị kết quả.
  const checkFarmLogEligibility = async (productionLotId: string) => {
    const requestId = ++eligibilityRequestRef.current;

    setEligibilityStatus("checking");
    setEligibilityMessage("");
    setMissingMilestones([]);

    try {
      const eligibility = await getPackagingEligibility(productionLotId);
      if (requestId !== eligibilityRequestRef.current) return;

      if (!eligibility.eligible) {
        setEligibilityStatus("ineligible");
        setEligibilityMessage(
          "Lô sản xuất chưa đủ mốc canh tác bắt buộc. Vui lòng bổ sung nhật ký trước khi đóng gói.",
        );
        setMissingMilestones(
          eligibility.missingMilestones.map((milestone) => milestone.name),
        );
        return;
      }

      setEligibilityStatus("eligible");
      setEligibilityMessage(
        "Lô đã đáp ứng đầy đủ mốc canh tác bắt buộc theo tiêu chuẩn và loại nông sản.",
      );
    } catch (error: unknown) {
      if (requestId !== eligibilityRequestRef.current) return;

      const details = getPackagingError(error);
      setEligibilityStatus("error");
      setEligibilityMessage(
        details.isNetworkError
          ? "Không thể kết nối để kiểm tra mốc canh tác. Vui lòng thử lại."
          : details.message,
      );
    }
  };

  const onSubmit = async (values: RecordPackagingFormValues) => {
    if (eligibilityStatus !== "eligible") {
      toast.error("Cần kiểm tra đủ mốc canh tác trước khi đóng gói");
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
      // Backend chặn vì thiếu mốc canh tác bắt buộc (race: cấu hình mốc hoặc
      // nhật ký đổi sau lần kiểm tra gần nhất) -> làm mới danh sách từ backend.
      const milestoneError = /chưa đủ mốc canh tác/i.test(details.message);

      if (milestoneError) {
        setEligibilityStatus("ineligible");
        setEligibilityMessage(details.message);
        try {
          const eligibility = await getPackagingEligibility(
            values.productionLotId,
          );
          setMissingMilestones(
            eligibility.missingMilestones.map((milestone) => milestone.name),
          );
        } catch {
          setMissingMilestones([]);
        }
      } else if (details.isNetworkError) {
        setEligibilityStatus("error");
        setEligibilityMessage(
          "Không thể kết nối để ghi sự kiện đóng gói. Vui lòng thử lại.",
        );
      } else {
        setEligibilityStatus("error");
        setEligibilityMessage(details.message);
      }

      toast.error(details.message);
    }
  };

  if (!sourceLotId) {
    return (
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardContent className="flex flex-col items-center justify-center py-16 text-center">
          <span className="grid size-14 place-items-center rounded-full bg-slate-100 text-slate-500">
            <PackageSearch className="size-7" />
          </span>
          <h3 className="mt-4 text-lg font-bold text-slate-900">
            Chưa có lô sản xuất được chọn
          </h3>
          <p className="mt-1 max-w-md text-sm text-slate-500">
            Ghi sự kiện đóng gói là chức năng gắn với một lô sản xuất cụ thể.
            Vui lòng mở từ trang chi tiết lô hoặc quét mã truy xuất để chọn lô
            cần đóng gói.
          </p>
        </CardContent>
      </Card>
    );
  }

  if (loadingLot) {
    return (
      <div className="flex items-center justify-center py-16 gap-2 text-slate-500">
        <LoaderCircle className="size-4 animate-spin" />
        Đang tải lô sản xuất...
      </div>
    );
  }

  if (lotLoadError || !lot) {
    return (
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardContent className="py-10">
          <Alert variant="destructive">
            <AlertTriangle className="size-4" />
            <AlertDescription className="flex flex-col gap-3 items-start">
              <span>{lotLoadError ?? "Không tìm thấy lô sản xuất đã chọn."}</span>
            </AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  const selectedLot = lot;

  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <CardHeader>
        <CardTitle>Ghi sự kiện đóng gói</CardTitle>
        <CardDescription>
          Nhập thông tin đóng gói cho lô sản xuất “{selectedLot.name}
          {selectedLot.productCategoryName
            ? ` - ${selectedLot.productCategoryName}`
            : ""}”.
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-6">
          <div className="space-y-2 rounded-lg border border-emerald-100 bg-emerald-50/20 p-4">
            <Label className="text-sm text-muted-foreground">
              Lô sản xuất đang đóng gói
            </Label>
            <div className="text-lg font-bold text-emerald-900">
              {selectedLot.name}
              {selectedLot.productCategoryName
                ? ` - ${selectedLot.productCategoryName}`
                : ""}
            </div>
          </div>
          {loading ? (
            <LotValidationStatus
              isValid={null}
              message=""
              loading
              className="mt-2"
            />
          ) : validation && !validation.valid ? (
            <LotValidationStatus
              isValid={validation.valid}
              message={validation.message}
              className="mt-2"
            />
          ) : (
            <FarmLogEligibilityAlert
              status={eligibilityStatus}
              productionLotName={selectedLot.name}
              missingMilestones={missingMilestones}
              message={eligibilityMessage || undefined}
              actionLabel={
                user?.roleCode === "VT-02"
                  ? "Xem lịch sử nhật ký"
                  : "Ghi bổ sung nhật ký"
              }
              onAction={
                eligibilityStatus === "ineligible" && sourceLotId
                  ? () =>
                    navigate(
                      user?.roleCode === "VT-02"
                        ? `/production-lots/${sourceLotId}/farm-logs`
                        : `/farm-logs/create?productionLotId=${encodeURIComponent(sourceLotId)}`,
                    )
                  : undefined
              }
              onRetry={
                eligibilityStatus === "error" ||
                  eligibilityStatus === "ineligible"
                  ? () => void checkFarmLogEligibility(sourceLotId)
                  : undefined
              }
            />
          )}

          <div className="space-y-2">
            <Label htmlFor="packagingSpecification">Quy cách đóng gói *</Label>
            <Input
              id="packagingSpecification"
              placeholder="VD: Bao 60kg, Túi 500g x 20 túi/thùng..."
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
              isSubmitting || !sourceLotId || eligibilityStatus !== "eligible" || !validation?.valid
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
