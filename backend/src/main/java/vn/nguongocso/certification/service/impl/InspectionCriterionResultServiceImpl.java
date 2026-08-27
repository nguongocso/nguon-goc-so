package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionResultRequest;
import vn.nguongocso.certification.dto.response.CanActivateSealCheckResponse;
import vn.nguongocso.certification.dto.response.InspectionCriterionResultResponse;
import vn.nguongocso.certification.entity.CategoryCriterion;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.service.InspectionCriterionResultService;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Triển khai service cho kết quả kiểm nghiệm.
 *
 * <p>
 * Mọi thao tác đều phải đảm bảo yêu cầu kiểm nghiệm / lô sản xuất thuộc
 * tổ chức của người dùng hiện tại (org boundary). Không tin bất kỳ ID nào
 * do client truyền vào.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InspectionCriterionResultServiceImpl
        implements InspectionCriterionResultService {

    private static final String MSG_REQUEST_NOT_FOUND =
            "Yêu cầu kiểm nghiệm không tồn tại.";

    private static final String MSG_LOT_NOT_FOUND =
            "Lô sản xuất không tồn tại.";

    private static final String MSG_CRITERION_NOT_FOUND =
            "Chỉ tiêu kiểm nghiệm không tồn tại.";

    private static final String MSG_RESULT_NOT_FOUND =
            "Kết quả kiểm nghiệm không tồn tại.";

    private static final String MSG_REQUEST_STATUS_INVALID =
            "Yêu cầu kiểm nghiệm phải ở trạng thái chờ kết quả hoặc không đạt";

    private static final String MSG_EXPIRY_BEFORE_RESULT_DATE =
            "Ngày hết hiệu lực phải sau ngày cấp";

    private static final String MSG_EXPIRY_IN_PAST =
            "Ngày hết hiệu lực phải >= ngày hiện tại";

    private static final String MSG_RESULTS_NOT_EMPTY =
            "Danh sách kết quả kiểm nghiệm không được để trống.";

    private static final String MSG_RESULTS_MUST_COVER_ALL =
            "Phải ghi kết quả cho tất cả chỉ tiêu của yêu cầu kiểm nghiệm.";

    private static final String MSG_CRITERION_NOT_IN_REQUEST =
            "Chỉ tiêu không thuộc yêu cầu kiểm nghiệm.";

    private static final String MSG_DUPLICATE_CRITERIA_IN_PAYLOAD =
            "Danh sách kết quả không được chứa chỉ tiêu trùng lặp.";

    private static final String MSG_FILE_EMPTY =
            "File không được để trống";

    private static final String MSG_FILE_TOO_LARGE =
            "File vượt quá dung lượng cho phép";

    private static final String MSG_FILE_TYPE_NOT_SUPPORTED =
            "Loại file không hỗ trợ. Chỉ chấp nhận JPG, PNG, PDF";

    private static final String MSG_FILE_SAVE_ERROR =
            "Lỗi hệ thống khi lưu file";

    private static final String MSG_FILE_NOT_FOUND =
            "Phiếu kết quả kiểm nghiệm chưa có file đính kèm";

    private static final String MSG_FILE_INVALID_PATH =
            "Đường dẫn file không hợp lệ";

    private static final String MSG_FILE_UNREADABLE =
            "File không tồn tại hoặc không thể đọc";

    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "image/jpeg", "image/png", "application/pdf");

    private final InspectionCriterionResultRepository resultRepository;
    private final InspectionCriterionRepository criterionRepository;
    private final InspectionRequestRepository requestRepository;
    private final CategoryCriterionRepository categoryCriterionRepository;
    private final ProductionLotRepository lotRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.upload.base-dir}")
    private String baseDir;

    @Value("${app.upload.inspection-result.relative-path:inspection-results}")
    private String inspectionResultRelativePath;

    @Value("${app.upload.inspection-result.max-size:5242880}")
    private long maxFileSize;

    @Override
    public InspectionCriterionResultResponse recordOrUpdateResult(
            String criterionId,
            InspectionCriterionResultRequest request,
            CustomUserDetails currentUser) {

        // Kiểm tra tồn tại chỉ tiêu
        UUID criterionUUID = parseUuid(criterionId, MSG_CRITERION_NOT_FOUND);
        InspectionCriterion criterion = criterionRepository
                .findById(criterionUUID)
                .orElseThrow(() -> new BusinessException(MSG_CRITERION_NOT_FOUND));

        // Org boundary: chỉ tiêu phải thuộc yêu cầu của lô thuộc tổ chức hiện tại
        requireCriterionAccess(criterion, currentUser);

        // Kiểm tra yêu cầu kiểm nghiệm ở trạng thái chờ kết quả hoặc không đạt
        InspectionRequest inspectionRequest = criterion.getInspectionRequest();
        InspectionRequestStatus requestStatus = inspectionRequest.getStatus();
        if (requestStatus != InspectionRequestStatus.PENDING_RESULT
                && requestStatus != InspectionRequestStatus.FAILED) {
            throw new IllegalStateException(MSG_REQUEST_STATUS_INVALID);
        }

        // Validate ngày cấp và ngày hết hạn — chỉ bắt buộc khi chỉ tiêu ĐẠT
        // Khi Không đạt (passed = false), resultDate / expiryDate được phép null
        if (Boolean.TRUE.equals(request.getPassed())) {
            if (request.getExpiryDate().isBefore(request.getResultDate())) {
                throw new IllegalArgumentException(MSG_EXPIRY_BEFORE_RESULT_DATE);
            }

            // Kiểm tra xem ngày hết hạn có >= hôm nay không
            if (request.getExpiryDate().isBefore(LocalDate.now(clock))) {
                throw new IllegalArgumentException(MSG_EXPIRY_IN_PAST);
            }
        }

        // Tìm hoặc tạo kết quả kiểm nghiệm
        Optional<InspectionCriterionResult> existingResult = resultRepository
                .findByInspectionCriterion_Id(criterionUUID);
        boolean isNewResult = existingResult.isEmpty();

        InspectionCriterionResult result = existingResult
                .orElseGet(() -> InspectionCriterionResult.builder()
                        .inspectionCriterion(criterion)
                        .createdBy(currentUser.getUser())
                        .build());

        // Cập nhật thông tin kết quả
        result.setResultDate(request.getResultDate());
        result.setExpiryDate(request.getExpiryDate());
        result.setPassed(request.getPassed());
        result.setFilePath(request.getFilePath());

        // Lưu kết quả
        result = resultRepository.save(result);

        // Cập nhật trạng thái yêu cầu kiểm nghiệm nếu tất cả chỉ tiêu đều đạt
        checkAndUpdateRequestStatus(inspectionRequest);

        // Ghi nhật ký hoạt động (TASK-27): phân biệt rõ thao tác ghi mới và cập nhật
        publishActivityLog(
                currentUser,
                isNewResult ? "RECORD_INSPECTION_RESULT" : "UPDATE_INSPECTION_RESULT",
                (isNewResult ? "Ghi" : "Cập nhật") + " kết quả kiểm nghiệm cho chỉ tiêu '"
                        + criterion.getCriterionName() + "' của yêu cầu kiểm nghiệm ID "
                        + inspectionRequest.getId()
                        + (Boolean.TRUE.equals(request.getPassed()) ? " (đạt)" : " (không đạt)"),
                "INSPECTION_CRITERION_RESULT",
                result.getId().toString());

        return toResponse(result);
    }

    @Override
    public List<InspectionCriterionResultResponse> recordResults(
            UUID inspectionRequestId,
            List<InspectionCriterionResultRequest> requests,
            CustomUserDetails currentUser) {

        // Org boundary + tồn tại yêu cầu
        InspectionRequest inspectionRequest =
                requireRequestAccess(inspectionRequestId, currentUser);

        InspectionRequestStatus requestStatus = inspectionRequest.getStatus();
        if (requestStatus != InspectionRequestStatus.PENDING_RESULT
                && requestStatus != InspectionRequestStatus.FAILED) {
            throw new BusinessException(MSG_REQUEST_STATUS_INVALID);
        }

        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(MSG_RESULTS_NOT_EMPTY);
        }

        // Chỉ tiêu của yêu cầu lấy từ DB, không tin ID client truyền vào
        List<InspectionCriterion> criteria = inspectionRequest.getCriteria();
        if (criteria == null || criteria.isEmpty()) {
            throw new BusinessException(
                    "Yêu cầu kiểm nghiệm không có chỉ tiêu nào.");
        }

        Map<UUID, InspectionCriterion> criterionById = criteria.stream()
                .collect(Collectors.toMap(
                        InspectionCriterion::getId,
                        c -> c));

        // ============================================================
        // 1. Validate toàn bộ payload TRƯỚC khi lưu (all-or-nothing)
        // ============================================================
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

            validateResultDates(item, today);
        }

        // Phải ghi đủ kết quả cho toàn bộ chỉ tiêu của yêu cầu
        if (seenCriterionIds.size() != criteria.size()) {
            throw new BusinessException(MSG_RESULTS_MUST_COVER_ALL);
        }

        // ============================================================
        // 2. Lưu toàn bộ kết quả
        // ============================================================
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

            results.add(result);
        }

        resultRepository.saveAll(results);

        // ============================================================
        // 3. Tính trạng thái cuối ĐÚNG MỘT LẦN sau khi toàn bộ kết quả
        //    hợp lệ đã được lưu (không có trạng thái trung gian)
        // ============================================================
        checkAndUpdateRequestStatus(inspectionRequest);

        // ============================================================
        // 4. Ghi nhật ký hoạt động (TASK-27): một bản ghi duy nhất
        //    cho toàn bộ thao tác ghi kết quả hàng loạt
        // ============================================================
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

    @Override
    @Transactional(readOnly = true)
    public InspectionCriterionResultResponse getResultByCriterion(
            String criterionId,
            CustomUserDetails currentUser) {

        UUID criterionUUID = parseUuid(criterionId, MSG_CRITERION_NOT_FOUND);
        InspectionCriterionResult result = resultRepository
                .findByInspectionCriterion_Id(criterionUUID)
                .orElseThrow(() ->
                        new BusinessException(MSG_RESULT_NOT_FOUND));

        requireCriterionAccess(result.getInspectionCriterion(), currentUser);

        return toResponse(result);
    }

    @Override
    public void deleteResult(String resultId, CustomUserDetails currentUser) {

        UUID resultUUID = parseUuid(resultId, MSG_RESULT_NOT_FOUND);
        InspectionCriterionResult result = resultRepository
                .findById(resultUUID)
                .orElseThrow(() ->
                        new BusinessException(MSG_RESULT_NOT_FOUND));

        requireCriterionAccess(result.getInspectionCriterion(), currentUser);

        InspectionRequest inspectionRequest =
                result.getInspectionCriterion().getInspectionRequest();

        // Lưu thông tin mô tả trước khi xóa để ghi nhật ký hoạt động
        String criterionName = result.getInspectionCriterion().getCriterionName();
        UUID requestId = inspectionRequest.getId();

        resultRepository.delete(result);

        // Cập nhật trạng thái yêu cầu sau khi xóa
        checkAndUpdateRequestStatus(inspectionRequest);

        // Ghi nhật ký hoạt động (TASK-27)
        publishActivityLog(
                currentUser,
                "DELETE_INSPECTION_RESULT",
                "Xóa kết quả kiểm nghiệm của chỉ tiêu '" + criterionName
                        + "' thuộc yêu cầu kiểm nghiệm ID " + requestId,
                "INSPECTION_CRITERION_RESULT",
                resultUUID.toString());
    }

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

        String newFileName =
                UUID.randomUUID().toString().replace("-", "") + extension;
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

        // Ghi nhật ký hoạt động (TASK-27): chỉ ghi tên tệp, không ghi nội dung tệp
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

        // Ngăn path traversal: file phải nằm trong baseDir
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

    @Override
    @Transactional(readOnly = true)
    public CanActivateSealCheckResponse checkCanActivateSeal(
            UUID productionLotId,
            CustomUserDetails currentUser) {

        // Org boundary: lô phải thuộc tổ chức hiện tại
        ProductionLot lot = lotRepository
                .findByIdAndOrganization_OrganizationId(
                        productionLotId,
                        currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_LOT_NOT_FOUND));

        /*
         * NCL-09-CN-009: loại nông sản KHÔNG bắt buộc kiểm nghiệm thì
         * luôn đủ điều kiện kích hoạt tem — không phụ thuộc kết quả.
         */
        ProductCategory category = lot.getProductCategory();
        boolean mandatoryInspection =
                category != null
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

        /*
         * Bắt buộc kiểm nghiệm: điều kiện kích hoạt tem xét trên TỔNG BỘ
         * CHỈ TIÊU ACTIVE được gán cho loại nông sản của lô (cùng nguồn
         * dữ liệu với GET /production-lots/{lotId}/test-criteria).
         *
         * Từng chỉ tiêu phải có kết quả MỚI NHẤT ĐẠT và CÒN HIỆU LỰC
         * (expiryDate >= ngày nghiệp vụ hiện tại). Nếu một chỉ tiêu bị
         * kiểm thử lại nhiều lần qua các yêu cầu khác nhau thì kết quả
         * mới nhất là căn cứ quyết định.
         */
        List<CategoryCriterion> assignments =
                categoryCriterionRepository
                        .findByCategoryIdAndCriteriaStatus(
                                category.getId(),
                                "ACTIVE");

        int totalCriteria = assignments.size();

        // Kết quả mới nhất theo mã chỉ tiêu trên toàn bộ yêu cầu của lô
        Map<String, InspectionCriterionResult> latestByCode =
                new HashMap<>();
        for (InspectionCriterionResult result : resultRepository
                .findAllByProductionLotId(productionLotId)) {

            // Bỏ qua chỉ tiêu không đạt — resultDate/expiryDate sẽ là null,
            // không tham gia so sánh ngày để tìm kết quả mới nhất.
            if (result.getResultDate() == null) {
                continue;
            }

            String code = result.getInspectionCriterion()
                    .getCriterionCode();
            InspectionCriterionResult current = latestByCode.get(code);
            LocalDateTime resultUpdatedAt = result.getUpdatedAt();
            LocalDateTime currentUpdatedAt =
                    current == null ? null : current.getUpdatedAt();

            boolean isNewer =
                    current == null
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

        /*
         * Đánh giá từng chỉ tiêu gán cho lô trên kết quả mới nhất:
         * chưa có kết quả / không đạt / hết hạn đều làm mất điều kiện.
         */
        int passedCriteria = 0;
        boolean hasExpiredResult = false;
        LocalDate earliestExpiry = null;

        for (CategoryCriterion assignment : assignments) {
            String code = assignment.getCriterion().getName();
            InspectionCriterionResult latest = latestByCode.get(code);

            // Chưa có bất kỳ kết quả kiểm nghiệm nào cho chỉ tiêu này
            if (latest == null) {
                continue;
            }

            // Đã kiểm tra !Boolean.TRUE.equals ở trên → chỉ tiêu đạt còn lại
            // luôn có expiryDate khác null (do validation ở tầng service).
            // earliestExpiry chỉ cập nhật cho chỉ tiêu đạt để tránh NPE.
            if (Boolean.TRUE.equals(latest.getPassed())
                    && latest.getExpiryDate() != null
                    && (earliestExpiry == null
                    || latest.getExpiryDate().isBefore(earliestExpiry))) {
                earliestExpiry = latest.getExpiryDate();
            }

            if (!Boolean.TRUE.equals(latest.getPassed())) {
                continue;
            }

            // Chỉ tiêu đạt luôn có expiryDate khác null (đã validate ở trên)
            if (latest.getExpiryDate().isBefore(today)) {
                hasExpiredResult = true;
                continue;
            }

            passedCriteria++;
        }

        boolean canActivate =
                totalCriteria > 0
                        && passedCriteria == totalCriteria;

        String reason = null;
        if (!canActivate) {
            if (totalCriteria == 0) {
                reason =
                        "Loại nông sản bắt buộc kiểm nghiệm nhưng chưa "
                                + "được cấu hình chỉ tiêu kiểm nghiệm";
            } else if (hasExpiredResult) {
                reason = "Kết quả kiểm nghiệm đã quá hạn";
            } else {
                reason =
                        "Lô chưa có kết quả kiểm nghiệm đạt cho tất cả "
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
     * Kiểm tra và cập nhật trạng thái yêu cầu kiểm nghiệm.
     * <p>
     * - Chưa đủ kết quả cho tất cả chỉ tiêu: PENDING_RESULT.
     * - Tất cả chỉ tiêu đạt và còn hiệu lực: PASSED.
     * - Đã có đủ kết quả nhưng có chỉ tiêu không đạt / hết hạn: FAILED.
     * <p>
     * Phương thức này phải được gọi SAU KHI toàn bộ kết quả đã được lưu
     * để trạng thái cuối được tính đúng một lần.
     */
    private void checkAndUpdateRequestStatus(InspectionRequest inspectionRequest) {

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
        }
    }

    /**
     * Kiểm tra quyền truy cập chỉ tiêu: chỉ tiêu phải thuộc yêu cầu kiểm nghiệm
     * của một lô thuộc tổ chức hiện tại của người dùng.
     * <p>
     * Trả lỗi "không tồn tại" thay vì lỗi phân quyền để không làm lộ
     * dữ liệu của tổ chức khác.
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
     * Kiểm tra quyền truy cập yêu cầu kiểm nghiệm theo ID.
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
     * Yêu cầu kiểm nghiệm thuộc tổ chức hiện tại hay không.
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
     * Validate ngày cấp / ngày hết hiệu lực của một chỉ tiêu.
     */
    private void validateResultDates(
            InspectionCriterionResultRequest item,
            LocalDate today) {

        // Khi "Không đạt" (passed = false), resultDate / expiryDate được phép
        // null — chỉ tiêu không đạt không có hiệu lực thời gian.
        if (Boolean.FALSE.equals(item.getPassed())) {
            return;
        }

        // Khi "Đạt" (passed = true), bắt buộc phải có ngày cấp và ngày hết hạn
        if (item.getResultDate() == null
                || item.getExpiryDate() == null
                || item.getPassed() == null) {
            throw new BusinessException(
                    "Kết quả kiểm nghiệm phải đầy đủ ngày cấp, ngày hết hiệu lực và kết luận.");
        }

        if (item.getExpiryDate().isBefore(item.getResultDate())) {
            throw new BusinessException(MSG_EXPIRY_BEFORE_RESULT_DATE);
        }

        if (item.getExpiryDate().isBefore(today)) {
            throw new BusinessException(MSG_EXPIRY_IN_PAST);
        }
    }

    /**
     * Ghi nhật ký hoạt động theo convention của hệ thống (TASK-27).
     * <p>
     * Actor lấy từ người dùng đã xác thực trong security context,
     * organization lấy từ organization của người thực hiện.
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
     * Chuyển chuỗi UUID sang UUID, trả lỗi nghiệp vụ nếu không hợp lệ.
     */
    private UUID parseUuid(String value, String errorMessage) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(errorMessage);
        }
    }

    /**
     * Xác định MediaType từ tên file phiếu kết quả.
     */
    private MediaType resolveContentType(String fileName) {
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
     * Chuyển đổi entity thành DTO.
     */
    private InspectionCriterionResultResponse toResponse(
            InspectionCriterionResult result) {

        return InspectionCriterionResultResponse.builder()
                .resultId(result.getId().toString())
                .criterionId(
                        result.getInspectionCriterion().getId().toString())
                .criterionCode(
                        result.getInspectionCriterion().getCriterionCode())
                .criterionName(
                        result.getInspectionCriterion().getCriterionName())
                .resultDate(result.getResultDate())
                .expiryDate(result.getExpiryDate())
                .passed(result.getPassed())
                .filePath(result.getFilePath())
                .createdByName(result.getCreatedBy().getFullName())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}