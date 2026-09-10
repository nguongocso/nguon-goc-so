# Codebase Map — Nguồn Gốc Số

> **Mục đích:** Bản đồ code nhanh để tra cứu khi tạo/sửa chức năng. Không cần đọc lại toàn bộ source.
>
> **Quy ước đường dẫn trong file này:**
> - `BA/` = `backend/src/main/java/vn/nguongocso/`
> - `M/`  = `backend/src/main/resources/db/migration/`
> - `FE/` = `frontend/src/`

---

## 1. Kiến trúc & Layers (sơ đồ nguyên tắc)

### Backend (Spring Boot 3.5, Java 21, MySQL, Flyway, JPA)

```
Controller → Service/Impl → Repository → Entity → MySQL
     ↓            ↓
  @PreAuthorize  @Auditable / eventPublisher → ActivityLog
     ↓
  DTO (Request/Response)  ←  ApiResult<T> wrapper
     ↓
  GlobalExceptionHandler → Business / Resource / Duplicate Exception → tiếng Việt
```

- UUID PK: `CHAR(36)`, `@JdbcTypeCode(SqlTypes.CHAR)`, `UUID.randomUUID()` trong `@PrePersist`.
- **Không có** `BaseEntity` — mỗi entity tự quản ID + timestamps.
- `application.properties`: `ddl-auto=validate`, `flyway.out-of-order=true`.

### Frontend (React 19 + Vite 8 + TypeScript + Tailwind + shadcn)

```
API layer        → hooks (optional)  → Pages         → Routes
 src/api/XApi.ts    hooks/useX.ts      pages/X/*.tsx    routes/AppRoutes.tsx
     ↓                                                  ↕
 src/types/X.ts      src/utils/validators/            ROLE_ACCESS
                     (RHF + zod schemas)
     ↓
 src/components/common/   ← shared: ListPageHeader / ListCard / ListToolbar / DataTableShell / Pagination / StatusBadge / ...
 src/components/ui/       ← shadcn primitives (button, card, dialog, ...)
```

- Alias: `@` = `frontend/src/`.
- Error toasts: `sonner` `toast.error(...)`.
- Route guard: `<RoleRoute allowedRoles={ROLE_ACCESS.xxx}>`.

---

## 2. Quick Reference: Chức năng → Files liên quan

### 2.1 Auth & User Management (package `auth`)

| Lớp | File |
|---|---|
| **BE Controller** | `BA/auth/controller/AuthController.java` (login/forgot/reset/select-org) |
| | `BA/auth/controller/UserProfileController.java` (profile/change-password) |
| | `BA/auth/controller/RoleController.java` (roles CRUD) |
| | `BA/auth/controller/LoginMonitoringController.java` (login history/anomalies/lock) |
| **BE Service** | `BA/auth/service/AuthService.java` + `impl/AuthServiceImpl.java` |
| | `BA/auth/service/UserService.java` + `impl/UserServiceImpl.java` |
| | `BA/auth/service/PasswordResetService.java` + `impl/PasswordResetServiceImpl.java` |
| | `BA/auth/service/LoginMonitoringService.java` + `impl/LoginMonitoringServiceImpl.java` |
| | `BA/auth/service/LoginAnomalyDetectionService.java` + `impl/LoginAnomalyDetectionServiceImpl.java` |
| | `BA/auth/service/AccountLockService.java` + `impl/AccountLockServiceImpl.java` |
| | `BA/auth/service/CustomUserDetailsService.java` |
| **BE Entity** | `User`, `Role`, `PasswordResetToken`, `LoginAttempt`, `LoginAnomaly`, `AccountLock`, `SuspiciousCase` |
| **BE Security** | `BA/auth/security/SecurityUtils.java` (currentUser/orgId), `BA/config/JwtTokenProvider.java`, `BA/config/JwtAuthenticationFilter.java` |
| **FE Pages** | `pages/auth/LoginPage.tsx`, `pages/auth/ForgotPasswordPage.tsx`, `pages/auth/ResetPasswordPage.tsx`, `pages/auth/OrganizationSelectionPage.tsx` |
| **FE Components** | `components/auth/LoginForm.tsx`, `ForgotPasswordForm.tsx`, `ResetPasswordForm.tsx` |
| **FE API** | `api/authApi.ts` |
| **FE Types** | `types/auth.ts` |
| **FE Validators** | `utils/validators.ts` (login/forgot/reset/changePassword/userProfile schemas) |
| **FE Hooks** | `hooks/useAuth.ts` |
| **FE Context** | `contexts/AuthContext.tsx` |
| **Migrations** | Schema: `V1` (users/roles/...), `V30` (login_attempts/anomalies/locks), `V31` (suspicious_cases), `V38` (password_reset_tokens), `V20260831110000` (ALTER users profile) |
| | Data: `V14`–`V17` (roles/permissions/admin) |

### 2.2 Permission / Phân quyền (package `permission`)

| Lớp | File |
|---|---|
| **BE** | `permission/controller/OrganizationRolePermissionController.java`, `permission/service/PermissionChecker.java` (+ `impl`), `permission/service/OrganizationRolePermissionService.java` (+ `impl`) |
| | Entities: `Permission`, `RolePermission`, `OrganizationRolePermission` |
| **FE Pages** | `pages/permission/RolePermissionConfigPage.tsx` |
| **FE Components** | `components/permission/RolePermissionConfig.tsx`, `PermissionGroup.tsx` |
| **FE API** | `api/permissionApi.ts` |
| **FE Types** | `types/permission.ts` |
| **Config** | `config/roleAccess.ts` — `ROLE_ACCESS` object + `hasAnyRole()` |
| **Migration** | `V13` (role_permissions, organization_role_permissions) |

### 2.3 Organization & Members (package `organization`)

| Lớp | File |
|---|---|
| **BE Controllers** | `OrganizationController`, `OrganizationProfileController`, `OrganizationMemberController`, `InvitationController`, `AdministrativeUnitController`, `AreaAssignmentAdminController`, `OrganizationDivisionAdminController`, `MeAreaController` |
| **BE Service** | `OrganizationService`, `OrganizationMemberService`, `InvitationService`, `AdministrativeUnitService`, `AreaAssignmentService`, `AreaScopeService` (+ impls) |
| **BE Entity** | `Organization`, `OrganizationUser`, `Invitation`, `AdministrativeUnit`, `UserAreaAssignment` |
| **FE Pages** | `pages/organization/OrganizationListPage.tsx`, `OrganizationDetailPage.tsx`, `OrganizationProfilePage.tsx`, `CreateOrganizationPage.tsx`, `CreateMemberPage.tsx`, `MemberPermissionsPage.tsx` |
| **FE Components** | `components/organization/CreateOrganizationForm.tsx`, `CreateOrganizationMemberForm.tsx`, `MemberList.tsx`, `OrganizationDetail.tsx`, `AddExistingUserDialog.tsx`, `DeactivateMemberDialog.tsx`, `ReactivateMemberDialog.tsx` |
| **FE Hooks** | `hooks/useMemberStatusActions.ts`, `hooks/useAdministrativeUnits.ts` |
| **FE API** | `api/organizationApi.ts`, `api/memberApi.ts`, `api/invitationApi.ts`, `api/areaAssignmentApi.ts`, `api/administrativeUnitApi.ts` |
| **FE Types** | `types/organization.ts`, `types/member.ts`, `types/invitation.ts`, `types/administrativeUnit.ts` |
| **Migrations** | `V1` (organizations, organization_users, invitations), `V42` (administrative_units, user_area_assignments) |

