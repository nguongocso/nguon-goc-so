import React, { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Eye, EyeOff, KeyRound, Lock, ShieldCheck } from "lucide-react";

import { changePassword } from "@/api/userApi";
import {
  type ChangePasswordFormValues,
  changePasswordSchema,
} from "@/utils/validators";

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

export const ChangePasswordForm: React.FC = () => {
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmNewPassword: "",
    },
  });

  const onSubmit = async (values: ChangePasswordFormValues) => {
    try {
      const response = await changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
        confirmNewPassword: values.confirmNewPassword,
      });

      if (response.success) {
        toast.success("Đổi mật khẩu thành công");
        reset();
      } else {
        throw new Error(response.message || "Đổi mật khẩu thất bại");
      }
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        error.message ||
        "Đổi mật khẩu thất bại. Vui lòng kiểm tra lại.";
      toast.error(message);
    }
  };

  return (
    <Card className="border border-slate-200/80 shadow-sm bg-white">
      <CardHeader className="border-b border-slate-100 bg-slate-50/50 pb-4">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-emerald-100/60 text-emerald-700">
            <KeyRound className="size-5" />
          </div>
          <div>
            <CardTitle className="text-base font-semibold text-slate-900">
              Đổi mật khẩu
            </CardTitle>
            <CardDescription className="text-xs text-slate-500">
              Thay đổi mật khẩu đăng nhập của bạn định kỳ để tăng cường bảo mật.
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-4 pt-6">
          {/* Mật khẩu hiện tại */}
          <div className="space-y-2">
            <Label
              htmlFor="currentPassword"
              className="flex items-center gap-1.5 text-slate-700 font-medium"
            >
              <Lock className="size-4 text-emerald-600" />
              Mật khẩu hiện tại
            </Label>
            <div className="relative">
              <Input
                id="currentPassword"
                type={showCurrentPassword ? "text" : "password"}
                {...register("currentPassword")}
                placeholder="Nhập mật khẩu hiện tại của bạn"
                className="pr-10 bg-white border-slate-300 focus-visible:ring-emerald-500"
              />
              <button
                type="button"
                onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                className="absolute inset-y-0 right-0 flex items-center pr-3 text-slate-400 hover:text-slate-600 focus:outline-none"
                tabIndex={-1}
              >
                {showCurrentPassword ? (
                  <EyeOff className="size-4" />
                ) : (
                  <Eye className="size-4" />
                )}
              </button>
            </div>
            {errors.currentPassword && (
              <p className="text-xs text-red-500 mt-1">
                {errors.currentPassword.message}
              </p>
            )}
          </div>

          {/* Mật khẩu mới */}
          <div className="space-y-2">
            <Label
              htmlFor="newPassword"
              className="flex items-center gap-1.5 text-slate-700 font-medium"
            >
              <Lock className="size-4 text-emerald-600" />
              Mật khẩu mới
            </Label>
            <div className="relative">
              <Input
                id="newPassword"
                type={showNewPassword ? "text" : "password"}
                {...register("newPassword")}
                placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)"
                className="pr-10 bg-white border-slate-300 focus-visible:ring-emerald-500"
              />
              <button
                type="button"
                onClick={() => setShowNewPassword(!showNewPassword)}
                className="absolute inset-y-0 right-0 flex items-center pr-3 text-slate-400 hover:text-slate-600 focus:outline-none"
                tabIndex={-1}
              >
                {showNewPassword ? (
                  <EyeOff className="size-4" />
                ) : (
                  <Eye className="size-4" />
                )}
              </button>
            </div>
            {errors.newPassword && (
              <p className="text-xs text-red-500 mt-1">
                {errors.newPassword.message}
              </p>
            )}
          </div>

          {/* Xác nhận mật khẩu mới */}
          <div className="space-y-2">
            <Label
              htmlFor="confirmNewPassword"
              className="flex items-center gap-1.5 text-slate-700 font-medium"
            >
              <ShieldCheck className="size-4 text-emerald-600" />
              Xác nhận mật khẩu mới
            </Label>
            <div className="relative">
              <Input
                id="confirmNewPassword"
                type={showConfirmPassword ? "text" : "password"}
                {...register("confirmNewPassword")}
                placeholder="Nhập lại mật khẩu mới"
                className="pr-10 bg-white border-slate-300 focus-visible:ring-emerald-500"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute inset-y-0 right-0 flex items-center pr-3 text-slate-400 hover:text-slate-600 focus:outline-none"
                tabIndex={-1}
              >
                {showConfirmPassword ? (
                  <EyeOff className="size-4" />
                ) : (
                  <Eye className="size-4" />
                )}
              </button>
            </div>
            {errors.confirmNewPassword && (
              <p className="text-xs text-red-500 mt-1">
                {errors.confirmNewPassword.message}
              </p>
            )}
          </div>
        </CardContent>

        <CardFooter className="flex justify-end border-t border-slate-100 pt-4">
          <Button
            type="submit"
            disabled={isSubmitting}
            className="bg-emerald-600 hover:bg-emerald-700 text-white"
          >
            {isSubmitting ? "Đang xử lý..." : "Cập nhật mật khẩu"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};
