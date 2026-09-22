package vn.nguongocso.farm.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.common.annotation.Auditable;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.AssignProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CloseProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRecallRequest;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.UpdateProductFeedbackProcessingRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.dto.response.PublicProductFeedbackCreatedResponse;
import vn.nguongocso.farm.dto.response.PublicProductFeedbackLookupResponse;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.event.ProductFeedbackSubmittedEvent;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.ProductFeedbackService;
import vn.nguongocso.farm.service.ProductFeedbackLookupCodeGenerator;
import vn.nguongocso.farm.service.ProductFeedbackLookupCodeGenerator.GeneratedLookupCode;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;
import vn.nguongocso.recall.entity.RecallRequest;
import vn.nguongocso.recall.enums.RecallRequestStatus;
import vn.nguongocso.recall.repository.RecallRequestRepository;
import vn.nguongocso.recall.service.RecallRequestService;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Triển khai dịch vụ quản lý phản ánh chất lượng sản phẩm.
*/
@Service
@RequiredArgsConstructor
public class ProductFeedbackServiceImpl implements ProductFeedbackService {
    private static final Logger log = LoggerFactory.getLogger(ProductFeedbackServiceImpl.class);

    private static final String ADMIN_ROLE = "VT-01";

    private static final String EVENT_RECORDER_ROLE = "VT-03";

    private static final String NOT_FOUND = "Không tìm thấy phản ánh";

    private static final int LOOKUP_CODE_GENERATION_ATTEMPTS = 5;

    private final ProductFeedbackRepository productFeedbackRepository;

    private final ProductionLotRepository productionLotRepository;

    private final TraceCodeRepository traceCodeRepository;

    private final RecallRequestRepository recallRequestRepository;

    private final RecallRequestService recallRequestService;

    private final OrganizationUserRepository organizationUserRepository;

    private final UserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final NotificationService notificationService;

    private final ProductFeedbackLookupCodeGenerator lookupCodeGenerator;

