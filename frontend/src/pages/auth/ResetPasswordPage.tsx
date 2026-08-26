import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { ResetPasswordForm } from "@/components/auth/ResetPasswordForm";
import { PublicBackground } from "@/components/layout/PublicBackground";
import { Logo } from "@/components/common/Logo";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/useAuth";

export const ResetPasswordPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading && user) {
      navigate("/dashboard", { replace: true });
    }
  }, [isLoading, navigate, user]);

  return (
    <PublicBackground>
      <Button
        variant="outline"
        className="absolute left-4 top-4 z-10 gap-2 border-emerald-300 text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800"
        onClick={() => navigate("/login")}
      >
        <ArrowLeft className="h-4 w-4" />
        Đăng nhập
      </Button>

      <main className="flex w-full max-w-[450px] flex-col items-center z-10 my-auto px-4">
        {/* Brand Logo */}
        <div className="mb-8 flex justify-center">
          <Logo
            height={150}
            showText={false}
            className="w-auto max-w-[360px]"
          />
        </div>

        <ResetPasswordForm />
      </main>
    </PublicBackground>
  );
};

export default ResetPasswordPage;