### 2.4 Farm Areas (package `farm`)

| Lớp | File |
|---|---|
| **BE** | `farm/controller/FarmAreaController.java`, `farm/service/FarmAreaService.java` + `impl/FarmAreaServiceImpl.java`, `farm/entity/FarmArea.java`, `farm/repository/FarmAreaRepository.java` |
| **FE Pages** | `pages/farm-area/FarmAreaListPage.tsx`, `CreateFarmAreaPage.tsx`, `EditFarmAreaPage.tsx` |
| **FE Components** | `components/farm-area/CreateFarmAreaForm.tsx`, `EditFarmAreaForm.tsx`, `FarmAreaDeleteDialog.tsx` |
| **FE API** | `api/farmAreaApi.ts` |
| **FE Types** | `types/farmArea.ts` |
| **Validator** | `utils/validators.ts` (createFarmAreaSchema) |
| **Migration** | `V3` (farm_areas) |

### 2.5 Product Categories (package `farm`)

| Lớp | File |
|---|---|
| **BE** | `farm/controller/ProductCategoryController.java`, `farm/service/ProductCategoryService.java` + `impl/ProductCategoryServiceImpl.java`, `farm/entity/ProductCategory.java`, `farm/repository/ProductCategoryRepository.java` |
| **FE Pages** | `pages/admin/ProductCategoryManagementPage.tsx`, `CreateProductCategoryPage.tsx`, `EditProductCategoryPage.tsx` |
| **FE Components** | `components/admin/product-category/ProductCategoryList.tsx`, `ProductCategoryForm.tsx` |
| **FE API** | `api/productCategoryApi.ts` |
| **FE Types** | `types/productCategory.ts` |
| **Migrations** | `V2` (product_categories + `V19` ALTER storage thresholds) |

### 2.6 Input Materials (package `farm`)

| Lớp | File |
|---|---|
| **BE** | `farm/controller/InputMaterialController.java`, `farm/service/InputMaterialService.java` + `impl`, `farm/entity/InputMaterial.java`, `farm/repository/InputMaterialRepository.java` |
| **FE Pages** | `pages/admin/InputMaterialManagementPage.tsx`, `InputMaterialFormPage.tsx`, `InputMaterialDetailPage.tsx` |
| **FE Components** | `components/admin/input-material/InputMaterialFormModal.tsx`, `InputMaterialDeleteDialog.tsx`, `components/input-material/InputMaterialSelect.tsx` |
| **FE API** | `api/inputMaterialApi.ts` |
| **FE Types** | `types/inputMaterial.ts`, `types/inputMaterialCropType.ts` |
| **Migration** | `V44` (input_materials + input_material_crop_types) |

### 2.7 Production Lots (package `farm`)

| Lớp | File |
|---|---|
| **BE Controllers** | `farm/controller/ProductionLotController.java` (list/create/edit/approve/cancel), `farm/controller/ProductFeedbackController.java` |
| **BE Service** | `farm/service/ProductionLotService.java` + `impl/ProductionLotServiceImpl.java`, `farm/service/HarvestEligibilityService.java` + `impl` |
| **BE Entity** | `farm/entity/ProductionLot.java` (+ cancellation fields V62), `farm/entity/LotAssignment.java` |
| **FE Pages** | `pages/production-lot/ProductionLotListPage.tsx`, `CreateProductionLotPage.tsx`, `ImportProductionLotPage.tsx`, `pages/farm/ProductionLotEditPage.tsx` |
| **FE Components** | `components/production-lot/ProductionLotList.tsx`, `ProductionLotBoard.tsx`, `CreateProductionLotForm.tsx`, `ImportProductionLotForm.tsx`, `CancelProductionLotDialog.tsx`, `Approveproductionlotdialog.tsx` |
| **FE API** | `api/productionLotApi.ts` |
| **FE Types** | `types/productionLot.ts`, `types/productionLotImport.ts` |
| **Validators** | `utils/validators.ts` (updateProductionLotSchema, importProductionLotSchema) |
| **Migrations** | `V4` (production_lot), `V48` (lot_assignments), `V62` (cancellation fields) |

### 2.8 Farm Logs — Nhật ký canh tác (package `farm`)

| Lớp | File |
|---|---|
| **BE** | `farm/controller/FarmLogController.java`, `farm/controller/FarmLogAttachmentController.java`, `farm/service/FarmLogService.java` + `impl/FarmLogServiceImpl.java`, `farm/service/AttachmentService.java` |
| | `farm/entity/FarmLog.java`, `farm/entity/FarmLogAttachment.java` |
| **FE Pages** | `pages/farm-log/FarmLogHistoryPage.tsx`, `CreateFarmLogPage.tsx`, `FarmLogDetailPage.tsx`, `CorrectFarmLogPage.tsx`, `AttachmentManagementPage.tsx` |
| **FE Components** | `components/farm-log/FarmLogList.tsx`, `FarmLogTab.tsx`, `CreateFarmLogForm.tsx`, `AttachmentManager.tsx` |
| **FE API** | `api/farmLogApi.ts`, `api/attachmentApi.ts` |
| **FE Types** | `types/farmLog.ts`, `types/attachment.ts` |
| **FE Utils** | `utils/farmLogCorrection.ts` |
| **Migrations** | `V5` (farm_logs, farm_log_attachments + V41 ALTER correction fields) |

### 2.9 Chain Events — Chuỗi sự kiện canh tác (package `event`)

| Lớp | File |
|---|---|
| **BE Controllers** | `event/controller/ChainEventController.java` (preprocessing/packaging/transport/correction + **scan-lookup**), `event/controller/EventValidationController.java` |
| **BE Service** | `event/service/ChainEventService.java` + `impl/ChainEventServiceImpl.java` — **class lớn nhất** (~1200 dòng), chứa: `recordHarvestEvent`, `recordPreprocessingEvent`, `recordPackagingEvent` (+ milestone check), `recordTransportEvent`, `validateStorageProcurementRelationship`, `scanLookup`, correction logic |
| | `event/service/EventValidationService.java` + `impl/EventValidationServiceImpl.java` |
| **BE Entity** | `event/entity/ChainEvent.java`, `event/entity/FailedEventLog.java` |
| **BE DTO** | Request: `RecordHarvestEventRequest`, `RecordPreprocessingEventRequest`, `RecordPackagingEventRequest`, `RecordTransportEventRequest`, `CorrectPreprocessingEventRequest`, `CorrectPackagingEventRequest` |
| | Response: `ChainEventResponse`, `ScanLookupResponse` (+ `storageEligible`), `LotValidationResponse`, `FailedEventLogResponse` |
| **BE Enums** | `ChainEventType` (HARVEST/PREPROCESSING/PACKAGING/TRANSPORT/PROCUREMENT/WAREHOUSE_RECEIPT/STORAGE_CONDITION/CORRECTION) |
| **FE API** | `api/preprocessingApi.ts`, `api/packagingApi.ts`, `api/transportEventApi.ts`, `api/eventValidationApi.ts`, `api/traceEventApi.ts` |
| **FE Pages** | `pages/preprocessing-event/CreatePreprocessingEventPage.tsx`, `CorrectPreprocessingEventPage.tsx` |
| | `pages/packaging-event/CreatePackagingEventPage.tsx`, `CorrectPackagingEventPage.tsx` |
| | `pages/transport-event/RecordTransportEventPage.tsx` |
| | `pages/scan-anomaly-alert/components/ScanQuickEventPage.tsx` |
| **FE Components** | `pages/preprocessing-event/components/CreatePreprocessingForm.tsx`, `CorrectPreprocessingForm.tsx` |
| | `pages/packaging-event/components/CreatePackagingForm.tsx`, `FarmLogEligibilityAlert.tsx`, `LocationPicker.tsx` |
| | `pages/transport-event/components/TransportEventForm.tsx` |
| | `components/event-validation/ProcurementEventForm.tsx`, `LotValidationStatus.tsx` |
| **FE Types** | `types/preprocessing.ts`, `types/packaging.ts`, `types/transportEvent.ts`, `types/eventValidation.ts`, `types/scan.ts` |
| **FE Validators** | `utils/validators/preprocessingEventSchema.ts`, `packagingEventSchema.ts`, `transportEventSchema.ts` |
| **Migrations** | `V9` (chain_events + code_ranges), `V10` (failed_event_logs), `V11` (offline_sync_logs), `V20` (ALTER chain_events hash) |

