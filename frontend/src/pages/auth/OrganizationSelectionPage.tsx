import React, { useEffect, useState } from "react";
import { Building2, LoaderCircle, LogOut } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/useAuth";
import {
  getOrganizations,
  selectOrganization,
} from "@/api/authApi";

import type { OrganizationSelection } from "@/types/organization";

const OrganizationSelectionPage: React.FC = () => {
  const navigate = useNavigate();

  const {
    selectionToken,
    completeLogin,
    logout,
  } = useAuth();

  const [organizations, setOrganizations] = useState<
    OrganizationSelection[]
  >([]);

  const [isLoading, setIsLoading] = useState(true);
  const [isSelecting, setIsSelecting] = useState(false);

  /**
   * Bước 2:
   * Sau khi username/password đúng,
   * lấy danh sách organization bằng Selection JWT.
   */
  useEffect(() => {
    const loadOrganizations = async () => {
      if (!selectionToken) {
        navigate("/login", { replace: true });
        return;
      }

      try {
        setIsLoading(true);

        const response = await getOrganizations();

        if (!response.success) {
          throw new Error(
            response.message || "Không thể lấy danh sách tổ chức."
          );
        }

        setOrganizations(response.data);
      } catch (error: any) {
        const message =
          error.response?.data?.message ||
          error.message ||
          "Không thể tải danh sách tổ chức.";

        toast.error(message);

        logout();
        navigate("/login", { replace: true });
      } finally {
        setIsLoading(false);
      }
    };

    loadOrganizations();
  }, [selectionToken, navigate, logout]);

  /**
   * Bước 3:
   * User chọn organization
   * → Backend cấp Access JWT.
   */
  const handleSelectOrganization = async (
    organizationId: string
  ) => {
    if (!selectionToken) {
      toast.error("Phiên chọn tổ chức đã hết hạn.");
      navigate("/login", { replace: true });
      return;
    }

    try {
      setIsSelecting(true);

      const response = await selectOrganization({
        organizationId,
      });

      if (!response.success) {
        throw new Error(
          response.message || "Không thể chọn tổ chức."
        );
      }

      const { accessToken, user } = response.data;

      completeLogin(accessToken, user);

      toast.success("Đăng nhập thành công!");

      navigate("/dashboard", { replace: true });
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        error.message ||
        "Không thể chọn tổ chức.";

      toast.error(message);
    } finally {
      setIsSelecting(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-emerald-50">
        <div className="flex flex-col items-center gap-3">
          <LoaderCircle className="size-8 animate-spin text-emerald-600" />

          <p className="text-sm text-stone-600">
            Đang tải danh sách tổ chức...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-emerald-50 px-4">
      <section className="w-full max-w-[520px] rounded-[28px] border border-emerald-200/60 bg-white/90 p-8 shadow-xl">
        {/* Header */}
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex size-14 items-center justify-center rounded-full bg-emerald-100">
            <Building2 className="size-7 text-emerald-600" />
          </div>

          <h1 className="text-2xl font-semibold text-stone-800">
            Chọn tổ chức
          </h1>

          <p className="mt-2 text-sm text-stone-500">
            Tài khoản của bạn thuộc nhiều tổ chức.
            <br />
            Vui lòng chọn tổ chức để tiếp tục.
          </p>
        </div>

        {/* Organization list */}
        {organizations.length === 0 ? (
          <div className="rounded-2xl border border-stone-200 bg-stone-50 p-6 text-center">
            <p className="text-sm text-stone-600">
              Tài khoản chưa được gán vào tổ chức nào.
            </p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {organizations.map((organization) => (
              <button
                key={organization.organizationId}
                type="button"
                disabled={isSelecting}
                onClick={() =>
                  handleSelectOrganization(
                    organization.organizationId
                  )
                }
                className="group flex w-full items-center gap-4 rounded-2xl border border-emerald-100 bg-white p-4 text-left transition-all duration-200 hover:border-emerald-400 hover:bg-emerald-50 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
              >
                {/* Icon */}
                <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-emerald-100 transition-colors group-hover:bg-emerald-200">
                  <Building2 className="size-6 text-emerald-600" />
                </div>

                {/* Info */}
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold text-stone-800">
                    {organization.organizationName}
                  </p>

                  <p className="mt-1 text-xs text-stone-500">
                    Mã tổ chức:{" "}
                    <span className="font-medium text-stone-600">
                      {organization.organizationCode}
                    </span>
                  </p>

                  <div className="mt-1 flex gap-2 text-xs text-stone-500">
                    <span>
                      Vai trò: {organization.roleName}
                    </span>
                  </div>
                </div>

                {/* Organization type */}
                <div className="hidden shrink-0 rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700 sm:block">
                  {organization.organizationType}
                </div>
              </button>
            ))}
          </div>
        )}

        {/* Logout */}
        <div className="mt-6 border-t border-stone-100 pt-5">
          <Button
            type="button"
            variant="outline"
            className="w-full gap-2 rounded-full"
            onClick={handleLogout}
            disabled={isSelecting}
          >
            <LogOut className="size-4" />
            Đăng xuất
          </Button>
        </div>
      </section>
    </div>
  );
};

export default OrganizationSelectionPage;