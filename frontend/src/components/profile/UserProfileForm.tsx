import React, { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Building, Lock, Mail, Phone, Shield, User, UserCheck } from "lucide-react";

import { getCurrent, updateUserProfile } from "@/api/authApi";
import { useAuth } from "@/hooks/useAuth";
import type { AuthUserInfo, UpdateUserProfileRequest } from "@/types/auth";
import {
  type UserProfileFormValues,
  userProfileSchema,
} from "@/utils/validators";
import { getRoleLabel } from "@/config/roleAccess";

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
  const [profile, setProfile] = useState<AuthUserInfo | null>(authUser);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<UserProfileFormValues>({
    resolver: zodResolver(userProfileSchema),
    defaultValues: {
      phone: authUser?.phone || "",
      email: authUser?.email || "",
    },
  });

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await getCurrent();
        if (response.success && response.data) {
          const data = response.data;
          setProfile(data);
          updateUser(data);
          reset({
            phone: data.phone || "",
            email: data.email || "",
          });
        }
      } catch {
        // Fallback sang authUser hiện tại trong context
        if (authUser) {
          setProfile(authUser);
          reset({
            phone: authUser.phone || "",
            email: authUser.email || "",
          });
        }
      } finally {
        setLoading(false);
      }
    };

    void fetchProfile();
  }, [reset, updateUser]);

  const handleCancel = () => {
    reset({
      phone: profile?.phone || "",
      email: profile?.email || "",
    });
    setIsEditing(false);
  };

  const onSubmit = async (data: UserProfileFormValues) => {
    try {
      const payload: UpdateUserProfileRequest = {
        phone: data.phone ? data.phone.trim() : null,
        email: data.email ? data.email.trim() : null,
      };

      const response = await updateUserProfile(payload);
      if (response.success && response.data) {
        const updated = response.data;
        setProfile(updated);
        updateUser(updated);
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

  const roleDisplay = getRoleLabel(profile?.roleCode);

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
        <CardContent className="space-y-5 pt-6">
          {/* Hàng 1: Tên đăng nhập & Họ và tên (Không thể sửa) */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label
                htmlFor="username"
                className="flex items-center gap-1.5 text-slate-700 font-medium"
              >
                <User className="size-4 text-slate-400" />
                Tên đăng nhập
                <span title="Không thể thay đổi">
                  <Lock className="size-3 text-slate-400 ml-1" />
                </span>
              </Label>
              <Input
                id="username"
                value={profile?.username || ""}
                disabled
                className="bg-slate-50/80 text-slate-600 cursor-not-allowed border-slate-200"
              />
            </div>

            <div className="space-y-2">
              <Label
                htmlFor="fullName"
                className="flex items-center gap-1.5 text-slate-700 font-medium"
              >
                <UserCheck className="size-4 text-slate-400" />
                Họ và tên
                <span title="Không thể thay đổi">
                  <Lock className="size-3 text-slate-400 ml-1" />
                </span>
              </Label>
              <Input
                id="fullName"
                value={profile?.fullName || ""}
                disabled
                className="bg-slate-50/80 text-slate-600 cursor-not-allowed border-slate-200"
              />
            </div>
          </div>

          {/* Hàng 2: Vai trò & Thuộc tổ chức (Không thể sửa) */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label
                htmlFor="roleName"
                className="flex items-center gap-1.5 text-slate-700 font-medium"
              >
                <Shield className="size-4 text-slate-400" />
                Vai trò
                <span title="Không thể thay đổi">
                  <Lock className="size-3 text-slate-400 ml-1" />
                </span>
              </Label>
              <Input
                id="roleName"
                value={roleDisplay}
                disabled
                className="bg-slate-50/80 text-slate-600 cursor-not-allowed border-slate-200 font-medium"
              />
            </div>

            <div className="space-y-2">
              <Label
                htmlFor="organizationName"
                className="flex items-center gap-1.5 text-slate-700 font-medium"
              >
                <Building className="size-4 text-slate-400" />
                Thuộc tổ chức
                <span title="Không thể thay đổi">
                  <Lock className="size-3 text-slate-400 ml-1" />
                </span>
              </Label>
              <Input
                id="organizationName"
                value={
                  profile?.organizationName
                    ? `${profile.organizationName}${
                        profile.organizationCode
                          ? ` (${profile.organizationCode})`
                          : ""
                      }`
                    : "Chưa gán tổ chức"
                }
                disabled
                className="bg-slate-50/80 text-slate-600 cursor-not-allowed border-slate-200"
              />
            </div>
          </div>

          {/* Hàng 3: Số điện thoại & Email (Có thể sửa khi bấm Chỉnh sửa) */}
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
              {!profile?.email && !isEditing && (
                <p className="text-xs text-amber-600 font-medium mt-1">
                  ⚠️ Chưa thiết lập email. Vui lòng bấm &ldquo;Chỉnh sửa&rdquo; để thêm email phục vụ việc đặt lại mật khẩu khi quên.
                </p>
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
