import { getOrganizationProfile, updateOrganizationProfile } from "@/api/organizationApi";
import { useAuth } from "@/hooks/useAuth";
import type { OrganizationProfile, UpdateOrganizationRequest } from "../../types/organization.ts";
import { type OrganizationProfileFormValues, organizationProfileSchema } from "@/utils/validators";
import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "../ui/card";
import { Label } from "../ui/label";
import { Button } from "../ui/button";
import { Input } from "../ui/input";

export const OrganizationProfileForm: React.FC = () => {
  const { user } = useAuth();
  const [profile, setProfile] = useState<OrganizationProfile | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<OrganizationProfileFormValues>({
    resolver: zodResolver(organizationProfileSchema),
  });

  const canEdit = user?.roleCode === 'VT-01' || user?.roleCode === 'VT-02';

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const data = await getOrganizationProfile();
        setProfile(data);
        reset({
          name: data.name,
          address: data.address || '',
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
  }, [reset]);

  const onSubmit = async (data: OrganizationProfileFormValues) => {
    try {
      const payload: UpdateOrganizationRequest = {
        name: data.name,
        address: data.address,
        phone: data.phone,
        email: data.email,
      };

      const updated = await updateOrganizationProfile(payload);
      setProfile(updated);
      setIsEditing(false);
      toast.success('Cập nhật hồ sơ thành công');
    } catch (error: any) {
      const message = error.response?.data?.message || 'Cập nhật thất bại. Vui lòng thử lại.';
      toast.error(message);
    }
  };

  if (loading) {
    return <div className="flex justify-center p-8">Đang tải....</div>
  }

  return (
    <Card className="max-w-2xl mx-auto">
      <CardHeader>
        <CardTitle>Hồ sơ tổ chức</CardTitle>
        <CardDescription>
          {profile?.name} - Mã: {profile?.code}
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
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
            <div className="space-y-2">
              <Label htmlFor="address">Địa chỉ</Label>
              <Input
                id="address"
                {...register('address')}
                disabled={!isEditing}
                placeholder="Nhập địa chỉ"
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
        <CardFooter className="flex justify-end gap-2">
          {!isEditing ? (
            canEdit && (
              <Button type="button" variant="edit" onClick={() => setIsEditing(true)}>
                Chỉnh sửa
              </Button>
            )
          ) : (
            <>
              <Button type="button" variant="outline" onClick={() => setIsEditing(false)}>
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