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

// ===== Auth =====
import LoginPage from "@/pages/auth/LoginPage";
import OrganizationSelectionPage from "@/pages/auth/OrganizationSelectionPage";

// ===== Pages – chung =====
import { DashboardPage } from "@/pages/daskboard/DashboardPase";
import { CreateFarmAreaPage } from "@/pages/farm-area/CreateFarmAreaPage";
import { EditFarmAreaPage } from "@/pages/farm-area/EditFarmAreaPage";
import CreateFarmLogPage from "@/pages/farm-log/CreateFarmLogPage";
import ProductionLotEditPage from "@/pages/farm/ProductionLotEditPage";

import { CreateOrganizationPage } from "@/pages/organization/CreateOrganizationPage";
import MemberPermissionsPage from "@/pages/organization/MemberPermissionsPage";
import OrganizationProfilePage from "@/pages/organization/OrganizationProfilePage";

import CreateProductionLotPage from "@/pages/production-lot/CreateProductionLotPage";
import ProductionLotListPage from "@/pages/production-lot/ProductionLotListPage";

import RecordTransportEventPage from "@/pages/transport-event/RecordTransportEventPage";

// ===== Admin =====
import CreateCodeRangePage from "@/pages/admin/CreateCodeRangePage";
import CodeRangeListPage from "@/pages/admin/CodeRangeListPage";
import ProductCategoryManagementPage from "@/pages/admin/ProductCategoryManagementPage";
import CreateProductCategoryPage from "@/pages/admin/CreateProductCategoryPage";
import EditProductCategoryPage from "@/pages/admin/EditProductCategoryPage";
import InputMaterialManagementPage from "@/pages/admin/InputMaterialManagementPage";
import { InputMaterialFormPage } from "@/pages/admin/InputMaterialFormPage";
import { InputMaterialDetailPage } from "@/pages/admin/InputMaterialDetailPage";
import StandardManagementPage from "@/pages/admin/StandardManagementPage";
import CreateStandardPage from "@/pages/admin/CreateStandardPage";
import EditStandardPage from "@/pages/admin/EditStandardPage";
import CriteriaManagementPage from "@/pages/admin/CriteriaManagementPage";
import SuspectTraceCodeListPage from "@/pages/admin/SuspectTraceCodeListPage";
import SuspectTraceCodeDetailPage from "@/pages/admin/SuspectTraceCodeDetailPage";

// ===== Packaging =====
import CreatePackagingEventPage from "@/pages/packaging-event/CreatePackagingEventPage";
import CorrectPackagingEventPage from "@/pages/packaging-event/CorrectPackagingEventPage";

// ===== Preprocessing =====
import CreatePreprocessingEventPage from "@/pages/preprocessing-event/CreatePreprocessingEventPage";
import CorrectPreprocessingEventPage from "@/pages/preprocessing-event/CorrectPreprocessingEventPage";

// ===== Organization =====
import { OrganizationListPage } from "@/pages/organization/OrganizationListPage";
import CreateMemberPage from "@/pages/organization/CreateMemberPage";

// ===== Farm logs =====
import FarmLogHistoryPage from "@/pages/farm-log/FarmLogHistoryPage";

// ===== Shipment =====
import { ProductionLotDetailPage } from "@/pages/public/shipment/ProductionLotDetailPage";
import { ShipmentDetailPage } from "@/pages/public/shipment/ShipmentDetailPage";
import CreateShipmentPage from "@/pages/shipment/CreateShipmentPage";
import LabelCancellationHistoryPage from "@/pages/shipment/LabelCancellationHistoryPage";
import CancelLabelsPage from "@/pages/shipment/CancelLabelsPage";

// ===== Public =====
import PublicHomePage from "@/pages/public/PublicHomePage";
import TraceLookupPage from "@/pages/public/TraceLookupPage";
import JoinOrganizationPage from "@/pages/public/JoinOrganizationPage";

