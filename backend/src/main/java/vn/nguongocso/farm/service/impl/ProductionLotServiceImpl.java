package vn.nguongocso.farm.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.CreateProductionLotRequest;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.event.PackagingValidationFailedEvent;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.service.ProductionLotService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import vn.nguongocso.exception.DuplicateResourceException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
import vn.nguongocso.farm.dto.response.UpdateProductionLotResponse;

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
        public CreateProductionLotResponse createProductionLot(
                        CreateProductionLotRequest request,
                        CustomUserDetails userDetails) {

                log.info(
                                "Bắt đầu xử lý tạo lô sản xuất với tên={}",
                                request.getName());

                UUID userId = userDetails.getUserId();
                UUID orgId = userDetails.getOrganizationId();

                User user = userRepository.findById(userId)
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Không tìm thấy thông tin tài khoản"));

                Organization organization = organizationRepository.findById(orgId)
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Không tìm thấy thông tin tổ chức tương ứng"));

                ProductCategory productCategory = productCategoryRepository
                                .findById(request.getProductCategoryId())
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Không tìm thấy loại nông sản đã chọn"));

                if (Boolean.FALSE.equals(productCategory.getIsActive())) {
                        throw new BusinessException(
                                        "Loại nông sản này hiện đang ngưng hoạt động");
                }

                FarmArea farmArea = null;

                if (request.getFarmAreaId() != null) {
                        farmArea = farmAreaRepository
                                        .findById(request.getFarmAreaId())
                                        .orElseThrow(
                                                        () -> new BusinessException(
                                                                        "Không tìm thấy khu vực canh tác đã chọn"));

                        if (!farmArea.getOrganization()
                                        .getOrganizationId()
                                        .equals(orgId)) {

                                throw new BusinessException(
                                                "Khu vực canh tác này không thuộc tổ chức của bạn");
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

                log.info(
                                "Đã tạo thành công lô sản xuất với id={}",
                                savedLot.getId());

                return mapToResponse(savedLot);
        }

        @Override
        @Transactional(readOnly = true)
        public List<CreateProductionLotResponse> getAllProductionLots(
                        CustomUserDetails userDetails) {

                UUID orgId = userDetails.getOrganizationId();

                log.info(
                                "Lấy danh sách lô sản xuất cho tổ chức id={}",
                                orgId);

                List<ProductionLot> lots = productionLotRepository
                                .findByOrganization_OrganizationId(orgId);

                return lots.stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public CreateProductionLotResponse submitProductionLot(
                        UUID id,
                        CustomUserDetails userDetails) {

                log.info(
                                "Bắt đầu xử lý gửi duyệt lô sản xuất với id={}",
                                id);

                UUID orgId = userDetails.getOrganizationId();

                ProductionLot lot = productionLotRepository.findById(id)
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Lô sản xuất không tồn tại"));

                if (!lot.getOrganization()
                                .getOrganizationId()
                                .equals(orgId)) {

                        throw new BusinessException(
                                        "Bạn không có quyền gửi duyệt lô sản xuất này");
                }

                if (lot.getStatus() != ProductionLotStatus.DRAFT) {
                        throw new BusinessException(
                                        "Chỉ có thể gửi duyệt lô sản xuất khi đang ở trạng thái nháp (DRAFT)");
                }

                lot.setStatus(ProductionLotStatus.PENDING);

                ProductionLot savedLot = productionLotRepository.save(lot);

                log.info(
                                "Đã gửi duyệt thành công lô sản xuất id={}, trạng thái={}",
                                savedLot.getId(),
                                savedLot.getStatus());

                return mapToResponse(savedLot);
        }

        @Override
        @Transactional
        public CreateProductionLotResponse approveProductionLot(
                        UUID id,
                        ApproveProductionLotRequest request,
                        CustomUserDetails userDetails) {

                log.info(
                                "Bắt đầu xử lý phê duyệt lô sản xuất với id={}, approved={}",
                                id,
                                request.getApproved());

                UUID orgId = userDetails.getOrganizationId();

                ProductionLot lot = productionLotRepository.findById(id)
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Lô sản xuất không tồn tại"));

                if ("VT-02".equals(userDetails.getRoleCode())
                                && !lot.getOrganization()
                                                .getOrganizationId()
                                                .equals(orgId)) {

                        throw new BusinessException(
                                        "Bạn không có quyền phê duyệt lô sản xuất của tổ chức khác");
                }

                if (lot.getStatus() != ProductionLotStatus.PENDING) {
                        throw new BusinessException(
                                        "Chỉ có thể phê duyệt lô sản xuất đang ở trạng thái chờ duyệt (PENDING)");
                }

                User approver = userRepository.findById(userDetails.getUserId())
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Không tìm thấy thông tin tài khoản người duyệt"));

                if (Boolean.TRUE.equals(request.getApproved())) {
                        lot.setStatus(ProductionLotStatus.APPROVED);
                        lot.setApprovedBy(approver);
                } else {
                        lot.setStatus(ProductionLotStatus.DRAFT);
                        lot.setApprovedBy(null);
                }

                lot.setApprovalNotes(request.getApprovalNotes());

                ProductionLot savedLot = productionLotRepository.save(lot);

                log.info(
                                "Đã xử lý phê duyệt lô sản xuất id={}, trạng thái mới={}",
                                savedLot.getId(),
                                savedLot.getStatus());

                return mapToResponse(savedLot);
        }

        @Override
        public CreateProductionLotResponse submitForApproval(UUID lotId, CustomUserDetails userDetails) {
                return null;
        }

        @Override
        @Transactional
        public UpdateProductionLotResponse updateProductionLot(
                        UUID id,
                        UpdateProductionLotRequest request,
                        CustomUserDetails userDetails) {

                log.info(
                                "Bắt đầu xử lý cập nhật lô sản xuất với id={}",
                                id);

                UUID orgId = userDetails.getOrganizationId();

                ProductionLot productionLot = productionLotRepository.findById(id)
                                .orElseThrow(
                                                () -> new ResourceNotFoundException(
                                                                "Lô sản xuất không tồn tại"));

                if (!productionLot.getOrganization()
                                .getOrganizationId()
                                .equals(orgId)) {

                        throw new AccessDeniedException(
                                        "Bạn không có quyền chỉnh sửa lô sản xuất này");
                }

                if (productionLot.getStatus() != ProductionLotStatus.DRAFT) {

                        throw new DuplicateResourceException(
                                        "Chỉ có thể cập nhật lô sản xuất khi đang ở trạng thái nháp");
                }

                ProductCategory productCategory = productCategoryRepository
                                .findById(request.getProductCategoryId())
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Không tìm thấy loại nông sản đã chọn"));

                if (Boolean.FALSE.equals(productCategory.getIsActive())) {
                        throw new BusinessException(
                                        "Loại nông sản này hiện đang ngưng hoạt động");
                }

                FarmArea farmArea = farmAreaRepository
                                .findById(request.getFarmAreaId())
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Không tìm thấy khu vực canh tác đã chọn"));

                if (!farmArea.getOrganization()
                                .getOrganizationId()
                                .equals(orgId)) {

                        throw new BusinessException(
                                        "Khu vực canh tác này không thuộc tổ chức của bạn");
                }

                productionLot.setName(request.getName());
                productionLot.setFarmArea(farmArea);
                productionLot.setProductCategory(productCategory);
                productionLot.setExpectedQuantity(
                                request.getExpectedQuantity());
                productionLot.setPlantingDate(
                                request.getPlantingDate());

                ProductionLot savedLot = productionLotRepository.save(productionLot);

                log.info(
                                "Cập nhật thành công lô sản xuất id={}",
                                savedLot.getId());

                return UpdateProductionLotResponse.builder()
                                .id(savedLot.getId())
                                .farmAreaId(
                                                savedLot.getFarmArea() != null
                                                                ? savedLot.getFarmArea().getId()
                                                                : null)
                                .productCategoryId(
                                                savedLot.getProductCategory().getId())
                                .name(savedLot.getName())
                                .expectedQuantity(savedLot.getExpectedQuantity())
                                .plantingDate(savedLot.getPlantingDate())
                                .status(savedLot.getStatus().name())
                                .updatedAt(savedLot.getUpdatedAt())
                                .build();
        }

        private CreateProductionLotResponse mapToResponse(
                        ProductionLot lot) {

                return CreateProductionLotResponse.builder()
                                .id(lot.getId())
                                .organizationName(lot.getOrganization().getName())
                                .farmAreaName(
                                                lot.getFarmArea() != null
                                                                ? lot.getFarmArea().getName()
                                                                : null)
                                .productCategoryName(
                                                lot.getProductCategory().getName())
                                .name(lot.getName())
                                .expectedQuantity(lot.getExpectedQuantity())
                                .actualQuantity(lot.getActualQuantity())
                                .plantingDate(lot.getPlantingDate())
                                .harvestDate(lot.getHarvestDate())
                                .status(lot.getStatus().name())
                                .approvalNotes(lot.getApprovalNotes())
                                .createdByName(
                                                lot.getCreatedBy() != null
                                                                ? lot.getCreatedBy().getFullName()
                                                                : null)
                                .approvedByName(
                                                lot.getApprovedBy() != null
                                                                ? lot.getApprovedBy().getFullName()
                                                                : null)
                                .createdAt(lot.getCreatedAt())
                                .updatedAt(lot.getUpdatedAt())
                                .build();
        }

        @Override
        @Transactional
        public CreateProductionLotResponse packageProductionLot(UUID id, CustomUserDetails userDetails) {
                // 1. Tìm kiếm và kiểm tra lô sản xuất
                ProductionLot lot = productionLotRepository.findById(id)
                                .orElseThrow(() -> new BusinessException("Lô sản xuất không tồn tại"));
                // 2. Kiểm tra trạng thái phải là HARVESTED (TC-03)
                if (lot.getStatus() != ProductionLotStatus.HARVESTED) {
                        throw new BusinessException(
                                        "Lô sản xuất phải ở trạng thái đã thu hoạch (HARVESTED) mới có thể đóng gói");
                }
                // 3. Lấy tất cả nhật ký được sắp xếp theo ngày thực hiện
                List<FarmLog> logs = farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(id);

                // Giả sử có danh sách hoạt động bắt buộc
                List<FarmActivityType> mandatoryTypes = Arrays.asList(
                                FarmActivityType.PLANTING,
                                FarmActivityType.FERTILIZING,
                                FarmActivityType.PESTICIDE,
                                FarmActivityType.HARVESTING);
                List<FarmActivityType> missingLogs = mandatoryTypes.stream()
                                .filter(type -> logs.stream().noneMatch(log -> log.getActivityType() == type))
                                .toList();
                if (!missingLogs.isEmpty()) {
                        // Phát sự kiện kiểm tra thất bại để hệ thống gửi thông báo (TC-04)
                        eventPublisher.publishEvent(new PackagingValidationFailedEvent(
                                        this,
                                        lot.getId(),
                                        userDetails.getOrganizationId(),
                                        lot.getName()));
                        // Ném lỗi chặn luồng nghiệp vụ
                        throw new BusinessException(
                                        "Không thể đóng gói. Lô thiếu các nhật ký bắt buộc: " + missingLogs);
                }
                // 4. Nếu đủ điều kiện, chuyển trạng thái sang PACKAGED
                lot.setStatus(ProductionLotStatus.PACKAGED);
                ProductionLot savedLot = productionLotRepository.save(lot);
                return mapToResponse(savedLot);
        }
}
