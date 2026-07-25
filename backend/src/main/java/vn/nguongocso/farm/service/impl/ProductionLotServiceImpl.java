package vn.nguongocso.farm.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.CreateProductionLotRequest;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.farm.dto.response.PackagingCheckResult;
import vn.nguongocso.farm.dto.response.UpdateProductionLotResponse;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.event.PackagingValidationFailedEvent;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.service.ProductionLotService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionLotServiceImpl implements ProductionLotService {

    private static final Logger log = LoggerFactory.getLogger(ProductionLotServiceImpl.class);

    private final ProductionLotRepository productionLotRepository;
    private final FarmAreaRepository farmAreaRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final FarmLogRepository farmLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CreateProductionLotResponse createProductionLot(CreateProductionLotRequest request, CustomUserDetails userDetails) {
        log.info("Bắt đầu xử lý tạo lô sản xuất với tên={}", request.getName());

        UUID userId = userDetails.getUserId();
        UUID orgId = userDetails.getOrganizationId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin tài khoản"));
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin tổ chức tương ứng"));

        ProductCategory productCategory = productCategoryRepository.findById(request.getProductCategoryId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy loại nông sản đã chọn"));
        if (Boolean.FALSE.equals(productCategory.getIsActive())) {
            throw new BusinessException("Loại nông sản này hiện đang ngưng hoạt động");
        }

        FarmArea farmArea = null;
        if (request.getFarmAreaId() != null) {
            farmArea = farmAreaRepository.findById(request.getFarmAreaId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy khu vực canh tác đã chọn"));

            if (!farmArea.getOrganization().getOrganizationId().equals(orgId)) {
                throw new BusinessException("Khu vực canh tác này không thuộc tổ chức của bạn");
            }
        }

        ProductionLot productionLot = ProductionLot.builder()
                .organization(organization)
                .farmArea(farmArea)
                .productCategory(productCategory)
                .name(request.getName())
                .expectedQuantity(request.getExpectedQuantity())
                .plantingDate(request.getPlantingDate())
                .status(ProductionLotStatus.DRAFT)
                .createdBy(user)
                .build();

        ProductionLot savedLot = productionLotRepository.save(productionLot);
        log.info("Đã tạo thành công lô sản xuất với id={}", savedLot.getId());

        return mapToResponse(savedLot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreateProductionLotResponse> getAllProductionLots(CustomUserDetails userDetails) {
        UUID orgId = userDetails.getOrganizationId();

        log.info("Lấy danh sách lô sản xuất cho tổ chức id={}", orgId);

        List<ProductionLot> lots = productionLotRepository.findByOrganization_OrganizationId(orgId);

        return lots.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CreateProductionLotResponse approveProductionLot(UUID lotId, ApproveProductionLotRequest request, CustomUserDetails userDetails) {
        log.info("Bắt đầu duyệt lô sản xuất với id={}",  lotId);

        UUID orgId = userDetails.getOrganizationId();
        UUID userId = userDetails.getUserId();

        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất"));

        if (!lot.getOrganization().getOrganizationId().equals(orgId)) {
            throw new BusinessException("Lô sản xuất không thuộc tổ chức của bạn");
        }

        if (lot.getStatus() != ProductionLotStatus.PENDING) {
            throw new BusinessException("Chỉ có thể duyệt lô đang ở trạng thái chờ duyệt");
        }

        User approver = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin tài khoản "));

        if (request.getApproved()) {
            lot.setStatus(ProductionLotStatus.APPROVED);
            lot.setApprovedBy(approver);
            lot.setApprovalNotes(null);
            log.info("Lô {} đã được duyệt bởi {}", lotId, userId);
        } else {
            lot.setStatus(ProductionLotStatus.DRAFT);
            lot.setApprovedBy(null);
            lot.setApprovalNotes(request.getReason());
            log.info("Lô {} bị từ chối bởi {}, lý do: {}", lotId, userId, request.getReason());
        }

        ProductionLot saved = productionLotRepository.save(lot);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CreateProductionLotResponse submitForApproval(UUID lotId, CustomUserDetails userDetails) {
        UUID orgId = userDetails.getOrganizationId();

        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất"));

        if (!lot.getOrganization().getOrganizationId().equals(orgId)) {
            throw new BusinessException("Bạn không có quyền với lô này");
        }

        if (lot.getStatus() != ProductionLotStatus.DRAFT) {
            throw new BusinessException("Chỉ  có thể gửi duyệt lô ở trạng thái DRAFT");
        }

        if (lot.getFarmArea() == null) {
            throw new BusinessException("Vui lòng chọn vùng trồng trước khi gửi duyệt");
        }

        lot.setStatus(ProductionLotStatus.PENDING);
        lot.setUpdatedAt(LocalDateTime.now());
        productionLotRepository.save(lot);

        log.info("Gửi duyệt lô thành công: lotId={}", lotId);
        return mapToResponse(lot);
    }

    private CreateProductionLotResponse mapToResponse(ProductionLot lot) {
        return CreateProductionLotResponse.builder()
                .id(lot.getId())
                .organizationName(lot.getOrganization().getName())
                .farmAreaName(lot.getFarmArea() != null ? lot.getFarmArea().getName() : null)
                .productCategoryName(lot.getProductCategory().getName())
                .name(lot.getName())
                .expectedQuantity(lot.getExpectedQuantity())
                .actualQuantity(lot.getActualQuantity())
                .plantingDate(lot.getPlantingDate())
                .harvestDate(lot.getHarvestDate())
                .status(lot.getStatus().name())
                .approvalNotes(lot.getApprovalNotes())
                .createdByName(lot.getCreatedBy() != null ? lot.getCreatedBy().getFullName() : null)
                .approvedByName(lot.getApprovedBy() != null ? lot.getApprovedBy().getFullName() : null)
                .createdAt(lot.getCreatedAt())
                .updatedAt(lot.getUpdatedAt())
                .build();
    }
    @Override
    @Transactional
    public UpdateProductionLotResponse updateProductionLot(UUID id, UpdateProductionLotRequest request, CustomUserDetails userDetails) {
        log.info("Bắt đầu xử lý cập nhật lô sản xuất với id={}", id);

        UUID orgId = userDetails.getOrganizationId();

        ProductionLot productionLot = productionLotRepository.findById(id)
                .orElseThrow(() -> new vn.nguongocso.exception.ResourceNotFoundException("Lô sản xuất không tồn tại"));

        if (!productionLot.getOrganization().getOrganizationId().equals(orgId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền chỉnh sửa lô sản xuất này");
        }

        if (productionLot.getStatus() != ProductionLotStatus.DRAFT) {
            throw new vn.nguongocso.exception.DuplicateResourceException("Chỉ có thể cập nhật lô sản xuất khi đang ở trạng thái nháp");
        }

        ProductCategory productCategory = productCategoryRepository.findById(request.getProductCategoryId())
                .orElseThrow(() -> new vn.nguongocso.exception.BusinessException("Không tìm thấy loại nông sản đã chọn"));
        if (Boolean.FALSE.equals(productCategory.getIsActive())) {
            throw new vn.nguongocso.exception.BusinessException("Loại nông sản này hiện đang ngưng hoạt động");
        }

        FarmArea farmArea = farmAreaRepository.findById(request.getFarmAreaId())
                .orElseThrow(() -> new vn.nguongocso.exception.BusinessException("Không tìm thấy khu vực canh tác đã chọn"));
        if (!farmArea.getOrganization().getOrganizationId().equals(orgId)) {
            throw new vn.nguongocso.exception.BusinessException("Khu vực canh tác này không thuộc tổ chức của bạn");
        }

        productionLot.setName(request.getName());
        productionLot.setFarmArea(farmArea);
        productionLot.setProductCategory(productCategory);
        productionLot.setExpectedQuantity(request.getExpectedQuantity());
        productionLot.setPlantingDate(request.getPlantingDate());

        ProductionLot savedLot = productionLotRepository.save(productionLot);
        log.info("Cập nhật thành công lô sản xuất id={}", savedLot.getId());

        return UpdateProductionLotResponse.builder()
                .id(savedLot.getId())
                .farmAreaId(savedLot.getFarmArea() != null ? savedLot.getFarmArea().getId() : null)
                .productCategoryId(savedLot.getProductCategory().getId())
                .name(savedLot.getName())
                .expectedQuantity(savedLot.getExpectedQuantity())
                .plantingDate(savedLot.getPlantingDate())
                .status(savedLot.getStatus().name())
                .updatedAt(savedLot.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PackagingCheckResult checkPackagingReadiness(UUID lotId) {
        log.info("Kiểm tra điều kiện đóng gói cho lô sản xuất id={}", lotId);

        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất"));

        if (lot.getStatus() != ProductionLotStatus.HARVESTED) {
            throw new BusinessException(
                    "Lô sản xuất chưa sẵn sàng để đóng gói. Trạng thái phải là HARVESTED."
            );
        }

        List<FarmLog> logs = farmLogRepository.findByProductionLotId(lot);

        Set<FarmActivityType> requiredTypes = Set.of(
                FarmActivityType.PLANTING,
                FarmActivityType.WATERING,
                FarmActivityType.FERTILIZING,
                FarmActivityType.PESTICIDE
        );

        Set<FarmActivityType> existingTypes = logs.stream()
                .map(FarmLog::getActivityType)
                .collect(Collectors.toSet());

        List<String> missing = requiredTypes.stream()
                .filter(type -> !existingTypes.contains(type))
                .map(Enum::name)
                .collect(Collectors.toList());

        boolean canPackage = missing.isEmpty();

        return PackagingCheckResult.builder()
                .lotId(lotId)
                .status(lot.getStatus())
                .canPackage(canPackage)
                .missingLogs(missing)
                .message(canPackage
                        ? "Lô sản xuất đã sẵn sàng để đóng gói"
                        : "Thiếu nhật ký canh tác bắt buộc: " + String.join(", ", missing))
                .build();
    }

    @Override
    @Transactional
    public CreateProductionLotResponse packageLot(UUID lotId, CustomUserDetails userDetails) {
        log.info("Bắt đầu đóng gói lô sản xuất id={}", lotId);

        // Kiểm tra điều kiện đóng gói
        PackagingCheckResult checkResult = checkPackagingReadiness(lotId);

        if (!checkResult.isCanPackage()) {
            // Phát sự kiện cảnh báo
            ProductionLot lot = productionLotRepository.findById(lotId)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất"));

            eventPublisher.publishEvent(new PackagingValidationFailedEvent(
                    this, lotId, lot.getOrganization().getOrganizationId(), lot.getName()
            ));

            throw new BusinessException(
                    "Không thể đóng gói. " + checkResult.getMessage()
            );
        }

        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất"));

        lot.setStatus(ProductionLotStatus.PACKAGED);
        lot.setUpdatedAt(LocalDateTime.now());
        productionLotRepository.save(lot);

        log.info("Đã đóng gói thành công lô sản xuất id={}", lotId);
        return mapToResponse(lot);
    }

}