// ===== Reports =====
import LookupStatisticsPage from "@/pages/report/LookupStatisticsPage";
import ActivityLogPage from "@/pages/report/ActivityLogPage";
import LoginHistoryPage from "@/pages/report/LoginHistoryPage";
import LoginAnomalyTrackingPage from "@/pages/report/LoginAnomalyTrackingPage";
import FailedEventLogsPage from "@/pages/report/FailedEventLogsPage";
import CropAreaAnalysisPage from "@/pages/report/CropAreaAnalysisPage";
import IndustryReportPage from "@/pages/report/IndustryReportPage";
import SeasonYieldComparisonPage from "@/pages/report/SeasonYieldComparisonPage";

// ===== Alerts =====
import ScanAnomalyAlertPage from "@/pages/scan-anomaly-alert/ScanAnomalyAlertPage";

// ===== Farm area =====
import FarmAreaListPage from "@/pages/farm-area/FarmAreaListPage";

// ===== Certification =====
import CreateCertificationPage from "@/pages/certification/CreateCertificationPage";
import CertificationListPage from "@/pages/certification/CertificationListPage";
import RecordInspectionResultPage from "@/pages/certification/RecordInspectionResultPage";
import CreateInspectionRequestPage from "@/pages/certification/CreateInspectionRequestPage";

// ===== Offline events =====
import OfflineEventPage from "@/pages/offline/OfflineEventPage";

// ===== Warehouse Receipt (NCL-05-CN-006) =====
import WarehouseReceiptPage from "@/pages/warehouse-receipt/WarehouseReceiptPage";
import WarehouseReceiptDetailPage from "@/pages/warehouse-receipt/WarehouseReceiptDetailPage";

// ===== Storage Condition (NCL-05-CN-007) =====
import StorageConditionPage from "@/pages/storage-condition/StorageConditionPage";
import EventChainVerificationPage from "@/pages/event-chain-verification/EventChainVerificationPage";

// ===== Notifications =====
import NotificationsPage from "@/pages/notification/NotificationsPage";

// ===== Export Open Data =====
import ExportOpenDataPage from "@/pages/export/ExportOpenDataPage";

// ===== Import Production Lot =====
import ImportProductionLotPage from "@/pages/production-lot/ImportProductionLotPage";

// ===== Permission Config =====
import RolePermissionConfigPage from "@/pages/permission/RolePermissionConfigPage";

// ===== Scan Quick Event =====
import ScanQuickEventPage from "@/pages/scan-anomaly-alert/components/ScanQuickEventPage";

// ===== Organization Detail =====
import OrganizationDetailPage from "@/pages/organization/OrganizationDetailPage";

// ===== Partner API Keys (NCL-12-CN-001) =====
import PartnerApiKeyListPage from "@/pages/apiKey/PartnerApiKeyListPage";
import CreatePartnerApiKeyPage from "@/pages/apiKey/CreatePartnerApiKeyPage";

// ===== Product Feedback =====
import ProductFeedbackManagementPage from "@/pages/product-feedback/ProductFeedbackManagementPage";

// ===== Mobile =====
import RecordMobileEventPage from "@/pages/mobile/RecordMobileEventPage";

// ===== Invitation =====
import CreateInvitationPage from "@/pages/invitation/CreateInvitationPage";

// ===== Backup Restore =====
import BackupRestorePage from "@/pages/admin/BackupRestorePage";

// ===== System Monitoring (NCL-10-CN-010) =====
import { SystemMonitoringPage } from "@/pages/admin/SystemMonitoringPage";

// ===== Recall requests (NCL-08-CN-008) =====
import { CreateRecallRequestPage } from "@/pages/recall-request/CreateRecallRequestPage";
import { RecallRequestListPage } from "@/pages/recall-request/RecallRequestListPage";
import { RecallRequestDetailPage } from "@/pages/recall-request/RecallRequestDetailPage";

// =====================================================
// Constants
// =====================================================

const COOPERATIVE_MANAGER_ROLES = [
    "VT-02",
] as const satisfies readonly AuthenticatedRoleCode[];


// =====================================================
// Helpers
// =====================================================

function PageLoader() {
    return (
        <div className="flex min-h-[60vh] items-center justify-center">
            <p className="text-sm text-muted-foreground">
                Đang tải...
            </p>
        </div>
    );
}


