import React from "react";
import { UserCheck } from "lucide-react";
import { UserProfileForm } from "@/components/profile/UserProfileForm";
import { ChangePasswordForm } from "@/components/profile/ChangePasswordForm";

export const ProfilePage: React.FC = () => {
  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-10">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <UserCheck className="size-6 text-emerald-600" />
            Hồ sơ cá nhân & Bảo mật
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Quản lý thông tin định danh, liên hệ, ảnh đại diện và chủ động đổi mật khẩu đăng nhập.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        <div className="lg:col-span-2 space-y-6">
          <UserProfileForm />
        </div>
        <div className="lg:col-span-1 space-y-6">
          <ChangePasswordForm />
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