### 2.10 Shipments — Lô hàng / Dán tem / Kích hoạt / Thu hồi (package `trace`)

| Lớp | File |
|---|---|
| **BE** | `trace/controller/ShipmentController.java` (+ `/activate`), `trace/controller/ShipmentRecallController.java` |
| | `trace/service/ShipmentService.java` + `impl/ShipmentServiceImpl.java` (+ `activateShipmentStamps`) |
| | `trace/service/ShipmentRecallService.java` + `impl/ShipmentRecallServiceImpl.java` |
| | `trace/service/QRCodeService.java` + `impl/QRCodeServiceImpl.java` |
| | `trace/entity/Shipment.java`, `trace/entity/TraceCode.java`, `trace/entity/Recall.java` |
| **FE API** | `api/shipmentApi.ts` |
| **FE Pages** | `pages/shipment/CreateShipmentPage.tsx`, `CancelLabelsPage.tsx`, `LabelCancellationHistoryPage.tsx`, `pages/public/shipment/ShipmentDetailPage.tsx`, `pages/public/shipment/ShipmentList.tsx` |
| **FE Components** | `components/shipment/CreateShipmentModal.tsx`, `ShipmentStatusBadge.tsx`, `ShipmentTimelineItem.tsx`, `ShipmentTimelineDialog.tsx`, `RecallShipmentDialog.tsx`, `ActivateShipmentDialog.tsx`, `QrCodeGrid.tsx`, `ExportLabelsDialog.tsx`, `DossierIneligibleDialog.tsx`, `ProcurementShipmentList.tsx` |
| **FE Hooks** | `hooks/useShipments.ts`, `useRecallShipment.ts`, `useDeleteDraftShipment.ts`, `useScanLookup.ts` |
| **FE Types** | `types/shipment.ts`, `types/scan.ts` |
| **Migrations** | `V6` (shipments, trace_codes, recalls), `V21` (suspect fields), `V32` (code_range), `V45` (label_cancellation), `V20260830130000` (unlock fields) |

### 2.11 Code Ranges — Quản lý khoảng mã (package `trace`)

| Lớp | File |
|---|---|
| **BE** | `trace/controller/CodeRangeController.java`, `trace/service/CodeRangeService.java` (+ `impl`), `trace/entity/CodeRange.java` |
| **FE Pages** | `pages/admin/CodeRangeListPage.tsx`, `CreateCodeRangePage.tsx` |
| **FE Components** | `components/admin/CreateCodeRangeForm.tsx` |
| **FE API** | `api/codeRangeApi.ts` |
| **FE Types** | `types/codeRange.ts` |
| **Migration** | `V9` (code_ranges) |

### 2.11.1 Code Range Supplement — Yêu cầu cấp bổ sung dải mã (NCL-04-CN-007, package `trace`)

| Lớp | File |
|---|---|
| **BE** | `trace/controller/CodeRangeSupplementController.java`, `trace/service/CodeRangeSupplementService.java` (+ `impl`), `trace/entity/CodeRangeSupplementRequest.java`, `trace/repository/CodeRangeSupplementRepository.java` (+ `GET /evidence-events` liệt kê sự kiện thu hoạch/sơ chế của tổ chức — VT-02 only; `CodeRangeSupplementResponse.evidenceEvents` resolve sẵn chi tiết từng ID để VT-01 xem khi duyệt) |
| **FE Pages (VT-01)** | `pages/admin/CodeRangeListPage.tsx` — chuẩn hóa đầy đủ bộ shared (2026-09-08): `useSetBreadcrumb` (Tổng quan → Quản lý dải mã truy xuất), `ListPageHeader` + `ListCard` + `ListToolbar` (SearchInput tìm theo tên tổ chức/tiền tố + FilterSelect trạng thái `OK/Gần hết/Đã hết` + RefreshButton), bảng auto-layout cột tự phân bổ, số `vi-VN` căn trái (`.` nghìn / `,` thập phân), tone màu cảnh báo đồng bộ giữa `% sử dụng` và badge Trạng thái (>80 danger / >50 warning / else success, badge bọc `flex justify-center`); phân trang client-side `PAGE_SIZE=10` clamp `safePage`; cột "Thao tác" → nút "Duyệt bổ sung" `variant="outline"` khi có yêu cầu chờ, `—` khi không; nút điều hướng tới trang chi tiết `/admin/code-range-supplements/:id` của yêu cầu PENDING mới nhất (không còn dialog inline); `CodeRangeSupplementListPage.tsx` giữ route `/admin/code-range-supplements` nhưng **đã ẩn khỏi Sidebar** |
| **FE Page (VT-02)** | `pages/shipment/CodeRangeSupplementPage.tsx` — trang gửi yêu cầu tại route `/code-range-supplements/create` (thay thế `CodeRangeSupplementDialog` đã xóa; mở từ tab "Lô hàng & Mã QR" + cảnh báo hạn mức, không có nút Quay lại); `ListPageHeader` + breadcrumb + `ListCard`; bằng chứng = bảng `DataTableShell` (checkbox + Loại + Lô + Thời điểm + Người ghi + Sản lượng thực) có `SearchInput` (tên lô/loại/người ghi) + `FilterSelect` loại + `Pagination` client-side `PAGE_SIZE=10`; nút [Gửi yêu cầu](create/xanh lá) + [Chọn/Bỏ chọn](outline), gửi xong ở lại trang |
| **FE Detail (VT-01)** | `pages/admin/CodeRangeSupplementDetailPage.tsx` — trang chi tiết + xét duyệt tại route `/admin/code-range-supplements/:id` (thay thế `SupplementReviewDialog` đã xóa): `ListPageHeader` + breadcrumb (Tổng quan → Quản lý dải mã → Chi tiết) + nút Quay lại; bố cục 2 cột (Label 35% / Value 65%); bằng chứng hiển thị rõ nghĩa từ `evidenceEvents` (`loại — tên lô — thời điểm — người ghi — SL`), fallback ID rút gọn + copy + xem chi tiết sự kiện (kèm dòng Sản lượng thực); select chuyển yêu cầu PENDING khác cùng tổ chức; nút [Từ chối](destructive) + [Duyệt](blue), validate rỗng SL thực cấp; duyệt/từ chối xong về `/admin/code-ranges` |
| **FE API** | `api/codeRangeSupplementApi.ts` |
| **FE Types** | `types/codeRangeSupplement.ts` |
| **FE Validator** | `utils/validators/codeRangeSupplementSchema.ts` |
| **Roles** | `supplementCreate` (VT-02), `supplementManage` (VT-01) |
| **Tests** | `docs/testing/NCL-04-CN-007_manual_test.md` |