// =====================================================
// Private Route
// =====================================================

function PrivateRoute({
                          children,
                      }: {
    children: ReactNode;
}) {
    const { user, isLoading } = useAuth();

    if (isLoading) {
        return <PageLoader />;
    }

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    // VT-06 chỉ tra cứu công khai,
    // không sử dụng khu vực quản trị nội bộ.
    if (!hasAnyRole(user.roleCode, AUTHENTICATED_ROLE_CODES)) {
        return <Navigate to="/unauthorized" replace />;
    }

    return <>{children}</>;
}


// =====================================================
// Role Route
// =====================================================

interface RoleRouteProps {
    children: ReactNode;
    allowedRoles: readonly AuthenticatedRoleCode[];
}

function RoleRoute({
                       children,
                       allowedRoles,
                   }: RoleRouteProps) {
    const { user, isLoading } = useAuth();

    if (isLoading) {
        return <PageLoader />;
    }

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (!hasAnyRole(user.roleCode, allowedRoles)) {
        return <Navigate to="/unauthorized" replace />;
    }

    return <>{children}</>;
}


// =====================================================
// Unauthorized
// =====================================================

function UnauthorizedPage() {
    return (
        <div className="flex min-h-screen items-center justify-center">
            <div className="text-center">
                <h1 className="text-2xl font-bold">
                    Bạn không có quyền truy cập
                </h1>

                <p className="mt-2 text-muted-foreground">
                    Tài khoản hiện tại không được cấp quyền sử dụng
                    chức năng này.
                </p>
            </div>
        </div>
    );
}


// =====================================================
// Routes
// =====================================================

