import React, { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { ArrowRight, MailWarning, X } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { hasAnyRole, ROLE_ACCESS } from "@/config/roleAccess";

export const MissingEmailBanner: React.FC = () => {
  const { user } = useAuth();
  const location = useLocation();
  const [dismissed, setDismissed] = useState(false);

  // Không hiển thị nếu chưa đăng nhập hoặc đang ở trang Hồ sơ
  if (!user || location.pathname === "/profile") {
    return null;
  }

  // Chỉ hiển thị cho các vai trò có quyền truy cập trang Hồ sơ người dùng
  const canAccessProfile = hasAnyRole(user.roleCode, ROLE_ACCESS.userProfile);
  if (!canAccessProfile) {
    return null;
  }

  // Nếu tài khoản đã có email thì không hiển thị
  const hasEmail = Boolean(user.email && user.email.trim().length > 0);
  if (hasEmail) {
    return null;
  }

  // Kiểm tra nếu người dùng đã bấm tắt trong phiên làm việc này
  const sessionKey = `dismiss_email_warning_${user.userId}`;
  const isSessionDismissed = sessionStorage.getItem(sessionKey) === "true";
  if (dismissed || isSessionDismissed) {
    return null;
  }

  const handleDismiss = () => {
    sessionStorage.setItem(sessionKey, "true");
    setDismissed(true);
  };

  return (
    <aside
      aria-label="Cảnh báo cập nhật email bảo mật"
      className="mb-4 relative overflow-hidden rounded-xl border border-amber-200 bg-gradient-to-r from-amber-50 via-orange-50/70 to-amber-50 p-4 shadow-sm"
    >
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-start gap-3">
          <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-amber-500/15 text-amber-700">
            <MailWarning className="size-5" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-amber-900">
              Tài khoản chưa được thiết lập địa chỉ email
            </h2>
            <p className="text-xs text-amber-800/90 mt-0.5 leading-relaxed">
              Vui lòng cập nhật email của bạn để có thể nhận liên kết đặt lại mật khẩu khi cần lấy lại tài khoản.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
          <Link
            to="/profile"
            className="inline-flex items-center gap-1.5 rounded-lg bg-amber-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition-colors hover:bg-amber-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amber-500"
          >
            Thêm email ngay
            <ArrowRight className="size-3.5" />
          </Link>
          <button
            type="button"
            onClick={handleDismiss}
            className="rounded-lg p-1.5 text-amber-700/70 hover:bg-amber-200/50 hover:text-amber-900 transition-colors"
            title="Bỏ qua nhắc nhở trong phiên này"
          >
            <X className="size-4" />
          </button>
        </div>
      </div>
    </aside>
  );
};