### 2.12 Standards — Tiêu chuẩn chất lượng (package `certification`)

| Lớp | File |
|---|---|
| **BE** | `certification/controller/StandardController.java`, `certification/service/StandardService.java` + `impl`, `certification/entity/Standard.java` |
| **FE Pages** | `pages/admin/StandardManagementPage.tsx`, `CreateStandardPage.tsx`, `EditStandardPage.tsx` |
| **FE Components** | `components/admin/StandardList.tsx`, `StandardForm.tsx` |
| **FE API** | `api/standardApi.ts` |
| **FE Types** | `types/standard.ts` |
| **Migrations** | `V7` (standards), `V55` (seed) |

### 2.13 Certifications & Inspection (package `certification`)

| Lớp | File |
|---|---|
| **BE Controllers** | `CertificationController`, `CertificationLookupController`, `ProductionLotCertificationController`, `InspectionRequestController`, `InspectionCriterionCatalogController`, `ProductCategoryCriterionController`, `InspectionCriterionResultController`, `TestingUnitController`, `AccreditationScopeController` (→ `certification/controller/`) |
| **BE Service** | `CertificationService`, `InspectionRequestService`, `InspectionCriterionCatalogService`, `CategoryCriterionAssignmentService`, `TestingUnitService`, `AccreditationScopeService`, `MilestoneValidationService`, `CultivationMilestoneService` (+ impls) |
| **BE Entity** | `Certification`, `ProductionLotCertification`, `InspectionRequest`, `InspectionCriterion`, `InspectionCriterionResult`, `InspectionCriterionCatalog`, `CategoryCriterion`, `TestingUnit`, `AccreditationScope`, `Standard`, `CultivationMilestone` |
| **FE Pages** | `pages/certification/CertificationListPage.tsx`, `CreateCertificationPage.tsx`, `CreateInspectionRequestPage.tsx`, `RecordInspectionResultPage.tsx` |
| **FE Components** | `components/certification/CertificationList.tsx`, `CertificationStatusBadge.tsx`, `CreateCertificationForm.tsx`, `AttachCertificationDialog.tsx`, `RecordInspectionResultDialog.tsx` |
| **FE API** | `api/certificationApi.ts` |
| **FE Types** | `types/certification.ts`, `types/inspectionCriterion.ts` |
| **Validator** | `utils/validators.ts` (createCertificationSchema) |
| **Migrations** | `V7` (standards/certs/lot_certs), `V23`–`V26` (inspection_*), `V33` (catalog/category_criteria), `V35` (testing_units), `V49` (accreditation_scopes), `V65` (cultivation_milestone merged), `V66` (seed milestone) |

### 2.14 Cultivation Milestones — Mốc canh tác (package `certification`)

| Lớp | File |
|---|---|
| **BE** | `certification/controller/CultivationMilestoneController.java`, `certification/service/CultivationMilestoneService.java` + `impl/CultivationMilestoneServiceImpl.java`, `certification/service/MilestoneValidationService.java` + `impl/MilestoneValidationServiceImpl.java`, `certification/entity/CultivationMilestone.java`, `certification/repository/CultivationMilestoneRepository.java` |
| **FE Pages** | `pages/admin/CultivationMilestoneManagementPage.tsx`, `CreateCultivationMilestonePage.tsx`, `EditCultivationMilestonePage.tsx` |
| **FE Components** | `components/admin/cultivation-milestone/CultivationMilestoneFormContent.tsx` |
| **FE API** | `api/cultivationMilestoneApi.ts` |
| **FE Types** | `types/cultivationMilestone.ts` |
| **Migrations** | `V63` (legacy catalog + mapping), `V65` (merge → single `cultivation_milestone` table), `V66` (seed 10 milestones) |
| **Docs** | `docs/api/certification/cultivation-milestone.md` |

### 2.15 Warehouse Receipt — Nhập kho / Thu mua (packages `event` + `trace`)

| Lớp | File |
|---|---|
| **BE** | `event/controller/WarehouseReceiptController.java`, `event/controller/ProcurementEventController.java`, `event/service/WarehouseReceiptService.java` + `impl`, `event/service/ProcurementEventService.java` + `impl` |
| **FE Pages** | `pages/warehouse-receipt/WarehouseReceiptPage.tsx`, `WarehouseReceiptDetailPage.tsx` |
| **FE Components** | `pages/warehouse-receipt/components/WarehouseReceiptCreateDialog.tsx`, `components/procurement/RecordProcurementDialog.tsx` |
| **FE API** | `api/warehouseReceiptApi.ts` |
| **FE Hooks** | `hooks/useWarehouseReceipt.ts`, `hooks/useProcurementEvent.ts` |
| **FE Types** | `types/warehouseReceipt.ts`, `types/procurementEvent.ts` |
| **Validator** | `utils/procurementEventSchema.ts` |

### 2.16 Storage Condition — Bảo quản (package `event`)

| Lớp | File |
|---|---|
| **BE** | `event/controller/ChainEventController.java` (endpoint `POST /storage-condition`), `event/service/ChainEventServiceImpl.java` (`validateStorageProcurementRelationship`, `recordStorageCondition`) |
| **FE Pages** | `pages/storage-condition/StorageConditionPage.tsx` |
| **FE API** | `api/storageConditionApi.ts` |
| **FE Types** | `types/storageCondition.ts` |
| **Quyền** | `ROLE_ACCESS.storageCondition = ['VT-03', 'VT-04']` |

### 2.17 Offline Sync — Đồng bộ ngoại tuyến (package `event`)

| Lớp | File |
|---|---|
| **BE** | `event/controller/ChainEventController.java` (`POST /chain-events/sync`) |
| | `event/service/OfflineSyncService.java` + `impl/OfflineSyncServiceImpl.java`, `OfflineSyncEventProcessor.java` (@Transactional(REQUIRES_NEW) per event) |
| | `event/entity/OfflineSyncLog.java` (bảng `offline_sync_logs`, V11) |
| **FE Pages** | `pages/offline/OfflineEventPage.tsx`, `pages/mobile/RecordMobileEventPage.tsx` |
| **FE Components** | `components/offline/OfflineEventList.tsx`, `components/mobile/RecordMobileEventForm.tsx` |
| **FE Services** | `services/offlineQueue.ts` (queue lưu tạm), `hooks/useOfflineSync.ts`, `hooks/useAutoGeolocation.ts` |
| **FE Types** | `types/offlineEvent.ts` |
| **FE Validator** | `utils/validators.ts` (mobileEventSchema) |
| **Pattern quan trọng** | Response: `OfflineEventSyncResponse {syncId, totalEvents, successCount, duplicateCount, failedCount, results[]}` — `OfflineEventSyncResultDto` có `status: SUCCESS/DUPLICATE/FAILED` + `retryable: boolean`. Dùng pattern này cho bất kỳ sync mới nào (ví dụ NCL-10-CN-012 offline farm logs). |
| **Migration** | `V11` (offline_sync_logs) |

