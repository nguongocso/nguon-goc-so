import { getOrganizationProfile, updateOrganizationProfile } from "@/api/organizationApi";
import { useAuth } from "@/hooks/useAuth";
import { useAdministrativeUnits } from "@/hooks/useAdministrativeUnits";
import type { OrganizationProfile, UpdateOrganizationRequest } from "../../types/organization.ts";
import { type OrganizationProfileFormValues, organizationProfileSchema } from "@/utils/validators";
import { AdministrativeUnitSingleSelect } from "@/components/common/AdministrativeUnitSingleSelect";
import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "../ui/card";
import { Label } from "../ui/label";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { AlertTriangle } from "lucide-react";
import { cn } from "@/lib/utils";

export const OrganizationProfileForm: React.FC = () => {
  const { user, updateUser } = useAuth();
  const [profile, setProfile] = useState<OrganizationProfile | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);

  const { units, loading: unitsLoading } = useAdministrativeUnits();

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<OrganizationProfileFormValues>({
    resolver: zodResolver(organizationProfileSchema),
  });

  const provinceId = watch('provinceId');
  const communeId = watch('communeId');

  const canEdit = user?.roleCode === 'VT-01' || user?.roleCode === 'VT-02';
  const isMissingTerritory = user?.roleCode === 'VT-02' && (!profile?.provinceId || !profile?.communeId);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const data = await getOrganizationProfile();
        setProfile(data);
        if (user && (user.organizationProvinceId !== data.provinceId || user.organizationCommuneId !== data.communeId)) {
          updateUser({
            ...user,
            organizationProvinceId: data.provinceId,
            organizationCommuneId: data.communeId,
          });
        }
        reset({
          name: data.name,
          address: data.address || '',
          provinceId: data.provinceId || '',
          communeId: data.communeId || '',
          phone: data.phone || '',
          email: data.email || '',
        });
      } catch (error) {
        toast.error('Không thể tải thông tin tổ chức');
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [reset, user, updateUser]);

  const onSubmit = async (data: OrganizationProfileFormValues) => {
    try {
      const payload: UpdateOrganizationRequest = {
        name: data.name,
        address: data.address,
        provinceId: data.provinceId || null,
        communeId: data.communeId || null,
        phone: data.phone,
        email: data.email,
      };

      const updated = await updateOrganizationProfile(payload);
      setProfile(updated);
      if (user) {
        updateUser({
          ...user,
          organizationProvinceId: updated.provinceId,
          organizationCommuneId: updated.communeId,
        });
        if (user.organizationId) {
          sessionStorage.removeItem(`session_read_org_territory_notice_${user.organizationId}`);
        }
      }
      setIsEditing(false);
      toast.success('Cập nhật hồ sơ thành công');
    } catch (error: any) {
      const message = error.response?.data?.message || 'Cập nhật thất bại. Vui lòng thử lại.';
      toast.error(message);
    }
  };

  if (loading) {
    return <div className="flex justify-center p-8">Đang tải....</div>;
  }

  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <CardHeader className="border-b border-slate-100 pb-4">
        <div>
          <CardTitle className="text-lg font-semibold text-slate-900">
            Thông tin chi tiết
          </CardTitle>
          <CardDescription className="mt-1">
            {profile?.name} — Mã định danh: <span className="font-mono font-medium text-slate-700">{profile?.code}</span>
          </CardDescription>
        </div>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-6 pt-6">
          <div className="space-y-2">
            <Label htmlFor="name">Tên tổ chức *</Label>
            <Input
              id="name"
              {...register('name')}
              disabled={!isEditing}
              placeholder="Nhập tên tổ chức"
            />
            {errors.name && <p className="text-sm text-red-500">{errors.name.message}</p>}
          </div>

          {/* Cụm chọn Địa bàn hành chính (Tỉnh/Thành phố → Xã/Phường) và Địa chỉ chi tiết */}
          <div
            className={cn(
              "rounded-lg border p-4 space-y-3 transition-colors",
              isMissingTerritory
                ? "border-amber-300 bg-amber-50/40 dark:border-amber-800/60 dark:bg-amber-950/20"
                : "border-slate-200/80 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/30"
            )}
          >
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                Địa bàn hành chính quản lý
              </span>
              {isMissingTerritory && (
                <span className="inline-flex items-center gap-1 text-xs font-medium text-amber-700 dark:text-amber-300">
                  <AlertTriangle className="size-3.5" />
                  Chưa thiết lập
                </span>
              )}
            </div>

            {isMissingTerritory && (
              <div className="flex items-start gap-2.5 rounded-md border border-amber-300/80 bg-amber-100/70 p-3 text-xs text-amber-950 dark:border-amber-800/60 dark:bg-amber-950/50 dark:text-amber-200">
                <AlertTriangle className="size-4 shrink-0 text-amber-600 dark:text-amber-400 mt-0.5" />
                <div className="space-y-0.5 leading-relaxed">
                  <p className="font-semibold text-amber-900 dark:text-amber-100">
                    Địa bàn hành chính quản lý chưa được hoàn tất
                  </p>
                  <p className="text-amber-800 dark:text-amber-300">
                    Vui lòng {!isEditing ? 'bấm "Chỉnh sửa" và ' : ''}chọn đầy đủ <strong>Tỉnh / Thành phố</strong> và <strong>Xã / Phường</strong> để các lô sản xuất của Hợp tác xã được đồng bộ vào phạm vi quản lý của Cán bộ ngành.
                  </p>
                </div>
              </div>
            )}

            {!isEditing ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="view-province">Tỉnh / Thành phố</Label>
                  <Input
                    id="view-province"
                    value={profile?.provinceName || ''}
                    disabled
                    placeholder="Chưa chọn tỉnh/thành phố"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="view-commune">Xã / Phường</Label>
                  <Input
                    id="view-commune"
                    value={profile?.communeName || ''}
                    disabled
                    placeholder="Chưa chọn xã/phường"
                  />
                </div>
              </div>
            ) : (
              <AdministrativeUnitSingleSelect
                units={units}
                provinceId={provinceId}
                communeId={communeId}
                onProvinceChange={(val) => setValue('provinceId', val, { shouldDirty: true })}
                onCommuneChange={(val) => setValue('communeId', val, { shouldDirty: true })}
                disabled={!isEditing}
                loading={unitsLoading}
              />
            )}

            <div className="space-y-2 pt-1">
              <Label htmlFor="address">Địa chỉ chi tiết (số nhà, đường, thôn/xóm)</Label>
              <Input
                id="address"
                {...register('address')}
                disabled={!isEditing}
                placeholder="Ví dụ: Thôn 1, Đội 3"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="phone">Số điện thoại</Label>
              <Input
                id="phone"
                {...register('phone')}
                disabled={!isEditing}
                placeholder="Nhập số điện thoại"
              />
              {errors.phone && <p className="text-sm text-red-500">{errors.phone.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                {...register('email')}
                disabled={!isEditing}
                placeholder="Nhập email"
                type="email"
              />
              {errors.email && <p className="text-sm text-red-500">{errors.email.message}</p>}
            </div>
          </div>
        </CardContent>
        <CardFooter className="flex justify-end gap-2 border-t border-slate-100 pt-4">
          {!isEditing ? (
            canEdit && (
              <Button type="button" variant="edit" onClick={() => setIsEditing(true)}>
                Chỉnh sửa
              </Button>
            )
          ) : (
            <>
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setIsEditing(false);
                  if (profile) {
                    reset({
                      name: profile.name,
                      address: profile.address || '',
                      provinceId: profile.provinceId || '',
                      communeId: profile.communeId || '',
                      phone: profile.phone || '',
                      email: profile.email || '',
                    });
                  }
                }}
              >
                Hủy
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Đang lưu...' : 'Lưu'}
              </Button>
            </>
          )}
        </CardFooter>
      </form>
    </Card>
  );
};
