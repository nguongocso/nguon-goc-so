import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { AlertTriangle } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

import { updateFarmArea, getCropTypes } from "@/api/farmAreaApi";
import type { AreaUnit, CropType, FarmArea } from "@/types/farmArea";
import { AREA_UNIT_LABELS, convertAreaFromHa, convertAreaToHa } from "@/types/farmArea";
import { LocationPicker } from "@/pages/packaging-event/components/LocationPicker";
import { selectAllOnFocus, preventMouseUpCollapse } from "@/utils/inputUtils";

const formSchema = z.object({
  name: z.string().min(1, "Tên vùng trồng không được để trống").max(255),
  cropType: z.string().uuid("Vui lòng chọn loại cây trồng"),
  latitude: z.number({ required_error: "Vui lòng chọn vị trí trên bản đồ" }),
  longitude: z.number({ required_error: "Vui lòng chọn vị trí trên bản đồ" }),
  area: z
    .number({ invalid_type_error: "Vui lòng nhập diện tích" })
    .positive("Diện tích phải lớn hơn 0"),
  areaUnit: z.enum(["HA", "KM2", "M2", "SAO", "CONG", "MAU"], {
    required_error: "Vui lòng chọn đơn vị diện tích",
  }),
});

type FormValues = z.infer<typeof formSchema>;

interface Props {
  farmArea: FarmArea;
  onSuccess: (updatedArea: FarmArea) => void;
  onCancel: () => void;
}