### 2.18 Recall Requests — Thu hồi hàng loạt (package `recall`)

| Lớp | File |
|---|---|
| **BE** | `recall/controller/RecallRequestController.java`, `recall/service/RecallRequestService.java` + `impl`, `recall/entity/RecallRequest.java` |
| **FE Pages** | `pages/recall-request/RecallRequestListPage.tsx`, `CreateRecallRequestPage.tsx`, `RecallRequestDetailPage.tsx` |
| **FE API** | `api/recallApi.ts` |
| **FE Types** | `types/recall.ts`, `types/recallRequest.ts` |
| **Migration** | `V22` (recall_requests) |
| **Ghi chú** | Khác với `Recall` trong package `trace` (thu hồi lô hàng/tem cá thể); `RecallRequest` là yêu cầu thu hồi hàng loạt (list/approve/reject). |

### 2.19 Report & Dashboard (package `report`)

| Lớp | File |
|---|---|
| **BE Controllers** | `ReportController.java` (dashboard summary/timeline/analysis), `LookupStatisticsController.java`, `CropAreaAnalysisController.java`, `DossierController.java` (GS1 export + dossier check), `SystemMonitoringController.java`, `OpenDataExportController.java` |
| **BE Service** | `ReportService.java`, `LookupStatisticsService.java`, `CropAreaAnalysisService.java`, `DossierService.java`, `SystemMonitoringService.java`, `MetricsBufferService.java`, `OpenDataExportService.java` (+ impls) |
| **BE Excel/PDF** | `report/excel/IndustryReportExcelGenerator.java`, `report/pdf/IndustryReportPdfGenerator.java` |
| **FE Dashboard Pages** | `pages/daskboard/DashboardPase.tsx` — MANHATTAN/MANAGEMENT/COOPERATIVE/EVENT-RECORDER/PROCUREMENT dashboards |
| **FE Dashboard Components** | `components/dashboard/DashboardContent.tsx`, `AdminDashboard.tsx`, `ManagementDashboard.tsx`, `CooperativeDashboard.tsx`, `EventRecorderDashboard.tsx`, `ProcurementDashboard.tsx`, `IndustryReportTab.tsx` |
| **FE Reports Pages** | `pages/report/LookupStatisticsPage.tsx`, `CropAreaAnalysisPage.tsx`, `SeasonYieldComparisonPage.tsx`, `IndustryReportPage.tsx`, `ActivityLogPage.tsx`, `LoginHistoryPage.tsx`, `LoginAnomalyTrackingPage.tsx`, `FailedEventLogsPage.tsx` |
| **FE Report Components** | `components/report/` (LookupStatisticsContent, LotStatsTable, AbnormalScansTable, CropAreaFilter, SeasonYieldComparisonTable, IndustryReportFilter, ActivityLogTable, ActivityLogFilter, etc.) |
| **FE API** | `api/reportApi.ts`, `api/lookupStatisticsApi.ts`, `api/cropAreaAnalysisApi.ts`, `api/seasonYieldComparisonApi.ts`, `api/activityLogApi.ts`, `api/loginHistoryApi.ts`, `api/loginAnomalyApi.ts`, `api/dossierApi.ts`, `api/exportApi.ts`, `api/monitoringApi.ts` |
| **FE Hooks** | `hooks/useCropAreaAnalysis.ts`, `hooks/useExportIndustryReport.ts` |
| **FE Types** | `types/dashboard.ts`, `types/lookupStatistics.ts`, `types/cropAreaAnalysis.ts`, `types/seasonYieldComparison.ts`, `types/report.ts`, `types/activityLog.ts`, `types/loginHistory.ts`, `types/loginAnomaly.ts`, `types/monitoring.ts`, `types/export.ts` |
| **FE Validators** | `utils/validators.ts` (exportOpenDataSchema) |

### 2.20 Alerts & Activity Logs (package `alert`)

| Lớp | File |
|---|---|
| **BE** | `alert/controller/AlertController.java`, `alert/controller/ActivityLogController.java`, `alert/service/AlertService.java` + `impl`, `alert/service/ActivityLogService.java` + `impl`, `alert/service/ScanAnomalyDetectionService.java` + `impl` |
| | `alert/entity/Alert.java`, `AlertDetails.java`, `ScanPoint.java`, `ActivityLog.java` (→ `activity_logs` table) |
| | `alert/event/ActivityLogEvent.java`, `alert/listener/ActivityLogListener.java`, `alert/specification/ActivityLogSpecification.java` |
| **FE Pages** | `pages/scan-anomaly-alert/ScanAnomalyAlertPage.tsx` |
| **FE Components** | `pages/scan-anomaly-alert/components/ScanAnomalyAlertDetailsDialog.tsx`, `ResolveScanAnomalyAlertDialog.tsx` |
| **FE API** | `api/scanAnomalyAlertApi.ts` |
| **FE Types** | `types/scanAnomalyAlert.ts` |
| **Migration** | `V8` (alerts), `V10` (activity_logs, trace_code_scan_logs) |
| **Pattern** | Audit log: `@Auditable(action, entityType, description)` trên service method → `AuditAspect` → `ActivityLogEvent` → `ActivityLogListener` (async) → `activity_logs`. Hoặc publish trực tiếp: `eventPublisher.publishEvent(ActivityLogEvent.builder()...)`. |

### 2.21 Notifications (package `notification`)

| Lớp | File |
|---|---|
| **BE** | `notification/controller/NotificationController.java`, `notification/service/NotificationService.java` + `impl`, `notification/entity/Notification.java` |
| **FE Pages** | `pages/notification/NotificationsPage.tsx` |
| **FE Components** | `components/notification/NotificationBell.tsx`, `NotificationPanel.tsx` |
| **FE Hooks** | `hooks/useNotifications.ts`, `hooks/useUnreadCount.ts` |
| **FE API** | `api/notificationApi.ts` |
| **FE Types** | `types/notification.ts` |
| **Migration** | `V8` (notifications) |

### 2.22 Integration — API Key đối tác (package `integration`)

| Lớp | File |
|---|---|
| **BE** | `integration/apikey/PartnerApiKeyController.java`, `PartnerApiKeyService.java`, `entity/PartnerApiKey.java`, `repository/PartnerApiKeyRepository.java` |
| | `integration/partner/PartnerLotController.java`, `PartnerLotService.java` |
| **FE Pages** | `pages/apiKey/PartnerApiKeyListPage.tsx`, `CreatePartnerApiKeyPage.tsx` |
| **FE Components** | `components/apiKey/CreateApiKeyModal.tsx`, `ApiKeyStatusBadge.tsx`, `RawApiKeyModal.tsx`, `RevokeApiKeyDialog.tsx` |
| **FE API** | `api/apiKeyApi.ts` |
| **FE Types** | `types/apiKey.ts` |
| **Migration** | `V27` (partner_api_keys) |
| **Security** | `BA/config/ApiKeyAuthenticationFilter.java` — header `X-API-KEY`, rate-limit 429, routes `/api/v1/partner/**` |

