import type { ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";

import { MainLayout } from "@/components/layout/MainLayout";
import {
  AUTHENTICATED_ROLE_CODES,
  ROLE_ACCESS,
  hasAnyRole,
  type AuthenticatedRoleCode,
} from "@/config/roleAccess";
import { useAuth } from "@/hooks/useAuth";
import LoginPage from "@/pages/auth/LoginPage";
import { DashboardPage } from "@/pages/daskboard/DashboardPase";
import { CreateFarmAreaPage } from "@/pages/farm-area/CreateFarmAreaPage";
import CreateFarmLogPage from "@/pages/farm-log/CreateFarmLogPage";
import ProductionLotEditPage from "@/pages/farm/ProductionLotEditPage";
import { CreateOrganizationPage } from "@/pages/organization/CreateOrganizationPage";
import MemberPermissionsPage from "@/pages/organization/MemberPermissionsPage";
import OrganizationProfilePage from "@/pages/organization/OrganizationProfilePage";
import CreateProductionLotPage from "@/pages/production-lot/CreateProductionLotPage";
import ProductionLotListPage from "@/pages/production-lot/ProductionLotListPage";
import RecordTransportEventPage from "@/pages/transport-event/RecordTransportEventPage";

import CreateCodeRangePage from "@/pages/admin/CreateCodeRangePage";
import CodeRangeListPage from "@/pages/admin/CodeRangeListPage";
import CreatePackagingEventPage from "@/pages/packaging-event/CreatePackagingEventPage";
import CorrectPackagingEventPage from "@/pages/packaging-event/CorrectPackagingEventPage";
import { OrganizationListPage } from "@/pages/organization/OrganizationListPage";
import CreateMemberPage from "@/pages/organization/CreateMemberPage";
import FarmLogHistoryPage from "@/pages/farm-log/FarmLogHistoryPage";
import { ProductionLotDetailPage } from "@/pages/shipment/ProductionLotDetailPage";
import TraceLookupPage from "@/pages/public/TraceLookupPage";
import LookupStatisticsPage from "@/pages/report/LookupStatisticsPage";

const COOPERATIVE_MANAGER_ROLES = [
  "VT-02",
] as const satisfies readonly AuthenticatedRoleCode[];

function PageLoader() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background text-sm text-muted-foreground">
      Đang tải...
    </div>
  );
}

