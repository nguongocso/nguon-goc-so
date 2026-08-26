import React, { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useSearchParams, useNavigate } from "react-router-dom";
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Eye,
  EyeOff,
  KeyRound,
  LoaderCircle,
  LockKeyhole,
} from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { resetPassword, validateResetToken } from "@/api/authApi";
import {
  resetPasswordSchema,
  type ResetPasswordFormValues,
} from "@/utils/validators";

const inputIconClass = "ml-[18px] size-[17px] shrink-0 text-emerald-600/70";

export const ResetPasswordForm: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const navigate = useNavigate();

  const [isValidating, setIsValidating] = useState(true);
  const [isTokenValid, setIsTokenValid] = useState(false);
  const [tokenError, setTokenError] = useState("");

  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      newPassword: "",
      confirmPassword: "",
    },
  });

  const newPasswordValue = watch("newPassword", "");

  useEffect(() => {
    if (!token) {
      setIsValidating(false);
      setIsTokenValid(false);
      setTokenError("Mã xác thực không hợp lệ hoặc đường dẫn bị thiếu token.");
      return;
    }

    const checkToken = async () => {
      try {
        setIsValidating(true);
        const res = await validateResetToken(token);
        if (res.data?.valid) {
          setIsTokenValid(true);
        } else {
          setIsTokenValid(false);
          setTokenError(res.data?.message || "Liên kết đặt lại mật khẩu đã hết hạn hoặc không còn hiệu lực.");
        }
      } catch (error: any) {
        setIsTokenValid(false);
        setTokenError("Không thể xác thực liên kết đặt lại mật khẩu. Vui lòng thử lại.");
      } finally {
        setIsValidating(false);
      }
    };

    checkToken();
  }, [token]);

  const onSubmit = async (data: ResetPasswordFormValues) => {
    setIsSubmitting(true);
    try {
      await resetPassword({
        token,
        newPassword: data.newPassword,
        confirmPassword: data.confirmPassword,
      });

      setIsSuccess(true);
      toast.success("Đặt lại mật khẩu thành công!");
    } catch (error: any) {
      const msg = error.response?.data?.message || "Đặt lại mật khẩu thất bại. Vui lòng thử lại.";
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const inputClass = cn(
    "h-full w-full rounded-full border-0 bg-transparent",
    "px-3 text-[0.88rem] text-stone-800 placeholder:text-stone-400",
    "focus:outline-none focus:ring-0 focus-visible:ring-0",
    "disabled:cursor-not-allowed disabled:opacity-50"
  );

  // Kiểm tra từng tiêu chí độ mạnh mật khẩu phục vụ UX
  const hasMinLength = newPasswordValue.length >= 8 && newPasswordValue.length <= 50;
  const hasUppercase = /[A-Z]/.test(newPasswordValue);
  const hasLowercase = /[a-z]/.test(newPasswordValue);
  const hasNumber = /\d/.test(newPasswordValue);
  const hasSpecial = /[\W_]/.test(newPasswordValue);

  return (
    <section
      className={cn(
        "w-full rounded-[28px] border border-white/60",
        "bg-white/80 p-8 shadow-2xl backdrop-blur-xl",
        "transition-all duration-300",
        "hover:shadow-emerald-900/10"
      )}
      aria-label="Đặt lại mật khẩu"
    >
      <header className="mb-6 text-center">
        <h1 className="text-2xl font-bold tracking-tight text-stone-900">
          Đặt lại mật khẩu mới
        </h1>
        <p className="mt-1.5 text-xs text-stone-500">
          Vui lòng thiết lập mật khẩu mới an toàn cho tài khoản của bạn.
        </p>
      </header>

      {isValidating ? (
        <div className="flex flex-col items-center justify-center py-10 text-center space-y-3">
          <LoaderCircle className="size-8 animate-spin text-emerald-600" />
          <p className="text-xs text-stone-500">Đang kiểm tra tính hợp lệ của liên kết...</p>
        </div>
      ) : !isTokenValid ? (
        <div className="space-y-6">
          <div className="flex flex-col items-center justify-center p-6 text-center rounded-2xl bg-amber-50 border border-amber-200">
            <AlertTriangle className="size-12 text-amber-600 mb-3" />
            <h2 className="text-base font-semibold text-amber-950 mb-1">
              Liên kết không hợp lệ hoặc đã hết hạn
            </h2>
            <p className="text-xs text-amber-800 leading-relaxed">
              {tokenError || "Liên kết này đã quá thời hạn 30 phút hoặc đã được sử dụng trước đó."}
            </p>
          </div>

          <div className="space-y-3">
            <Button
              type="button"
              className={cn(
                "h-[48px] w-full rounded-full",
                "bg-emerald-600 text-sm font-semibold text-white",
                "hover:bg-emerald-700 transition-all duration-200"
              )}
              onClick={() => navigate("/forgot-password")}
            >
              Gửi lại yêu cầu mới
            </Button>
            <Button
              type="button"
              variant="outline"
              className="h-[44px] w-full rounded-full text-xs text-stone-600 hover:text-stone-900"
              onClick={() => navigate("/login")}
            >
              Quay lại đăng nhập
            </Button>
          </div>
        </div>
      ) : isSuccess ? (
        <div className="space-y-6">
          <div className="flex flex-col items-center justify-center p-6 text-center rounded-2xl bg-emerald-50 border border-emerald-200">
            <CheckCircle2 className="size-12 text-emerald-600 mb-3" />
            <h2 className="text-base font-semibold text-emerald-950 mb-1">
              Đổi mật khẩu thành công!
            </h2>
            <p className="text-xs text-emerald-800 leading-relaxed">
              Mật khẩu tài khoản của bạn đã được cập nhật an toàn. Mọi phiên đăng nhập cũ đã được chấm dứt.
            </p>
          </div>

          <Button
            type="button"
            className={cn(
              "h-[48px] w-full rounded-full",
              "bg-emerald-600 text-sm font-semibold text-white",
              "hover:bg-emerald-700 transition-all duration-200 shadow-md shadow-emerald-200"
            )}
            onClick={() => navigate("/login")}
          >
            Đăng nhập ngay
          </Button>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          {/* Mật khẩu mới */}
          <div className="space-y-1.5">
            <Label
              htmlFor="newPassword"
              className="text-xs font-semibold uppercase tracking-wider text-stone-600"
            >
              Mật khẩu mới
            </Label>
            <div
              className={cn(
                "relative flex h-[50px] items-center rounded-full border border-stone-200/80",
                "bg-white shadow-inner transition-all duration-200",
                "focus-within:border-emerald-500 focus-within:ring-2 focus-within:ring-emerald-500/20",
                errors.newPassword && "border-red-400 focus-within:border-red-400 focus-within:ring-red-400/20"
              )}
            >
              <LockKeyhole className={inputIconClass} aria-hidden="true" />
              <Input
                id="newPassword"
                type={showNewPassword ? "text" : "password"}
                autoComplete="new-password"
                className={inputClass}
                placeholder="Nhập mật khẩu mới"
                disabled={isSubmitting}
                {...register("newPassword")}
              />
              <button
                type="button"
                className="mr-2 grid size-8 place-items-center text-stone-400 hover:text-emerald-600"
                onClick={() => setShowNewPassword((prev) => !prev)}
                tabIndex={-1}
              >
                {showNewPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
            {errors.newPassword && (
              <p className="mx-4 text-xs text-red-500" role="alert">
                {errors.newPassword.message}
              </p>
            )}
          </div>

          {/* Checklist độ mạnh mật khẩu */}
          <div className="rounded-xl border border-stone-200/80 bg-stone-50/60 p-3 text-[11px] space-y-1 text-stone-600">
            <div className="font-semibold text-stone-700 mb-1">Yêu cầu độ mạnh mật khẩu:</div>
            <div className="grid grid-cols-2 gap-1">
              <span className={cn("flex items-center gap-1", hasMinLength ? "text-emerald-600 font-medium" : "text-stone-400")}>
                {hasMinLength ? "✓" : "○"} 8-50 ký tự
              </span>
              <span className={cn("flex items-center gap-1", hasUppercase ? "text-emerald-600 font-medium" : "text-stone-400")}>
                {hasUppercase ? "✓" : "○"} Chữ in hoa (A-Z)
              </span>
              <span className={cn("flex items-center gap-1", hasLowercase ? "text-emerald-600 font-medium" : "text-stone-400")}>
                {hasLowercase ? "✓" : "○"} Chữ thường (a-z)
              </span>
              <span className={cn("flex items-center gap-1", hasNumber ? "text-emerald-600 font-medium" : "text-stone-400")}>
                {hasNumber ? "✓" : "○"} Chữ số (0-9)
              </span>
              <span className={cn("flex items-center gap-1 col-span-2", hasSpecial ? "text-emerald-600 font-medium" : "text-stone-400")}>
                {hasSpecial ? "✓" : "○"} Ký tự đặc biệt (@, $, !, %, *, ?, &, ...)
              </span>
            </div>
          </div>

          {/* Xác nhận mật khẩu mới */}
          <div className="space-y-1.5">
            <Label
              htmlFor="confirmPassword"
              className="text-xs font-semibold uppercase tracking-wider text-stone-600"
            >
              Xác nhận mật khẩu mới
            </Label>
            <div
              className={cn(
                "relative flex h-[50px] items-center rounded-full border border-stone-200/80",
                "bg-white shadow-inner transition-all duration-200",
                "focus-within:border-emerald-500 focus-within:ring-2 focus-within:ring-emerald-500/20",
                errors.confirmPassword && "border-red-400 focus-within:border-red-400 focus-within:ring-red-400/20"
              )}
            >
              <KeyRound className={inputIconClass} aria-hidden="true" />
              <Input
                id="confirmPassword"
                type={showConfirmPassword ? "text" : "password"}
                autoComplete="new-password"
                className={inputClass}
                placeholder="Nhập lại mật khẩu mới"
                disabled={isSubmitting}
                {...register("confirmPassword")}
              />
              <button
                type="button"
                className="mr-2 grid size-8 place-items-center text-stone-400 hover:text-emerald-600"
                onClick={() => setShowConfirmPassword((prev) => !prev)}
                tabIndex={-1}
              >
                {showConfirmPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
            {errors.confirmPassword && (
              <p className="mx-4 text-xs text-red-500" role="alert">
                {errors.confirmPassword.message}
              </p>
            )}
          </div>

          <Button
            type="submit"
            className={cn(
              "mt-2 h-[50px] w-full rounded-full",
              "bg-emerald-600 text-sm font-semibold text-white",
              "shadow-lg shadow-emerald-200 hover:bg-emerald-700",
              "transition-all duration-200"
            )}
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <>
                <LoaderCircle className="mr-2 h-4 w-4 animate-spin" />
                Đang lưu mật khẩu...
              </>
            ) : (
              "Xác nhận đặt lại mật khẩu"
            )}
          </Button>

          <div className="pt-2 text-center">
            <button
              type="button"
              onClick={() => navigate("/login")}
              className="inline-flex items-center gap-1.5 text-xs text-emerald-700 font-medium hover:text-emerald-800 hover:underline"
            >
              <ArrowLeft className="size-3.5" />
              Quay lại đăng nhập
            </button>
          </div>
        </form>
      )}

      <p className="mt-6 text-center text-[0.72rem] text-stone-400">
        Bảo mật & minh bạch – Truy xuất nguồn gốc thực vật
      </p>
    </section>
  );
};