### 2.23 Public Trace — Truy xuất công khai (package `publicapi`)

| Lớp | File |
|---|---|
| **BE** | `publicapi/controller/PublicTraceController.java`, `publicapi/controller/PartnerTraceController.java`, `publicapi/service/PublicTraceService.java` + `impl`, `publicapi/service/LocationIQServiceImpl.java` |
| **FE Pages** | `pages/public/PublicHomePage.tsx`, `TraceLookupPage.tsx`, `JoinOrganizationPage.tsx` |
| **FE Components** | `components/public/Timeline.tsx`, `ProductInfo.tsx`, `RouteMap.tsx`, `VerifiedAlert.tsx`, `RecallAlert.tsx`, `LockAlert.tsx`, `PublicCertificationsSection.tsx`, `ProductFeedbackForm.tsx` |
| **FE API** | `api/publicApi.ts` |
| **FE Types** | `types/publicTrace.ts`, `types/publicInspection.ts`, `types/publicCertification.ts`, `types/productFeedback.ts` |

### 2.24 Backup & Restore (package `backup`)

| Lớp | File |
|---|---|
| **BE** | `backup/controller/BackupController.java`, `backup/service/BackupService.java` + `impl`, `backup/service/RestoreService.java` + `impl`, `backup/scheduler/BackupScheduler.java`, `backup/entity/BackupSchedule.java`, `backup/entity/BackupRestoreHistory.java` |
| **FE Pages** | `pages/admin/BackupRestorePage.tsx` |
| **FE Components** | `components/backup/BackupStatus.tsx`, `BackupSchedule.tsx`, `BackupHistoryTable.tsx`, `BackupHistoryFilter.tsx`, `ScheduleEditDialog.tsx`, `RestoreConfirmDialog.tsx` |
| **FE API** | `api/backupApi.ts` |
| **FE Hooks** | `hooks/useBackup.ts` |
| **FE Types** | `types/backup.ts` |
| **Migration** | `V12` (backup_schedules, backup_restore_history), `V18` (seed backup) |

### 2.25 Help Content (package `help`)

| Lớp | File |
|---|---|
| **BE** | `help/controller/HelpController.java`, `help/service/HelpService.java` + `impl`, `help/entity/HelpContent.java` |
| **FE Components** | `components/help/HelpButton.tsx` (tái sử dụng mọi nơi) |
| **FE API** | `api/helpApi.ts` |
| **FE Hooks** | `hooks/useHelp.ts` |
| **FE Types** | `types/help.ts` |
| **Migrations** | `V28` (help_content), `V29`/`V34`/`V37`/`V46`/`V52`/`V67` (seeds cho từng screen) |

### 2.26 Profile (FE only)

| Lớp | File |
|---|---|
| **FE Pages** | `pages/profile/ProfilePage.tsx`, `UserProfilePage.tsx` |
| **FE Components** | `components/profile/UserProfileForm.tsx`, `ChangePasswordForm.tsx` |
| **FE API** | (dùng `authApi.ts`) |

### 2.27 Product Feedback — Phản ánh

| Lớp | File |
|---|---|
| **BE** | `farm/controller/ProductFeedbackController.java`, `farm/controller/ProductFeedbackManagementController.java`, `farm/service/ProductFeedbackService.java` + `impl`, `farm/entity/ProductFeedback.java` |
| **FE Pages** | `pages/product-feedback/ProductFeedbackManagementPage.tsx` |
| **FE API** | `api/productFeedbackApi.ts` |
| **FE Types** | `types/productFeedback.ts` |

### 2.28 Recall Cases — Vụ việc thu hồi (package `trace/recall`)

> **Trạng thái: HOÀN TẤT** — User Story `NCL-08-CN-012 close recall case` trên branch `feature/NCL-08-CN-012-close-recall-case`.

| Lớp | File |
|---|---|
| **BE Controller** | `trace/recall/controller/RecallCaseController.java` — `GET /api/v1/recall-cases`, `GET /{id}`, `POST /{id}/close` (đều `@PreAuthorize hasRole('VT-02')`) |
| **BE Service** | `trace/recall/service/RecallCaseService.java` + `impl/RecallCaseServiceImpl.java` — lazy materialize case từ các lô có shipment `RECALLED` khi list/detail |
| **BE Entity** | `trace/recall/entity/RecallCase.java` (→ `recall_cases`), `trace/recall/entity/RecallLotResult.java` (→ `recall_lot_results`) |
| **BE Enums** | `RecallCaseStatus` (`OPEN`, `CLOSED` — một chiều), `LotResolution` (`DESTROYED`, `RETURNED`, `REPROCESSED`, `NOT_RECALLED`) |
| **BE DTO** | `trace/recall/dto/request/CloseRecallCaseRequest.java`, `trace/recall/dto/response/RecallCaseResponse.java`, `RecallLotResultResponse.java` |
| **BE Tests** | `backend/src/test/java/vn/nguongocso/trace/recall/` |
| **BE liên quan (đã sửa)** | `notification/service/NotificationService(+Impl)` (thông báo cho tổ chức thu mua khi đóng case), `publicapi/service/impl/PublicTraceServiceImpl` (đổi nội dung cảnh báo công khai theo QTN-09 — không ẩn/xóa) |
| **FE Pages** | `pages/recall/RecallCaseListPage.tsx`, `RecallCaseDetailPage.tsx`, `pages/recall/CloseRecallCaseDialog.tsx` |
| **FE API/Types** | `api/recallCaseApi.ts` + `types/recallCase.ts` |
| **FE Route/Quyền (đã thêm)** | `routes/AppRoutes.tsx` (`/recall-cases`, `/recall-cases/:id`), `config/roleAccess.ts` (`recallCaseManage: ['VT-02']`). Sidebar đã có item "Vụ việc thu hồi" |
| **Docs API** | `docs/api/recall/RecallCase.md` |
| **Migration** | `V20260910000000` (recall_cases, recall_lot_results) |
| **Ghi chú** | Khác `Recall` (§2.18 ghi chú — thu hồi lô/tem cá thể) và `RecallRequest`/bulk recall (NCL-08-CN-008/011): `RecallCase` là vụ việc thu hồi ở cấp tổ chức, gom kết quả xử lý từng shipment `RECALLED` (bảng `recall_lot_results`), chỉ đóng khi mọi lô đã có kết quả + bắt buộc biện pháp khắc phục. |

---

## 3. Cross-cutting Infrastructure

### 3.1 Response Wrapper & Exceptions

| Thành phần | File |
|---|---|
| `ApiResult<T>` | `BA/common/ApiResult.java` — `{success, status, message, data, errors, path, timestamp}` |
| `PageResponse<T>` | `BA/common/PageResponse.java` |
| `BusinessException` | `BA/exception/BusinessException.java` (HTTP status + message tiếng Việt) |
| `ResourceNotFoundException` | `BA/exception/ResourceNotFoundException.java` (404) |
| `DuplicateResourceException` | `BA/exception/DuplicateResourceException.java` (409) |
| `GlobalExceptionHandler` | `BA/exception/GlobalExceptionHandler.java` (@RestControllerAdvice, map ALL exceptions → ApiResult tiếng Việt) |

