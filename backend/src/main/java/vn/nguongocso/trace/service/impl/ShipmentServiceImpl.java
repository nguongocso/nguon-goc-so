package vn.nguongocso.trace.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.response.InspectionEligibilityResult;
import vn.nguongocso.certification.enums.InspectionBlockReasonCode;
import vn.nguongocso.certification.service.InspectionEligibilityService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.trace.dto.request.CreateShipmentRequest;
import vn.nguongocso.trace.dto.response.ShipmentResponse;
import vn.nguongocso.trace.dto.response.ProcurementShipmentResponse;
import vn.nguongocso.trace.dto.response.ShipmentSummaryResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.ShipmentHandover;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.QRCodeService;
import vn.nguongocso.trace.service.ShipmentService;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.response.TraceCodeResponse;
import vn.nguongocso.trace.dto.request.SplitShipmentRequest;
import vn.nguongocso.trace.dto.request.SplitShipmentAllocationRequest;
import vn.nguongocso.trace.dto.response.PartnerOrganizationResponse;
import vn.nguongocso.trace.dto.response.SplitPreviewResponse;
import vn.nguongocso.trace.dto.response.SplitShipmentResponse;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventHashService;

/**
 * Service xử lý nghiệp vụ quản lý lô hàng và sinh mã truy xuất.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final CodeRangeRepository codeRangeRepository;
    private final ProductionLotRepository productionLotRepository;
    private final QRCodeService qrCodeService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final PermissionChecker permissionChecker;
    private final InspectionEligibilityService inspectionEligibilityService;
    private final OrganizationRepository organizationRepository;
    private final ShipmentHandoverRepository shipmentHandoverRepository;
    private final ChainEventRepository chainEventRepository;
    private final EventHashService eventHashService;
    private final ObjectMapper objectMapper;

    private static final String ORG_MANAGER_ROLE = "VT-02";

    private static final String ORGANIZATION_ACCESS_MESSAGE = "Bạn không thuộc tổ chức của lô sản xuất.";

    private static final String INVALID_LOT_STATUS_MESSAGE = "Chỉ có thể tạo lô hàng từ lô sản xuất đã đóng gói.";

    private static final String CODE_RANGE_NOT_FOUND_MESSAGE = "Tổ chức chưa được cấp dải mã truy xuất.";

    private static final String CODE_RANGE_LIMIT_EXCEEDED_MESSAGE = "Số lượng tem vượt quá hạn mức dải mã còn lại.";

    private static final String PRODUCTION_LOT_NOT_FOUND_MESSAGE = "Không tìm thấy lô sản xuất.";

    /**
     * Tạo lô hàng và sinh mã truy xuất cho lô sản xuất.
     *
     * @param request thông tin tạo lô hàng
     * @return thông tin lô hàng sau khi tạo
     * @throws BusinessException nếu không đủ điều kiện tạo lô hàng
     */
    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request) {

        CustomUserDetails currentUser = getCurrentUser();

        validateRole(currentUser, ORG_MANAGER_ROLE, "Bạn không có quyền tạo lô hàng.");

        ProductionLot productionLot = findProductionLot(request.getProductionLotId());

        validateOrganization(currentUser, productionLot);

        validateProductionLotStatus(productionLot);

        /*
         * QTN-30 (NCL-11-CN-005): lô chưa đạt kiểm nghiệm không được tạo
         * lô hàng. Gate bắt buộc phía backend — chặn TRƯỚC khi persist
         * Shipment / sinh TraceCode / trừ hạn mức dải mã (TC-01).
         */
        validateInspectionEligibility(productionLot);

        CodeRange codeRange = findAvailableCodeRange(currentUser);

        // Đồng bộ usedCount với thực tế từ max code_value
        String maxCode = traceCodeRepository.findMaxCodeValueByOrganization(currentUser.getOrganizationId(),
                codeRange.getPrefix());
        long actualUsedCount = 0;
        if (maxCode != null && maxCode.startsWith(codeRange.getPrefix())) {
            String seqStr = maxCode.substring(codeRange.getPrefix().length());
            try {
                actualUsedCount = Long.parseLong(seqStr);
            } catch (NumberFormatException ignored) {
            }
        }
        codeRange.setUsedCount(actualUsedCount); // cập nhật usedCount trước khi validate

        validateCodeRangeLimit(codeRange, request.getTotalQuantity());

        Shipment shipment = createShipmentEntity(request, productionLot, currentUser, codeRange);

        shipmentRepository.save(shipment);

        List<TraceCode> traceCodes = generateTraceCodes(shipment, codeRange, request.getTotalQuantity());

        traceCodes = traceCodeRepository.saveAll(traceCodes);

        updateCodeRange(codeRange, request.getTotalQuantity());
        codeRangeRepository.save(codeRange);

        shipment.setStatus(ShipmentStatus.CODE_PRINTED);

        publishActivityLog(
                currentUser,
                "CREATE",
                "Tạo lô hàng " + shipment.getName() + " cho lô sản xuất " + productionLot.getName(),
                "Shipment",
                shipment.getId().toString());

        return buildShipmentResponse(shipment, traceCodes, currentUser.getFullName());
    }

    /**
     * Kích hoạt tem cho lô hàng và cập nhật trạng thái tem liên kết.
     *
     * @param shipmentId id lô hàng cần kích hoạt
     * @return thông tin lô hàng sau khi kích hoạt
     * @throws BusinessException nếu không đủ điều kiện kích hoạt tem
     */
    @Override
    public ShipmentResponse activateShipmentStamps(UUID shipmentId) {
        CustomUserDetails currentUser = getCurrentUser();

        validateRole(currentUser, ORG_MANAGER_ROLE, "Bạn không có quyền kích hoạt tem.");

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng."));

        if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException("Bạn không có quyền kích hoạt tem của tổ chức khác.");
        }

        ProductionLot productionLot = shipment.getProductionLot();
        if (productionLot == null || productionLot.getStatus() != ProductionLotStatus.PACKAGED) {
            throw new BusinessException(INVALID_LOT_STATUS_MESSAGE);
        }

        if (shipment.getStatus() == ShipmentStatus.ACTIVATED) {
            throw new BusinessException("Tem đã được kích hoạt trước đó.");
        }

        if (shipment.getStatus() != ShipmentStatus.CODE_PRINTED) {
            throw new BusinessException("Lô hàng chưa được cấp hoặc in mã tem.");
        }

        /*
         * QTN-21 / QTN-30 (NCL-11-CN-005): rào chắn thứ hai trước khi
         * kích hoạt tem — lô chưa đạt kiểm nghiệm không thể kích hoạt.
         * Không thay thế gate tại POST /shipments.
         */
        validateInspectionEligibility(productionLot);

        User actor = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại."));

        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipmentRepository.save(shipment);

        List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(shipmentId);
        LocalDateTime now = LocalDateTime.now();
        for (TraceCode tc : traceCodes) {
            tc.setStatus(TraceCodeStatus.ACTIVE);
            tc.setActivatedAt(now);
            tc.setActivatedBy(actor);
        }
        traceCodeRepository.saveAll(traceCodes);

        publishActivityLog(
                currentUser,
                "ACTIVATE",
                "Kích hoạt tem cho lô hàng " + shipment.getName(),
                "Shipment",
                shipment.getId().toString());

        String createdByName = null;
        if (shipment.getCreatedBy() != null) {
            createdByName = userRepository.findById(shipment.getCreatedBy().getUserId())
                    .map(User::getFullName)
                    .orElse(null);
        }

        return buildShipmentResponse(shipment, traceCodes, createdByName);
    }

    /**
     * Lấy danh sách lô hàng theo ID của lô sản xuất.
     *
     * @param productionLotId ID của lô sản xuất
     * @return danh sách ShipmentResponse
     * @throws BusinessException nếu không tìm thấy lô sản xuất hoặc không thuộc tổ
     *                           chức
     */
    @Override
    public List<ShipmentResponse> getShipmentsByProductionLot(UUID productionLotId) {
        CustomUserDetails currentUser = getCurrentUser();
        ProductionLot productionLot = findProductionLot(productionLotId);

        // Kiểm tra tổ chức
        if (!productionLot.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(ORGANIZATION_ACCESS_MESSAGE);
        }

        List<Shipment> shipments = shipmentRepository.findByProductionLotId(productionLotId);
        List<ShipmentResponse> shipmentResponses = shipments.stream()
                .map(shipment -> {
                    List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(shipment.getId());
                    String createdByName = null;
                    if (shipment.getCreatedBy() != null) {
                        createdByName = userRepository.findById(shipment.getCreatedBy().getUserId())
                                .map(User::getFullName)
                                .orElse(null);
                    }
                    return buildShipmentResponse(shipment, traceCodes, createdByName);
                })
                .collect(Collectors.toList());
        return shipmentResponses;
    }

    /**
     * Lấy danh sách lô hàng theo ID của lô sản xuất với phân trang.
     *
     * @param productionLotId ID của lô sản xuất
     * @param page            số trang (bắt đầu từ 0)
     * @param size            số bản ghi trên mỗi trang
     * @return dữ liệu phân trang
     */
    @Override
    public PageResponse<ShipmentResponse> getShipmentsByProductionLotPaged(
            UUID productionLotId, int page, int size) {

        CustomUserDetails currentUser = getCurrentUser();
        ProductionLot productionLot = findProductionLot(productionLotId);

        if (!productionLot.getOrganization().getOrganizationId()
                .equals(currentUser.getOrganizationId())) {
            throw new BusinessException(ORGANIZATION_ACCESS_MESSAGE);
        }

        PageRequest pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Shipment> shipmentsPage =
                shipmentRepository.findByProductionLotId(productionLotId, pageable);

        List<ShipmentResponse> responses = shipmentsPage.getContent().stream()
                .map(shipment -> {
                    List<TraceCode> traceCodes =
                            traceCodeRepository.findByShipmentId(shipment.getId());
                    String createdByName = null;
                    if (shipment.getCreatedBy() != null) {
                        createdByName = userRepository
                                .findById(shipment.getCreatedBy().getUserId())
                                .map(User::getFullName)
                                .orElse(null);
                    }
                    return buildShipmentResponse(shipment, traceCodes, createdByName);
                })
                .toList();

        return PageResponse.from(shipmentsPage, responses);
    }

    /**
     * Lấy thông tin người dùng đang đăng nhập từ SecurityContext.
     *
     * @return thông tin người dùng hiện tại
     */
    private CustomUserDetails getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return (CustomUserDetails) authentication.getPrincipal();
    }

    private void publishActivityLog(CustomUserDetails currentUser, String action, String description, String entityType,
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
     * Kiểm tra người dùng có đúng vai trò được phép thực hiện nghiệp vụ.
     *
     * @param currentUser  người dùng hiện tại
     * @param expectedRole mã vai trò yêu cầu
     * @param message      thông báo lỗi nếu không đủ quyền
     * @throws BusinessException nếu người dùng không có quyền
     */
    private void validateRole(CustomUserDetails currentUser, String expectedRole, String message) {

        if (!expectedRole.equals(currentUser.getRoleCode())) {
            throw new BusinessException(message);
        }
    }

    /**
     * Tìm lô sản xuất theo id.
     *
     * @param productionLotId id lô sản xuất
     * @return lô sản xuất
     * @throws BusinessException nếu không tìm thấy lô sản xuất
     */
    private ProductionLot findProductionLot(UUID productionLotId) {

        return productionLotRepository.findById(productionLotId)
                .orElseThrow(() -> new BusinessException(PRODUCTION_LOT_NOT_FOUND_MESSAGE));
    }

    /**
     * Kiểm tra người dùng có quyền thao tác trên lô sản xuất
     * thuộc tổ chức của mình.
     *
     * @param currentUser   người dùng hiện tại
     * @param productionLot lô sản xuất cần kiểm tra
     * @throws BusinessException nếu khác tổ chức
     */
    private void validateOrganization(CustomUserDetails currentUser, ProductionLot productionLot) {

        if (!productionLot.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {

            throw new BusinessException(ORGANIZATION_ACCESS_MESSAGE);
        }
    }

    /**
     * Kiểm tra lô sản xuất đã ở trạng thái đóng gói
     * trước khi tạo lô hàng.
     *
     * @param productionLot lô sản xuất
     * @throws BusinessException nếu trạng thái không hợp lệ
     */
    private void validateProductionLotStatus(ProductionLot productionLot) {

        if (productionLot.getStatus() != ProductionLotStatus.PACKAGED) {

            throw new BusinessException(INVALID_LOT_STATUS_MESSAGE);
        }
    }

    /**
     * Gate QTN-30 (NCL-11-CN-005): lô chưa đạt kiểm nghiệm không được
     * tạo lô hàng / kích hoạt tem.
     *
     * <p>
     * Đánh giá qua {@link InspectionEligibilityService} — nguồn sự thật
     * dùng chung với pre-check {@code can-activate-seal}. Khi không đủ
     * điều kiện, trả {@code 409 CONFLICT} kèm {@code errors} chứa
     * {@code reasonCode} và thống kê chỉ tiêu; không persist bất kỳ dữ
     * liệu nào (Shipment / TraceCode / hạn mức dải mã giữ nguyên).
     * </p>
     *
     * @param productionLot lô sản xuất cần đánh giá
     * @throws BusinessException 409 CONFLICT nếu lô chưa đạt điều kiện
     */
    private void validateInspectionEligibility(ProductionLot productionLot) {

        InspectionEligibilityResult eligibility =
                inspectionEligibilityService.evaluateForShipment(productionLot);

        if (eligibility.isEligible()) {
            return;
        }

        InspectionBlockReasonCode reasonCode = eligibility.getReasonCode();

        throw new BusinessException(
                HttpStatus.CONFLICT,
                eligibility.getMessage(),
                buildInspectionBlockDetails(eligibility, reasonCode));
    }

    /**
     * Dựng {@code errors} cho response lỗi 409 — shape thống kê của
     * {@code CanActivateSealCheckResponse} + {@code reasonCode}
     * (tài liệu QTN-30 §5.1).
     */
    private Map<String, Object> buildInspectionBlockDetails(
            InspectionEligibilityResult eligibility,
            InspectionBlockReasonCode reasonCode) {

        Map<String, Object> details = new HashMap<>();
        details.put("reasonCode", reasonCode != null ? reasonCode.name() : null);
        details.put("totalCriteria", eligibility.getTotalCriteria());
        details.put("passedCriteria", eligibility.getPassedCriteria());
        details.put("failedOrExpiredCriteria",
                eligibility.getFailedOrExpiredCriteria());
        details.put("earliestExpiryDate", eligibility.getEarliestExpiryDate());
        return details;
    }

    /**
     * Lấy dải mã truy xuất còn hiệu lực của tổ chức.
     *
     * @param currentUser người dùng hiện tại
     * @return dải mã truy xuất
     * @throws BusinessException nếu tổ chức chưa được cấp dải mã
     */
    private CodeRange findAvailableCodeRange(CustomUserDetails currentUser) {

        return codeRangeRepository
                .findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(CODE_RANGE_NOT_FOUND_MESSAGE));
    }

    /**
     * Kiểm tra số lượng tem cần sinh có vượt quá
     * số lượng mã còn lại trong dải mã hay không.
     *
     * @param codeRange        dải mã truy xuất
     * @param requiredQuantity số lượng tem cần sinh
     * @throws BusinessException nếu vượt quá hạn mức
     */
    private void validateCodeRangeLimit(CodeRange codeRange, long requiredQuantity) {

        long remaining = Math.max(0, codeRange.getTotalLimit() - codeRange.getUsedCount());

        if (requiredQuantity > remaining) {

            throw new BusinessException(CODE_RANGE_LIMIT_EXCEEDED_MESSAGE);
        }
    }

    /**
     * Khởi tạo đối tượng lô hàng từ yêu cầu tạo lô hàng.
     *
     * @param request       thông tin tạo lô hàng
     * @param productionLot lô sản xuất
     * @param currentUser   người dùng tạo
     * @return đối tượng lô hàng
     */
    private Shipment createShipmentEntity(CreateShipmentRequest request, ProductionLot productionLot,
            CustomUserDetails currentUser, CodeRange codeRange) {

        Shipment shipment = new Shipment();

        shipment.setProductionLot(productionLot);
        shipment.setOrganization(productionLot.getOrganization());
        shipment.setCodeRange(codeRange);

        shipment.setName(request.getName());
        shipment.setTotalQuantity(request.getTotalQuantity());
        shipment.setPackagingInfo(request.getPackagingInfo());

        shipment.setStatus(ShipmentStatus.DRAFT);

        User createdBy = new User();
        createdBy.setUserId(currentUser.getUserId());

        shipment.setCreatedBy(createdBy);

        return shipment;
    }

    /**
     * Sinh danh sách mã truy xuất cho lô hàng.
     *
     * @param shipment  lô hàng
     * @param codeRange dải mã truy xuất
     * @param quantity  số lượng mã cần sinh
     * @return danh sách mã truy xuất
     */
    private List<TraceCode> generateTraceCodes(Shipment shipment, CodeRange codeRange, long quantity) {

        List<TraceCode> traceCodes = new ArrayList<>();

        long startSequence = codeRange.getUsedCount() + 1;

        UUID organizationId = shipment.getOrganization().getOrganizationId();
        UUID productionLotId = shipment.getProductionLot().getId();
        UUID shipmentId = shipment.getId();

        for (long i = 0; i < quantity; i++) {

            String codeValue = generateUniqueCode(codeRange.getPrefix(), startSequence + i);

            String qrImagePath = qrCodeService.generateQRCode(codeValue, organizationId, productionLotId, shipmentId);

            TraceCode traceCode = new TraceCode();

            traceCode.setQrImage(qrImagePath);

            traceCode.setShipment(shipment);

            traceCode.setCodeValue(codeValue);

            traceCode.setStatus(TraceCodeStatus.INACTIVE);

            traceCodes.add(traceCode);
        }

        return traceCodes;
    }

    /**
     * Sinh giá trị mã truy xuất duy nhất từ tiền tố
     * và số thứ tự trong dải mã.
     *
     * @param prefix   tiền tố mã
     * @param sequence số thứ tự
     * @return mã truy xuất
     */
    private String generateUniqueCode(String prefix, long sequence) {

        return prefix + String.format("%08d", sequence);
    }

    private void updateCodeRange(CodeRange codeRange, long quantity) {

        codeRange.setUsedCount(codeRange.getUsedCount() + quantity);
    }

    /**
     * Xây dựng dữ liệu phản hồi sau khi tạo lô hàng
     * và sinh mã truy xuất thành công.
     *
     * @param shipment      lô hàng
     * @param traceCodes    danh sách mã truy xuất
     * @param createdByName tên người tạo
     * @return thông tin phản hồiF
     */
    private ShipmentResponse buildShipmentResponse(Shipment shipment, List<TraceCode> traceCodes,
            String createdByName) {

        return ShipmentResponse.builder().id(shipment.getId()).productionLotId(shipment.getProductionLot().getId())
                .productionLotName(shipment.getProductionLot().getName()).name(shipment.getName())
                .totalQuantity(shipment.getTotalQuantity()).packagingInfo(shipment.getPackagingInfo())
                .status(shipment.getStatus())
                .traceCodes(traceCodes.stream()
                        .map(traceCode -> TraceCodeResponse.builder().id(traceCode.getId())
                                .codeValue(traceCode.getCodeValue()).qrImage(traceCode.getQrImage())
                                .status(traceCode.getStatus()).build())
                        .toList())
                .createdByName(createdByName).createdAt(shipment.getCreatedAt())
                .parentShipmentId(shipment.getParentShipment() != null ? shipment.getParentShipment().getId() : null)
                .recipientOrganization(shipment.getRecipientOrganization() != null
                        ? partnerResponse(shipment.getRecipientOrganization()) : null)
                .childCount(shipmentRepository.countByParentShipment_Id(shipment.getId()))
                .splitAt(shipment.getSplitAt()).build();
    }

    private void checkAndSendAlert(CodeRange range) {
        double percent = (double) range.getUsedCount() / range.getTotalLimit() * 100;
        if (percent >= 80 && percent < 100) {
            notificationService.sendAlert(
                    "Cảnh báo: Dải mã " + range.getPrefix() + " đã sử dụng " + range.getUsedCount() + "/"
                            + range.getTotalLimit() + " (gần mức hết hạn)");
        } else if (percent >= 100) {
            notificationService.sendAlert(
                    "Cảnh báo: Dải mã " + range.getPrefix() + " đã vượt hạn mức " + range.getTotalLimit() + "!");
        }
    }

    /**
     * Tra cứu lô hàng bằng mã truy xuất (codeValue in trên tem QR).
     * Dùng bởi VT-04 để xác nhận lô hàng trước khi ghi sự kiện thu mua.
     *
     * @param code mã truy xuất quét từ QR
     * @return thông tin tóm tắt lô hàng
     * @throws BusinessException nếu không tìm thấy mã hoặc lô hàng không hợp lệ
     */
    @Override
    public ShipmentSummaryResponse getShipmentByCode(String code) {
        TraceCode traceCode = traceCodeRepository.findByCodeValue(code)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng với mã: " + code));

        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new BusinessException("Mã truy xuất không liên kết với lô hàng nào.");
        }

        if (shipment.getStatus() == ShipmentStatus.RECALLED) {
            throw new BusinessException(
                    "Lô hàng " + shipment.getName() + " đã bị thu hồi, không thể ghi nhận thu mua.");
        }

        String productionLotName = null;
        if (shipment.getProductionLot() != null) {
            productionLotName = shipment.getProductionLot().getName();
        }

        return ShipmentSummaryResponse.builder()
                .id(shipment.getId())
                .name(shipment.getName())
                .status(shipment.getStatus())
                .productionLotName(productionLotName)
                .totalQuantity(shipment.getTotalQuantity())
                .build();
    }

    /**
     * Lấy danh sách lô hàng liên quan đến Doanh nghiệp thu mua (VT‑04) hiện tại:
     * chỉ lô đã thu mua/nhập kho hoặc đã xác nhận bàn giao cho tổ chức.
     *
     * @return danh sách lô hàng đủ điều kiện
     */
    @Override
    public List<ProcurementShipmentResponse> getEligibleShipments() {
        CustomUserDetails currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getOrganizationId() == null) {
            return List.of();
        }

        UUID currentOrgId = currentUser.getOrganizationId();
        Set<UUID> relatedShipmentIds = new HashSet<>();

        Map<UUID, ShipmentHandover> latestHandoverByShipment = shipmentHandoverRepository
                .findByToOrganizationOrganizationId(currentOrgId)
                .stream()
                .collect(Collectors.toMap(
                        handover -> handover.getShipment().getId(),
                        handover -> handover,
                        (first, second) -> first.getCreatedAt().isAfter(second.getCreatedAt())
                                ? first
                                : second));
        latestHandoverByShipment.values().stream()
                .filter(handover -> handover.getStatus() == ShipmentHandoverStatus.ACCEPTED)
                .map(handover -> handover.getShipment().getId())
                .forEach(relatedShipmentIds::add);

        relatedShipmentIds.addAll(chainEventRepository.findShipmentIdsByRecordedOrganizationIdAndEventTypeIn(
                currentOrgId,
                List.of(ChainEventType.PROCUREMENT, ChainEventType.WAREHOUSE_RECEIPT)));

        List<Shipment> shipments = shipmentRepository.findByStatusOrderByCreatedAtDesc(ShipmentStatus.ACTIVATED);

        return shipments.stream()
                .filter(shipment -> relatedShipmentIds.contains(shipment.getId()))
                .map(shipment -> {
                    String productionLotName = null;
                    String productCategoryName = null;
                    String organizationName = null;
                    if (shipment.getProductionLot() != null) {
                        productionLotName = shipment.getProductionLot().getName();
                        if (shipment.getProductionLot().getProductCategory() != null) {
                            productCategoryName = shipment.getProductionLot().getProductCategory().getName();
                        }
                    }
                    if (shipment.getOrganization() != null) {
                        organizationName = shipment.getOrganization().getName();
                    } else if (shipment.getProductionLot() != null
                            && shipment.getProductionLot().getOrganization() != null) {
                        organizationName = shipment.getProductionLot().getOrganization().getName();
                    }

                    return ProcurementShipmentResponse.builder()
                            .id(shipment.getId())
                            .name(shipment.getName())
                            .status(shipment.getStatus())
                            .productionLotName(productionLotName)
                            .productCategoryName(productCategoryName)
                            .organizationName(organizationName)
                            .totalQuantity(shipment.getTotalQuantity())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin chi tiết lô hàng theo ID.
     *
     * @param id ID của lô hàng
     * @return thông tin chi tiết lô hàng
     * @throws BusinessException nếu không tìm thấy hoặc bị chặn quyền
     */
    @Override
    public ShipmentResponse getShipmentById(UUID id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng với ID: " + id));

        // Kiểm tra quyền hạn chi tiết động qua PermissionChecker
        permissionChecker.check("shipment", "READ");

        CustomUserDetails currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("Chưa đăng nhập");
        }

        // Kiểm tra ranh giới dữ liệu (Data Boundary): VT-02 và VT-03 chỉ được xem lô
        // hàng thuộc tổ chức của mình
        String roleCode = currentUser.getRoleCode();
        if ("VT-02".equals(roleCode) || "VT-03".equals(roleCode)) {
            UUID userOrgId = currentUser.getOrganizationId();
            if (userOrgId == null || !userOrgId.equals(shipment.getOrganization().getOrganizationId())) {
                throw new BusinessException("Bạn không có quyền truy cập lô hàng của tổ chức khác.");
            }
        } else if ("VT-04".equals(roleCode)) {
            UUID userOrgId = currentUser.getOrganizationId();
            if (userOrgId == null || shipment.getRecipientOrganization() == null
                    || !userOrgId.equals(shipment.getRecipientOrganization().getOrganizationId())) {
                throw splitError(HttpStatus.FORBIDDEN,
                        "Lô hàng không được giao cho tổ chức của bạn.", "RECIPIENT_MISMATCH");
            }
        }

        // Lấy danh sách TraceCode liên kết với lô hàng
        List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(id);

        String createdByName = shipment.getCreatedBy() != null ? shipment.getCreatedBy().getFullName() : null;

        return buildShipmentResponse(shipment, traceCodes, createdByName);
    }

    @Override
    public PageResponse<PartnerOrganizationResponse> getPartnerOrganizations(String keyword, int page, int size) {
        requireSplitActor("organization", "READ");
        Page<Organization> partners = organizationRepository
                .searchActiveEnterprisePartners(
                        OrganizationType.ENTERPRISE, OrganizationStatus.ACTIVE,
                        getCurrentUser().getOrganizationId(), keyword == null ? "" : keyword.trim(),
                        PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by("name")));
        return PageResponse.from(partners, partners.getContent().stream().map(this::partnerResponse).toList());
    }

    @Override
    public SplitPreviewResponse getSplitPreview(UUID shipmentId) {
        requireSplitActor("shipment", "SPLIT");
        Shipment shipment = findOwnedShipmentForPreview(shipmentId);
        List<TraceCode> codes = traceCodeRepository.findByShipmentId(shipmentId).stream()
                .sorted(Comparator.comparing(TraceCode::getCodeValue).thenComparing(TraceCode::getId)).toList();
        long inactive = codes.stream().filter(c -> c.getStatus() == TraceCodeStatus.INACTIVE).count();
        String reason = previewBlockReason(shipment, codes, inactive);
        return SplitPreviewResponse.builder().shipmentId(shipment.getId()).shipmentName(shipment.getName())
                .status(shipment.getStatus()).productionLotId(shipment.getProductionLot().getId())
                .productionLotName(shipment.getProductionLot().getName()).declaredQuantity(shipment.getTotalQuantity())
                .assignableQuantity(inactive).nonInactiveQuantity(codes.size() - inactive)
                .availableCodeRange(codes.isEmpty() ? null : SplitPreviewResponse.CodeRange.builder()
                        .fromCode(codes.getFirst().getCodeValue()).toCode(codes.getLast().getCodeValue())
                        .quantity(inactive).build())
                .canSplit(reason == null).blockReasonCode(reason).blockMessage(previewMessage(reason)).build();
    }

    @Override
    public SplitShipmentResponse splitShipment(UUID shipmentId, SplitShipmentRequest request) {
        requireSplitActor("shipment", "SPLIT");
        CustomUserDetails actor = getCurrentUser();
        Shipment parent = shipmentRepository.findOwnedByIdForSplitUpdate(shipmentId, actor.getOrganizationId())
                .orElseThrow(() -> splitError(HttpStatus.NOT_FOUND, "Không tìm thấy lô hàng.", "SHIPMENT_NOT_FOUND"));
        validateParentForSplit(parent);
        List<TraceCode> codes = traceCodeRepository.findAllByShipmentIdForSplitUpdate(shipmentId);
        validateLockedCodes(parent, codes);
        validateAllocations(request, parent, codes);

        LocalDateTime splitAt = LocalDateTime.now();
        User splitBy = userRepository.getReferenceById(actor.getUserId());
        String sourceLastEventHash = chainEventRepository.findTopByShipmentIdOrderByCreatedAtDesc(parent.getId())
                .map(ChainEvent::getHash).orElse(null);
        List<SplitShipmentResponse.ChildShipment> children = new ArrayList<>();
        for (SplitShipmentAllocationRequest allocation : request.getAllocations()) {
            Organization recipient = validRecipient(allocation.getRecipientOrganizationId(), parent.getOrganization().getOrganizationId());
            Shipment child = new Shipment();
            child.setProductionLot(parent.getProductionLot()); child.setOrganization(parent.getOrganization());
            child.setCodeRange(parent.getCodeRange()); child.setParentShipment(parent); child.setRecipientOrganization(recipient);
            child.setSplitAt(splitAt); child.setSplitBy(splitBy); child.setCreatedBy(splitBy);
            child.setName(allocation.getName().trim()); child.setPackagingInfo(allocation.getPackagingInfo());
            child.setTotalQuantity(allocation.getQuantity()); child.setStatus(ShipmentStatus.CODE_PRINTED);
            child = shipmentRepository.save(child);
            List<TraceCode> allocated = codesForRange(codes, allocation.getFromCode(), allocation.getToCode());
            Shipment persistedChild = child;
            allocated.forEach(code -> code.setShipment(persistedChild));
            traceCodeRepository.saveAll(allocated);
            saveSplitEvent(child, parent, recipient, allocation, actor, splitAt, sourceLastEventHash);
            children.add(SplitShipmentResponse.ChildShipment.builder().id(child.getId()).parentShipmentId(parent.getId())
                    .name(child.getName()).status(child.getStatus()).recipientOrganization(partnerResponse(recipient))
                    .totalQuantity(child.getTotalQuantity()).firstCode(allocation.getFromCode())
                    .lastCode(allocation.getToCode()).build());
        }
        parent.setStatus(ShipmentStatus.SPLIT); parent.setSplitAt(splitAt); parent.setSplitBy(splitBy);
        shipmentRepository.save(parent);
        publishActivityLog(actor, "SPLIT_SHIPMENT", "Tách lô hàng " + parent.getName(), "SHIPMENT", parent.getId().toString());
        return SplitShipmentResponse.builder().sourceShipment(SplitShipmentResponse.SourceShipment.builder()
                .id(parent.getId()).name(parent.getName()).status(parent.getStatus()).declaredQuantity(parent.getTotalQuantity())
                .allocatedQuantity(parent.getTotalQuantity()).build()).children(children).totalChildren(children.size())
                .totalAllocatedQuantity(parent.getTotalQuantity()).splitByName(actor.getFullName()).splitAt(splitAt).build();
    }

    private void requireSplitActor(String resource, String action) {
        CustomUserDetails actor = getCurrentUser();
        if (actor == null || !ORG_MANAGER_ROLE.equals(actor.getRoleCode())) throw accessDenied();
        try { permissionChecker.check(resource, action); } catch (BusinessException ex) { throw accessDenied(); }
    }

    private Shipment findOwnedShipmentForPreview(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> splitError(HttpStatus.NOT_FOUND, "Không tìm thấy lô hàng.", "SHIPMENT_NOT_FOUND"));
        if (!shipment.getOrganization().getOrganizationId().equals(getCurrentUser().getOrganizationId()))
            throw splitError(HttpStatus.FORBIDDEN, "Bạn không có quyền tách lô hàng của tổ chức khác.", "CROSS_ORGANIZATION_ACCESS");
        return shipment;
    }

    private void validateParentForSplit(Shipment parent) {
        if (parent.getParentShipment() != null) throw splitError(HttpStatus.CONFLICT, "Không hỗ trợ tách tiếp một lô con.", "CHILD_SPLIT_NOT_ALLOWED");
        if (parent.getStatus() == ShipmentStatus.SPLIT || shipmentRepository.existsByParentShipment_Id(parent.getId()))
            throw splitError(HttpStatus.CONFLICT, "Lô hàng đã được tách trước đó.", "ALREADY_SPLIT");
        if (parent.getStatus() != ShipmentStatus.CODE_PRINTED)
            throw splitError(HttpStatus.CONFLICT, "Chỉ có thể tách lô hàng đã sinh mã và chưa kích hoạt.", "INVALID_SHIPMENT_STATUS");
    }

    private void validateLockedCodes(Shipment parent, List<TraceCode> codes) {
        if (codes.size() != parent.getTotalQuantity() || codes.size() < 2 ||
                codes.stream().anyMatch(c -> c.getStatus() != TraceCodeStatus.INACTIVE))
            throw splitError(HttpStatus.CONFLICT, "Thông tin mã tem đã thay đổi. Vui lòng tải lại thông tin lô.", "TRACE_CODE_STATE_CHANGED");
    }

    private void validateAllocations(SplitShipmentRequest request, Shipment parent, List<TraceCode> codes) {
        if (request.getAllocations() == null || request.getAllocations().size() < 2)
            throw splitError(HttpStatus.BAD_REQUEST, "Phải phân bổ lô hàng cho ít nhất hai đối tác.", "SPLIT_001");
        Set<UUID> recipients = new HashSet<>(); long total = 0; Set<UUID> selected = new HashSet<>();
        for (SplitShipmentAllocationRequest allocation : request.getAllocations()) {
            if (!recipients.add(allocation.getRecipientOrganizationId())) throw splitError(HttpStatus.BAD_REQUEST, "Mỗi đối tác chỉ được xuất hiện một lần trong yêu cầu tách lô.", "SPLIT_003");
            if (allocation.getQuantity() == null || allocation.getQuantity() <= 0) throw splitError(HttpStatus.BAD_REQUEST, "Số lượng phân bổ phải lớn hơn 0.", "SPLIT_002");
            List<TraceCode> range = codesForRange(codes, allocation.getFromCode(), allocation.getToCode());
            if (range.size() != allocation.getQuantity()) throw splitError(HttpStatus.BAD_REQUEST, "Số lượng lô con phải bằng số mã trong khoảng đã chọn.", "SPLIT_009");
            for (TraceCode code : range) if (!selected.add(code.getId())) throw splitError(HttpStatus.BAD_REQUEST, "Các khoảng mã phải liên tục, không trùng và phủ toàn bộ mã của lô cha.", "SPLIT_008");
            total += allocation.getQuantity(); validRecipient(allocation.getRecipientOrganizationId(), parent.getOrganization().getOrganizationId());
        }
        if (total != parent.getTotalQuantity()) throw splitError(HttpStatus.BAD_REQUEST, "Tổng số lượng phân bổ phải bằng " + parent.getTotalQuantity() + ".", "SPLIT_004");
        if (selected.size() != codes.size()) throw splitError(HttpStatus.BAD_REQUEST, "Các khoảng mã phải liên tục, không trùng và phủ toàn bộ mã của lô cha.", "SPLIT_008");
    }

    private List<TraceCode> codesForRange(List<TraceCode> codes, String from, String to) {
        int start = -1, end = -1;
        for (int i = 0; i < codes.size(); i++) { if (codes.get(i).getCodeValue().equals(from)) start = i; if (codes.get(i).getCodeValue().equals(to)) end = i; }
        if (start < 0 || end < start) throw splitError(HttpStatus.BAD_REQUEST, "Khoảng mã không hợp lệ hoặc chứa mã không thể phân bổ.", "SPLIT_007");
        return codes.subList(start, end + 1);
    }

    private Organization validRecipient(UUID id, UUID sourceOrganizationId) {
        if (sourceOrganizationId.equals(id)) throw splitError(HttpStatus.BAD_REQUEST, "Không thể chọn tổ chức nguồn làm đối tác nhận.", "SPLIT_006");
        Organization recipient = organizationRepository.findById(id)
                .orElseThrow(() -> splitError(HttpStatus.NOT_FOUND, "Không tìm thấy đối tác nhận.", "PARTNER_NOT_FOUND"));
        if (recipient.getType() != OrganizationType.ENTERPRISE || recipient.getStatus() != OrganizationStatus.ACTIVE)
            throw splitError(HttpStatus.BAD_REQUEST, "Đối tác nhận không hợp lệ hoặc đã ngừng hoạt động.", "SPLIT_005");
        return recipient;
    }

    private void saveSplitEvent(Shipment child, Shipment parent, Organization recipient, SplitShipmentAllocationRequest allocation,
            CustomUserDetails actor, LocalDateTime splitAt, String sourceLastEventHash) {
        try {
            Map<String, Object> data = new HashMap<>(); data.put("sourceShipmentId", parent.getId().toString());
            data.put("sourceShipmentName", parent.getName());
            data.put("recipientOrganizationId", recipient.getOrganizationId().toString());
            data.put("recipientOrganizationName", recipient.getName());
            data.put("allocatedQuantity", allocation.getQuantity());
            data.put("fromCode", allocation.getFromCode());
            data.put("toCode", allocation.getToCode());
            data.put("sourceLastEventHash", sourceLastEventHash);
            User recordedBy = userRepository.getReferenceById(actor.getUserId());
            ChainEvent event = ChainEvent.builder().shipment(child).eventType(ChainEventType.SPLIT)
                    .eventData(objectMapper.writeValueAsString(data)).recordedAt(splitAt).recordedBy(recordedBy)
                    .recordedOrganizationId(actor.getOrganizationId()).isCorrection(false).build();
            event.setPreviousHash(null); event.setHash(eventHashService.calculateHash(event, "")); chainEventRepository.save(event);
        } catch (Exception e) { throw new IllegalStateException("Không thể ghi sự kiện tách lô.", e); }
    }

    private PartnerOrganizationResponse partnerResponse(Organization org) { return PartnerOrganizationResponse.builder().id(org.getOrganizationId()).code(org.getCode()).name(org.getName()).build(); }
    private BusinessException accessDenied() { return splitError(HttpStatus.FORBIDDEN, "Bạn không có quyền tách lô hàng.", "ACCESS_DENIED"); }
    private BusinessException splitError(HttpStatus status, String message, String code) { return new BusinessException(status, message, Map.of("code", code)); }
    private String previewBlockReason(Shipment s, List<TraceCode> codes, long inactive) { if (s.getParentShipment() != null) return "CHILD_SHIPMENT"; if (s.getStatus() == ShipmentStatus.SPLIT || shipmentRepository.existsByParentShipment_Id(s.getId())) return "ALREADY_SPLIT"; if (s.getStatus() != ShipmentStatus.CODE_PRINTED) return "INVALID_STATUS"; if (codes.size() < 2) return "INSUFFICIENT_CODES"; return inactive != codes.size() || codes.size() != s.getTotalQuantity() ? "NON_INACTIVE_CODE_EXISTS" : null; }
    private String previewMessage(String reason) {
        if (reason == null) return null;
        return switch (reason) {
            case "INVALID_STATUS" -> "Chỉ có thể tách lô hàng đã sinh mã và chưa kích hoạt.";
            case "ALREADY_SPLIT" -> "Lô hàng đã được tách trước đó.";
            case "CHILD_SHIPMENT" -> "Không hỗ trợ tách tiếp một lô con.";
            case "INSUFFICIENT_CODES" -> "Lô hàng cần ít nhất hai mã tem chưa kích hoạt để tách.";
            case "NON_INACTIVE_CODE_EXISTS" -> "Tất cả mã tem của lô phải ở trạng thái chưa kích hoạt.";
            default -> "Lô hàng chưa đáp ứng điều kiện tách.";
        };
    }
}