    /** Tạo phản ánh mới từ phản hồi của người tiêu dùng. */
    @Override
    @Transactional
    public PublicProductFeedbackCreatedResponse createFeedback(
            UUID productionLotId,
            CreateProductFeedbackRequest request) {
        ProductionLot productionLot = productionLotRepository.findById(productionLotId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô sản xuất"));

        TraceCode traceCode = null;
        if (hasText(request.getTraceCodeValue())) {
            traceCode = traceCodeRepository
                    .findByCodeValueAndShipment_ProductionLot_Id(request.getTraceCodeValue().trim(), productionLotId)
                    .orElseThrow(() -> new BusinessException("Mã tem không thuộc lô sản xuất của phản ánh"));
        }
        GeneratedLookupCode lookupCode = generateUniqueLookupCode();
        ProductFeedback feedback = ProductFeedback.builder()
                .productionLot(productionLot)
                .traceCode(traceCode)
                .content(request.getContent().trim())
                .status(ProductFeedbackStatus.NEW)
                .severity(ProductFeedbackSeverity.INFORMATION)
                .lookupCodeHash(lookupCode.hash())
                .build();

        ProductFeedback savedFeedback = productFeedbackRepository.save(feedback);
        publishSubmittedEvent(savedFeedback);
        sendNewFeedbackNotification(savedFeedback);

        return PublicProductFeedbackCreatedResponse.builder()
                .id(savedFeedback.getId())
                .productionLotId(productionLotId)
                .status(savedFeedback.getStatus())
                .createdAt(savedFeedback.getCreatedAt())
                .lookupCode(lookupCode.displayValue())
                .build();
    }
    /** Tra cứu trạng thái phản ánh theo mã tra cứu công khai. */
    @Override
    @Transactional(readOnly = true)
    public PublicProductFeedbackLookupResponse lookupPublicFeedback(String lookupCode) {
        String lookupCodeHash;
        try {
            lookupCodeHash = lookupCodeGenerator.hash(lookupCode);
        } catch (IllegalArgumentException exception) {
            throw new ResourceNotFoundException(NOT_FOUND);
        }
        ProductFeedback feedback = productFeedbackRepository.findByLookupCodeHash(lookupCodeHash)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        return PublicProductFeedbackLookupResponse.builder()
                .status(feedback.getStatus())
                .publicResponse(feedback.getPublicResponse())
                .build();
    }
    /** Lấy danh sách phản ánh theo bộ lọc và phân trang. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductFeedbackResponse> getFeedbacks(
            String keyword,
            ProductFeedbackStatus status,
            ProductFeedbackSeverity severity,
            UUID productionLotId,
            UUID assignedToUserId,
            Pageable pageable) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        Specification<ProductFeedback> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!ADMIN_ROLE.equals(currentUser.getRoleCode())) {
                predicates.add(cb.equal(
                        root.get("productionLot").get("organization").get("organizationId"),
                        currentUser.getOrganizationId()));
            }
            if (hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("content")), pattern),
                        cb.like(cb.lower(root.get("productionLot").get("name")), pattern),
                        cb.like(cb.lower(root.join("traceCode", JoinType.LEFT).get("codeValue")), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (productionLotId != null) {
                predicates.add(cb.equal(root.get("productionLot").get("id"), productionLotId));
            }
            if (assignedToUserId != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("userId"), assignedToUserId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<ProductFeedback> page = productFeedbackRepository.findAll(specification, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::mapToResponse).toList());
    }
    /** Lấy chi tiết phản ánh theo ID. */
    @Override
    @Transactional(readOnly = true)
    public ProductFeedbackResponse getFeedbackById(UUID feedbackId) {
        return mapToResponse(loadVisibleFeedback(feedbackId));
    }
    /** Gán người xử lý cho phản ánh. */
    @Override
    @Transactional
    @Auditable(action = "ASSIGN_PRODUCT_FEEDBACK", entityType = "PRODUCT_FEEDBACK",
            description = "'Gán người xử lý cho phản ánh ID: ' + #feedbackId"
                    + " + ', người xử lý: ' + #request.assignedToUserId")
    public ProductFeedbackResponse assign(UUID feedbackId, AssignProductFeedbackRequest request) {
        ProductFeedback feedback = loadOwnedFeedback(feedbackId);
        ensureNotClosed(feedback);

        UUID organizationId = feedback.getProductionLot().getOrganization().getOrganizationId();
        OrganizationUser membership = organizationUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(organizationId, request.getAssignedToUserId())
                .orElseThrow(() -> new BusinessException("Người được chọn không đủ điều kiện xử lý phản ánh"));

        if (membership.getStatus() != OrganizationUserStatus.ACTIVE
                || !EVENT_RECORDER_ROLE.equals(membership.getRole().getCode())) {
            throw new BusinessException("Người được chọn không đủ điều kiện xử lý phản ánh");
        }
        feedback.setAssignedTo(membership.getUser());
        feedback.setAssignedAt(LocalDateTime.now());
        if (feedback.getStatus() == ProductFeedbackStatus.NEW) {
            feedback.setStatus(ProductFeedbackStatus.IN_PROGRESS);
        }
        return mapToResponse(productFeedbackRepository.save(feedback));
    }
    /** Cập nhật quá trình xử lý phản ánh. */
    @Override
    @Transactional
    @Auditable(action = "UPDATE_PRODUCT_FEEDBACK_PROCESSING", entityType = "PRODUCT_FEEDBACK",
            description = "'Cập nhật xử lý phản ánh ID: ' + #feedbackId"
                    + " + ', mức độ: ' + #request.severity"
                    + " + ', mã tem: ' + #request.traceCodeId")
    public ProductFeedbackResponse updateProcessing(
            UUID feedbackId,
            UpdateProductFeedbackProcessingRequest request) {
        ProductFeedback feedback = loadOwnedFeedback(feedbackId);
        ensureNotClosed(feedback);
        ensureAssigned(feedback);

        if (feedback.getStatus() != ProductFeedbackStatus.IN_PROGRESS
                && feedback.getStatus() != ProductFeedbackStatus.ESCALATED_TO_RECALL) {
            throw new BusinessException(HttpStatus.CONFLICT, "Trạng thái phản ánh không cho phép cập nhật xử lý");
        }
        TraceCode traceCode = resolveTraceCode(feedback, request.getSeverity(), request.getTraceCodeId());
        feedback.setSeverity(request.getSeverity());
        feedback.setTraceCode(traceCode);
        feedback.setProcessingContent(normalize(request.getProcessingContent()));
        feedback.setPublicResponse(normalize(request.getPublicResponse()));
        return mapToResponse(productFeedbackRepository.save(feedback));
    }
    /** Đóng phản ánh sau khi xử lý xong. */
    @Override
    @Transactional
    @Auditable(action = "CLOSE_PRODUCT_FEEDBACK", entityType = "PRODUCT_FEEDBACK",
            description = "'Đóng phản ánh ID: ' + #feedbackId"
                    + " + ', lý do: ' + #request.closeReason")
    public ProductFeedbackResponse close(UUID feedbackId, CloseProductFeedbackRequest request) {
        ProductFeedback feedback = loadOwnedFeedback(feedbackId);
        ensureNotClosed(feedback);
        ensureAssigned(feedback);

        String effectiveProcessingContent = hasText(request.getProcessingContent())
                ? request.getProcessingContent().trim()
                : normalize(feedback.getProcessingContent());
        if (!hasText(effectiveProcessingContent)) {
            throw new BusinessException("Vui lòng nhập nội dung xử lý trước khi đóng phản ánh");
        }
        if (recallRequestRepository.existsBySourceFeedback_IdAndStatus(
                feedbackId, RecallRequestStatus.PENDING)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Phải xử lý xong yêu cầu thu hồi trước khi đóng phản ánh");
        }
        User currentUser = userRepository.findById(SecurityUtils.getCurrentUserDetails().getUserId())
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));
        feedback.setProcessingContent(effectiveProcessingContent);
        feedback.setPublicResponse(normalize(request.getPublicResponse()));
        feedback.setCloseReason(request.getCloseReason().trim());
        feedback.setClosedBy(currentUser);
        feedback.setClosedAt(LocalDateTime.now());
        feedback.setStatus(ProductFeedbackStatus.CLOSED);
        return mapToResponse(productFeedbackRepository.save(feedback));
    }
    /** Chuyển phản ánh thành yêu cầu thu hồi sản phẩm. */
    @Override
    @Transactional
    @Auditable(action = "ESCALATE_PRODUCT_FEEDBACK_TO_RECALL", entityType = "PRODUCT_FEEDBACK",
            description = "'Chuyển phản ánh sang yêu cầu thu hồi ID: ' + #feedbackId"
                    + " + ', lý do: ' + #request.reason")
    public RecallRequestResponse createRecallRequest(
            UUID feedbackId,
            CreateProductFeedbackRecallRequest request) {
        ProductFeedback feedback = loadOwnedFeedback(feedbackId);
        ensureNotClosed(feedback);
        ensureAssigned(feedback);
        if (feedback.getStatus() != ProductFeedbackStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "Phản ánh phải đang được xử lý trước khi yêu cầu thu hồi");
        }
        if (feedback.getSeverity() == ProductFeedbackSeverity.INFORMATION) {
            throw new BusinessException("Chỉ phản ánh nghi ngờ chất lượng hoặc tem giả mới được yêu cầu thu hồi");
        }
        if (recallRequestRepository.existsBySourceFeedback_IdAndStatus(
                feedbackId, RecallRequestStatus.PENDING)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Phản ánh đã có yêu cầu thu hồi đang chờ duyệt");
        }
        RecallRequestResponse response = recallRequestService.createFromFeedback(
                feedback,
                request.getShipmentId(),
                request.getReason(),
                request.getEvidence(),
                SecurityUtils.getCurrentUserDetails());
        feedback.setStatus(ProductFeedbackStatus.ESCALATED_TO_RECALL);
        productFeedbackRepository.save(feedback);
        return response;
    }
    /** Lấy phản ánh mà người dùng có quyền xem. */
    private ProductFeedback loadVisibleFeedback(UUID feedbackId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        if (ADMIN_ROLE.equals(currentUser.getRoleCode())) {
            return productFeedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        }
        return productFeedbackRepository
                .findByIdAndProductionLot_Organization_OrganizationId(feedbackId, currentUser.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }
    /** Lấy phản ánh thuộc tổ chức của người dùng hiện tại. */
    private ProductFeedback loadOwnedFeedback(UUID feedbackId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        return productFeedbackRepository
                .findByIdAndProductionLot_Organization_OrganizationId(feedbackId, currentUser.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }
    /** Kiểm tra phản ánh chưa bị đóng. */
    private void ensureNotClosed(ProductFeedback feedback) {
        if (feedback.getStatus() == ProductFeedbackStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Phản ánh đã được đóng");
        }
    }
    /** Kiểm tra phản ánh đã có người xử lý. */
    private void ensureAssigned(ProductFeedback feedback) {
        if (feedback.getAssignedTo() == null) {
            throw new BusinessException("Phản ánh chưa được gán người xử lý");
        }
    }
    /** Xác định mã tem liên kết theo mức độ phản ánh. */
    private TraceCode resolveTraceCode(
            ProductFeedback feedback,
            ProductFeedbackSeverity severity,
            UUID traceCodeId) {
        if (severity == ProductFeedbackSeverity.COUNTERFEIT_SUSPECTED && traceCodeId == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Phản ánh phải được liên kết với mã tem cụ thể");
        }
        if (traceCodeId == null) {
            return severity == ProductFeedbackSeverity.COUNTERFEIT_SUSPECTED
                    ? feedback.getTraceCode()
                    : null;
        }
        return traceCodeRepository
                .findByIdAndShipment_ProductionLot_Id(traceCodeId, feedback.getProductionLot().getId())
                .orElseThrow(() -> new BusinessException("Mã tem không thuộc lô sản xuất của phản ánh"));
    }
    /** Chuyển entity phản ánh sang DTO phản hồi. */
    private ProductFeedbackResponse mapToResponse(ProductFeedback feedback) {
        ProductionLot lot = feedback.getProductionLot();
        RecallRequest latestRecall = feedback.getId() == null
                ? null
                : recallRequestRepository.findTopBySourceFeedback_IdOrderByRequestedAtDesc(feedback.getId())
                        .orElse(null);
        return ProductFeedbackResponse.builder()
                .id(feedback.getId())
                .productionLotId(lot.getId())
                .productionLotName(lot.getName())
                .organizationId(lot.getOrganization() != null ? lot.getOrganization().getOrganizationId() : null)
                .organizationName(lot.getOrganization() != null ? lot.getOrganization().getName() : null)
                .productCategoryName(lot.getProductCategory() != null ? lot.getProductCategory().getName() : null)
                .traceCodeId(feedback.getTraceCode() != null ? feedback.getTraceCode().getId() : null)
                .traceCodeValue(feedback.getTraceCode() != null ? feedback.getTraceCode().getCodeValue() : null)
                .content(feedback.getContent())
                .status(feedback.getStatus())
                .severity(feedback.getSeverity())
                .assignedToUserId(feedback.getAssignedTo() != null ? feedback.getAssignedTo().getUserId() : null)
                .assignedToName(feedback.getAssignedTo() != null ? feedback.getAssignedTo().getFullName() : null)
                .assignedAt(feedback.getAssignedAt())
                .processingContent(feedback.getProcessingContent())
                .publicResponse(feedback.getPublicResponse())
                .closeReason(feedback.getCloseReason())
                .closedByUserId(feedback.getClosedBy() != null ? feedback.getClosedBy().getUserId() : null)
                .closedByName(feedback.getClosedBy() != null ? feedback.getClosedBy().getFullName() : null)
                .closedAt(feedback.getClosedAt())
                .latestRecallRequestId(latestRecall != null ? latestRecall.getId() : null)
                .latestRecallRequestStatus(latestRecall != null ? latestRecall.getStatus().name() : null)
                .hasPendingRecallRequest(latestRecall != null && latestRecall.getStatus() == RecallRequestStatus.PENDING)
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
    /** Phát sự kiện khi có phản ánh mới được gửi. */
    private void publishSubmittedEvent(ProductFeedback savedFeedback) {
        ProductionLot lot = savedFeedback.getProductionLot();
        UUID orgId = lot.getOrganization() != null ? lot.getOrganization().getOrganizationId() : null;
        eventPublisher.publishEvent(new ProductFeedbackSubmittedEvent(
                this,
                savedFeedback.getId(),
                lot.getId(),
                lot.getName(),
                orgId,
                savedFeedback.getContent()));
    }
    /** Sinh mã tra cứu duy nhất cho phản ánh. */
    private GeneratedLookupCode generateUniqueLookupCode() {
        for (int attempt = 0; attempt < LOOKUP_CODE_GENERATION_ATTEMPTS; attempt++) {
            GeneratedLookupCode candidate = lookupCodeGenerator.generate();
            if (!productFeedbackRepository.existsByLookupCodeHash(candidate.hash())) {
                return candidate;
            }
        }
        throw new IllegalStateException("Không thể sinh mã tra cứu phản ánh duy nhất");
    }
    /** Gửi thông báo khi có phản ánh mới. */
    private void sendNewFeedbackNotification(ProductFeedback feedback) {
        ProductionLot lot = feedback.getProductionLot();
        String orgName = lot.getOrganization() != null ? lot.getOrganization().getName() : "N/A";
        try {
            notificationService.sendAlert(String.format(
                    "Phản ánh mới về lô sản xuất \"%s\" (ID: %s) từ tổ chức \"%s\". Nội dung: \"%s\"",
                    lot.getName(), lot.getId(), orgName, feedback.getContent()));
        } catch (Exception exception) {
            log.warn("Không thể gửi thông báo phản ánh sản phẩm: {}", exception.getMessage());
        }
    }
    /** Kiểm tra chuỗi có nội dung hay không. */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
    /** Xóa khoảng trắng đầu cuối, trả null nếu chuỗi rỗng. */
    private static String normalize(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
