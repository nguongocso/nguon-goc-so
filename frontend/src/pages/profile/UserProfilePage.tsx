import React from "react";
import { UserCheck } from "lucide-react";
import { UserProfileForm } from "@/components/profile/UserProfileForm";

const UserProfilePage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <UserCheck className="size-6 text-emerald-600" />
            Hồ sơ người dùng
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Thông tin định danh, vai trò và thông tin liên hệ của tài khoản.
          </p>
        </div>
      </div>

      <UserProfileForm />
    </div>
  );
};

export default UserProfilePage;
