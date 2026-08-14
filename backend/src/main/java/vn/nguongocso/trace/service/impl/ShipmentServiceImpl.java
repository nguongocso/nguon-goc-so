package vn.nguongocso.trace.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.annotation.Auditable;
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
    @Auditable(action = "CREATE", entityType = "SHIPMENT", description = "'Tạo mới lô hàng cho lô sản xuất ID: ' + #request.productionLotId + ', Số lượng: ' + #request.totalQuantity")
    public ShipmentResponse createShipment(CreateShipmentRequest request) {

        CustomUserDetails currentUser = getCurrentUser();

        validateRole(currentUser, ORG_MANAGER_ROLE, "Bạn không có quyền tạo lô hàng.");

        ProductionLot productionLot = findProductionLot(request.getProductionLotId());

        validateOrganization(currentUser, productionLot);

        validateProductionLotStatus(productionLot);

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

        Shipment shipment = createShipmentEntity(request, productionLot, currentUser);

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
    @Auditable(action = "ACTIVATE", entityType = "SHIPMENT", description = "'Kích hoạt tem cho lô hàng ID: ' + #shipmentId")
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
        return shipments.stream()
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
     * Lấy dải mã truy xuất còn hiệu lực của tổ chức.
     *
     * @param currentUser người dùng hiện tại
     * @return dải mã truy xuất
     * @throws BusinessException nếu tổ chức chưa được cấp dải mã
     */
    private CodeRange findAvailableCodeRange(CustomUserDetails currentUser) {

        return codeRangeRepository.findByOrganizationOrganizationId(currentUser.getOrganizationId())
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
            CustomUserDetails currentUser) {

        Shipment shipment = new Shipment();

        shipment.setProductionLot(productionLot);
        shipment.setOrganization(productionLot.getOrganization());

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
                .createdByName(createdByName).createdAt(shipment.getCreatedAt()).build();
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

        // Không cho phép ghi sự kiện nếu lô hàng đã bị thu hồi
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
     * Lấy danh sách lô hàng đủ điều kiện thu mua (status = ACTIVATED).
     * Dùng cho Doanh nghiệp thu mua (VT‑04) xem danh sách lô hàng sẵn sàng.
     *
     * @return danh sách ProcurementShipmentResponse
     */
    @Override
    public List<ProcurementShipmentResponse> getEligibleShipments() {
        List<Shipment> shipments = shipmentRepository.findByStatusOrderByCreatedAtDesc(ShipmentStatus.ACTIVATED);

        return shipments.stream()
                .map(shipment -> {
                    String productionLotName = null;
                    String productCategoryName = null;
                    if (shipment.getProductionLot() != null) {
                        productionLotName = shipment.getProductionLot().getName();
                        if (shipment.getProductionLot().getProductCategory() != null) {
                            productCategoryName = shipment.getProductionLot().getProductCategory().getName();
                        }
                    }

                    return ProcurementShipmentResponse.builder()
                            .id(shipment.getId())
                            .name(shipment.getName())
                            .status(shipment.getStatus())
                            .productionLotName(productionLotName)
                            .productCategoryName(productCategoryName)
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
        }

        // Lấy danh sách TraceCode liên kết với lô hàng
        List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(id);

        String createdByName = shipment.getCreatedBy() != null ? shipment.getCreatedBy().getFullName() : null;

        return buildShipmentResponse(shipment, traceCodes, createdByName);
    }
}