### 3.2 Audit System

| Thành phần | File | Vai trò |
|---|---|---|
| `@Auditable` | `BA/common/annotation/Auditable.java` | Annotate service method, hỗ trợ SpEL `#param`/`#result` |
| `AuditAspect` | `BA/common/aspect/AuditAspect.java` | @AfterReturning aspect → publish event async |
| `ActivityLogEvent` | `BA/alert/event/ActivityLogEvent.java` | Event payload (userId, orgId, action, description, entityType) |
| `ActivityLogListener` | `BA/alert/listener/ActivityLogListener.java` | @Async @EventListener @Transactional(REQUIRES_NEW) → ghi `activity_logs` |
| `IpUtils` | `BA/common/util/IpUtils.java` | Lấy client IP (X-Forwarded-For) |

### 3.3 Security Config

| Thành phần | File |
|---|---|
| Filter chain | `BA/config/SecurityConfig.java` — stateless JWT, permitAll auth/public/actuator |
| JWT Provider | `BA/config/JwtTokenProvider.java` — create/parse/validate JWT (claims: userId, orgId, role, tokenType) |
| JWT Filter | `BA/config/JwtAuthenticationFilter.java` — read ACCESS token → set SecurityContext |
| API Key Filter | `BA/config/ApiKeyAuthenticationFilter.java` — `/api/v1/partner/**`, `X-API-KEY` header, 429 rate-limit |
| Maintenance Filter | `BA/config/MaintenanceFilter.java` — 503 during restore (VT-01 exempt) |

### 3.4 Feign Clients (internal service calls)

Không có Feign — mọi call đều trực tiếp trong cùng JVM qua service layer.

### 3.5 Config & Utilities

| Thành phần | File | Ghi chú |
|---|---|---|
| Clock TZ | `BA/config/TimeConfig.java` | `Asia/Ho_Chi_Minh` |
| Async | `BA/config/AsyncConfig.java` | `@EnableAsync` |
| Web | `BA/config/WebConfig.java` | Serve `/files/qr/**`, `/uploads/**` |
| Metrics | `BA/config/MetricsCollectorFilter.java` | Đo latency public-trace/data-gateway |
| Geo | `BA/common/util/GeoDistanceUtils.java` | Tính khoảng cách địa lý |
| Event Hash | `BA/event/service/EventHashService.java` | SHA hash chuỗi sự kiện |

### 3.6 Frontend Shared Components (components/common/)

| Component | File | Mô tả |
|---|---|---|
| `ListPageHeader` | `common/ListPageHeader.tsx` | Header trang list (icon + title + description + actions) |
| `ListCard` | `common/ListCard.tsx` | Card wrapper (`p-4 space-y-4`) |
| `ListToolbar` | `common/ListToolbar.tsx` | Toolbar (left/right responsive) |
| `DataTableShell` | `common/DataTableShell.tsx` | Table wrapper (bordered, loading/empty state, optional `emptyAction`) |
| `DataTablePagination` | `common/DataTablePagination.tsx` | Legacy table pagination (rất ít dùng — prefer `Pagination`) |
| `Pagination` | `common/Pagination.tsx` | Phân trang chuẩn (prev/next + range/total) |
| `StatusBadge` | `common/StatusBadge.tsx` | Badge trạng thái (label + tone + icon) |
| `SearchInput` | `common/SearchInput.tsx` | Ô tìm kiếm |
| `FilterSelect` | `common/FilterSelect.tsx` | Select dropdown lọc |
| `RefreshButton` | `common/RefreshButton.tsx` | Nút làm mới |
| `ScanCodeField` | `common/ScanCodeField.tsx` | Trường quét/nhập mã QR (camera scan + manual) |
| `ConfirmDialog` | `common/ConfirmDialog.tsx` | Dialog xác nhận xóa/hành động |
| `OfflineStatusBadge` | `common/OfflineStatusBadge.tsx` | Badge trạng thái offline sync |
| `LotLookupResult` | `common/LotLookupResult.tsx` | Kết quả tra cứu lô (thẻ thông tin) |
| `StatCard` | `common/StatCard.tsx` | Card thống kê |
| `AppBreadcrumb` | `common/AppBreadcrumb.tsx` | Breadcrumb tự động + `useSetBreadcrumb()` override |
| `AdministrativeUnitCascadeSelect` | `common/AdministrativeUnitCascadeSelect.tsx` | Cascade select tỉnh/huyện/xã |
| `ProvinceUnitMultiSelect` | `common/ProvinceUnitMultiSelect.tsx` | Multi-select tỉnh |
| `DetailSection` / `DetailField` | `common/detail/DetailSection.tsx` | Layout hiển thị chi tiết |

### 3.7 Frontend Config Files

| File | Nội dung |
|---|---|
| `config/roleAccess.ts` | `ROLE_ACCESS` (tất cả quyền × roles), `hasAnyRole()`, `getRoleLabel()` |
| `config/actionMappings.ts` | `ACTION_LABELS`, `ACTION_COLORS` cho activity log |
| `config/runtimeConfig.ts` | `getApiBaseUrl()`, `getAssetUrl()` |
| `contexts/AuthContext.tsx` | `AuthProvider`, `useAuth()`, login/logout flow |

---

## 4. Routing & Role Map (Frontend)

- **Single file**: `FE/routes/AppRoutes.tsx` (~1428 dòng).
- **Guard**: `PrivateRoute` (chưa login → redirect `/login`) + `RoleRoute allowedRoles={ROLE_ACCESS.xxx}`.
- **Roles**: VT-01 (Platform Admin), VT-02 (Cooperative Manager), VT-03 (Event Recorder), VT-04 (Procurement), VT-05 (Regulator).

### Quick Route Reference

| Route | Page | Allowed Roles |
|---|---|---|
| `/dashboard` | DashboardPage | All Authenticated |
| `/production-lots` | ProductionLotListPage | VT-02, VT-03 |
| `/production-lots/create` | CreateProductionLotPage | VT-02 |
| `/production-lots/:id/edit` | ProductionLotEditPage | VT-02 |
| `/farm-areas` | FarmAreaListPage | VT-02 |
| `/farm-logs/create` | CreateFarmLogPage | VT-02, VT-03 |
| `/preprocessing-events/create` | CreatePreprocessingEventPage | VT-02, VT-03 |
| `/packaging-events/create` | CreatePackagingEventPage | VT-02, VT-03 |
| `/transport-events/record` | RecordTransportEventPage | VT-03 |
| `/chain-events/scan` | ScanQuickEventPage | VT-03 |
| `/shipments/:id` | ShipmentDetailPage | VT-02, VT-03, VT-04 |
| `/warehouse-receipt` | WarehouseReceiptPage | VT-04 |
| `/storage-condition` | StorageConditionPage | VT-03, VT-04 |
| `/certifications` | CertificationListPage | VT-02 |
| `/recall-requests` | RecallRequestListPage | VT-02 |
| `/recall-cases` | RecallCaseListPage | VT-02 — NCL-08-CN-012 |
| `/recall-cases/:id` | RecallCaseDetailPage | VT-02 — NCL-08-CN-012 |
| `/admin/code-ranges` | CodeRangeListPage | VT-01 |
| `/admin/product-categories` | ProductCategoryManagementPage | VT-01 |
| `/admin/input-materials` | InputMaterialManagementPage | VT-01, VT-02, VT-03, VT-04 |
| `/admin/standards` | StandardManagementPage | VT-01 |
| `/admin/inspection-criteria` | InspectionCriteriaManagementPage | VT-01 |
| `/admin/cultivation-milestones` | CultivationMilestoneManagementPage | VT-01 |
| `/admin/suspect-trace-codes` | SuspectTraceCodeListPage | VT-01 |
| `/integration/api-keys` | PartnerApiKeyListPage | VT-01, VT-02 |
| `/organizations` | OrganizationListPage | VT-01 |
| `/reports/lookup-statistics` | LookupStatisticsPage | VT-01, VT-02 |
| `/reports/crop-area-analysis` | CropAreaAnalysisPage | VT-01, VT-05 |
| `/reports/industry` | IndustryReportPage | VT-05 |
| `/activity-logs` | ActivityLogPage | VT-02 |
| `/login-history` | LoginHistoryPage | All Authenticated |
| `/notifications` | NotificationsPage | All Authenticated |
| `/event-chain-verification` | EventChainVerificationPage | VT-01, VT-04, VT-05 |
| `/offline-events` | OfflineEventPage | VT-02, VT-03 |
| `/export/open-data` | ExportOpenDataPage | VT-05 |

