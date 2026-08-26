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
      // Ngay cả khi có lỗi mạng/lỗi khác, hiển thị thông báo an toàn
      const msg = error.response?.data?.message || "Có lỗi xảy ra khi gửi yêu cầu. Vui lòng thử lại.";
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  const inputClass = cn(
    "h-full w-full rounded-full border-0 bg-transparent",
    "px-3 text-[0.88rem] text-stone-800 placeholder:text-stone-400",
    "focus:outline-none focus:ring-0 focus-visible:ring-0",
    "disabled:cursor-not-allowed disabled:opacity-50"
  );

  return (
    <section
      className={cn(
        "w-full rounded-[28px] border border-white/60",
        "bg-white/80 p-8 shadow-2xl backdrop-blur-xl",
        "transition-all duration-300",
        "hover:shadow-emerald-900/10"
      )}
      aria-label="Khôi phục mật khẩu"
    >
      <header className="mb-7 text-center">
        <h1 className="text-2xl font-bold tracking-tight text-stone-900">
          Quên mật khẩu
        </h1>
        <p className="mt-1.5 text-xs text-stone-500">
          Nhập tên đăng nhập hoặc địa chỉ email đã đăng ký của bạn.
        </p>
      </header>

      {isSubmitted ? (
        <div className="space-y-6">
          <div className="flex flex-col items-center justify-center p-6 text-center rounded-2xl bg-emerald-50/80 border border-emerald-200">
            <CheckCircle2 className="size-12 text-emerald-600 mb-3" />
            <h2 className="text-base font-semibold text-emerald-950 mb-1">
              Đã tiếp nhận yêu cầu
            </h2>
            <p className="text-xs text-emerald-800 leading-relaxed">
              Nếu tài khoản tồn tại trên hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến email đã đăng ký của bạn.
            </p>
            <p className="mt-2 text-[11px] text-emerald-700">
              ⏳ Liên kết đặt lại mật khẩu có hiệu lực trong vòng <strong>30 phút</strong>. Hãy kiểm tra cả hộp thư <strong>Spam / Rác</strong>.
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
              onClick={() => navigate("/login")}
            >
              Quay lại đăng nhập
            </Button>
            <Button
              type="button"
              variant="outline"
              className="h-[44px] w-full rounded-full text-xs text-stone-600 hover:text-stone-900"
              onClick={() => setIsSubmitted(false)}
            >
              Gửi lại yêu cầu khác
            </Button>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
          <div className="space-y-1.5">
            <Label
              htmlFor="emailOrUsername"
              className="text-xs font-semibold uppercase tracking-wider text-stone-600"
            >
              Tên đăng nhập hoặc Email
            </Label>
            <div
              className={cn(
                "relative flex h-[50px] items-center rounded-full border border-stone-200/80",
                "bg-white shadow-inner transition-all duration-200",
                "focus-within:border-emerald-500 focus-within:ring-2 focus-within:ring-emerald-500/20",
                errors.emailOrUsername && "border-red-400 focus-within:border-red-400 focus-within:ring-red-400/20"
              )}
            >
              <UserRound className={inputIconClass} aria-hidden="true" />
              <Input
                id="emailOrUsername"
                type="text"
                autoComplete="username"
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
              "mt-2 h-[50px] w-full rounded-full",
              "bg-emerald-600 text-sm font-semibold text-white",
              "shadow-lg shadow-emerald-200 hover:bg-emerald-700",
              "transition-all duration-200"
            )}
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <LoaderCircle className="mr-2 h-4 w-4 animate-spin" />
                Đang gửi yêu cầu...
              </>
            ) : (
              "Gửi liên kết đặt lại mật khẩu"
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
