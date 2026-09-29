import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, CheckCircle2, ShieldCheck, Sparkles } from "lucide-react";
import { LoginForm } from "../../components/auth/LoginForm";
import { Logo } from "@/components/common/Logo";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/useAuth";
import loginBanner from "@/assets/login-banner.jpg";

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading && user) {
      navigate("/dashboard", { replace: true });
    }
  }, [isLoading, navigate, user]);

  return (
    <div className="relative flex min-h-screen w-full flex-col lg:flex-row overflow-hidden bg-slate-50">
      {/* ============================================================
          LEFT HERO BANNER: Showcasing Smart Agriculture & Traceability
          ============================================================ */}
      <div className="relative hidden lg:flex lg:w-[58%] xl:w-[60%] flex-col justify-between overflow-hidden p-10 xl:p-14 text-white">
        {/* Photographic Background */}
        <img
          src={loginBanner}
          alt="Hệ thống truy xuất nguồn gốc nông sản Nguồn Gốc Số"
          className="absolute inset-0 h-full w-full object-cover object-center"
        />

        {/* Ambient Gradient Overlays for contrast & brand aesthetics */}
        <div className="absolute inset-0 bg-gradient-to-t from-emerald-950/90 via-emerald-950/40 to-black/30" />
        <div className="absolute inset-0 bg-emerald-900/15 mix-blend-multiply" />

        {/* Top Branding Pill */}
        <div className="relative z-10 flex items-center justify-between">
          <div className="inline-flex items-center gap-2 rounded-full border border-emerald-400/40 bg-emerald-950/60 px-4 py-1.5 text-xs font-medium text-emerald-200 backdrop-blur-md shadow-lg">
            <span className="flex h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Nền tảng Nông nghiệp số 4.0</span>
          </div>

          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-black/30 px-3.5 py-1 text-xs font-medium text-emerald-100 backdrop-blur-md">
              <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
              Chuẩn Quốc gia
            </span>
            <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-black/30 px-3.5 py-1 text-xs font-medium text-emerald-100 backdrop-blur-md">
              <Sparkles className="h-3.5 w-3.5 text-amber-300" />
              Tích hợp AI
            </span>
          </div>
        </div>

        {/* Bottom Headline & Highlights */}
        <div className="relative z-10 max-w-2xl space-y-4">
          <div className="space-y-2">
            <h1 className="text-3xl font-extrabold tracking-tight text-white drop-shadow-md xl:text-4xl leading-tight">
              Minh bạch nguồn gốc, <br />
              <span className="text-emerald-300">Vững niềm tin nông sản Việt</span>
            </h1>
            <p className="text-sm font-medium leading-relaxed text-emerald-100/95 drop-shadow xl:text-base">
              Số hóa quy trình canh tác, nhật ký nông vụ bằng trợ lý AI giọng nói và kết nối chuỗi cung ứng minh bạch từ nông trại đến bàn ăn.
            </p>
          </div>

          {/* Quick Feature Badges */}
          <div className="flex flex-wrap items-center gap-3 pt-2 text-xs font-semibold text-emerald-200">
            <div className="flex items-center gap-1.5 rounded-lg bg-emerald-950/60 border border-emerald-400/30 px-3 py-1.5 backdrop-blur-sm">
              <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
              <span>Chuẩn VietGAP / GlobalGAP</span>
            </div>
            <div className="flex items-center gap-1.5 rounded-lg bg-emerald-950/60 border border-emerald-400/30 px-3 py-1.5 backdrop-blur-sm">
              <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
              <span>Truy xuất QR tức thì</span>
            </div>
            <div className="flex items-center gap-1.5 rounded-lg bg-emerald-950/60 border border-emerald-400/30 px-3 py-1.5 backdrop-blur-sm">
              <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
              <span>Trợ lý nhập liệu giọng nói AI</span>
            </div>
          </div>
        </div>
      </div>

      {/* ============================================================
          RIGHT AUTH PANEL: Login Form Area
          ============================================================ */}
      <div className="relative flex flex-1 flex-col justify-between overflow-y-auto px-4 py-8 sm:px-8 lg:px-12 xl:px-16 bg-gradient-to-b from-emerald-50/70 via-white to-green-50/40">
        {/* Subtle decorative glow elements on the right */}
        <div className="pointer-events-none absolute -top-24 -right-24 h-96 w-96 rounded-full bg-emerald-200/30 blur-3xl" />
        <div className="pointer-events-none absolute bottom-0 right-1/4 h-80 w-80 rounded-full bg-lime-100/40 blur-3xl" />

        {/* Top Navigation */}
        <div className="relative z-10 flex items-center justify-between">
          <Button
            variant="outline"
            size="sm"
            className="gap-2 border-emerald-200 bg-white/80 text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800 shadow-sm backdrop-blur-sm"
            onClick={() => navigate("/")}
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Quay về trang chủ</span>
          </Button>

          <span className="text-xs text-muted-foreground hidden sm:inline-block">
            Phiên bản 2.4.0
          </span>
        </div>

        {/* Form Center Wrapper */}
        <main className="relative z-10 my-auto flex w-full max-w-[440px] flex-col items-center self-center py-6">
          {/* Brand Logo */}
          <div className="mb-6 flex flex-col items-center text-center">
            <Logo
              height={100}
              showText={false}
              className="w-auto max-w-[280px] drop-shadow-sm transition-transform hover:scale-105"
            />
            <p className="mt-2 text-xs font-medium text-emerald-800/80">
              Hệ thống Quản lý & Truy xuất Nguồn gốc Thực vật
            </p>
          </div>

          {/* Login Form Component */}
          <LoginForm />
        </main>

        {/* Footer */}
        <div className="relative z-10 text-center text-xs text-muted-foreground pt-4">
          © {new Date().getFullYear()} Nguồn Gốc Số. Nền tảng Nông nghiệp Công nghệ cao.
        </div>
      </div>
    </div>
  );
};

export default LoginPage;