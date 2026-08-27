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
          setTokenError(
            res.data?.message ||
              "Liên kết đặt lại mật khẩu đã hết hạn hoặc không còn hiệu lực."
          );
        }
      } catch (error: any) {
        setIsTokenValid(false);
        setTokenError(
          "Không thể xác thực liên kết đặt lại mật khẩu. Vui lòng thử lại."
        );
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
      const msg =
        error.response?.data?.message ||
        "Đặt lại mật khẩu thất bại. Vui lòng thử lại.";
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const inputShellClass = (hasError: boolean) =>
    cn(
      "flex min-h-[52px] items-center rounded-full",
      "border border-emerald-200/50",
      "bg-white/80 backdrop-blur-sm shadow-sm",
      "transition-[border-color,box-shadow,background] duration-150",
      "focus-within:border-emerald-400",
      "focus-within:bg-white",
      "focus-within:ring-4",
      "focus-within:ring-emerald-100/60",
      "focus-within:shadow-md",
      hasError && "border-red-300/80 ring-3 ring-red-100/40"
    );

  const inputClass =
    "h-[50px] rounded-full border-0 bg-transparent " +
    "px-[14px] py-0 pl-[11px] text-[0.9rem] " +
    "text-foreground shadow-none " +
    "placeholder:text-stone-400 " +
    "focus-visible:border-0 " +
    "focus-visible:ring-0 " +
    "aria-invalid:ring-0 " +
    "aria-invalid:border-0 " +
    "[&:-webkit-autofill]:[-webkit-text-fill-color:var(--foreground)] " +
    "[&:-webkit-autofill]:[box-shadow:0_0_0px_1000px_rgba(255,255,255,0.8)_inset] " +
    "[&:-webkit-autofill]:[transition:background-color_9999s_ease-in-out_0s]";

  // Tiêu chuẩn kiểm tra độ mạnh mật khẩu
  const hasMinLength =
    newPasswordValue.length >= 8 && newPasswordValue.length <= 50;
  const hasUppercase = /[A-Z]/.test(newPasswordValue);
  const hasLowercase = /[a-z]/.test(newPasswordValue);
  const hasNumber = /\d/.test(newPasswordValue);
  const hasSpecial = /[\W_]/.test(newPasswordValue);

  return (
    <section
      className={cn(
        "w-full rounded-[28px]",
        "border border-emerald-200/60",
        "bg-white/70 backdrop-blur-xl",
        "p-[32px]",
        "shadow-[0_20px_50px_-12px_rgba(16,185,129,0.25)]",
        "max-[520px]:rounded-[22px]",
        "max-[520px]:px-5",
        "max-[520px]:py-[26px]"
      )}
      aria-labelledby="reset-password-title"
    >
      <header className="mb-6 text-center">
        <h1
          id="reset-password-title"
          className="text-2xl font-bold tracking-tight text-stone-900"
        >
          Đặt lại mật khẩu mới
        </h1>
        <p className="mt-1.5 text-xs text-stone-500">
          Vui lòng thiết lập mật khẩu mới an toàn cho tài khoản của bạn.
        </p>
      </header>

      {isValidating ? (
        <div className="flex flex-col items-center justify-center py-10 text-center space-y-3">
          <LoaderCircle className="size-8 animate-spin text-emerald-600" />
          <p className="text-xs text-stone-500">
            Đang kiểm tra tính hợp lệ của liên kết...
          </p>
        </div>
      ) : !isTokenValid ? (
        <div className="space-y-5">
          <div className="flex flex-col items-center justify-center p-6 text-center rounded-2xl bg-amber-50/80 border border-amber-200/80 shadow-inner">
            <AlertTriangle className="size-12 text-amber-600 mb-3" />
            <h2 className="text-base font-semibold text-amber-950 mb-1">
              Liên kết không hợp lệ hoặc đã hết hạn
            </h2>
            <p className="text-xs text-amber-800 leading-relaxed">
              {tokenError ||
                "Liên kết này đã quá thời hạn 30 phút hoặc đã được sử dụng trước đó."}
            </p>
          </div>

          <div className="space-y-3 pt-1">
            <Button
              type="button"
              className={cn(
                "h-[52px] w-full rounded-full",
                "bg-green-600",
                "text-[0.92rem] font-semibold text-white",
                "shadow-lg shadow-green-200",
                "hover:bg-green-700",
                "transition-all duration-200",
                "focus-visible:border-white",
                "focus-visible:ring-green-300/50"
              )}
              onClick={() => navigate("/forgot-password")}
            >
              Gửi lại yêu cầu mới
            </Button>
            <Button
              type="button"
              variant="outline"
              className={cn(
                "h-[48px] w-full rounded-full",
                "border border-emerald-200/80 bg-white/80",
                "text-[0.88rem] font-medium text-emerald-800",
                "hover:bg-emerald-50 hover:text-emerald-900 transition-all duration-200 shadow-sm"
              )}
              onClick={() => navigate("/login")}
            >
              Quay lại đăng nhập
            </Button>
          </div>
        </div>
      ) : isSuccess ? (
        <div className="space-y-5">
          <div className="flex flex-col items-center justify-center p-6 text-center rounded-2xl bg-emerald-50/80 border border-emerald-200/80 shadow-inner">
            <CheckCircle2 className="size-12 text-emerald-600 mb-3" />
            <h2 className="text-base font-semibold text-emerald-950 mb-1">
              Đổi mật khẩu thành công!
            </h2>
            <p className="text-xs text-emerald-800 leading-relaxed">
              Mật khẩu tài khoản của bạn đã được cập nhật an toàn. Mọi phiên đăng
              nhập cũ đã được chấm dứt.
            </p>
          </div>

          <Button
            type="button"
            className={cn(
              "h-[52px] w-full rounded-full",
              "bg-green-600",
              "text-[0.92rem] font-semibold text-white",
              "shadow-lg shadow-green-200",
              "hover:bg-green-700",
              "transition-all duration-200",
              "focus-visible:border-white",
              "focus-visible:ring-green-300/50"
            )}
            onClick={() => navigate("/login")}
          >
            Đăng nhập ngay
          </Button>
        </div>
      ) : (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="flex flex-col gap-3.5"
          noValidate
        >
          {/* Mật khẩu mới */}
          <div className="flex flex-col gap-1.5">
            <Label
              htmlFor="newPassword"
              className="text-xs font-semibold uppercase tracking-wider text-stone-600 px-1"
            >
              Mật khẩu mới
            </Label>
            <div className={inputShellClass(Boolean(errors.newPassword))}>
              <LockKeyhole className={inputIconClass} aria-hidden="true" />
              <Input
                id="newPassword"
                type={showNewPassword ? "text" : "password"}
                autoComplete="new-password"
                aria-invalid={Boolean(errors.newPassword)}
                className={inputClass}
                placeholder="Nhập mật khẩu mới"
                disabled={isSubmitting}
                {...register("newPassword")}
              />
              <button
                type="button"
                className={cn(
                  "mr-[5px] grid size-[42px]",
                  "shrink-0 cursor-pointer place-items-center",
                  "rounded-full border-0 bg-transparent",
                  "text-stone-400 hover:text-emerald-600",
                  "focus-visible:outline-2",
                  "focus-visible:outline-offset-2",
                  "focus-visible:outline-emerald-400"
                )}
                onClick={() => setShowNewPassword((prev) => !prev)}
                tabIndex={-1}
                aria-label={showNewPassword ? "Ẩn mật khẩu" : "Hiển thị mật khẩu"}
              >
                {showNewPassword ? (
                  <EyeOff className="size-[17px]" aria-hidden="true" />
                ) : (
                  <Eye className="size-[17px]" aria-hidden="true" />
                )}
              </button>
            </div>
            {errors.newPassword && (
              <p className="mx-4 text-xs text-red-500" role="alert">
                {errors.newPassword.message}
              </p>
            )}
          </div>

          {/* Checklist độ mạnh mật khẩu */}
          <div className="rounded-2xl border border-emerald-200/50 bg-emerald-50/40 p-3.5 text-[11px] space-y-1.5 text-stone-600">
            <div className="font-semibold text-stone-700">
              Yêu cầu độ mạnh mật khẩu:
            </div>
            <div className="grid grid-cols-2 gap-1.5 pt-0.5">
              <span
                className={cn(
                  "flex items-center gap-1.5",
                  hasMinLength
                    ? "text-emerald-700 font-medium"
                    : "text-stone-400"
                )}
              >
                <span className="font-bold">{hasMinLength ? "✓" : "○"}</span> 8–50 ký tự
              </span>
              <span
                className={cn(
                  "flex items-center gap-1.5",
                  hasUppercase
                    ? "text-emerald-700 font-medium"
                    : "text-stone-400"
                )}
              >
                <span className="font-bold">{hasUppercase ? "✓" : "○"}</span> Chữ in hoa (A-Z)
              </span>
              <span
                className={cn(
                  "flex items-center gap-1.5",
                  hasLowercase
                    ? "text-emerald-700 font-medium"
                    : "text-stone-400"
                )}
              >
                <span className="font-bold">{hasLowercase ? "✓" : "○"}</span> Chữ thường (a-z)
              </span>
              <span
                className={cn(
                  "flex items-center gap-1.5",
                  hasNumber
                    ? "text-emerald-700 font-medium"
                    : "text-stone-400"
                )}
              >
                <span className="font-bold">{hasNumber ? "✓" : "○"}</span> Chữ số (0-9)
              </span>
              <span
                className={cn(
                  "flex items-center gap-1.5 col-span-2",
                  hasSpecial
                    ? "text-emerald-700 font-medium"
                    : "text-stone-400"
                )}
              >
                <span className="font-bold">{hasSpecial ? "✓" : "○"}</span> Ký tự đặc biệt (@, $, !, %, *, ?, &, ...)
              </span>
            </div>
          </div>

          {/* Xác nhận mật khẩu mới */}
          <div className="flex flex-col gap-1.5">
            <Label
              htmlFor="confirmPassword"
              className="text-xs font-semibold uppercase tracking-wider text-stone-600 px-1"
            >
              Xác nhận mật khẩu mới
            </Label>
            <div className={inputShellClass(Boolean(errors.confirmPassword))}>
              <KeyRound className={inputIconClass} aria-hidden="true" />
              <Input
                id="confirmPassword"
                type={showConfirmPassword ? "text" : "password"}
                autoComplete="new-password"
                aria-invalid={Boolean(errors.confirmPassword)}
                className={inputClass}
                placeholder="Nhập lại mật khẩu mới"
                disabled={isSubmitting}
                {...register("confirmPassword")}
              />
              <button
                type="button"
                className={cn(
                  "mr-[5px] grid size-[42px]",
                  "shrink-0 cursor-pointer place-items-center",
                  "rounded-full border-0 bg-transparent",
                  "text-stone-400 hover:text-emerald-600",
                  "focus-visible:outline-2",
                  "focus-visible:outline-offset-2",
                  "focus-visible:outline-emerald-400"
                )}
                onClick={() => setShowConfirmPassword((prev) => !prev)}
                tabIndex={-1}
                aria-label={
                  showConfirmPassword ? "Ẩn mật khẩu" : "Hiển thị mật khẩu"
                }
              >
                {showConfirmPassword ? (
                  <EyeOff className="size-[17px]" aria-hidden="true" />
                ) : (
                  <Eye className="size-[17px]" aria-hidden="true" />
                )}
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
              "mt-2 h-[52px] w-full rounded-full",
              "bg-green-600",
              "text-[0.92rem] font-semibold text-white",
              "shadow-lg shadow-green-200",
              "hover:bg-green-700",
              "transition-all duration-200",
              "focus-visible:border-white",
              "focus-visible:ring-green-300/50"
            )}
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <>
                <LoaderCircle
                  className="mr-2 animate-spin"
                  aria-hidden="true"
                />
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
              className="inline-flex items-center gap-1.5 text-xs font-medium text-emerald-700 hover:text-emerald-800 hover:underline transition-colors"
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
