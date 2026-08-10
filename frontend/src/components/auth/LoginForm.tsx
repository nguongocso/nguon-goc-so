import React, { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Eye,
  EyeOff,
  LoaderCircle,
  LockKeyhole,
  UserRound,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { useAuth } from "@/hooks/useAuth";

import type { LoginRequest } from "@/types/auth";

import {
  loginSchema,
  type LoginFormValues,
} from "../../utils/validators";

import {
  login,
  getOrganizations,
  selectOrganization,
} from "../../api/authApi";

import {
  removeSelectionToken,
} from "@/utils/storage";

const inputIconClass =
  "ml-[18px] size-[17px] shrink-0 text-emerald-600/70";

export const LoginForm: React.FC = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const navigate = useNavigate();
  const {
    loginWithSelection,
    completeLogin,
    logout,
  } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  /**
   * ========================================
   * LOGIN FLOW
   * ========================================
   *
   * BƯỚC 1:
   * Username + password
   *
   * Backend trả về:
   * ORG_SELECTION JWT
   *
   * BƯỚC 2:
   * Lưu ORG_SELECTION JWT
   *
   * BƯỚC 3:
   * GET /auth/organizations
   *
   * Request này sử dụng ORG_SELECTION JWT.
   *
   * BƯỚC 4:
   * Chuyển sang /select-organization
   *
   * ========================================
   *
   * LƯU Ý:
   *
   * Login KHÔNG nhận ACCESS JWT.
   *
   * ACCESS JWT chỉ được cấp sau khi:
   *
   * POST /auth/select-organization
   *
   * ========================================
   */
  const onSubmit = async (data: LoginFormValues) => {
    setIsLoading(true);

    try {
      /**
       * ========================================
       * DỌN TOKEN CŨ
       * ========================================
       *
       * Tránh trường hợp browser còn ACCESS JWT
       * hoặc ORG_SELECTION JWT của phiên trước.
       */
      logout();

      /**
       * ========================================
       * BƯỚC 1
       * USERNAME + PASSWORD
       * ========================================
       */
      const loginData: LoginRequest = {
        username: data.username,
        password: data.password,
      };

      const response = await login(loginData);

      if (!response.success) {
        throw new Error(
          response.message || "Đăng nhập thất bại."
        );
      }

      /**
       * ========================================
       * LẤY ORG_SELECTION TOKEN
       * ========================================
       */
      const selectionToken =
        response.data?.selectionToken;

      if (!selectionToken) {
        throw new Error(
          "Backend không trả về ORG_SELECTION token."
        );
      }

      /**
       * ========================================
       * BƯỚC 2
       * LƯU ORG_SELECTION TOKEN
       * ========================================
       *
       * Không lưu accessToken ở đây.
       *
       * Backend chưa cấp accessToken.
       */
      loginWithSelection(
        selectionToken,
        response.data.user
      );

      /**
       * ========================================
       * BƯỚC 3
       * LẤY DANH SÁCH ORGANIZATION
       * ========================================
       *
       * axiosConfig sẽ tự động lấy:
       *
       * getSelectionToken()
       *
       * rồi gửi:
       *
       * Authorization:
       * Bearer <ORG_SELECTION_JWT>
       */
      const organizationResponse =
        await getOrganizations();

      if (!organizationResponse.success) {
        throw new Error(
          organizationResponse.message ||
            "Không thể lấy danh sách tổ chức."
        );
      }

      const organizations =
        organizationResponse.data ?? [];

      /**
       * ========================================
       * KHÔNG CÓ ORGANIZATION
       * ========================================
       */
      if (organizations.length === 0) {
        removeSelectionToken();

        toast.error(
          "Tài khoản chưa được gán vào tổ chức nào."
        );

        return;
      }

      /**
       * Khi user chỉ thuộc một organization, hoàn tất giống
       * flow đăng nhập một bước của phiên bản cũ.
       */
      if (organizations.length === 1) {
        const organizationResponse =
          await selectOrganization({
            organizationId: organizations[0].organizationId,
          });

        if (!organizationResponse.success) {
          throw new Error(
            organizationResponse.message ||
              "Không thể chọn tổ chức."
          );
        }

        const {
          accessToken,
          user,
        } = organizationResponse.data;

        completeLogin(accessToken, user);

        toast.success("Đăng nhập thành công!");

        navigate("/dashboard", {
          replace: true,
        });

        return;
      }

      /**
       * ========================================
       * BƯỚC 4
       * CHUYỂN SANG TRANG CHỌN ORGANIZATION
       * ========================================
       *
       * Chưa có ACCESS JWT ở bước này.
       */
      navigate("/select-organization", {
        replace: true,
      });
    } catch (error: any) {
      console.error("Login error:", error);

      /**
       * Nếu flow ORG_SELECTION thất bại,
       * xóa token tạm thời.
       */
      removeSelectionToken();
      logout();

      const message =
        error?.response?.data?.message ||
        error?.message ||
        "Đăng nhập thất bại. Vui lòng thử lại.";

      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * ========================================
   * INPUT SHELL
   * ========================================
   */
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
      hasError &&
        "border-red-300/80 ring-3 ring-red-100/40"
    );

  /**
   * ========================================
   * INPUT CLASS
   * ========================================
   */
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
      aria-labelledby="login-form-title"
    >
      <h1
        id="login-form-title"
        className="sr-only"
      >
        Đăng nhập
      </h1>

      <form
        className="flex flex-col gap-3.5"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        {/* =========================
            USERNAME
        ========================== */}
        <div className="flex flex-col gap-1.5">
          <Label
            className="sr-only"
            htmlFor="username"
          >
            Tên đăng nhập
          </Label>

          <div
            className={inputShellClass(
              Boolean(errors.username)
            )}
          >
            <UserRound
              className={inputIconClass}
              aria-hidden="true"
            />

            <Input
              id="username"
              autoComplete="username"
              autoFocus
              aria-invalid={Boolean(
                errors.username
              )}
              className={inputClass}
              placeholder="Tên đăng nhập"
              {...register("username")}
            />
          </div>

          {errors.username && (
            <p
              className="mx-4 text-xs text-red-500"
              role="alert"
            >
              {errors.username.message}
            </p>
          )}
        </div>

        {/* =========================
            PASSWORD
        ========================== */}
        <div className="flex flex-col gap-1.5">
          <Label
            className="sr-only"
            htmlFor="password"
          >
            Mật khẩu
          </Label>

          <div
            className={inputShellClass(
              Boolean(errors.password)
            )}
          >
            <LockKeyhole
              className={inputIconClass}
              aria-hidden="true"
            />

            <Input
              id="password"
              type={
                showPassword
                  ? "text"
                  : "password"
              }
              autoComplete="current-password"
              aria-invalid={Boolean(
                errors.password
              )}
              className={inputClass}
              placeholder="Mật khẩu"
              {...register("password")}
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
              onClick={() =>
                setShowPassword(
                  (current) => !current
                )
              }
              aria-label={
                showPassword
                  ? "Ẩn mật khẩu"
                  : "Hiển thị mật khẩu"
              }
              title={
                showPassword
                  ? "Ẩn mật khẩu"
                  : "Hiển thị mật khẩu"
              }
            >
              {showPassword ? (
                <EyeOff
                  className="size-[17px]"
                  aria-hidden="true"
                />
              ) : (
                <Eye
                  className="size-[17px]"
                  aria-hidden="true"
                />
              )}
            </button>
          </div>

          {errors.password && (
            <p
              className="mx-4 text-xs text-red-500"
              role="alert"
            >
              {errors.password.message}
            </p>
          )}
        </div>

        {/* =========================
            LOGIN BUTTON
        ========================== */}
        <Button
          type="submit"
          className={cn(
            "mt-0.5 h-[52px] w-full rounded-full",
            "bg-emerald-600",
            "text-[0.92rem] font-semibold text-white",
            "shadow-lg shadow-emerald-200",
            "hover:bg-emerald-700",
            "transition-all duration-200",
            "focus-visible:border-white",
            "focus-visible:ring-emerald-300/50"
          )}
          disabled={isLoading}
        >
          {isLoading && (
            <LoaderCircle
              className="mr-2 animate-spin"
              aria-hidden="true"
            />
          )}

          {isLoading
            ? "Đang xác thực..."
            : "Đăng nhập"}
        </Button>
      </form>

      <p className="mt-6 text-center text-[0.72rem] text-stone-400">
        Bảo mật & minh bạch – Truy xuất nguồn gốc
        thực vật
      </p>
    </section>
  );
};