---

## 5. Database — Migration Files Map

### 5.1 Schema Migrations

| File | Nội dung |
|---|---|
| `schema/V1` | roles, permissions, organizations, users, organization_users, invitations |
| `schema/V2` | product_categories (`+V19` ALTER storage thresholds) |
| `schema/V3` | farm_areas |
| `schema/V4` | production_lot (`+V62` ALTER cancellation fields) |
| `schema/V5` | farm_logs, farm_log_attachments (`+V41` ALTER correction fields) |
| `schema/V6` | shipments, trace_codes, recalls (`+V21`, `V32`, `V45`, `V20260830130000` ALTERs) |
| `schema/V7` | standards, certifications, production_lot_certifications |
| `schema/V8` | alerts, notifications |
| `schema/V9` | chain_events, code_ranges, product_feedbacks |
| `schema/V10` | report_access_log, activity_logs, trace_code_scan_logs, failed_event_logs |
| `schema/V11` | dossier_export_history, production_lot_import_history, offline_sync_logs |
| `schema/V12` | backup_schedules, backup_restore_history |
| `schema/V13` | role_permissions, organization_role_permissions |
| `schema/V22` | recall_requests |
| `schema/V23`–`V26` | inspection_requests/criteria/definitions/results |
| `schema/V27` | partner_api_keys |
| `schema/V28` | help_content |
| `schema/V30` | login_attempts, login_anomalies, account_locks |
| `schema/V31` | suspicious_cases |
| `schema/V33` | inspection_criterion_catalog, category_criteria |
| `schema/V35` | testing_units |
| `schema/V36` | label_export_history |
| `schema/V38` | password_reset_tokens |
| `schema/V42` | administrative_units, user_area_assignments |
| `schema/V44` | input_materials, input_material_crop_types |
| `schema/V45` | label_cancellation_history |
| `schema/V48` | lot_assignments |
| `schema/V49` | accreditation_scopes |
| `schema/V65` | cultivation_milestone (merged from V63 catalog+mapping) |
| `schema/V20260910000000` | recall_cases, recall_lot_results (NCL-08-CN-012) |

### 5.2 Data Seeds

| File | Nội dung |
|---|---|
| `data/V14`–`V17` | roles, permissions, role_permissions, default admin account |
| `data/V18` | backup_schedules |
| `data/V29`, `V34`, `V37`, `V46`, `V52`, `V67` | help_content (theo feature) |
| `data/V39`, `V53`, `V54` | input_material/farm_log permissions |
| `data/V43` | administrative_units (tỉnh/huyện/xã) |
| `data/V50` | backfill organization_province |
| `data/V51` | input_materials (seed data) |
| `data/V55`–`V56` | standards, inspection_criterion_catalog |
| `data/V64` | cultivation_milestone (legacy catalog) |
| `data/V66` | cultivation_milestone (merged table, 10 milestones) |

---

## 6. Sidebar Menu Map (FE)

`components/layout/Sidebar.tsx` — `MENU_GROUPS` object, tự lọc theo role:

| Group | Label | Items |
|---|---|---|
| `management` | Quản lý | Tổ chức, Quản lý thành viên, Cấu hình quyền, Danh mục nông sản, Tiêu chuẩn, Chứng nhận, Khóa API, Dải mã, Tem nghi vấn, Vật tư, Mốc canh tác, Đơn vị kiểm nghiệm, Phân công địa bàn, Phản ánh |
| `operations` | Vận hành sản xuất | Vùng trồng, Lô sản xuất, Ghi sự kiện, Quét nhanh, Bảo quản, Cảnh báo, Nhật ký lỗi, Offline, Thu hồi |
| `reports` | Thống kê & Báo cáo | Thống kê tra cứu, Phân tích vùng trồng, So sánh mùa vụ, Báo cáo ngành, Xuất dữ liệu mở |
| `procurement` | Thu mua | Nhập kho |
| `system` | Hệ thống | Kiểm chứng dòng sự kiện, Lịch sử hoạt động, Lịch sử đăng nhập, Giám sát đăng nhập, Sao lưu & Phục hồi, Giám sát hệ thống, Hồ sơ |

---

## 7. Tips Điều Tra Nhanh

### Khi sửa/thêm chức năng cho một feature cụ thể:

1. **Tìm feature trong §2** → xem danh sách files BE (Controller → Service → Entity → Migration) và FE (Page → Component → API → Types → Validator).
2. **Đọc `Service/Impl`** trước — nơi chứa logic nghiệp vụ chính (không cần đọc Controller/Repository trừ khi thay đổi endpoint/schema).
3. **Đọc `entity/`** để hiểu schema hiện tại; nếu cần thêm cột/bảng → kiểm tra `V/migration/` đã có chưa, migration tiếp theo cần tên `V2026MMDD...`.
4. **Kiểm tra `types/*.ts`** ở FE trước khi viết form/API — để biết field nào Backend đang trả.
5. **Tìm pattern tương tự** trong các feature đã VERIFIED (xem `context.md` §6) để copy đúng conventions.

### Khi cần thêm migration mới:

- Tên file: `V2026MMDDHHMMSS__description.sql` (dải `> V20260904120000`).
- Đặt trong: `backend/src/main/resources/db/migration/schema/` (schema) hoặc `data/` (seed).
- UUID columns: `CHAR(36)` + `@JdbcTypeCode(SqlTypes.CHAR)`.
- Sau khi thêm migration + entity mới → `./mvnw.cmd clean test` (validate pass).

### Khi cần thêm route mới ở FE:

1. Thêm key vào `config/roleAccess.ts` (nếu là role mới) hoặc dùng key hiện có.
2. Thêm route vào `routes/AppRoutes.tsx` trong `<PrivateRoute>` block, wrap bằng `<RoleRoute allowedRoles={ROLE_ACCESS.xxx}>`.
3. Thêm item vào `Sidebar.tsx` → `MENU_GROUPS` (nếu cần hiện trong menu).
4. Dùng `useSetBreadcrumb()` trong page để set breadcrumb.
