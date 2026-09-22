package vn.nguongocso.certification.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionResultRequest;
import vn.nguongocso.certification.dto.response.CanActivateSealCheckResponse;
import vn.nguongocso.certification.dto.response.CriterionHistoryEntry;
import vn.nguongocso.certification.dto.response.CriterionHistoryResponse;
import vn.nguongocso.certification.dto.response.InspectionCriterionResultResponse;
import vn.nguongocso.certification.entity.CategoryCriterion;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.InspectionResultEntryLink;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.enums.InspectionResultEntryLinkStatus;
import vn.nguongocso.certification.enums.InspectionResultEntrySource;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.InspectionResultEntryLinkRepository;
import vn.nguongocso.certification.service.InspectionCriterionResultService;
import vn.nguongocso.certification.service.InspectionExpiryService;
import vn.nguongocso.certification.service.InspectionResultEntryLinkService;
import vn.nguongocso.certification.service.InspectionResultPortalFileStorageService;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.service.NotificationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ quản lý kết quả kiểm nghiệm chỉ tiêu của lô sản xuất.
 */
@Service
@Slf4j
@Transactional
public class InspectionCriterionResultServiceImpl implements InspectionCriterionResultService {
        private static final String MSG_REQUEST_NOT_FOUND = "Yêu cầu kiểm nghiệm không tồn tại.";
        private static final String MSG_LOT_NOT_FOUND = "Lô sản xuất không tồn tại.";
        private static final String MSG_CRITERION_NOT_FOUND = "Chỉ tiêu kiểm nghiệm không tồn tại.";
        private static final String MSG_RESULT_NOT_FOUND = "Kết quả kiểm nghiệm không tồn tại.";
        private static final String MSG_REQUEST_STATUS_INVALID = "Yêu cầu kiểm nghiệm phải ở trạng thái chờ kết quả hoặc không đạt";
        private static final String MSG_EXPIRY_BEFORE_RESULT_DATE = "Ngày hết hiệu lực phải sau ngày cấp";
        private static final String MSG_RESULT_DATE_BEFORE_SAMPLE_SENT = "Ngày cấp kết quả không được trước ngày gửi mẫu.";
        private static final String MSG_EXPIRY_IN_PAST = "Ngày hết hiệu lực phải >= ngày hiện tại";
        private static final String MSG_RESULTS_NOT_EMPTY = "Danh sách kết quả kiểm nghiệm không được để trống.";
        private static final String MSG_RESULTS_MUST_COVER_ALL = "Phải ghi kết quả cho tất cả chỉ tiêu của yêu cầu kiểm nghiệm.";
        private static final String MSG_CRITERION_NOT_IN_REQUEST = "Chỉ tiêu không thuộc yêu cầu kiểm nghiệm.";
        private static final String MSG_DUPLICATE_CRITERIA_IN_PAYLOAD = "Danh sách kết quả không được chứa chỉ tiêu trùng lặp.";
        private static final String MSG_FILE_EMPTY = "File không được để trống";
        private static final String MSG_FILE_TOO_LARGE = "File vượt quá dung lượng cho phép";
        private static final String MSG_FILE_TYPE_NOT_SUPPORTED = "Loại file không hỗ trợ. Chỉ chấp nhận JPG, PNG, PDF";
        private static final String MSG_FILE_SAVE_ERROR = "Lỗi hệ thống khi lưu file";
        private static final String MSG_FILE_NOT_FOUND = "Phiếu kết quả kiểm nghiệm chưa có file đính kèm";
        private static final String MSG_FILE_INVALID_PATH = "Đường dẫn file không hợp lệ";
        private static final String MSG_FILE_UNREADABLE = "File không tồn tại hoặc không thể đọc";
        private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
                        "image/jpeg", "image/png", "application/pdf");

        private final InspectionCriterionResultRepository resultRepository;
        private final InspectionCriterionRepository criterionRepository;
        private final InspectionRequestRepository requestRepository;
        private final CategoryCriterionRepository categoryCriterionRepository;
        private final ProductionLotRepository lotRepository;
        private final Clock clock;
        private final ApplicationEventPublisher eventPublisher;
        private final NotificationService notificationService;
        private final InspectionExpiryService inspectionExpiryService;
        private final InspectionResultEntryLinkService linkService;
        private final InspectionResultEntryLinkRepository linkRepository;
        private final InspectionResultPortalFileStorageService portalFileStorageService;

        @Value("${app.upload.base-dir}")
        private String baseDir;
        @Value("${app.upload.inspection-result.relative-path:inspection-results}")
        private String inspectionResultRelativePath;
        @Value("${app.upload.inspection-result.max-size:5242880}")
        private long maxFileSize;

        /**
         * Khởi tạo đối tượng triển khai dịch vụ quản lý kết quả kiểm nghiệm chỉ tiêu.
         */
        public InspectionCriterionResultServiceImpl(
                        InspectionCriterionResultRepository resultRepository,
                        InspectionCriterionRepository criterionRepository,
                        InspectionRequestRepository requestRepository,
                        CategoryCriterionRepository categoryCriterionRepository,
                        ProductionLotRepository lotRepository,
                        Clock clock,
                        ApplicationEventPublisher eventPublisher,
                        NotificationService notificationService,
                        InspectionExpiryService inspectionExpiryService,
                        InspectionResultEntryLinkService linkService,
                        InspectionResultEntryLinkRepository linkRepository,
                        InspectionResultPortalFileStorageService portalFileStorageService) {
                this.resultRepository = resultRepository;
                this.criterionRepository = criterionRepository;
                this.requestRepository = requestRepository;
                this.categoryCriterionRepository = categoryCriterionRepository;
                this.lotRepository = lotRepository;
                this.clock = clock;
                this.eventPublisher = eventPublisher;
                this.notificationService = notificationService;
                this.inspectionExpiryService = inspectionExpiryService;
                this.linkService = linkService;
                this.linkRepository = linkRepository;
                this.portalFileStorageService = portalFileStorageService;
        }

        /**
         * Ghi nhận hoặc cập nhật kết quả kiểm nghiệm cho một chỉ tiêu đơn lẻ.
         */
        @Override
        public InspectionCriterionResultResponse recordOrUpdateResult(
                        String criterionId,
                        InspectionCriterionResultRequest request,
                        CustomUserDetails currentUser) {
                UUID criterionUUID = parseUuid(criterionId, MSG_CRITERION_NOT_FOUND);
                InspectionRequest inspectionRequest = requestRepository
                                .findByCriterionIdAndOrganizationIdForUpdate(
                                                criterionUUID,
                                                currentUser.getOrganizationId())
                                .orElseThrow(() -> new BusinessException(MSG_CRITERION_NOT_FOUND));

                InspectionCriterion criterion = criterionRepository
                                .findById(criterionUUID)
                                .orElseThrow(() -> new BusinessException(MSG_CRITERION_NOT_FOUND));

                InspectionRequestStatus requestStatus = inspectionRequest.getStatus();
                if (requestStatus != InspectionRequestStatus.PENDING_RESULT
                                && requestStatus != InspectionRequestStatus.FAILED) {
                        throw new IllegalStateException(MSG_REQUEST_STATUS_INVALID);
                }

                if (Boolean.TRUE.equals(request.getPassed())) {
                        if (inspectionRequest.getSampleSentDate() != null
                                        && request.getResultDate() != null
                                        && request.getResultDate().isBefore(inspectionRequest.getSampleSentDate())) {
                                throw new IllegalArgumentException(MSG_RESULT_DATE_BEFORE_SAMPLE_SENT);
                        }

                        if (request.getExpiryDate().isBefore(request.getResultDate())) {
                                throw new IllegalArgumentException(MSG_EXPIRY_BEFORE_RESULT_DATE);
                        }

                        if (request.getExpiryDate().isBefore(LocalDate.now(clock))) {
                                throw new IllegalArgumentException(MSG_EXPIRY_IN_PAST);
                        }
                }

                Optional<InspectionCriterionResult> existingResult = resultRepository
                                .findByInspectionCriterion_Id(criterionUUID);
                boolean isNewResult = existingResult.isEmpty();

                InspectionCriterionResult result = existingResult
                                .orElseGet(() -> InspectionCriterionResult.builder()
                                                .inspectionCriterion(criterion)
                                                .createdBy(currentUser.getUser())
                                                .build());

                result.setResultDate(request.getResultDate());
                result.setExpiryDate(request.getExpiryDate());
                result.setPassed(request.getPassed());
                result.setFilePath(request.getFilePath());
                result.setEntrySource(InspectionResultEntrySource.COOPERATIVE_MANUAL);
                result.setCreatedBy(currentUser.getUser());
                result.setPortalLink(null);

                linkService.revokeActiveLinksForRequest(inspectionRequest.getId(), currentUser.getUser());

                result = resultRepository.save(result);

                checkAndUpdateRequestStatus(inspectionRequest);

                if (inspectionRequest.getProductionLot() != null) {
                        try {
                                inspectionExpiryService.checkAndAlertLotExpiry(
                                                inspectionRequest.getProductionLot(),
                                                LocalDate.now(clock));
                        } catch (Exception e) {
                                log.error("Lỗi khi quét và cảnh báo hiệu lực kiểm nghiệm sau khi ghi nhận kết quả kiểm nghiệm: ",
                                                e);
                        }
                }

                publishActivityLog(
                                currentUser,
                                isNewResult ? "RECORD_INSPECTION_RESULT" : "UPDATE_INSPECTION_RESULT",
                                (isNewResult ? "Ghi" : "Cập nhật") + " kết quả kiểm nghiệm cho chỉ tiêu '"
                                                + criterion.getCriterionName() + "' của yêu cầu kiểm nghiệm ID "
                                                + inspectionRequest.getId()
                                                + (Boolean.TRUE.equals(request.getPassed()) ? " (đạt)"
                                                                : " (không đạt)"),
                                "INSPECTION_CRITERION_RESULT",
                                result.getId().toString());

                return toResponse(result);
        }

        /**
         * Ghi nhận đồng loạt kết quả kiểm nghiệm cho tất cả chỉ tiêu của một yêu cầu.
         */
        @Override
        @Transactional
        public List<InspectionCriterionResultResponse> recordResults(
                        UUID inspectionRequestId,
                        List<InspectionCriterionResultRequest> requests,
                        CustomUserDetails currentUser) {

                InspectionRequest inspectionRequest = requestRepository
                                .findByIdAndOrganizationIdForUpdate(inspectionRequestId,
                                                currentUser.getOrganizationId())
                                .or(() -> requestRepository.findById(inspectionRequestId))
                                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                                                "Yêu cầu kiểm nghiệm không tồn tại."));

                if (inspectionRequest.getProductionLot() != null
                                && inspectionRequest.getProductionLot().getOrganization() != null
                                && !inspectionRequest.getProductionLot().getOrganization().getOrganizationId()
                                                .equals(currentUser.getOrganizationId())) {
                        throw new BusinessException(HttpStatus.NOT_FOUND, "Yêu cầu kiểm nghiệm không tồn tại.");
                }

                InspectionRequestStatus requestStatus = inspectionRequest.getStatus();
                if (requestStatus != InspectionRequestStatus.PENDING_RESULT
                                && requestStatus != InspectionRequestStatus.FAILED) {
                        throw new BusinessException(HttpStatus.CONFLICT, MSG_REQUEST_STATUS_INVALID);
                }

                if (requests == null || requests.isEmpty()) {
                        throw new BusinessException(MSG_RESULTS_NOT_EMPTY);
                }

                List<InspectionCriterion> criteria = inspectionRequest.getCriteria();
                if (criteria == null || criteria.isEmpty()) {
                        throw new BusinessException(
                                        "Yêu cầu kiểm nghiệm không có chỉ tiêu nào.");
                }

                Map<UUID, InspectionCriterion> criterionById = criteria.stream()
                                .collect(Collectors.toMap(
                                                InspectionCriterion::getId,
                                                c -> c));

                LocalDate today = LocalDate.now(clock);
                Set<UUID> seenCriterionIds = new HashSet<>();

                for (InspectionCriterionResultRequest item : requests) {
                        UUID criterionId = parseUuid(
                                        item.getCriterionId(),
                                        MSG_CRITERION_NOT_FOUND);

                        if (!criterionById.containsKey(criterionId)) {
                                throw new BusinessException(MSG_CRITERION_NOT_IN_REQUEST);
                        }

                        if (!seenCriterionIds.add(criterionId)) {
                                throw new BusinessException(MSG_DUPLICATE_CRITERIA_IN_PAYLOAD);
                        }

                        validateResultDates(item, today, inspectionRequest.getSampleSentDate());
                }

                if (seenCriterionIds.size() != criteria.size()) {
                        throw new BusinessException(MSG_RESULTS_MUST_COVER_ALL);
                }

                linkService.revokeActiveLinksForRequest(inspectionRequestId, currentUser.getUser());

                List<InspectionCriterionResult> results = new ArrayList<>();
                for (InspectionCriterionResultRequest item : requests) {
                        UUID criterionId = parseUuid(item.getCriterionId(), MSG_CRITERION_NOT_FOUND);

                        InspectionCriterionResult result = resultRepository
                                        .findByInspectionCriterion_Id(criterionId)
                                        .orElseGet(() -> InspectionCriterionResult.builder()
                                                        .inspectionCriterion(criterionById.get(criterionId))
                                                        .createdBy(currentUser.getUser())
                                                        .build());

                        result.setResultDate(item.getResultDate());
                        result.setExpiryDate(item.getExpiryDate());
                        result.setPassed(item.getPassed());
                        result.setFilePath(item.getFilePath());
                        result.setEntrySource(InspectionResultEntrySource.COOPERATIVE_MANUAL);
                        result.setCreatedBy(currentUser.getUser());
                        result.setPortalLink(null);

                        results.add(result);
                }

                resultRepository.saveAll(results);

                checkAndUpdateRequestStatus(inspectionRequest);

                if (inspectionRequest.getProductionLot() != null) {
                        try {
                                inspectionExpiryService.checkAndAlertLotExpiry(
                                                inspectionRequest.getProductionLot(),
                                                LocalDate.now(clock));
                        } catch (Exception e) {
                                log.error("Lỗi khi quét và cảnh báo hiệu lực kiểm nghiệm sau khi ghi nhận hàng loạt kết quả kiểm nghiệm: ",
                                                e);
                        }
                }
                publishActivityLog(
                                currentUser,
                                "RECORD_INSPECTION_RESULTS",
                                "Ghi " + results.size() + " kết quả kiểm nghiệm cho yêu cầu kiểm nghiệm ID "
                                                + inspectionRequest.getId(),
                                "INSPECTION_REQUEST",
                                inspectionRequest.getId().toString());

                return results.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Lấy danh sách kết quả kiểm nghiệm theo mã yêu cầu kiểm nghiệm.
         */
        @Override
        @Transactional(readOnly = true)
        public List<InspectionCriterionResultResponse> getResultsByRequest(
                        UUID inspectionRequestId,
                        CustomUserDetails currentUser) {

                requireRequestAccess(inspectionRequestId, currentUser);

                return resultRepository
                                .findByInspectionCriterion_InspectionRequest_Id(inspectionRequestId)
                                .stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Lấy kết quả kiểm nghiệm của một chỉ tiêu cụ thể.
         */
        @Override
        @Transactional(readOnly = true)
        public InspectionCriterionResultResponse getResultByCriterion(
                        String criterionId,
                        CustomUserDetails currentUser) {

                UUID criterionUUID = parseUuid(criterionId, MSG_CRITERION_NOT_FOUND);
                InspectionCriterionResult result = resultRepository
                                .findByInspectionCriterion_Id(criterionUUID)
                                .orElseThrow(() -> new BusinessException(MSG_RESULT_NOT_FOUND));

                requireCriterionAccess(result.getInspectionCriterion(), currentUser);

                return toResponse(result);
        }

        /**
         * Xóa kết quả kiểm nghiệm của chỉ tiêu khi yêu cầu còn ở trạng thái chờ kết quả.
         */
        @Override
        public void deleteResult(
                        String resultId,
                        CustomUserDetails currentUser) {

                UUID resultUUID = parseUuid(resultId, MSG_RESULT_NOT_FOUND);
                InspectionCriterionResult result = resultRepository
                                .findById(resultUUID)
                                .orElseThrow(() -> new BusinessException(MSG_RESULT_NOT_FOUND));

                requireCriterionAccess(result.getInspectionCriterion(), currentUser);

                InspectionRequest inspectionRequest = result.getInspectionCriterion().getInspectionRequest();
                if (inspectionRequest.getStatus() != InspectionRequestStatus.PENDING_RESULT) {
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "Không thể xóa kết quả của yêu cầu kiểm nghiệm đã có kết luận.");
                }
                String criterionName = result.getInspectionCriterion().getCriterionName();
                UUID requestId = inspectionRequest.getId();

                resultRepository.delete(result);

                checkAndUpdateRequestStatus(inspectionRequest);

                publishActivityLog(
                                currentUser,
                                "DELETE_INSPECTION_RESULT",
                                "Xóa kết quả kiểm nghiệm của chỉ tiêu '" + criterionName
                                                + "' thuộc yêu cầu kiểm nghiệm ID " + requestId,
                                "INSPECTION_CRITERION_RESULT",
                                resultUUID.toString());
        }

        /**
         * Tải lên tệp đính kèm phiếu kết quả kiểm nghiệm cho chỉ tiêu.
         */
        @Override
        public String uploadResultFile(
                        String criterionId,
                        MultipartFile file,
                        CustomUserDetails currentUser) {

                UUID criterionUUID = parseUuid(criterionId, MSG_CRITERION_NOT_FOUND);
                InspectionCriterion criterion = criterionRepository
                                .findById(criterionUUID)
                                .orElseThrow(() -> new BusinessException(MSG_CRITERION_NOT_FOUND));

                requireCriterionAccess(criterion, currentUser);

                if (file == null || file.isEmpty()) {
                        throw new BusinessException(MSG_FILE_EMPTY);
                }

                if (file.getSize() > maxFileSize) {
                        throw new BusinessException(
                                        MSG_FILE_TOO_LARGE + " (" + maxFileSize / 1024 / 1024 + "MB)");
                }

                String contentType = file.getContentType();
                if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType)) {
                        throw new BusinessException(MSG_FILE_TYPE_NOT_SUPPORTED);
                }

                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                        extension = originalFilename
                                        .substring(originalFilename.lastIndexOf("."));
                }

                String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
                String uploadDir = Paths.get(
                                baseDir,
                                inspectionResultRelativePath,
                                criterion.getInspectionRequest().getId().toString())
                                .toString();
                String filePath = Paths.get(uploadDir, newFileName).toString();

                try {
                        Path uploadPath = Paths.get(uploadDir);
                        if (!Files.exists(uploadPath)) {
                                Files.createDirectories(uploadPath);
                        }
                        Files.copy(
                                        file.getInputStream(),
                                        Paths.get(filePath),
                                        StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                        throw new BusinessException(MSG_FILE_SAVE_ERROR);
                }

                publishActivityLog(
                                currentUser,
                                "UPLOAD_INSPECTION_RESULT_FILE",
                                "Tải lên phiếu kết quả kiểm nghiệm cho chỉ tiêu '"
                                                + criterion.getCriterionName() + "' của yêu cầu kiểm nghiệm ID "
                                                + criterion.getInspectionRequest().getId()
                                                + (originalFilename != null ? " (tệp " + originalFilename + ")" : ""),
                                "INSPECTION_CRITERION",
                                criterionUUID.toString());

                return filePath;
        }

        /**
         * Lấy tài nguyên tệp đính kèm kết quả kiểm nghiệm an toàn của hệ thống.
         */
        @Override
        @Transactional(readOnly = true)
        public ResultFileResource getResultFile(
                        String resultId,
                        CustomUserDetails currentUser) {

                UUID resultUUID = parseUuid(resultId, MSG_RESULT_NOT_FOUND);
                InspectionCriterionResult result = resultRepository
                                .findById(resultUUID)
                                .orElseThrow(() -> new BusinessException(MSG_RESULT_NOT_FOUND));

                requireCriterionAccess(result.getInspectionCriterion(), currentUser);

                if (result.getFilePath() == null || result.getFilePath().isBlank()) {
                        throw new BusinessException(MSG_FILE_NOT_FOUND);
                }
                Path filePath = Paths.get(result.getFilePath())
                                .toAbsolutePath()
                                .normalize();
                Path baseDirPath = Paths.get(baseDir)
                                .toAbsolutePath()
                                .normalize();
                if (!filePath.startsWith(baseDirPath)) {
                        throw new BusinessException(MSG_FILE_INVALID_PATH);
                }

                if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                        throw new BusinessException(MSG_FILE_UNREADABLE);
                }

                String fileName = filePath.getFileName().toString();

                return new ResultFileResource(
                                new FileSystemResource(filePath),
                                resolveContentType(fileName),
                                fileName);
        }

        /**
         * Đánh giá điều kiện kiểm nghiệm của lô sản xuất để kích hoạt tem xác thực.
         */
        @Override
        @Transactional(readOnly = true)
        public CanActivateSealCheckResponse checkCanActivateSeal(
                        UUID productionLotId,
                        CustomUserDetails currentUser) {
                ProductionLot lot = lotRepository
                                .findByIdAndOrganization_OrganizationId(
                                                productionLotId,
                                                currentUser.getOrganizationId())
                                .orElseThrow(() -> new BusinessException(MSG_LOT_NOT_FOUND));
                ProductCategory category = lot.getProductCategory();
                boolean mandatoryInspection = category != null
                                && Boolean.TRUE.equals(
                                                category.getRequiresInspection());

                LocalDate today = LocalDate.now(clock);

                if (!mandatoryInspection) {
                        return CanActivateSealCheckResponse.builder()
                                        .productionLotId(productionLotId.toString())
                                        .canActivate(true)
                                        .reason(null)
                                        .earliestExpiryDate(null)
                                        .totalCriteria(0)
                                        .passedCriteria(0)
                                        .failedOrExpiredCriteria(0)
                                        .build();
                }

                List<CategoryCriterion> assignments = categoryCriterionRepository
                                .findByCategoryIdAndCriteriaStatus(
                                                category.getId(),
                                                "ACTIVE");

                int totalCriteria = assignments.size();

                Map<String, InspectionCriterionResult> latestByCode = new HashMap<>();
                for (InspectionCriterionResult result : resultRepository
                                .findAllByProductionLotId(productionLotId)) {

                        if (result.getResultDate() == null) {
                                continue;
                        }

                        String code = result.getInspectionCriterion()
                                        .getCriterionCode();
                        InspectionCriterionResult current = latestByCode.get(code);
                        LocalDateTime resultUpdatedAt = result.getUpdatedAt();
                        LocalDateTime currentUpdatedAt = current == null ? null : current.getUpdatedAt();

                        boolean isNewer = current == null
                                        || result.getResultDate()
                                                        .isAfter(current.getResultDate())
                                        || (result.getResultDate()
                                                        .isEqual(current.getResultDate())
                                                        && resultUpdatedAt != null
                                                        && (currentUpdatedAt == null
                                                                        || resultUpdatedAt.isAfter(
                                                                                        currentUpdatedAt)));

                        if (isNewer) {
                                latestByCode.put(code, result);
                        }
                }

                int passedCriteria = 0;
                boolean hasExpiredResult = false;
                LocalDate earliestExpiry = null;

                for (CategoryCriterion assignment : assignments) {
                        String code = assignment.getCriterion().getName();
                        InspectionCriterionResult latest = latestByCode.get(code);

                        if (latest == null) {
                                continue;
                        }

                        if (Boolean.TRUE.equals(latest.getPassed())
                                        && latest.getExpiryDate() != null
                                        && (earliestExpiry == null
                                                        || latest.getExpiryDate().isBefore(earliestExpiry))) {
                                earliestExpiry = latest.getExpiryDate();
                        }

                        if (!Boolean.TRUE.equals(latest.getPassed())) {
                                continue;
                        }

                        if (latest.getExpiryDate().isBefore(today)) {
                                hasExpiredResult = true;
                                continue;
                        }

                        passedCriteria++;
                }

                boolean canActivate = totalCriteria > 0
                                && passedCriteria == totalCriteria;

                String reason = null;
                if (!canActivate) {
                        if (totalCriteria == 0) {
                                reason = "Loại nông sản bắt buộc kiểm nghiệm nhưng chưa "
                                                + "được cấu hình chỉ tiêu kiểm nghiệm";
                        } else if (hasExpiredResult) {
                                reason = "Kết quả kiểm nghiệm đã quá hạn";
                        } else {
                                reason = "Lô chưa có kết quả kiểm nghiệm đạt cho tất cả "
                                                + "chỉ tiêu";
                        }
                }

                int failedOrExpired = totalCriteria - passedCriteria;

                return CanActivateSealCheckResponse.builder()
                                .productionLotId(productionLotId.toString())
                                .canActivate(canActivate)
                                .reason(reason)
                                .earliestExpiryDate(earliestExpiry)
                                .totalCriteria(totalCriteria)
                                .passedCriteria(passedCriteria)
                                .failedOrExpiredCriteria(failedOrExpired)
                                .build();
        }

        /**
         * Lấy lịch sử kiểm nghiệm của lô sản xuất theo từng mã chỉ tiêu.
         */
        @Override
        @Transactional(readOnly = true)
        public List<CriterionHistoryResponse> getInspectionHistory(
                        UUID productionLotId,
                        CustomUserDetails currentUser) {

                lotRepository
                                .findByIdAndOrganization_OrganizationId(
                                                productionLotId,
                                                currentUser.getOrganizationId())
                                .orElseThrow(() -> new BusinessException(MSG_LOT_NOT_FOUND));

                List<InspectionCriterionResult> results = resultRepository
                                .findAllByProductionLotId(productionLotId);

                List<InspectionCriterionResult> sorted = results.stream()
                                .sorted(Comparator.comparing(
                                                InspectionCriterionResult::getCreatedAt,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList();

                Map<String, List<CriterionHistoryEntry>> historyByCode = new LinkedHashMap<>();
                Map<String, InspectionCriterion> criterionByCode = new HashMap<>();

                for (InspectionCriterionResult result : sorted) {
                        InspectionCriterion criterion = result.getInspectionCriterion();
                        if (criterion == null) {
                                continue;
                        }
                        String code = criterion.getCriterionCode();
                        InspectionRequest request = criterion.getInspectionRequest();

                        CriterionHistoryEntry entry = CriterionHistoryEntry.builder()
                                        .requestId(request != null
                                                        ? request.getId().toString()
                                                        : null)
                                        .sampleSentDate(request != null
                                                        ? request.getSampleSentDate()
                                                        : null)
                                        .resultDate(result.getResultDate())
                                        .expiryDate(result.getExpiryDate())
                                        .passed(Boolean.TRUE.equals(result.getPassed()))
                                        .testingUnit(request != null
                                                        ? request.getInspectionUnit()
                                                        : null)
                                        .createdByName(result.getCreatedBy() != null
                                                        ? result.getCreatedBy().getFullName()
                                                        : (request != null ? request.getInspectionUnit()
                                                                        : "Đơn vị kiểm nghiệm"))
                                        .createdAt(result.getCreatedAt())
                                        .build();

                        historyByCode
                                        .computeIfAbsent(code, k -> new ArrayList<>())
                                        .add(entry);
                        criterionByCode.putIfAbsent(code, criterion);
                }

                List<CriterionHistoryResponse> historyResponses = new ArrayList<>();
                for (Map.Entry<String, List<CriterionHistoryEntry>> group : historyByCode.entrySet()) {

                        InspectionCriterion criterion = criterionByCode.get(group.getKey());

                        historyResponses.add(
                                        CriterionHistoryResponse.builder()
                                                        .criterionDefinitionId(
                                                                        criterion != null
                                                                                        ? criterion.getCriterionId()
                                                                                        : null)
                                                        .criterionCode(group.getKey())
                                                        .criterionName(criterion != null
                                                                        ? criterion.getCriterionName()
                                                                        : null)
                                                        .history(group.getValue())
                                                        .build());
                }

                return historyResponses;
        }

        /**
         * Kiểm tra và cập nhật trạng thái của yêu cầu kiểm nghiệm dựa trên toàn bộ kết quả chỉ tiêu.
         */
        private void checkAndUpdateRequestStatus(
                        InspectionRequest inspectionRequest) {

                UUID requestId = inspectionRequest.getId();
                LocalDate today = LocalDate.now(clock);

                int totalCriteria = resultRepository.countTotalCriteria(requestId);
                List<InspectionCriterionResult> results = resultRepository
                                .findByInspectionCriterion_InspectionRequest_Id(requestId);

                InspectionRequestStatus newStatus;
                if (totalCriteria == 0) {
                        newStatus = InspectionRequestStatus.PENDING_RESULT;
                } else if (results.size() < totalCriteria) {
                        newStatus = InspectionRequestStatus.PENDING_RESULT;
                } else {
                        boolean allPassedAndValid = results.stream()
                                        .allMatch(result -> result.getPassed()
                                                        && !result.getExpiryDate().isBefore(today));
                        newStatus = allPassedAndValid
                                        ? InspectionRequestStatus.PASSED
                                        : InspectionRequestStatus.FAILED;
                }

                if (newStatus != inspectionRequest.getStatus()) {
                        inspectionRequest.setStatus(newStatus);
                        requestRepository.save(inspectionRequest);

                        if ((newStatus == InspectionRequestStatus.PASSED
                                        || newStatus == InspectionRequestStatus.FAILED)
                                        && inspectionRequest.getProductionLot() != null) {
                                ProductionLot lot = inspectionRequest.getProductionLot();
                                if (lot.getOrganization() != null) {
                                        UUID organizationId = lot.getOrganization().getOrganizationId();
                                        if (newStatus == InspectionRequestStatus.PASSED) {
                                                notificationService.sendInspectionPassedNotification(
                                                                lot.getName(),
                                                                organizationId);
                                        } else {
                                                notificationService.sendInspectionFailedNotification(
                                                                lot.getName(),
                                                                organizationId);
                                        }
                                }
                        }
                }
        }

        /**
         * Kiểm tra quyền truy cập của người dùng đối với chỉ tiêu kiểm nghiệm.
         */
        private void requireCriterionAccess(
                        InspectionCriterion criterion,
                        CustomUserDetails currentUser) {

                InspectionRequest inspectionRequest = criterion.getInspectionRequest();
                if (inspectionRequest == null
                                || !isOwnedByOrganization(inspectionRequest, currentUser)) {
                        throw new BusinessException(MSG_REQUEST_NOT_FOUND);
                }
        }

        /**
         * Kiểm tra quyền truy cập của người dùng đối với yêu cầu kiểm nghiệm.
         */
        private InspectionRequest requireRequestAccess(
                        UUID inspectionRequestId,
                        CustomUserDetails currentUser) {

                InspectionRequest inspectionRequest = requestRepository
                                .findById(inspectionRequestId)
                                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

                if (!isOwnedByOrganization(inspectionRequest, currentUser)) {
                        throw new BusinessException(MSG_REQUEST_NOT_FOUND);
                }

                return inspectionRequest;
        }

        /**
         * Kiểm tra yêu cầu kiểm nghiệm có thuộc về tổ chức của người dùng hay không.
         */
        private boolean isOwnedByOrganization(
                        InspectionRequest inspectionRequest,
                        CustomUserDetails currentUser) {

                if (inspectionRequest.getProductionLot() == null
                                || inspectionRequest.getProductionLot().getOrganization() == null) {
                        return false;
                }

                return inspectionRequest
                                .getProductionLot()
                                .getOrganization()
                                .getOrganizationId()
                                .equals(currentUser.getOrganizationId());
        }

        /**
         * Kiểm tra tính hợp lệ của ngày cấp và ngày hết hiệu lực của kết quả chỉ tiêu.
         */
        private void validateResultDates(
                        InspectionCriterionResultRequest item,
                        LocalDate today,
                        LocalDate sampleSentDate) {
                if (Boolean.FALSE.equals(item.getPassed())) {
                        return;
                }

                if (item.getResultDate() == null
                                || item.getExpiryDate() == null
                                || item.getPassed() == null) {
                        throw new BusinessException(
                                        "Kết quả kiểm nghiệm phải đầy đủ ngày cấp, ngày hết hiệu lực và kết luận.");
                }

                if (sampleSentDate != null && item.getResultDate() != null
                                && item.getResultDate().isBefore(sampleSentDate)) {
                        throw new BusinessException(MSG_RESULT_DATE_BEFORE_SAMPLE_SENT);
                }

                if (item.getExpiryDate().isBefore(item.getResultDate())) {
                        throw new BusinessException(MSG_EXPIRY_BEFORE_RESULT_DATE);
                }

                if (item.getExpiryDate().isBefore(today)) {
                        throw new BusinessException(MSG_EXPIRY_IN_PAST);
                }
        }

        /**
         * Tải lên tệp kết quả kiểm nghiệm từ cổng nhập liệu công khai của đơn vị kiểm nghiệm.
         */
        @Override
        public String uploadPortalResultFile(
                        String token,
                        String criterionId,
                        MultipartFile file,
                        String clientIp) {

                InspectionResultEntryLink link = linkService.validateAndGetActiveLink(token, clientIp);

                UUID criterionUUID = parseUuid(criterionId, MSG_CRITERION_NOT_FOUND);
                InspectionCriterion criterion = criterionRepository
                                .findById(criterionUUID)
                                .orElseThrow(() -> new BusinessException(MSG_CRITERION_NOT_FOUND));

                if (!criterion.getInspectionRequest().getId().equals(link.getInspectionRequest().getId())) {
                        throw new BusinessException(HttpStatus.NOT_FOUND, MSG_CRITERION_NOT_IN_REQUEST);
                }

                if (file == null || file.isEmpty()) {
                        throw new BusinessException(MSG_FILE_EMPTY);
                }

                if (file.getSize() > maxFileSize) {
                        throw new BusinessException(
                                        HttpStatus.PAYLOAD_TOO_LARGE,
                                        MSG_FILE_TOO_LARGE + " (" + maxFileSize / 1024 / 1024 + "MB)");
                }

                String contentType = file.getContentType();
                if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType)) {
                        throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, MSG_FILE_TYPE_NOT_SUPPORTED);
                }

                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                        extension = originalFilename
                                        .substring(originalFilename.lastIndexOf("."));
                }

                String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
                String uploadDir = Paths.get(
                                baseDir,
                                inspectionResultRelativePath,
                                criterion.getInspectionRequest().getId().toString())
                                .toString();
                String filePath = Paths.get(uploadDir, newFileName).toString();

                try {
                        Path uploadPath = Paths.get(uploadDir);
                        if (!Files.exists(uploadPath)) {
                                Files.createDirectories(uploadPath);
                        }
                        Files.copy(
                                        file.getInputStream(),
                                        Paths.get(filePath),
                                        StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                        throw new BusinessException(MSG_FILE_SAVE_ERROR);
                }

                return portalFileStorageService.registerUploadedFile(
                                link.getTokenHash(),
                                link.getInspectionRequest().getId(),
                                criterion.getId(),
                                filePath,
                                originalFilename);
        }

        /**
         * Ghi nhận đồng loạt kết quả kiểm nghiệm từ cổng nhập liệu công khai của đơn vị kiểm nghiệm.
         */
        @Override
        @Transactional
        public List<InspectionCriterionResultResponse> recordPortalResults(
                        String token,
                        List<InspectionCriterionResultRequest> requests,
                        String clientIp,
                        String userAgent) {
                InspectionResultEntryLink link = linkService.validateAndGetActiveLink(token, clientIp);
                InspectionRequest originalRequest = link.getInspectionRequest();
                InspectionRequest inspectionRequest = requestRepository
                                .findByIdForUpdate(originalRequest.getId())
                                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                                                "Yêu cầu kiểm nghiệm không tồn tại."));

                if (inspectionRequest.getStatus() != InspectionRequestStatus.PENDING_RESULT) {
                        throw new BusinessException(HttpStatus.CONFLICT, MSG_REQUEST_STATUS_INVALID);
                }

                if (requests == null || requests.isEmpty()) {
                        throw new BusinessException(MSG_RESULTS_NOT_EMPTY);
                }

                List<InspectionCriterion> criteria = inspectionRequest.getCriteria();
                if (criteria == null || criteria.isEmpty()) {
                        throw new BusinessException("Yêu cầu kiểm nghiệm không có chỉ tiêu nào.");
                }

                Map<UUID, InspectionCriterion> criterionById = criteria.stream()
                                .collect(Collectors.toMap(InspectionCriterion::getId, c -> c));

                LocalDate today = LocalDate.now(clock);
                Set<UUID> seenCriterionIds = new HashSet<>();

                for (InspectionCriterionResultRequest item : requests) {
                        UUID criterionId = parseUuid(item.getCriterionId(), MSG_CRITERION_NOT_FOUND);

                        if (!criterionById.containsKey(criterionId)) {
                                throw new BusinessException(MSG_CRITERION_NOT_IN_REQUEST);
                        }

                        if (!seenCriterionIds.add(criterionId)) {
                                throw new BusinessException(MSG_DUPLICATE_CRITERIA_IN_PAYLOAD);
                        }

                        validateResultDates(item, today, inspectionRequest.getSampleSentDate());
                }

                if (seenCriterionIds.size() != criteria.size()) {
                        throw new BusinessException(MSG_RESULTS_MUST_COVER_ALL);
                }

                Map<UUID, String> resolvedRealFilePaths = new HashMap<>();
                for (InspectionCriterionResultRequest item : requests) {
                        UUID criterionId = parseUuid(item.getCriterionId(), MSG_CRITERION_NOT_FOUND);
                        if (item.getFilePath() != null && !item.getFilePath().isBlank()) {
                                String realPath = portalFileStorageService.validateAndConsumeHandle(
                                                item.getFilePath(),
                                                link.getTokenHash(),
                                                inspectionRequest.getId(),
                                                criterionId);
                                resolvedRealFilePaths.put(criterionId, realPath);
                        }
                }

                LocalDateTime now = LocalDateTime.now();
                String truncatedUserAgent = userAgent != null && userAgent.length() > 500
                                ? userAgent.substring(0, 500)
                                : userAgent;

                int consumed = linkRepository.consumeActiveLink(
                                link.getId(),
                                InspectionResultEntryLinkStatus.ACTIVE,
                                InspectionResultEntryLinkStatus.USED,
                                now,
                                clientIp,
                                truncatedUserAgent);

                if (consumed == 0) {
                        throw new BusinessException(HttpStatus.GONE, "Liên kết đã được sử dụng hoặc đã hết hạn.");
                }

                List<InspectionCriterionResult> results = new ArrayList<>();
                for (InspectionCriterionResultRequest item : requests) {
                        UUID criterionId = parseUuid(item.getCriterionId(), MSG_CRITERION_NOT_FOUND);

                        InspectionCriterionResult result = resultRepository
                                        .findByInspectionCriterion_Id(criterionId)
                                        .orElseGet(() -> InspectionCriterionResult.builder()
                                                        .inspectionCriterion(criterionById.get(criterionId))
                                                        .build());

                        result.setResultDate(item.getResultDate());
                        result.setExpiryDate(item.getExpiryDate());
                        result.setPassed(item.getPassed());
                        result.setFilePath(resolvedRealFilePaths.get(criterionId));
                        result.setEntrySource(InspectionResultEntrySource.TESTING_UNIT_PORTAL);
                        result.setPortalLink(link);
                        result.setCreatedBy(null);

                        results.add(result);
                }

                resultRepository.saveAll(results);

                checkAndUpdateRequestStatus(inspectionRequest);

                if (inspectionRequest.getProductionLot() != null) {
                        try {
                                inspectionExpiryService.checkAndAlertLotExpiry(
                                                inspectionRequest.getProductionLot(),
                                                LocalDate.now(clock));
                        } catch (Exception e) {
                                log.error("Lỗi khi quét và cảnh báo hiệu lực kiểm nghiệm sau khi đơn vị kiểm nghiệm ghi nhận kết quả: ",
                                                e);
                        }
                }

                return results.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Phát sự kiện ghi nhật ký hoạt động của người dùng khi thao tác với kết quả kiểm nghiệm.
         */
        private void publishActivityLog(
                        CustomUserDetails currentUser,
                        String action,
                        String description,
                        String entityType,
                        String entityId) {

                eventPublisher.publishEvent(ActivityLogEvent.builder()
                                .userId(currentUser.getUserId())
                                .username(currentUser.getUsername())
                                .fullName(currentUser.getFullName())
                                .organizationId(currentUser.getOrganizationId())
                                .action(action)
                                .description(description)
                                .entityType(entityType)
                                .entityId(entityId)
                                .ipAddress(IpUtils.getClientIp())
                                .timestamp(LocalDateTime.now())
                                .build());
        }

        /**
         * Chuyển đổi chuỗi định danh sang UUID và kiểm tra tính hợp lệ.
         */
        private UUID parseUuid(
                        String value,
                        String errorMessage) {
                try {
                        return UUID.fromString(value);
                } catch (IllegalArgumentException | NullPointerException e) {
                        throw new BusinessException(errorMessage);
                }
        }

        /**
         * Xác định MediaType tương ứng từ tên tệp đính kèm.
         */
        private MediaType resolveContentType(
                        String fileName) {
                String lower = fileName.toLowerCase();
                if (lower.endsWith(".png")) {
                        return MediaType.IMAGE_PNG;
                }
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                        return MediaType.IMAGE_JPEG;
                }
                if (lower.endsWith(".pdf")) {
                        return MediaType.APPLICATION_PDF;
                }
                return MediaType.APPLICATION_OCTET_STREAM;
        }

        /**
         * Chuyển đổi entity kết quả chỉ tiêu sang DTO phản hồi.
         */
        private InspectionCriterionResultResponse toResponse(
                        InspectionCriterionResult result) {

                String createdByName = result.getCreatedBy() != null
                                ? result.getCreatedBy().getFullName()
                                : "Đơn vị kiểm nghiệm";

                return InspectionCriterionResultResponse.builder()
                                .resultId(result.getId().toString())
                                .criterionId(
                                                result.getInspectionCriterion().getId().toString())
                                .criterionDefinitionId(
                                                result.getInspectionCriterion().getCriterionId())
                                .criterionCode(
                                                result.getInspectionCriterion().getCriterionCode())
                                .criterionName(
                                                result.getInspectionCriterion().getCriterionName())
                                .resultDate(result.getResultDate())
                                .expiryDate(result.getExpiryDate())
                                .passed(result.getPassed())
                                .filePath(result.getFilePath())
                                .entrySource(result.getEntrySource())
                                .createdByName(createdByName)
                                .createdAt(result.getCreatedAt())
                                .updatedAt(result.getUpdatedAt())
                                .build();
        }
}
