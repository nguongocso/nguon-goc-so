# 4 package backend: config / exception / export / event

Nhánh: format/config-event-exception-export
Stash trước đó đã lưu thay đổi nhánh Clean/docs-file.

## 1. config (main + test)
- backend/src/main/java/vn/nguongocso/config/ApiKeyAuthenticationFilter.java
- backend/src/main/java/vn/nguongocso/config/AsyncConfig.java
- backend/src/main/java/vn/nguongocso/config/JwtAuthenticationFilter.java
- backend/src/main/java/vn/nguongocso/config/JwtTokenProvider.java
- backend/src/main/java/vn/nguongocso/config/MaintenanceFilter.java
- backend/src/main/java/vn/nguongocso/config/MetricsCollectorFilter.java
- backend/src/main/java/vn/nguongocso/config/SecurityConfig.java
- backend/src/main/java/vn/nguongocso/config/TestProfileDataLoader.java
- backend/src/main/java/vn/nguongocso/config/TimeConfig.java
- backend/src/main/java/vn/nguongocso/config/WebConfig.java

## 2. exception (main + test)
- backend/src/main/java/vn/nguongocso/exception/BusinessException.java
- backend/src/main/java/vn/nguongocso/exception/DuplicateResourceException.java
- backend/src/main/java/vn/nguongocso/exception/GlobalExceptionHandler.java
- backend/src/main/java/vn/nguongocso/exception/ResourceNotFoundException.java
- backend/src/test/java/vn/nguongocso/exception/GlobalExceptionHandlerTest.java

## 3. export (main + test + subpackages)
- backend/src/main/java/vn/nguongocso/export/constant/MandatoryFields.java
- backend/src/main/java/vn/nguongocso/export/controller/BatchProfileTemplateControl...java
- backend/src/main/java/vn/nguongocso/export/controller/ExportController.java
- backend/src/main/java/vn/nguongocso/export/controller/ProfileTemplateController.java
- backend/src/main/java/vn/nguongocso/export/dto/request/CreateProfileTemplateReque...java
- backend/src/main/java/vn/nguongocso/export/dto/request/ExportOpenDataRequest.java
- backend/src/main/java/vn/nguongocso/export/dto/request/FieldSelectionDto.java
- backend/src/main/java/vn/nguongocso/export/dto/request/UpdateProfileTemplateReque...java
- backend/src/main/java/vn/nguongocso/export/dto/response/FieldGroupDefinition.java
- backend/src/main/java/vn/nguongocso/export/dto/response/FieldItemDefinition.java
- backend/src/main/java/vn/nguongocso/export/dto/response/ProfileTemplateFieldRespo...java
- backend/src/main/java/vn/nguongocso/export/dto/response/ProfileTemplateResponse.java
- backend/src/main/java/vn/nguongocso/export/dto/response/Qtn11ErrorDetailDto.java
- backend/src/main/java/vn/nguongocso/export/entity/ExportLog.java
- backend/src/main/java/vn/nguongocso/export/entity/ProfileTemplate.java
- backend/src/main/java/vn/nguongocso/export/entity/ProfileTemplateField.java
- backend/src/main/java/vn/nguongocso/export/enums/ProfileFieldGroup.java
- backend/src/main/java/vn/nguongocso/export/exception/MandatoryFieldsViolationExce...java
- backend/src/main/java/vn/nguongocso/export/exception/TemplateNotOwnedException.java
- backend/src/main/java/vn/nguongocso/export/repository/ExportLogRepository.java
- backend/src/main/java/vn/nguongocso/export/repository/ProfileTemplateFieldReposit...java
- backend/src/main/java/vn/nguongocso/export/repository/ProfileTemplateRepository.java
- backend/src/main/java/vn/nguongocso/export/schema/OpenDataSchema.java
- backend/src/main/java/vn/nguongocso/export/service/ExportService.java
- backend/src/main/java/vn/nguongocso/export/service/ProfileTemplateService.java
- backend/src/main/java/vn/nguongocso/export/service/impl/ExportServiceImpl.java
- backend/src/main/java/vn/nguongocso/export/service/impl/ProfileTemplateServiceImp...java
- backend/src/main/java/vn/nguongocso/export/util/ExportDisplayFormatter.java
- backend/src/test/java/vn/nguongocso/export/JacksonSerializationTest.java
- backend/src/test/java/vn/nguongocso/export/service/ExportWithTemplateServiceTest...java
- backend/src/test/java/vn/nguongocso/export/service/ProfileTemplateServiceTest.java
- backend/src/test/java/vn/nguongocso/export/util/ExportDisplayFormatterTest.java