function PrivateRoute({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth();

  if (isLoading) return <PageLoader />;
  if (!user) return <Navigate to="/login" replace />;

  // VT-06 chỉ tra cứu công khai, không sử dụng khu vực quản trị nội bộ.
  if (!hasAnyRole(user.roleCode, AUTHENTICATED_ROLE_CODES)) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

interface RoleRouteProps {
  children: ReactNode;
  allowedRoles: readonly AuthenticatedRoleCode[];
}

function RoleRoute({ children, allowedRoles }: RoleRouteProps) {
  const { user, isLoading } = useAuth();

  if (isLoading) return <PageLoader />;
  if (!user) return <Navigate to="/login" replace />;

  if (!hasAnyRole(user.roleCode, allowedRoles)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
}

function UnauthorizedPage() {
  return (
    <main className="grid min-h-[60vh] place-items-center p-6 text-center">
      <div>
        <h1 className="text-2xl font-bold">Bạn không có quyền truy cập</h1>
        <p className="mt-2 text-muted-foreground">
          Tài khoản hiện tại không được cấp quyền sử dụng chức năng này.
        </p>
      </div>
    </main>
  );
}

const AppRoutes = () => (
  <Routes>
    {/* Route công khai */}
    <Route path="/login" element={<LoginPage />} />

    {/* Toàn bộ route nội bộ dùng chung Header + Sidebar + Outlet */}
    <Route
      element={
        <PrivateRoute>
          <MainLayout />
        </PrivateRoute>
      }
    >
      {/* Dashboard thật */}
      <Route index element={<DashboardPage />} />

      {/* Hồ sơ tổ chức — VT-01, VT-02 */}
      <Route
        path="organizations/profile"
        element={
          <RoleRoute allowedRoles={ROLE_ACCESS.organizationProfile}>
            <OrganizationProfilePage />
          </RoleRoute>
        }
      />

      {/* Tạo tổ chức — VT-01 */}
      <Route
        path="organizations/create"
        element={
          <RoleRoute allowedRoles={ROLE_ACCESS.organizationCreate}>
            <CreateOrganizationPage />
          </RoleRoute>
        }
      />

      <Route
        path="organizations"
        element={
          <RoleRoute allowedRoles={ROLE_ACCESS.organizationList}>
            <OrganizationListPage />
          </RoleRoute>
        }
      />

      {/* Cấp quyền thành viên — VT-02 */}
      <Route
        path="members"
        element={
          <RoleRoute allowedRoles={["VT-02"]}>
            <MemberPermissionsPage />
          </RoleRoute>
        }
      />
      <Route
        path="members/create"
        element={
          <RoleRoute allowedRoles={["VT-02"]}>
            <CreateMemberPage />
          </RoleRoute>
        }
      />

      {/* Tạo vùng trồng — VT-02 */}
      <Route
        path="farm-areas/create"
        element={
          <RoleRoute allowedRoles={ROLE_ACCESS.farmAreaCreate}>
            <CreateFarmAreaPage />
          </RoleRoute>
        }
      />

      {/* Danh sách lô sản xuất — VT-01, VT-02, VT-03 */}
      <Route
        path="production-lots"
        element={
          <RoleRoute allowedRoles={ROLE_ACCESS.productionLotList}>
            <ProductionLotListPage />
          </RoleRoute>
        }
      />

      {/* Tạo lô sản xuất — VT-02 */}
      <Route
        path="production-lots/create"
        element={
          <RoleRoute allowedRoles={COOPERATIVE_MANAGER_ROLES}>
            <CreateProductionLotPage />
          </RoleRoute>
        }
      />

      {/* Chỉnh sửa lô sản xuất — VT-02 */}
      <Route
        path="production-lots/:id/edit"
        element={
          <RoleRoute allowedRoles={ROLE_ACCESS.productionLotEdit}>
            <ProductionLotEditPage />
          </RoleRoute>
        }
      />

      {/* Ghi nhật ký canh tác — VT-03 */}
      <Route
        path="farm-logs/create"
        element={
          <RoleRoute allowedRoles={ROLE_ACCESS.farmLogCreate}>
            <CreateFarmLogPage />
          </RoleRoute>
        }
      />

      {/* 👇 Các route quản lý dải mã — chỉ VT-01 (Admin) */}
      <Route
        path="admin/code-ranges"
        element={
          <RoleRoute allowedRoles={["VT-01"]}>
            <CodeRangeListPage />
          </RoleRoute>
        }
      />
      <Route
        path="admin/code-ranges/create"
        element={
          <RoleRoute allowedRoles={["VT-01"]}>
            <CreateCodeRangePage />
          </RoleRoute>
        }
      />

      {/* Lịch sử nhật ký canh tác — VT-02 */}
      <Route
        path="production-lots/:productionLotId/farm-logs"
        element={
          <RoleRoute allowedRoles={["VT-02"]}>
            <FarmLogHistoryPage />
          </RoleRoute>
        }
      />

      {/* Chi tiết lô sản xuất — chứa chức năng Lô hàng & Mã QR */}
      <Route
        path="production-lots/:id"
        element={
          <RoleRoute allowedRoles={["VT-01", "VT-02", "VT-03"]}>
            <ProductionLotDetailPage />
          </RoleRoute>
        }
      />

      {/* Ghi và đính chính sự kiện đóng gói — VT-02, VT-03 */}
      <Route
        path="packaging-events/create"
        element={
          <RoleRoute allowedRoles={ROLE_ACCESS.packagingEventCreate}>
            <CreatePackagingEventPage />
          </RoleRoute>
        }
      />

      <Route
        path="packaging-events/:id/correct"
        element={
          <RoleRoute allowedRoles={ROLE_ACCESS.packagingEventCorrect}>
            <CorrectPackagingEventPage />
          </RoleRoute>
        }
      />

      {/* Ghi sự kiện vận chuyển — chỉ VT-03 */}
      <Route
        path="transport-events/record"
        element={
          <RoleRoute allowedRoles={ROLE_ACCESS.transportEventRecord}>
            <RecordTransportEventPage />
          </RoleRoute>
        }
      />

      {/* Thống kê tra cứu — VT-01, VT-02 */}
      <Route
        path="reports/lookup-statistics"
        element={
          <RoleRoute allowedRoles={["VT-01", "VT-02"]}>
            <LookupStatisticsPage />
          </RoleRoute>
        }
      />

      {/* Trang báo không đủ quyền vẫn nằm trong layout */}
      <Route path="unauthorized" element={<UnauthorizedPage />} />
    </Route>

    {/* Route công khai tra cứu */}
    <Route path="/public/trace/:codeValue" element={<TraceLookupPage />} />

    {/* Route không tồn tại */}
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
);

export default AppRoutes;
