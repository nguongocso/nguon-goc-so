import React, { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import {
  Camera,
  Mail,
  Phone,
  UploadCloud,
  UserCheck,
} from "lucide-react";

import { getProfile, updateProfile, uploadAvatar } from "@/api/userApi";
import { useAuth } from "@/hooks/useAuth";
import type { AuthUserInfo } from "@/types/auth";
import type { UserProfile } from "@/types/user";
import {
  type UserProfileFormValues,
  userProfileSchema,
} from "@/utils/validators";
import { getAssetUrl } from "@/config/runtimeConfig";

import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export const UserProfileForm: React.FC = () => {
  const { user: authUser, updateUser } = useAuth();
  const [profile, setProfile] = useState<UserProfile | AuthUserInfo | null>(authUser);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<UserProfileFormValues>({
    resolver: zodResolver(userProfileSchema),
    defaultValues: {
      fullName: authUser?.fullName || "",
      phone: authUser?.phone || "",
      email: authUser?.email || "",
    },
  });

  useEffect(() => {
    let isMounted = true;
    const fetchProfile = async () => {
      try {
        const response = await getProfile();
        if (response.success && response.data && isMounted) {
          const data = response.data;
          setProfile(data);
          reset({
            fullName: data.fullName || "",
            phone: data.phone || "",
            email: data.email || "",
          });
        }
      } catch {
        if (authUser && isMounted) {
          setProfile(authUser);
          reset({
            fullName: authUser.fullName || "",
            phone: authUser.phone || "",
            email: authUser.email || "",
          });
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    void fetchProfile();
    return () => {
      isMounted = false;
    };
  }, []);

  const handleCancel = () => {
    reset({
      fullName: profile?.fullName || "",
      phone: profile?.phone || "",
      email: profile?.email || "",
    });
    setIsEditing(false);
  };

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    const file = files[0];
    if (file.size > 5 * 1024 * 1024) {
      toast.error("Dung lượng ảnh không được vượt quá 5MB");
      return;
    }

    try {
      setUploadingAvatar(true);
      const response = await uploadAvatar(file);
      if (response.success && response.data) {
        const newAvatarUrl = response.data.avatarUrl;
        toast.success("Tải lên ảnh đại diện thành công");
        setProfile((prev) => (prev ? { ...prev, avatarUrl: newAvatarUrl } : null));
        if (authUser) {
          updateUser({ ...authUser, avatarUrl: newAvatarUrl });
        }
      } else {
        throw new Error(response.message || "Tải lên ảnh đại diện thất bại");
      }
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        error.message ||
        "Tải lên ảnh đại diện thất bại";
      toast.error(message);
    } finally {
      setUploadingAvatar(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const onSubmit = async (data: UserProfileFormValues) => {
    try {
      const payload = {
        fullName: data.fullName.trim(),
        phone: data.phone ? data.phone.trim() : null,
        email: data.email ? data.email.trim() : null,
      };

      const response = await updateProfile(payload);
      if (response.success && response.data) {
        const updated = response.data;
        setProfile(updated);
        if (authUser) {
          updateUser({
            ...authUser,
            fullName: updated.fullName,
            phone: updated.phone,
            email: updated.email,
            avatarUrl: updated.avatarUrl !== undefined ? updated.avatarUrl : authUser.avatarUrl,
          });
        }
        setIsEditing(false);
        toast.success("Cập nhật thông tin hồ sơ thành công");
      } else {
        throw new Error(response.message || "Cập nhật thất bại.");
      }
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        error.message ||
        "Cập nhật thất bại. Vui lòng thử lại.";
      toast.error(message);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center p-12 text-sm text-muted-foreground">
        Đang tải thông tin hồ sơ...
      </div>
    );
  }

  const avatarSrc = profile?.avatarUrl;
  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <CardHeader className="border-b border-slate-100 pb-4">
        <CardTitle className="text-lg font-semibold text-slate-900 flex items-center gap-2">
          <UserCheck className="size-5 text-emerald-600" />
          Thông tin chi tiết
        </CardTitle>
        <CardDescription>
          {profile?.fullName} — Tên đăng nhập:{" "}
          <span className="font-mono font-medium text-slate-700">
            {profile?.username}
          </span>
          {profile?.organizationName && (
            <>
              {" "}
              · Tổ chức:{" "}
              <span className="font-medium text-slate-700">
                {profile.organizationName}
              </span>
              {profile.organizationCode && (
                <span className="font-mono text-slate-500 text-xs">
                  {" "}
                  ({profile.organizationCode})
                </span>
              )}
            </>
          )}
        </CardDescription>
      </CardHeader>

      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-6 pt-6">
          {/* Khu vực ảnh đại diện */}
          <div className="flex flex-col sm:flex-row items-center gap-5 p-4 rounded-lg bg-slate-50/70 border border-slate-200/80">
            <div className="relative group">
              <div className="size-20 rounded-full border-2 border-emerald-500/40 bg-emerald-100 flex items-center justify-center overflow-hidden text-emerald-800 font-bold text-2xl shadow-inner">
                {avatarSrc ? (
                  <img
                    src={getAssetUrl(avatarSrc)}
                    alt={profile?.fullName || "Avatar"}
                    className="size-full object-cover"
                  />
                ) : (
                  profile?.fullName?.charAt(0).toUpperCase() || "U"
                )}
              </div>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingAvatar}
                className="absolute bottom-0 right-0 p-1.5 rounded-full bg-emerald-600 hover:bg-emerald-700 text-white shadow-md transition-colors"
                title="Thay đổi ảnh đại diện"
              >
                <Camera className="size-3.5" />
              </button>
            </div>

            <div className="space-y-1 text-center sm:text-left flex-1">
              <div className="flex items-center gap-2 justify-center sm:justify-start">
                <span className="text-sm font-semibold text-slate-800">
                  Ảnh đại diện tài khoản
                </span>
              </div>
              <p className="text-xs text-slate-500">
                Hỗ trợ định dạng JPG, PNG, GIF hoặc WEBP (tối đa 5MB).
              </p>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                className="hidden"
                onChange={handleAvatarChange}
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingAvatar}
                className="mt-1 text-xs h-8 gap-1.5 border-slate-300"
              >
                <UploadCloud className="size-3.5 text-emerald-600" />
                {uploadingAvatar ? "Đang tải lên..." : "Tải ảnh mới lên"}
              </Button>
            </div>
          </div>

          {/* Họ và tên */}
          <div className="space-y-2">
            <Label
              htmlFor="fullName"
              className="flex items-center gap-1.5 text-slate-700 font-medium"
            >
              <UserCheck className="size-4 text-emerald-600" />
              Họ và tên
            </Label>
            <Input
              id="fullName"
              {...register("fullName")}
              disabled={!isEditing}
              placeholder="Nhập họ và tên hiển thị"
              className={
                isEditing
                  ? "bg-white border-slate-300 focus-visible:ring-emerald-500"
                  : "bg-slate-50/80 text-slate-600 border-slate-200"
              }
            />
            {errors.fullName && (
              <p className="text-xs text-red-500 mt-1">
                {errors.fullName.message}
              </p>
            )}
          </div>

          {/* Hàng 3: Số điện thoại & Email */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label
                htmlFor="phone"
                className="flex items-center gap-1.5 text-slate-700 font-medium"
              >
                <Phone className="size-4 text-emerald-600" />
                Số điện thoại
              </Label>
              <Input
                id="phone"
                {...register("phone")}
                disabled={!isEditing}
                placeholder="Nhập số điện thoại (10 số)"
                className={
                  isEditing
                    ? "bg-white border-slate-300 focus-visible:ring-emerald-500"
                    : "bg-slate-50/80 text-slate-600 border-slate-200"
                }
              />
              {errors.phone && (
                <p className="text-xs text-red-500 mt-1">{errors.phone.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label
                htmlFor="email"
                className="flex items-center gap-1.5 text-slate-700 font-medium"
              >
                <Mail className="size-4 text-emerald-600" />
                Email
              </Label>
              <Input
                id="email"
                type="email"
                {...register("email")}
                disabled={!isEditing}
                placeholder="Nhập địa chỉ email"
                className={
                  isEditing
                    ? "bg-white border-slate-300 focus-visible:ring-emerald-500"
                    : "bg-slate-50/80 text-slate-600 border-slate-200"
                }
              />
              {errors.email && (
                <p className="text-xs text-red-500 mt-1">{errors.email.message}</p>
              )}
            </div>
          </div>
        </CardContent>

        <CardFooter className="flex justify-end gap-2 border-t border-slate-100 pt-4">
          {!isEditing ? (
            <Button
              type="button"
              variant="edit"
              onClick={() => setIsEditing(true)}
            >
              Chỉnh sửa
            </Button>
          ) : (
            <>
              <Button
                type="button"
                variant="outline"
                onClick={handleCancel}
                disabled={isSubmitting}
              >
                Hủy
              </Button>
              <Button
                type="submit"
                variant="default"
                disabled={isSubmitting}
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
              >
                {isSubmitting ? "Đang lưu..." : "Lưu thay đổi"}
              </Button>
            </>
          )}
        </CardFooter>
      </form>
    </Card>
  );
};