export const EditFarmAreaForm = ({ farmArea, onSuccess, onCancel }: Props) => {
  const [cropTypes, setCropTypes] = useState<CropType[]>([]);
  const [loading, setLoading] = useState(true);

  const initialDisplayArea = convertAreaFromHa(farmArea.area, farmArea.areaUnit);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: farmArea.name || "",
      cropType: farmArea.cropTypeId || "",
      latitude: farmArea.latitude || 0,
      longitude: farmArea.longitude || 0,
      area: initialDisplayArea || undefined,
      areaUnit: farmArea.areaUnit || "HA",
    },
  });

  const latitude = watch("latitude");
  const longitude = watch("longitude");
  const selectedAreaUnit = watch("areaUnit");
  const selectedCropType = watch("cropType");

  const currentPosition =
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    !(latitude === 0 && longitude === 0)
      ? { lat: latitude, lng: longitude }
      : undefined;

  useEffect(() => {
    const fetchCropTypes = async () => {
      try {
        const data = await getCropTypes();
        setCropTypes(data);
      } catch {
        toast.error("Không thể tải danh sách loại cây trồng");
      } finally {
        setLoading(false);
      }
    };
    void fetchCropTypes();
  }, []);

  const handleLocationSelect = (lat: number, lng: number) => {
    setValue("latitude", lat, { shouldValidate: true, shouldDirty: true });
    setValue("longitude", lng, { shouldValidate: true, shouldDirty: true });
  };

  const onSubmit = async (values: FormValues) => {
    const legacyUnits: AreaUnit[] = ["HA", "KM2"];
    const payload = { ...values };

    if (!legacyUnits.includes(values.areaUnit)) {
      payload.area = convertAreaToHa(values.area, values.areaUnit);
      payload.areaUnit = "HA";
    }

    try {
      const result = await updateFarmArea(farmArea.id, payload);
      toast.success(`Cập nhật vùng trồng "${result.name}" thành công!`);
      onSuccess(result);
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Có lỗi xảy ra khi cập nhật vùng trồng");
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20 text-muted-foreground">
        <span className="animate-spin mr-2 h-5 w-5 border-2 border-emerald-500 border-t-transparent rounded-full" />
        Đang tải...
      </div>
    );
  }

  const hasCropTypes = cropTypes.length > 0;
  const cropTypePlaceholder = hasCropTypes
    ? "Chọn loại cây trồng"
    : "Chưa có loại cây trồng nào";

  const associatedLots = farmArea.associatedLotsCount || 0;

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <CardTitle className="text-lg font-semibold text-slate-900">
            Chỉnh sửa thông tin vùng trồng
          </CardTitle>
        </CardHeader>

        <CardContent className="space-y-6 pt-6">
          {associatedLots > 0 && (
            <Alert className="bg-amber-50 border-amber-200 text-amber-900">
              <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
              <div>
                <AlertTitle className="font-semibold text-sm">Vùng trồng đang có lô sản xuất!</AlertTitle>
                <AlertDescription className="text-xs mt-0.5">
                  Vùng trồng này đang liên kết với <span className="font-bold">{associatedLots} lô sản xuất</span>. Thông tin cập nhật sẽ được ghi vết lịch sử hoạt động hệ thống.
                </AlertDescription>
              </div>
            </Alert>
          )}

          {/* Nhóm 1: Tên & Loại cây trồng */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label htmlFor="name" className="text-sm font-medium text-emerald-800">
                Tên vùng trồng <span className="text-red-500">*</span>
              </Label>
              <Input
                id="name"
                {...register("name")}
                placeholder="VD: Vùng chè Tân Cương"
                className="border-emerald-200 focus-visible:ring-emerald-100"
              />
              {errors.name && (
                <p className="text-sm text-red-500">{errors.name.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="cropType" className="text-sm font-medium text-emerald-800">
                Loại cây trồng <span className="text-red-500">*</span>
              </Label>
              <Select
                value={selectedCropType}
                onValueChange={(value) =>
                  setValue("cropType", value || "", {
                    shouldValidate: true,
                    shouldDirty: true,
                  })
                }
                disabled={!hasCropTypes}
              >
                <SelectTrigger id="cropType" className="w-full border-emerald-200 focus:ring-emerald-100">
                  <SelectValue placeholder={cropTypePlaceholder}>
                    {selectedCropType && hasCropTypes
                      ? cropTypes.find((c) => c.id === selectedCropType)?.name
                      : ""}
                  </SelectValue>
                </SelectTrigger>
                {hasCropTypes && (
                  <SelectContent>
                    {cropTypes.map((type) => (
                      <SelectItem key={type.id} value={type.id}>
                        {type.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                )}
              </Select>
              {errors.cropType && (
                <p className="text-sm text-red-500">{errors.cropType.message}</p>
              )}
            </div>
          </div>

          {/* Nhóm 2: Vị trí trên bản đồ */}
          <div className="space-y-2">
            <Label className="text-sm font-medium text-emerald-800">
              Vị trí trên bản đồ <span className="text-red-500">*</span>
            </Label>

            <LocationPicker
              onLocationSelect={handleLocationSelect}
              initialPosition={currentPosition}
              height="340px"
            />

            {(errors.latitude || errors.longitude) && (
              <p className="text-sm text-red-500">
                Vui lòng chọn vị trí trên bản đồ
              </p>
            )}
          </div>

          {/* Nhóm 3: Diện tích */}
          <div className="space-y-1.5">
            <Label htmlFor="area" className="text-sm font-medium text-emerald-800">
              Diện tích <span className="text-red-500">*</span>
            </Label>
            <div className="flex gap-2">
              <Input
                id="area"
                type="number"
                step="0.01"
                className="w-32 border-emerald-200 focus-visible:ring-emerald-100"
                {...register("area", { valueAsNumber: true })}
                onFocus={selectAllOnFocus}
                onMouseUp={preventMouseUpCollapse}
                placeholder="VD: 5.5"
              />
              <Select
                value={selectedAreaUnit}
                onValueChange={(value) =>
                  setValue("areaUnit", value as AreaUnit, { shouldValidate: true })
                }
              >
                <SelectTrigger className="w-full flex-1 border-emerald-200 focus:ring-emerald-100">
                  <SelectValue>
                    {selectedAreaUnit ? AREA_UNIT_LABELS[selectedAreaUnit] : ""}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent side="bottom" align="start" className="min-w-[220px] w-(--anchor-width)">
                  {(Object.entries(AREA_UNIT_LABELS) as [AreaUnit, string][]).map(
                    ([unit, label]) => (
                      <SelectItem key={unit} value={unit}>
                        {label}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>
            </div>
            {errors.area && (
              <p className="text-sm text-red-500">{errors.area.message}</p>
            )}
            {errors.areaUnit && (
              <p className="text-sm text-red-500">{errors.areaUnit.message}</p>
            )}
          </div>
        </CardContent>

        {/* Footer */}
        <div className="flex justify-end gap-2 border-t border-emerald-100 px-6 py-4">
          <Button type="button" variant="outline" onClick={onCancel}>
            Hủy
          </Button>

          <Button type="submit" className="bg-emerald-600 hover:bg-emerald-700 text-white" disabled={isSubmitting}>
            {isSubmitting ? "Đang lưu..." : "Lưu thay đổi"}
          </Button>
        </div>
      </Card>
    </form>
  );
};
