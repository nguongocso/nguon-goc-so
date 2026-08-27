import React, { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, CheckCircle2, LoaderCircle, UserRound } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { forgotPassword } from "@/api/authApi";
import {
  forgotPasswordSchema,
  type ForgotPasswordFormValues,
} from "@/utils/validators";

const inputIconClass = "ml-[18px] size-[17px] shrink-0 text-emerald-600/70";

export const ForgotPasswordForm: React.FC = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      emailOrUsername: "",
    },
  });

  const onSubmit = async (data: ForgotPasswordFormValues) => {
    setIsLoading(true);
    try {
      await forgotPassword(data);
      setIsSubmitted(true);
      toast.success("Yêu cầu đã được tiếp nhận");
    } catch (error: any) {
      const msg =
        error.response?.data?.message ||
        "Có lỗi xảy ra khi gửi yêu cầu. Vui lòng thử lại.";
      toast.error(msg);
    } finally {
      setIsLoading(false);
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
      aria-labelledby="forgot-password-title"
    >
      <header className="mb-6 text-center">
        <h1
          id="forgot-password-title"
          className="text-2xl font-bold tracking-tight text-stone-900"
        >
          Quên mật khẩu
        </h1>
        <p className="mt-1.5 text-xs text-stone-500">
          Nhập tên đăng nhập hoặc email đã đăng ký của bạn.
        </p>
      </header>

      {isSubmitted ? (
        <div className="space-y-5">
          <div className="flex flex-col items-center justify-center p-6 text-center rounded-2xl bg-emerald-50/80 border border-emerald-200/80 shadow-inner">
            <CheckCircle2 className="size-12 text-emerald-600 mb-3" />
            <h2 className="text-base font-semibold text-emerald-950 mb-1">
              Đã tiếp nhận yêu cầu
            </h2>
            <p className="text-xs text-emerald-800 leading-relaxed">
              Nếu tài khoản tồn tại trên hệ thống, hướng dẫn đặt lại mật khẩu đã
              được gửi đến email của bạn.
            </p>
            <p className="mt-2 text-[11px] text-emerald-700">
              ⏳ Liên kết có hiệu lực trong vòng <strong>30 phút</strong>. Hãy kiểm
              tra cả hộp thư <strong>Spam / Rác</strong>.
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
              onClick={() => navigate("/login")}
            >
              Quay lại đăng nhập
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
              onClick={() => setIsSubmitted(false)}
            >
              Gửi lại yêu cầu khác
            </Button>
          </div>
        </div>
      ) : (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="flex flex-col gap-3.5"
          noValidate
        >
          {/* Email / Username Field */}
          <div className="flex flex-col gap-1.5">
            <Label
              htmlFor="emailOrUsername"
              className="text-xs font-semibold uppercase tracking-wider text-stone-600 px-1"
            >
              Tên đăng nhập hoặc Email
            </Label>
            <div
              className={inputShellClass(Boolean(errors.emailOrUsername))}
            >
              <UserRound className={inputIconClass} aria-hidden="true" />
              <Input
                id="emailOrUsername"
                type="text"
                autoComplete="username"
                autoFocus
                aria-invalid={Boolean(errors.emailOrUsername)}
                className={inputClass}
                placeholder="Ví dụ: nongdan01 hoặc email@domain.com"
                disabled={isLoading}
                {...register("emailOrUsername")}
              />
            </div>
            {errors.emailOrUsername && (
              <p className="mx-4 text-xs text-red-500" role="alert">
                {errors.emailOrUsername.message}
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
            disabled={isLoading}
          >
            {isLoading && (
              <LoaderCircle
                className="mr-2 animate-spin"
                aria-hidden="true"
              />
            )}
            {isLoading ? "Đang gửi yêu cầu..." : "Gửi liên kết đặt lại mật khẩu"}
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