const AppRoutes = () => (
    <Routes>

        {/* =================================================
        PUBLIC / AUTH ROUTES
    ================================================= */}

        <Route
            path="/"
            element={<PublicHomePage />}
        />

        <Route
            path="/login"
            element={<LoginPage />}
        />

        {/*
      Bước trung gian sau khi login thành công.

      Lúc này:
      - Có selectionToken
      - Chưa có accessToken
      - Chưa có AuthUserInfo
      - Chưa được vào MainLayout
    */}
        <Route
            path="/select-organization"
            element={<OrganizationSelectionPage />}
        />

        <Route
            path="/public/trace/:codeValue"
            element={<TraceLookupPage />}
        />

        <Route
            path="/join"
            element={<JoinOrganizationPage />}
        />


        {/* =================================================
        PROTECTED ROUTES
    ================================================= */}

        <Route
            element={
                <PrivateRoute>
                    <MainLayout />
                </PrivateRoute>
            }
        >

            {/* Dashboard */}
            <Route
                path="/dashboard"
                element={<DashboardPage />}
            />


            {/* =================================================
          ORGANIZATION
      ================================================= */}

            <Route
                path="organizations/profile"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.organizationProfile}
                    >
                        <OrganizationProfilePage />
                    </RoleRoute>
                }
            />

            <Route
                path="organizations/create"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.organizationCreate}
                    >
                        <CreateOrganizationPage />
                    </RoleRoute>
                }
            />

            <Route
                path="organizations"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.organizationList}
                    >
                        <OrganizationListPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          MEMBERS
      ================================================= */}

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


            {/* =================================================
          FARM AREAS
      ================================================= */}

            <Route
                path="farm-areas/create"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.farmAreaCreate}
                    >
                        <CreateFarmAreaPage />
                    </RoleRoute>
                }
            />

            <Route
                path="farm-areas/:id/edit"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.farmAreaCreate}
                    >
                        <EditFarmAreaPage />
                    </RoleRoute>
                }
            />

            <Route
                path="chinhsuavungtrong/:id"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.farmAreaCreate}
                    >
                        <EditFarmAreaPage />
                    </RoleRoute>
                }
            />

            <Route
                path="farm-areas"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <FarmAreaListPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          PRODUCTION LOTS
      ================================================= */}

            <Route
                path="production-lots"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.productionLotList}
                    >
                        <ProductionLotListPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/create"
                element={
                    <RoleRoute
                        allowedRoles={COOPERATIVE_MANAGER_ROLES}
                    >
                        <CreateProductionLotPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/:id/edit"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.productionLotEdit}
                    >
                        <ProductionLotEditPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/:id"
                element={
                    <RoleRoute
                        allowedRoles={["VT-01", "VT-02", "VT-03"]}
                    >
                        <ProductionLotDetailPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/:productionLotId/shipments/create"
                element={
                    <RoleRoute
                        allowedRoles={["VT-01", "VT-02", "VT-03"]}
                    >
                        <CreateShipmentPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/:lotId/shipments/:shipmentId"
                element={
                    <RoleRoute
                        allowedRoles={["VT-01", "VT-02", "VT-03", "VT-04"]}
                    >
                        <ShipmentDetailPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/import"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <ImportProductionLotPage />
                    </RoleRoute>
                }
            />

            {/* =================================================
          SHIPMENT DETAIL (NCL-07-CN-002 / NCL-12-CN-003)
      ================================================= */}

            <Route
                path="shipments/:id"
                element={
                    <RoleRoute allowedRoles={["VT-02", "VT-03", "VT-04"]}>
                        <ShipmentDetailPage />
                    </RoleRoute>
                }
            />

            <Route
                path="shipments/:id/cancellation-history"
                element={
                    <RoleRoute allowedRoles={["VT-02", "VT-03", "VT-04"]}>
                        <LabelCancellationHistoryPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/:lotId/shipments/:id/cancellation-history"
                element={
                    <RoleRoute allowedRoles={["VT-02", "VT-03", "VT-04"]}>
                        <LabelCancellationHistoryPage />
                    </RoleRoute>
                }
            />

            <Route
                path="shipments/:id/cancel-labels"
                element={
                    <RoleRoute allowedRoles={["VT-02", "VT-03", "VT-04"]}>
                        <CancelLabelsPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/:lotId/shipments/:id/cancel-labels"
                element={
                    <RoleRoute allowedRoles={["VT-02", "VT-03", "VT-04"]}>
                        <CancelLabelsPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          FARM LOGS
      ================================================= */}

            <Route
                path="farm-logs/create"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.farmLogCreate}
                    >
                        <CreateFarmLogPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/:productionLotId/farm-logs"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <FarmLogHistoryPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          PREPROCESSING
      ================================================= */}

            <Route
                path="preprocessing-events/create"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.preprocessingEventCreate}
                    >
                        <CreatePreprocessingEventPage />
                    </RoleRoute>
                }
            />

            <Route
                path="preprocessing-events/:id/correct"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.preprocessingEventCorrect}
                    >
                        <CorrectPreprocessingEventPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          PACKAGING
      ================================================= */}

            <Route
                path="packaging-events/create"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.packagingEventCreate}
                    >
                        <CreatePackagingEventPage />
                    </RoleRoute>
                }
            />

            <Route
                path="packaging-events/:id/correct"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.packagingEventCorrect}
                    >
                        <CorrectPackagingEventPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          TRANSPORT
      ================================================= */}

            <Route
                path="transport-events/record"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.transportEventRecord}
                    >
                        <RecordTransportEventPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          SCAN QUICK EVENT
      ================================================= */}

            <Route
                path="chain-events/scan"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.scanQuickEvent}
                    >
                        <ScanQuickEventPage />
                    </RoleRoute>
                }
            />

            {/* =================================================
          OFFLINE EVENTS (NCL-10-CN-005 / NCL-10-CN-006)
      ================================================= */}

            <Route
                path="offline-events"
                element={
                    <RoleRoute allowedRoles={AUTHENTICATED_ROLE_CODES}>
                        <OfflineEventPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          ADMIN
      ================================================= */}

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

            <Route
                path="admin/product-categories"
                element={
                    <RoleRoute allowedRoles={["VT-01"]}>
                        <ProductCategoryManagementPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/product-categories/create"
                element={
                    <RoleRoute allowedRoles={["VT-01"]}>
                        <CreateProductCategoryPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/product-categories/:id/edit"
                element={
                    <RoleRoute allowedRoles={["VT-01"]}>
                        <EditProductCategoryPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/input-materials"
                element={
                    <RoleRoute allowedRoles={["VT-01", "VT-02", "VT-03", "VT-04"]}>
                        <InputMaterialManagementPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/input-materials/create"
                element={
                    <RoleRoute allowedRoles={["VT-01"]}>
                        <InputMaterialFormPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/input-materials/:id/edit"
                element={
                    <RoleRoute allowedRoles={["VT-01"]}>
                        <InputMaterialFormPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/input-materials/:id"
                element={
                    <RoleRoute allowedRoles={["VT-01", "VT-02", "VT-03", "VT-04"]}>
                        <InputMaterialDetailPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/standards"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.standardManagement}
                    >
                        <StandardManagementPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/standards/create"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.standardManagement}
                    >
                        <CreateStandardPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/standards/:id/edit"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.standardManagement}
                    >
                        <EditStandardPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/standards/:standardId/criteria"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.standardManagement}
                    >
                        <CriteriaManagementPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/backup-restore"
                element={
                    <RoleRoute allowedRoles={["VT-01"]}>
                        <BackupRestorePage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/system-monitoring"
                element={
                    <RoleRoute allowedRoles={["VT-01"]}>
                        <SystemMonitoringPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/suspect-trace-codes"
                element={
                    <RoleRoute allowedRoles={["VT-01"]}>
                        <SuspectTraceCodeListPage />
                    </RoleRoute>
                }
            />

            <Route
                path="admin/suspect-trace-codes/:traceCodeId"
                element={
                    <RoleRoute allowedRoles={["VT-01"]}>
                        <SuspectTraceCodeDetailPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          REPORTS
      ================================================= */}

            <Route
                path="reports/lookup-statistics"
                element={
                    <RoleRoute allowedRoles={["VT-01", "VT-02"]}>
                        <LookupStatisticsPage />
                    </RoleRoute>
                }
            />

            <Route
                path="activity-logs"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <ActivityLogPage />
                    </RoleRoute>
                }
            />

            <Route
                path="login-history"
                element={
                    <PrivateRoute>
                        <LoginHistoryPage />
                    </PrivateRoute>
                }
            />

            <Route
                path="login-anomalies"
                element={
                    <RoleRoute allowedRoles={["VT-01"]}>
                        <LoginAnomalyTrackingPage />
                    </RoleRoute>
                }
            />

            <Route
                path="failed-event-logs"
                element={
                    <RoleRoute allowedRoles={["VT-02", "VT-03"]}>
                        <FailedEventLogsPage />
                    </RoleRoute>
                }
            />

            <Route
                path="reports/crop-area-analysis"
                element={
                    <RoleRoute allowedRoles={["VT-01", "VT-05"]}>
                        <CropAreaAnalysisPage />
                    </RoleRoute>
                }
            />

            <Route
                path="reports/season-yield-comparison"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.seasonYieldComparison}
                    >
                        <SeasonYieldComparisonPage />
                    </RoleRoute>
                }
            />

            <Route
                path="reports/industry"
                element={
                    <RoleRoute allowedRoles={["VT-05"]}>
                        <IndustryReportPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          NOTIFICATIONS
      ================================================= */}

            <Route
                path="notifications"
                element={
                    <RoleRoute
                        allowedRoles={AUTHENTICATED_ROLE_CODES}
                    >
                        <NotificationsPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          ALERTS
      ================================================= */}

            <Route
                path="alerts/scan-anomaly"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.scanAnomalyAlerts}
                    >
                        <ScanAnomalyAlertPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          CERTIFICATION
      ================================================= */}

            <Route
                path="certifications"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <CertificationListPage />
                    </RoleRoute>
                }
            />

            <Route
                path="certifications/create"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <CreateCertificationPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/:lotId/inspection-requests/create"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <CreateInspectionRequestPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/:id/inspection-requests/create"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <CreateInspectionRequestPage />
                    </RoleRoute>
                }
            />

            <Route
                path="production-lots/:lotId/inspection-requests/:requestId/results"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <RecordInspectionResultPage />
                    </RoleRoute>
                }
            />

            <Route
                path="inspection-requests/:requestId/results"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <RecordInspectionResultPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          PARTNER API KEYS (NCL-12-CN-001)
      ================================================= */}

            <Route
                path="integration/api-keys"
                element={
                    <RoleRoute allowedRoles={ROLE_ACCESS.apiKeyManagement}>
                        <PartnerApiKeyListPage />
                    </RoleRoute>
                }
            />

            <Route
                path="integration/api-keys/create"
                element={
                    <RoleRoute allowedRoles={ROLE_ACCESS.apiKeyManagement}>
                        <CreatePartnerApiKeyPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          EXPORT OPEN DATA
      ================================================= */}

            <Route
                path="export/open-data"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.exportOpenData}
                    >
                        <ExportOpenDataPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          PERMISSION CONFIG
      ================================================= */}

            <Route
                path="permissions/config"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.rolePermissionConfig}
                    >
                        <RolePermissionConfigPage />
                    </RoleRoute>
                }
            />

            {/* =================================================
          WAREHOUSE RECEIPT (NCL-05-CN-006)
      ================================================= */}

            <Route
                path="warehouse-receipt"
                element={
                    <RoleRoute allowedRoles={ROLE_ACCESS.warehouseReceipt}>
                        <WarehouseReceiptPage />
                    </RoleRoute>
                }
            />

            <Route
                path="warehouse-receipt/:eventId"
                element={
                    <RoleRoute allowedRoles={ROLE_ACCESS.warehouseReceipt}>
                        <WarehouseReceiptDetailPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          STORAGE CONDITION (NCL-05-CN-007)
      ================================================= */}

            <Route
                path="storage-condition"
                element={
                    <RoleRoute allowedRoles={ROLE_ACCESS.storageCondition}>
                        <StorageConditionPage />
                    </RoleRoute>
                }
            />

            <Route
                path="event-chain-verification"
                element={
                    <RoleRoute allowedRoles={ROLE_ACCESS.eventChainVerification}>
                        <EventChainVerificationPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          OFFLINE EVENTS
      ================================================= */}

            <Route
                path="offline-events"
                element={
                    <RoleRoute allowedRoles={["VT-02", "VT-03"]}>
                        <OfflineEventPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          ORGANIZATION DETAIL
      ================================================= */}

            <Route
                path="organizations/:id"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.organizationList}
                    >
                        <OrganizationDetailPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          MOBILE EVENT
      ================================================= */}

            <Route
                path="mobile/record-event"
                element={
                    <RoleRoute allowedRoles={["VT-02", "VT-03"]}>
                        <RecordMobileEventPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          INVITATION
      ================================================= */}

            <Route
                path="invitations/create"
                element={
                    <RoleRoute allowedRoles={["VT-02"]}>
                        <CreateInvitationPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          RECALL REQUESTS (NCL-08-CN-008)
      ================================================= */}

            <Route
                path="recall-requests"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.recallRequestManage}
                    >
                        <RecallRequestListPage />
                    </RoleRoute>
                }
            />

            {/* NOTE: route /create must be declared before /:id to avoid capture */}
            <Route
                path="recall-requests/create"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.recallRequestCreate}
                    >
                        <CreateRecallRequestPage />
                    </RoleRoute>
                }
            />

            <Route
                path="recall-requests/:id"
                element={
                    <RoleRoute
                        allowedRoles={ROLE_ACCESS.recallRequestManage}
                    >
                        <RecallRequestDetailPage />
                    </RoleRoute>
                }
            />

            {/* =================================================
          PRODUCT FEEDBACK
      ================================================= */}

            <Route
                path="product-feedbacks"
                element={
                    <RoleRoute
                        allowedRoles={
                            ROLE_ACCESS.productFeedbackManagement
                        }
                    >
                        <ProductFeedbackManagementPage />
                    </RoleRoute>
                }
            />


            {/* =================================================
          UNAUTHORIZED
      ================================================= */}

            <Route
                path="unauthorized"
                element={<UnauthorizedPage />}
            />

        </Route>


        {/* =================================================
        FALLBACK
    ================================================= */}

        <Route
            path="*"
            element={<Navigate to="/" replace />}
        />

    </Routes>
);

export default AppRoutes;
