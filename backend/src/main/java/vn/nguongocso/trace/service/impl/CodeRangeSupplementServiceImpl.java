package vn.nguongocso.trace.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.common.annotation.Auditable;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.dto.request.ApproveSupplementRequest;
import vn.nguongocso.trace.dto.request.CreateSupplementRequest;
import vn.nguongocso.trace.dto.request.RejectSupplementRequest;
import vn.nguongocso.trace.dto.response.CodeRangeSupplementResponse;
import vn.nguongocso.trace.dto.response.EvidenceEventResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.CodeRangeSupplementRequest;
import vn.nguongocso.trace.enums.CodeRangeSupplementStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.CodeRangeSupplementRepository;
import vn.nguongocso.trace.service.CodeRangeSupplementService;

/**
 * Triển khai dịch vụ yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007).
 *
 * <p>
 * Bổ sung hạn mức = tăng {@code totalLimit} của dải mã hiện có của tổ chức
 * (không tạo dải mã mới vì {@code prefix} UNIQUE toàn hệ thống).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CodeRangeSupplementServiceImpl implements CodeRangeSupplementService {

    static final String MSG_NO_PERMISSION_CREATE = "Bạn không có quyền tạo yêu cầu cấp bổ sung dải mã.";
    static final String MSG_NO_PERMISSION_MANAGE = "Bạn không có quyền xử lý yêu cầu cấp bổ sung dải mã.";
    static final String MSG_ORG_NOT_FOUND = "Không tìm thấy tổ chức.";
    static final String MSG_PENDING_EXISTS = "Tổ chức đã có yêu cầu cấp bổ sung đang chờ duyệt.";
    static final String MSG_REQUEST_NOT_FOUND = "Không tìm thấy yêu cầu cấp bổ sung dải mã.";
    static final String MSG_NOT_PENDING = "Chỉ có thể xử lý yêu cầu ở trạng thái PENDING.";
    static final String MSG_REJECT_REASON_REQUIRED = "Lý do từ chối không được để trống.";
    static final String MSG_APPROVED_QTY_INVALID = "Số lượng thực cấp phải lớn hơn 0 và không vượt quá số lượng đề nghị.";
    static final String MSG_NO_CODE_RANGE = "Tổ chức chưa được cấp dải mã truy xuất.";
    static final String MSG_EVIDENCE_REQUIRED = "Phải chọn ít nhất một sự kiện thu hoạch hoặc sơ chế làm bằng chứng.";
    static final String MSG_EVIDENCE_NOT_FOUND = "Bằng chứng chứa sự kiện không tồn tại.";
    static final String MSG_EVIDENCE_INVALID_TYPE = "Bằng chứng chỉ chấp nhận sự kiện thu hoạch (HARVEST) hoặc sơ chế (PREPROCESSING).";
    static final String MSG_EVIDENCE_WRONG_ORG = "Bằng chứng phải là sự kiện của tổ chức bạn.";
    static final String MSG_USER_NOT_FOUND = "Người dùng không tồn tại.";
    static final String MSG_FORBIDDEN_ORG = "Bạn không có quyền thao tác với yêu cầu của tổ chức khác.";

    private static final String ROLE_COOPERATIVE_MANAGER = "VT-02";
    private static final String ROLE_PLATFORM_ADMIN = "VT-01";

    /** Loại sự kiện được chấp nhận làm bằng chứng sản lượng thực. */
    static final List<ChainEventType> EVIDENCE_EVENT_TYPES =
            List.of(ChainEventType.HARVEST, ChainEventType.PREPROCESSING);

    /** Giới hạn số sự kiện bằng chứng trả về cho dialog (mới nhất trước). */
    private static final int MAX_EVIDENCE_EVENTS = 200;

    private final CodeRangeSupplementRepository supplementRepository;
    private final CodeRangeRepository codeRangeRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ChainEventRepository chainEventRepository;
    private final ProductionLotRepository productionLotRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    @Auditable(action = "CREATE_CODE_RANGE_SUPPLEMENT", entityType = "CODE_RANGE_SUPPLEMENT_REQUEST",
            description = "'Tạo yêu cầu cấp bổ sung dải mã, số lượng: ' + #request.requestedQuantity")
    public CodeRangeSupplementResponse create(CreateSupplementRequest request, CustomUserDetails currentUser) {
        validateRole(currentUser, ROLE_COOPERATIVE_MANAGER, MSG_NO_PERMISSION_CREATE);

        Organization organization = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_ORG_NOT_FOUND));

        // Chặn tạo yêu cầu mới khi cùng tổ chức đã có yêu cầu đang chờ duyệt (TC-03)
        if (supplementRepository.existsByOrganization_OrganizationIdAndStatus(
                organization.getOrganizationId(), CodeRangeSupplementStatus.PENDING)) {
            throw new BusinessException(MSG_PENDING_EXISTS);
        }

        // Tổ chức chưa có dải mã thì chưa thể xin bổ sung (TC-06)
        codeRangeRepository.findFirstReadOnlyByOrganizationOrganizationIdOrderByCreatedAtDesc(
                organization.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_NO_CODE_RANGE));

        List<UUID> evidenceIds = validateEvidence(organization.getOrganizationId(), request.getEvidenceEventIds());

        User requester = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        CodeRangeSupplementRequest supplement = new CodeRangeSupplementRequest();
        supplement.setOrganization(organization);
        supplement.setRequestedBy(requester);
        supplement.setRequestedAt(LocalDateTime.now());
        supplement.setRequestedQuantity(request.getRequestedQuantity());
        supplement.setReason(request.getReason());
        supplement.setEvidenceEventIds(toEvidenceJson(evidenceIds));
        supplement.setStatus(CodeRangeSupplementStatus.PENDING);

        CodeRangeSupplementRequest saved = supplementRepository.save(supplement);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CodeRangeSupplementResponse> list(String status, int page, int size,
            CustomUserDetails currentUser) {
        Pageable pageable = toPageable(page, size);

        Page<CodeRangeSupplementRequest> result;
        if (status != null && !status.isBlank()) {
            result = supplementRepository.findByStatus(parseStatus(status), pageable);
        } else {
            result = supplementRepository.findAll(pageable);
        }

        return PageResponse.from(result, result.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CodeRangeSupplementResponse> listMine(String status, int page, int size,
            CustomUserDetails currentUser) {
        Pageable pageable = toPageable(page, size);
        UUID organizationId = currentUser.getOrganizationId();

        Page<CodeRangeSupplementRequest> result;
        if (status != null && !status.isBlank()) {
            result = supplementRepository.findByOrganization_OrganizationIdAndStatus(
                    organizationId, parseStatus(status), pageable);
        } else {
            result = supplementRepository.findByOrganization_OrganizationId(organizationId, pageable);
        }

        return PageResponse.from(result, result.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CodeRangeSupplementResponse getById(UUID id, CustomUserDetails currentUser) {
        CodeRangeSupplementRequest supplement = supplementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        // VT-02 chỉ được xem yêu cầu của tổ chức mình (QTN-01, TC-07)
        if (ROLE_COOPERATIVE_MANAGER.equals(currentUser.getRoleCode())
                && !supplement.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(MSG_FORBIDDEN_ORG);
        }

        return toResponse(supplement);
    }

    @Override
    @Auditable(action = "APPROVE_CODE_RANGE_SUPPLEMENT", entityType = "CODE_RANGE_SUPPLEMENT_REQUEST",
            description = "'Duyệt yêu cầu cấp bổ sung dải mã ID: ' + #id")
    public CodeRangeSupplementResponse approve(UUID id, ApproveSupplementRequest request,
            CustomUserDetails currentUser) {
        validateRole(currentUser, ROLE_PLATFORM_ADMIN, MSG_NO_PERMISSION_MANAGE);

        CodeRangeSupplementRequest supplement = supplementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        if (supplement.getStatus() != CodeRangeSupplementStatus.PENDING) {
            throw new BusinessException(MSG_NOT_PENDING);
        }

        Long approvedQuantity = request != null ? request.getApprovedQuantity() : null;
        if (approvedQuantity == null || approvedQuantity <= 0
                || approvedQuantity > supplement.getRequestedQuantity()) {
            throw new BusinessException(MSG_APPROVED_QTY_INVALID);
        }

        // Tăng hạn mức của dải mã mới nhất trong cùng transaction (dùng lock ghi)
        CodeRange codeRange = codeRangeRepository.findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(
                supplement.getOrganization().getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_NO_CODE_RANGE));
        codeRange.setTotalLimit(codeRange.getTotalLimit() + approvedQuantity);
        codeRangeRepository.save(codeRange);

        User approver = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        supplement.setStatus(CodeRangeSupplementStatus.APPROVED);
        supplement.setApprovedQuantity(approvedQuantity);
        supplement.setApprovedBy(approver);
        supplement.setApprovedAt(LocalDateTime.now());
        supplement.setApprovalRemarks(request != null ? request.getRemarks() : null);

        CodeRangeSupplementRequest saved = supplementRepository.save(supplement);

        boolean partial = approvedQuantity < supplement.getRequestedQuantity();
        String title = "Yêu cầu cấp bổ sung mã đã được duyệt";
        String content = partial
                ? "Yêu cầu cấp bổ sung " + supplement.getRequestedQuantity() + " mã cho tổ chức \""
                        + supplement.getOrganization().getName() + "\" đã được duyệt một phần"
                        + " (đề nghị " + supplement.getRequestedQuantity()
                        + " mã, thực cấp " + approvedQuantity + " mã)."
                        + " Hạn mức mới: " + codeRange.getTotalLimit() + " mã."
                : "Yêu cầu cấp bổ sung " + supplement.getRequestedQuantity() + " mã cho tổ chức \""
                        + supplement.getOrganization().getName() + "\" đã được duyệt toàn bộ"
                        + " (thực cấp " + approvedQuantity + " mã)."
                        + " Hạn mức mới: " + codeRange.getTotalLimit() + " mã.";

        int notifiedCount = notifyOrganization(saved, title, content);

        CodeRangeSupplementResponse response = toResponse(saved);
        response.setNotifiedCount(notifiedCount);
        return response;
    }

    @Override
    @Auditable(action = "REJECT_CODE_RANGE_SUPPLEMENT", entityType = "CODE_RANGE_SUPPLEMENT_REQUEST",
            description = "'Từ chối yêu cầu cấp bổ sung dải mã ID: ' + #id")
    public CodeRangeSupplementResponse reject(UUID id, RejectSupplementRequest request,
            CustomUserDetails currentUser) {
        validateRole(currentUser, ROLE_PLATFORM_ADMIN, MSG_NO_PERMISSION_MANAGE);

        CodeRangeSupplementRequest supplement = supplementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        if (supplement.getStatus() != CodeRangeSupplementStatus.PENDING) {
            throw new BusinessException(MSG_NOT_PENDING);
        }

        if (request == null || request.getRejectionReason() == null
                || request.getRejectionReason().isBlank()) {
            throw new BusinessException(MSG_REJECT_REASON_REQUIRED);
        }

        User rejecter = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        supplement.setStatus(CodeRangeSupplementStatus.REJECTED);
        supplement.setRejectedBy(rejecter);
        supplement.setRejectedAt(LocalDateTime.now());
        supplement.setRejectionReason(request.getRejectionReason());

        CodeRangeSupplementRequest saved = supplementRepository.save(supplement);

        String title = "Yêu cầu cấp bổ sung mã bị từ chối";
        String content = "Yêu cầu cấp bổ sung " + supplement.getRequestedQuantity() + " mã cho tổ chức \""
                + supplement.getOrganization().getName() + "\" đã bị từ chối."
                + " Lý do: " + request.getRejectionReason();
        int notifiedCount = notifyOrganization(saved, title, content);

        CodeRangeSupplementResponse response = toResponse(saved);
        response.setNotifiedCount(notifiedCount);
        return response;
    }

    /**
     * Kiểm tra vai trò của người dùng (belt-and-suspenders với {@code @PreAuthorize}).
     */
    private void validateRole(CustomUserDetails currentUser, String expectedRole, String message) {
        if (!expectedRole.equals(currentUser.getRoleCode())) {
            throw new BusinessException(message);
        }
    }

    /**
     * Kiểm tra bằng chứng sản lượng thực: sự kiện phải tồn tại, thuộc loại
     * thu hoạch (HARVEST) hoặc sơ chế (PREPROCESSING) và thuộc tổ chức yêu cầu.
     *
     * @return danh sách ID sự kiện đã khử trùng lặp, giữ nguyên thứ tự
     */
    private List<UUID> validateEvidence(UUID organizationId, List<UUID> evidenceEventIds) {
        if (evidenceEventIds == null || evidenceEventIds.isEmpty()) {
            throw new BusinessException(MSG_EVIDENCE_REQUIRED);
        }

        List<UUID> distinctIds = new ArrayList<>(new LinkedHashSet<>(evidenceEventIds));
        List<ChainEvent> events = chainEventRepository.findAllById(distinctIds);
        if (events.size() != distinctIds.size()) {
            throw new BusinessException(MSG_EVIDENCE_NOT_FOUND);
        }

        Map<UUID, ChainEvent> eventById = events.stream()
                .collect(Collectors.toMap(ChainEvent::getId, Function.identity()));

        for (UUID eventId : distinctIds) {
            ChainEvent event = eventById.get(eventId);
            if (event.getEventType() != ChainEventType.HARVEST
                    && event.getEventType() != ChainEventType.PREPROCESSING) {
                throw new BusinessException(MSG_EVIDENCE_INVALID_TYPE);
            }
            if (!belongsToOrganization(event, organizationId)) {
                throw new BusinessException(MSG_EVIDENCE_WRONG_ORG);
            }
        }

        return distinctIds;
    }

    /**
     * Xác định một sự kiện có thuộc tổ chức hay không: qua lô hàng gắn kèm,
     * hoặc qua {@code productionLotId} trong {@code eventData} (sự kiện
     * thu hoạch/sơ chế chưa gắn lô hàng).
     */
    private boolean belongsToOrganization(ChainEvent event, UUID organizationId) {
        if (event.getShipment() != null) {
            return event.getShipment().getOrganization() != null
                    && organizationId.equals(event.getShipment().getOrganization().getOrganizationId());
        }

        String eventData = event.getEventData();
        if (eventData == null || eventData.isBlank()) {
            return false;
        }

        try {
            JsonNode node = objectMapper.readTree(eventData);
            JsonNode lotIdNode = node.get("productionLotId");
            if (lotIdNode == null || lotIdNode.isNull()) {
                return false;
            }
            UUID lotId = UUID.fromString(lotIdNode.asText());
            return productionLotRepository.findById(lotId)
                    .map(ProductionLot::getOrganization)
                    .map(org -> organizationId.equals(org.getOrganizationId()))
                    .orElse(false);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Liệt kê sự kiện bằng chứng sản lượng thực (HARVEST / PREPROCESSING) của
     * tổ chức để VT-02 chọn khi tạo yêu cầu cấp bổ sung dải mã.
     *
     * <p>Bằng chứng có thể là sự kiện đã gắn lô hàng (tra tổ chức qua lô hàng)
     * hoặc sự kiện tự do lưu {@code productionLotId} trong eventData. Kết quả
     * sắp xếp mới nhất trước và giới hạn số lượng để dialog hiển thị gọn.</p>
     */
    @Override
    public List<EvidenceEventResponse> listEvidenceEvents(CustomUserDetails currentUser) {
        validateRole(currentUser, ROLE_COOPERATIVE_MANAGER, MSG_NO_PERMISSION_CREATE);
        UUID organizationId = currentUser.getOrganizationId();

        // 1) Sự kiện đã gắn lô hàng thuộc tổ chức
        List<ChainEvent> events = new ArrayList<>(chainEventRepository
                .findByEventTypeInAndShipment_Organization_OrganizationId(
                        EVIDENCE_EVENT_TYPES, organizationId));

        // 2) Sự kiện tự do (chưa gắn lô hàng) — tra tổ chức qua productionLotId trong eventData
        chainEventRepository.findByShipmentIsNullAndEventTypeIn(EVIDENCE_EVENT_TYPES).stream()
                .filter(event -> belongsToOrganization(event, organizationId))
                .forEach(events::add);

        List<ChainEvent> sorted = events.stream()
                .sorted(Comparator.comparing(ChainEvent::getRecordedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_EVIDENCE_EVENTS)
                .toList();

        Map<UUID, String> lotNames = loadLotNames(sorted);

        return sorted.stream()
                .map(event -> toEvidenceResponse(event, lotNames))
                .toList();
    }

    /** Tải tên các lô sản xuất liên quan theo lô (một query duy nhất). */
    private Map<UUID, String> loadLotNames(List<ChainEvent> events) {
        Set<UUID> lotIds = new HashSet<>();
        for (ChainEvent event : events) {
            UUID lotId = resolveProductionLotId(event);
            if (lotId != null) {
                lotIds.add(lotId);
            }
        }
        if (lotIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        productionLotRepository.findAllById(lotIds)
                .forEach(lot -> names.put(lot.getId(), lot.getName()));
        return names;
    }

    /** Xác định lô sản xuất của sự kiện: qua lô hàng gắn kèm hoặc eventData. */
    private UUID resolveProductionLotId(ChainEvent event) {
        if (event.getShipment() != null && event.getShipment().getProductionLot() != null) {
            return event.getShipment().getProductionLot().getId();
        }
        String eventData = event.getEventData();
        if (eventData == null || eventData.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(eventData);
            JsonNode lotIdNode = node.get("productionLotId");
            if (lotIdNode == null || lotIdNode.isNull()) {
                return null;
            }
            return UUID.fromString(lotIdNode.asText());
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Resolve chi tiết các sự kiện bằng chứng từ list ID đã lưu để VT-01 xem
     * khi duyệt (loại sự kiện, tên lô, thời điểm, người ghi).
     *
     * <p>Giữ đúng thứ tự ID gốc; sự kiện đã bị xóa thì bỏ qua (FE fallback
     * hiển thị ID). Tải tên lô gộp một query duy nhất qua {@link #loadLotNames}.</p>
     */
    private List<EvidenceEventResponse> resolveEvidenceDetails(List<UUID> evidenceIds) {
        if (evidenceIds == null || evidenceIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, ChainEvent> byId = new HashMap<>();
        chainEventRepository.findAllById(evidenceIds).forEach(event -> byId.put(event.getId(), event));
        List<ChainEvent> ordered = evidenceIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
        if (ordered.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> lotNames = loadLotNames(ordered);
        return ordered.stream()
                .map(event -> toEvidenceResponse(event, lotNames))
                .toList();
    }

    private EvidenceEventResponse toEvidenceResponse(ChainEvent event, Map<UUID, String> lotNames) {
        UUID lotId = resolveProductionLotId(event);
        return EvidenceEventResponse.builder()
                .eventId(event.getId())
                .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                .recordedAt(event.getRecordedAt())
                .recordedByName(event.getRecordedBy() != null ? event.getRecordedBy().getFullName() : null)
                .shipmentId(event.getShipment() != null ? event.getShipment().getId() : null)
                .productionLotId(lotId)
                .productionLotName(lotId != null ? lotNames.get(lotId) : null)
                .quantity(extractQuantity(event))
                .build();
    }

    /**
     * Trích xuất số lượng sản lượng từ {@code eventData} JSON của sự kiện.
     *
     * <p>Sự kiện thu hoạch (HARVEST) lưu ở trường "quantity"; sự kiện sơ chế
     * (PREPROCESSING) lưu ở "outputQuantity"/"inputQuantity" nên phải ưu tiên
     * key theo loại sự kiện, nếu không dialog bằng chứng sẽ không hiện sản
     * lượng thực. Chấp nhận cả số và chuỗi số.</p>
     */
    private BigDecimal extractQuantity(ChainEvent event) {
        String eventData = event.getEventData();
        if (eventData == null || eventData.isBlank()) {
            return null;
        }
        List<String> keys;
        if (event.getEventType() == ChainEventType.PREPROCESSING) {
            keys = List.of("outputQuantity", "inputQuantity", "quantity", "weight",
                    "actualQuantity", "harvestQuantity");
        } else {
            keys = List.of("quantity", "harvestQuantity", "actualQuantity", "weight",
                    "outputQuantity", "inputQuantity");
        }
        try {
            JsonNode node = objectMapper.readTree(eventData);
            for (String key : keys) {
                BigDecimal value = parseQuantityNode(node.get(key));
                if (value != null) {
                    return value;
                }
            }
        } catch (JsonProcessingException e) {
            // bỏ qua, trả về null
        }
        return null;
    }

    /** Parse một node số lượng: nhận cả number và chuỗi số, còn lại trả null. */
    private BigDecimal parseQuantityNode(JsonNode qtyNode) {
        if (qtyNode == null || qtyNode.isNull()) {
            return null;
        }
        try {
            if (qtyNode.isNumber()) {
                return qtyNode.decimalValue();
            }
            if (qtyNode.isTextual()) {
                String text = qtyNode.asText().trim().replace(",", ".");
                if (!text.isEmpty()) {
                    return new BigDecimal(text);
                }
            }
        } catch (NumberFormatException e) {
            // bỏ qua key này, thử key tiếp theo
        }
        return null;
    }

    /**
     * Gửi thông báo kết quả duyệt cho người tạo yêu cầu + quản lý HTX (VT-02)
     * của tổ chức.
     *
     * @return số lượng thông báo đã tạo
     */
    private int notifyOrganization(CodeRangeSupplementRequest supplement, String title, String content) {
        UUID organizationId = supplement.getOrganization().getOrganizationId();

        List<UUID> recipientIds = new ArrayList<>();
        recipientIds.add(supplement.getRequestedBy().getUserId());

        List<OrganizationUser> managers = organizationUserRepository
                .findAllByOrganization_OrganizationIdAndRole_Code(organizationId, ROLE_COOPERATIVE_MANAGER);
        for (OrganizationUser manager : managers) {
            UUID userId = manager.getUser().getUserId();
            if (!recipientIds.contains(userId)) {
                recipientIds.add(userId);
            }
        }

        return notificationService.sendCodeRangeSupplementNotification(title, content, recipientIds);
    }

    private Pageable toPageable(int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
    }

    private CodeRangeSupplementStatus parseStatus(String status) {
        try {
            return CodeRangeSupplementStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái không hợp lệ: " + status);
        }
    }

    private String toEvidenceJson(List<UUID> evidenceIds) {
        try {
            return objectMapper.writeValueAsString(evidenceIds.stream().map(UUID::toString).toList());
        } catch (JsonProcessingException e) {
            throw new BusinessException("Bằng chứng không hợp lệ.");
        }
    }

    private List<UUID> fromEvidenceJson(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(evidenceJson);
            List<UUID> ids = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    ids.add(UUID.fromString(item.asText()));
                }
            }
            return ids;
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * Chuyển đổi entity sang response DTO (kèm chi tiết bằng chứng đã resolve
     * để VT-01 xem khi duyệt).
     */
    private CodeRangeSupplementResponse toResponse(CodeRangeSupplementRequest entity) {
        List<UUID> evidenceIds = fromEvidenceJson(entity.getEvidenceEventIds());
        CodeRangeSupplementResponse.UserInfo requestedBy = entity.getRequestedBy() != null
                ? CodeRangeSupplementResponse.UserInfo.builder()
                        .userId(entity.getRequestedBy().getUserId())
                        .fullName(entity.getRequestedBy().getFullName())
                        .build()
                : null;

        CodeRangeSupplementResponse.UserInfo approvedBy = entity.getApprovedBy() != null
                ? CodeRangeSupplementResponse.UserInfo.builder()
                        .userId(entity.getApprovedBy().getUserId())
                        .fullName(entity.getApprovedBy().getFullName())
                        .build()
                : null;

        CodeRangeSupplementResponse.UserInfo rejectedBy = entity.getRejectedBy() != null
                ? CodeRangeSupplementResponse.UserInfo.builder()
                        .userId(entity.getRejectedBy().getUserId())
                        .fullName(entity.getRejectedBy().getFullName())
                        .build()
                : null;

        return CodeRangeSupplementResponse.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization().getOrganizationId())
                .organizationName(entity.getOrganization().getName())
                .requestedBy(requestedBy)
                .requestedAt(entity.getRequestedAt())
                .requestedQuantity(entity.getRequestedQuantity())
                .approvedQuantity(entity.getApprovedQuantity())
                .status(entity.getStatus().name())
                .reason(entity.getReason())
                .evidenceEventIds(evidenceIds)
                .evidenceEvents(resolveEvidenceDetails(evidenceIds))
                .approvedBy(approvedBy)
                .approvedAt(entity.getApprovedAt())
                .approvalRemarks(entity.getApprovalRemarks())
                .rejectedBy(rejectedBy)
                .rejectedAt(entity.getRejectedAt())
                .rejectionReason(entity.getRejectionReason())
                .notifiedCount(0)
                .build();
    }
}