## 4. event (main + test + subpackages)
- backend/src/main/java/vn/nguongocso/event/controller/ChainEventController.java
- backend/src/main/java/vn/nguongocso/event/controller/ChainVerificationController...java
- backend/src/main/java/vn/nguongocso/event/controller/EventValidationController.java
- backend/src/main/java/vn/nguongocso/event/controller/ProcurementEventController.java
- backend/src/main/java/vn/nguongocso/event/controller/PublicJourneyController.java
- backend/src/main/java/vn/nguongocso/event/controller/ShipmentTimelineController.java
- backend/src/main/java/vn/nguongocso/event/controller/WarehouseReceiptController.java
- backend/src/main/java/vn/nguongocso/event/dto/request/CorrectPackagingEventReques...java
- backend/src/main/java/vn/nguongocso/event/dto/request/CorrectPreprocessingEventRe...java
- backend/src/main/java/vn/nguongocso/event/dto/request/OfflineEventSyncRequest.java
- backend/src/main/java/vn/nguongocso/event/dto/request/RecordHarvestEventRequest.java
- backend/src/main/java/vn/nguongocso/event/dto/request/RecordMobileEventRequest.java
- backend/src/main/java/vn/nguongocso/event/dto/request/RecordOfflineEventDto.java
- backend/src/main/java/vn/nguongocso/event/dto/request/RecordPackagingEventRequest...java
- backend/src/main/java/vn/nguongocso/event/dto/request/RecordPreprocessingEventReq...java
- backend/src/main/java/vn/nguongocso/event/dto/request/RecordProcurementEventReque...java
- backend/src/main/java/vn/nguongocso/event/dto/request/RecordTransportEventRequest...java
- backend/src/main/java/vn/nguongocso/event/dto/request/RecordWarehouseEntryRequest...java
- backend/src/main/java/vn/nguongocso/event/dto/request/RecordWarehouseExitRequest...java
- backend/src/main/java/vn/nguongocso/event/dto/request/StorageConditionRequest.java
- backend/src/main/java/vn/nguongocso/event/dto/request/WarehouseReceiptRequest.java
- backend/src/main/java/vn/nguongocso/event/dto/response/ChainEventResponse.java
- backend/src/main/java/vn/nguongocso/event/dto/response/ChainVerificationResponse...java
- backend/src/main/java/vn/nguongocso/event/dto/response/CoopWarehouseEventResponse...java
- backend/src/main/java/vn/nguongocso/event/dto/response/EventVerificationItem.java
- backend/src/main/java/vn/nguongocso/event/dto/response/FailedEventLogResponse.java
- backend/src/main/java/vn/nguongocso/event/dto/response/JourneyPointResponse.java
- backend/src/main/java/vn/nguongocso/event/dto/response/JourneyResponse.java
- backend/src/main/java/vn/nguongocso/event/dto/response/LotValidationResponse.java
- backend/src/main/java/vn/nguongocso/event/dto/response/OfflineEventSyncResponse.java
- backend/src/main/java/vn/nguongocso/event/dto/response/OfflineEventSyncResultDto...java
- backend/src/main/java/vn/nguongocso/event/dto/response/ScanLookupResponse.java
- backend/src/main/java/vn/nguongocso/event/dto/response/StorageConditionResponse.java
- backend/src/main/java/vn/nguongocso/event/dto/response/ThresholdInfo.java
- backend/src/main/java/vn/nguongocso/event/dto/response/WarehouseReceiptResponse.java
- backend/src/main/java/vn/nguongocso/event/entity/ChainEvent.java
- backend/src/main/java/vn/nguongocso/event/entity/FailedEventLog.java
- backend/src/main/java/vn/nguongocso/event/entity/OfflineSyncLog.java
- backend/src/main/java/vn/nguongocso/event/enums/ChainEventType.java
- backend/src/main/java/vn/nguongocso/event/repository/ChainEventRepository.java
- backend/src/main/java/vn/nguongocso/event/repository/FailedEventLogRepository.java
- backend/src/main/java/vn/nguongocso/event/repository/OfflineSyncLogRepository.java
- backend/src/main/java/vn/nguongocso/event/service/ChainEventService.java
- backend/src/main/java/vn/nguongocso/event/service/EventHashService.java
- backend/src/main/java/vn/nguongocso/event/service/EventValidationService.java
- backend/src/main/java/vn/nguongocso/event/service/JourneyService.java
- backend/src/main/java/vn/nguongocso/event/service/OfflineSyncService.java
- backend/src/main/java/vn/nguongocso/event/service/ProcurementEventService.java
- backend/src/main/java/vn/nguongocso/event/service/WarehouseReceiptService.java
- backend/src/main/java/vn/nguongocso/event/service/impl/ChainEventServiceImpl.java
- backend/src/main/java/vn/nguongocso/event/service/impl/EventValidationServiceImpl...java
- backend/src/main/java/vn/nguongocso/event/service/impl/OfflineSyncEventProcessor...java
- backend/src/main/java/vn/nguongocso/event/service/impl/OfflineSyncServiceImpl.java
- backend/src/main/java/vn/nguongocso/event/service/impl/ProcurementEventServiceImp...java
- backend/src/main/java/vn/nguongocso/event/service/impl/WarehouseReceiptServiceImp...java
- backend/src/test/java/vn/nguongocso/event/controller/ChainEventControllerTest.java
- backend/src/test/java/vn/nguongocso/event/controller/EventValidationControllerTes...java
- backend/src/test/java/vn/nguongocso/event/controller/OfflineSyncControllerTest.java
- backend/src/test/java/vn/nguongocso/event/controller/ProcurementEventControllerTe...java
- backend/src/test/java/vn/nguongocso/event/controller/WarehouseReceiptControllerTe...java
- backend/src/test/java/vn/nguongocso/event/impl/ActivityLogEmissionPolicyTest.java
- backend/src/test/java/vn/nguongocso/event/impl/ChainEventScanLookupTest.java
- backend/src/test/java/vn/nguongocso/event/impl/ChainEventServiceImplTest.java
- backend/src/test/java/vn/nguongocso/event/impl/MobileChainEventServiceImplTest.java
- backend/src/test/java/vn/nguongocso/event/service/ChainVerificationServiceTest.java
- backend/src/test/java/vn/nguongocso/event/service/EventHashServiceTest.java
- backend/src/test/java/vn/nguongocso/event/service/ProcurementEventServiceTest.java
- backend/src/test/java/vn/nguongocso/event/service/impl/EventValidationServiceImpl...java
- backend/src/test/java/vn/nguongocso/event/service/impl/OfflineSyncEventProcessorF...java
- backend/src/test/java/vn/nguongocso/event/service/impl/OfflineSyncEventProcessorT...java
- backend/src/test/java/vn/nguongocso/event/service/impl/OfflineSyncServiceImplTest...java

Tổng cộng: ~120 file (main + test, không tính target/classes).
