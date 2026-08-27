package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateInspectionRequest;
import vn.nguongocso.certification.dto.response.InspectionRequestDetailResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestResponse;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionCatalogRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.certification.service.impl.DuplicateInspectionRequestException;
import vn.nguongocso.certification.service.impl.InspectionRequestServiceImpl;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class InspectionRequestServiceImplTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private InspectionRequestRepository inspectionRequestRepository;

    @Mock
    private InspectionCriterionCatalogRepository inspectionCriterionCatalogRepository;

    @Mock
    private CategoryCriterionRepository categoryCriterionRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private ProductionLotCertificationRepository productionLotCertificationRepository;

    @Mock
    private InspectionCriterionResultRepository inspectionCriterionResultRepository;

    @Mock
    private Clock clock;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InspectionRequestServiceImpl inspectionRequestService;

    private UUID lotId;
    private UUID orgId;

    private CustomUserDetails currentUser;

    private User user;

    private ProductionLot lot;

    private Standard standard;

    private UUID categoryId;

    private ProductCategory category;

    private InspectionCriterionCatalog catalogCriterion;

    @BeforeEach
    void setUp() {

        lotId = UUID.randomUUID();

        orgId = UUID.randomUUID();

        /*
         * User test.
         */
        user = new User();
        user.setUserId(UUID.randomUUID());

        /*
         * Current user.
         *
         * Role VT-02 = quản lý hợp tác xã.
         */
        currentUser = Mockito.mock(CustomUserDetails.class);

        lenient().when(currentUser.getRoleCode())
                .thenReturn("VT-02");

        lenient().when(currentUser.getOrganizationId())
                .thenReturn(orgId);

        lenient().when(currentUser.getUser())
                .thenReturn(user);

        /*
         * Business clock: dùng zone hệ thống để LocalDate.now(clock)
         * khớp với LocalDate.now() dùng trong fixture.
         */
        lenient().when(clock.instant())
                .thenReturn(Clock.systemDefaultZone().instant());
        lenient().when(clock.getZone())
                .thenReturn(ZoneId.systemDefault());

        categoryId = UUID.randomUUID();

        /*
         * Production lot test.
         */
        lot = new ProductionLot();

        lot.setId(lotId);

        lot.setName("Lô 01");

        lot.setStatus(
                ProductionLotStatus.APPROVED);

        lot.setOrganization(
                new vn.nguongocso.organization.entity.Organization());

        lot.getOrganization()
                .setOrganizationId(orgId);

        /*
         * Standard test (chỉ dùng cho snapshot legacy ở getDetail).
         */
        standard = new Standard();

        standard.setId(
                UUID.randomUUID());

        standard.setName("VietGAP");

        /*
         * Product category của lô (NCL-09-CN-009).
         */
        category = new ProductCategory();

        category.setId(categoryId);

        category.setName("Rau ăn lá");

        lot.setProductCategory(category);

        /*
         * Chỉ tiêu trong danh mục dùng chung (NCL-09-CN-009).
         */
        catalogCriterion = InspectionCriterionCatalog.builder()
                .id(101L)
                .name("Dư lượng thuốc trừ sâu")
                .unit("mg/kg")
                .maxThreshold(new java.math.BigDecimal("0.5"))
                .status("ACTIVE")
                .build();
    }

    /**
     * Test tạo yêu cầu kiểm nghiệm thành công.
     *
     * Logic service:
     *
     * InspectionRequestStatus.PENDING_RESULT
     *        ↓
     * mapStatus()
     *        ↓
     * "PENDING"
     */
    @Test
    void createInspectionRequest_shouldCreatePendingRequest_whenValid() {

        /*
         * Arrange
         */
        CreateInspectionRequest request =
                new CreateInspectionRequest();

        request.setTestingUnit(
                "Lab ABC");

        request.setSampleSentDate(
                LocalDate.now());

        request.setCriteriaIds(
                List.of(101L));

        /*
         * Production lot tồn tại
         * và thuộc organization hiện tại.
         */
        when(
                productionLotRepository
                        .findByIdAndOrganization_OrganizationId(
                                lotId,
                                orgId))
                .thenReturn(
                        Optional.of(lot));

        /*
         * Lô đã có sự kiện HARVEST.
         */
        when(
                chainEventRepository
                        .existsByProductionLotIdOrUnassignedEventDataAndEventType(
                                lotId,
                                lotId.toString(),
                                ChainEventType.HARVEST))
                .thenReturn(true);

        /*
         * Chỉ tiêu tồn tại trong danh mục dùng chung.
         */
        when(
                inspectionCriterionCatalogRepository
                        .findById(101L))
                .thenReturn(
                        Optional.of(
                                catalogCriterion));

        /*
         * Chỉ tiêu được gán cho loại nông sản của lô.
         */
        when(
                categoryCriterionRepository
                        .existsByCategory_IdAndCriterion_Id(
                                categoryId,
                                101L))
                .thenReturn(true);

        /*
         * Không có request PENDING_RESULT
         * trước đó.
         */
        when(
                inspectionRequestRepository
                        .findByProductionLot_IdAndStatus(
                                lotId,
                                InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(
                        List.of());

        /*
         * Giả lập save().
         *
         * Service cần ID sau khi save để
         * mapping sang response.
         */
        when(
                inspectionRequestRepository
                        .save(any(InspectionRequest.class)))
                .thenAnswer(invocation -> {

                    InspectionRequest saved =
                            invocation.getArgument(
                                    0);

                    saved.setId(
                            UUID.randomUUID());

                    return saved;
                });

        /*
         * Act
         */
        InspectionRequestResponse response =
                inspectionRequestService
                        .createInspectionRequest(
                                lotId,
                                request,
                                currentUser);

        /*
         * Assert
         */
        assertThat(response)
                .isNotNull();

        /*
         * PENDING_RESULT được map thành
         * String "PENDING".
         */
        assertThat(response.getStatus())
                .isEqualTo("PENDING");

        /*
         * Có đúng một criterion.
         */
        assertThat(response.getCriteria())
                .hasSize(1);

        /*
         * Kiểm tra code — code snapshot bằng tên chỉ tiêu
         * trong danh mục dùng chung.
         */
        assertThat(
                response.getCriteria()
                        .get(0)
                        .getCode())
                .isEqualTo(
                        "Dư lượng thuốc trừ sâu");

        /*
         * Kiểm tra name.
         */
        assertThat(
                response.getCriteria()
                        .get(0)
                        .getName())
                .isEqualTo(
                        "Dư lượng thuốc trừ sâu");

        /*
         * Chỉ tiêu mới không còn gắn Standard —
         * snapshot tham chiếu danh mục qua criterion_id.
         */
        assertThat(
                response.getCriteria()
                        .get(0)
                        .getStandardId())
                .isNull();

        assertThat(
                response.getCriteria()
                        .get(0)
                        .getStandardName())
                .isNull();

        /*
         * Repository phải được gọi save().
         */
        verify(
                inspectionRequestRepository)
                .save(any(InspectionRequest.class));

        /*
         * TASK-27: tạo yêu cầu kiểm nghiệm phải ghi nhật ký hoạt động
         * đúng action, đúng đối tượng, đúng người thực hiện.
         */
        ArgumentCaptor<ActivityLogEvent> logCaptor =
                ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher)
                .publishEvent(logCaptor.capture());

        ActivityLogEvent logEvent = logCaptor.getValue();
        assertThat(logEvent.getAction())
                .isEqualTo("CREATE_INSPECTION_REQUEST");
        assertThat(logEvent.getEntityType())
                .isEqualTo("INSPECTION_REQUEST");
        assertThat(logEvent.getEntityId())
                .isEqualTo(response.getTestRequestId().toString());
        assertThat(logEvent.getOrganizationId())
                .isEqualTo(orgId);
        assertThat(logEvent.getTimestamp())
                .isNotNull();
    }

    /**
     * Regression test NCL-11-CN-002:
     *
     * HARVEST được ghi với shipment_id = NULL và productionLotId
     * nằm trong event_data JSON vẫn phải được nhận diện khi tạo
     * yêu cầu kiểm nghiệm.
     *
     * Service phải gọi repository với đúng cặp tham số:
     *
     *     (lotId UUID, lotId.toString(), HARVEST)
     *
     * để repository so khớp JSON event_data qua JSON_EXTRACT.
     */
    @Test
    void createInspectionRequest_shouldRecognizeHarvestStoredInEventData_whenNoShipmentLinked() {

        /*
         * Arrange
         */
        CreateInspectionRequest request =
                new CreateInspectionRequest();

        request.setTestingUnit(
                "Lab ABC");

        request.setSampleSentDate(
                LocalDate.now());

        request.setCriteriaIds(
                List.of(101L));

        /*
         * Lô đã thu hoạch (status HARVESTED) nhưng chưa
         * có shipment nào được gắn.
         */
        lot.setStatus(
                ProductionLotStatus.HARVESTED);

        /*
         * Production lot tồn tại và thuộc organization.
         */
        when(
                productionLotRepository
                        .findByIdAndOrganization_OrganizationId(
                                lotId,
                                orgId))
                .thenReturn(
                        Optional.of(lot));

        /*
         * HARVEST tồn tại ở dạng chưa gắn shipment:
         *
         * event_type = 'HARVEST'
         * shipment_id = NULL
         * event_data.productionLotId = lotId.toString()
         *
         * Repository nhận diện qua productionLotIdText.
         */
        when(
                chainEventRepository
                        .existsByProductionLotIdOrUnassignedEventDataAndEventType(
                                lotId,
                                lotId.toString(),
                                ChainEventType.HARVEST))
                .thenReturn(true);

        /*
         * Chỉ tiêu tồn tại trong danh mục dùng chung
         * và được gán cho loại nông sản của lô.
         */
        when(
                inspectionCriterionCatalogRepository
                        .findById(101L))
                .thenReturn(
                        Optional.of(
                                catalogCriterion));

        when(
                categoryCriterionRepository
                        .existsByCategory_IdAndCriterion_Id(
                                categoryId,
                                101L))
                .thenReturn(true);

        /*
         * Không có request PENDING_RESULT trước đó.
         */
        when(
                inspectionRequestRepository
                        .findByProductionLot_IdAndStatus(
                                lotId,
                                InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(
                        List.of());

        /*
         * Giả lập save().
         */
        when(
                inspectionRequestRepository
                        .save(any(InspectionRequest.class)))
                .thenAnswer(invocation -> {

                    InspectionRequest saved =
                            invocation.getArgument(
                                    0);

                    saved.setId(
                            UUID.randomUUID());

                    return saved;
                });

        /*
         * Act
         */
        InspectionRequestResponse response =
                inspectionRequestService
                        .createInspectionRequest(
                                lotId,
                                request,
                                currentUser);

        /*
         * Assert
         */
        assertThat(response)
                .isNotNull();

        assertThat(response.getStatus())
                .isEqualTo("PENDING");

        /*
         * Service phải hỏi repository với đúng contract
         * để so khớp event_data JSON của HARVEST.
         */
        verify(
                chainEventRepository)
                .existsByProductionLotIdOrUnassignedEventDataAndEventType(
                        lotId,
                        lotId.toString(),
                        ChainEventType.HARVEST);
    }

    /**
     * Test từ chối tạo request nếu đã tồn tại
     * request PENDING_RESULT với cùng bộ chỉ tiêu.
     *
     * confirmDuplicate mặc định = false.
     *
     * Bộ chỉ tiêu:
     *
     * RESIDUE_PESTICIDE
     *
     * phải trùng với request hiện tại.
     */
    @Test
    void createInspectionRequest_shouldRejectDuplicatePendingCriteria_withoutConfirmation() {

        /*
         * Arrange
         */
        CreateInspectionRequest request =
                new CreateInspectionRequest();

        request.setTestingUnit(
                "Lab ABC");

        request.setSampleSentDate(
                LocalDate.now());

        request.setCriteriaIds(
                List.of(101L));

        /*
         * ID của request cũ.
         *
         * Exception DuplicateInspectionRequestException
         * cần ID này.
         */
        UUID existingRequestId =
                UUID.randomUUID();

        /*
         * Criterion của request cũ — snapshot mới
         * tham chiếu danh mục qua criterionId,
         * không gắn Standard.
         *
         * Quan trọng:
         *
         * resolveCriterionKey() tạo khóa
         * "CAT:<criterionId>:<code>" nên criterionId
         * và criterionCode phải khớp request mới.
         */
        InspectionCriterion existingCriterion =
                InspectionCriterion.builder()
                        .criterionCode(
                                "Dư lượng thuốc trừ sâu")
                        .criterionName(
                                "Dư lượng thuốc trừ sâu")
                        .criterionId(101L)
                        .build();

        /*
         * Request PENDING_RESULT đã tồn tại.
         */
        InspectionRequest existing =
                InspectionRequest.builder()
                        .productionLot(lot)
                        .inspectionUnit(
                                "Lab ABC")
                        .sampleSentDate(
                                LocalDate.now())
                        .status(
                                InspectionRequestStatus
                                        .PENDING_RESULT)
                        .createdBy(user)
                        .criteria(
                                List.of(
                                        existingCriterion))
                        .build();

        /*
         * Phải set ID.
         */
        existing.setId(
                existingRequestId);

        /*
         * Production lot tồn tại.
         */
        when(
                productionLotRepository
                        .findByIdAndOrganization_OrganizationId(
                                lotId,
                                orgId))
                .thenReturn(
                        Optional.of(lot));

        /*
         * Có HARVEST event.
         */
        when(
                chainEventRepository
                        .existsByProductionLotIdOrUnassignedEventDataAndEventType(
                                lotId,
                                lotId.toString(),
                                ChainEventType.HARVEST))
                .thenReturn(true);

        /*
         * Chỉ tiêu tồn tại trong danh mục dùng chung
         * và được gán cho loại nông sản của lô.
         */
        when(
                inspectionCriterionCatalogRepository
                        .findById(101L))
                .thenReturn(
                        Optional.of(
                                catalogCriterion));

        when(
                categoryCriterionRepository
                        .existsByCategory_IdAndCriterion_Id(
                                categoryId,
                                101L))
                .thenReturn(true);

        /*
         * Repository trả về request PENDING_RESULT
         * đang có cùng bộ criterion.
         */
        when(
                inspectionRequestRepository
                        .findByProductionLot_IdAndStatus(
                                lotId,
                                InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(
                        List.of(existing));

        /*
         * Không mock save().
         *
         * Nếu service chạy tới save() thì test sẽ fail
         * do đó cũng giúp đảm bảo duplicate được phát hiện
         * trước khi save.
         */
        assertThatThrownBy(() ->
                inspectionRequestService
                        .createInspectionRequest(
                                lotId,
                                request,
                                currentUser))
                .isInstanceOf(
                        DuplicateInspectionRequestException.class)
                .hasMessageContaining(
                        "cùng bộ chỉ tiêu");

        /*
         * Không được phép save request mới.
         */
        verify(
                inspectionRequestRepository,
                never())
                .save(any(InspectionRequest.class));
    }

    /**
     * Regression test cho fix bảo mật org scope.
     *
     * Khi lotId = null và status = null,
     * service phải query theo organization hiện tại,
     * không được lấy toàn bộ yêu cầu kiểm nghiệm
     * của mọi tổ chức.
     */
    @Test
    void getInspectionRequests_lotIdNull_statusNull_shouldScopeByOrganization() {

        /*
         * Arrange
         */
        Pageable pageable =
                Pageable.unpaged();

        when(
                inspectionRequestRepository
                        .findByProductionLot_Organization_OrganizationId(
                                orgId,
                                pageable))
                .thenReturn(
                        Page.empty());

        /*
         * Act
         */
        inspectionRequestService
                .getInspectionRequests(
                        null,
                        null,
                        pageable,
                        currentUser);

        /*
         * Assert
         */
        verify(
                inspectionRequestRepository)
                .findByProductionLot_Organization_OrganizationId(
                        orgId,
                        pageable);

        verify(
                inspectionRequestRepository,
                never())
                .findAll(any(Pageable.class));
    }

    /**
     * Regression test cho fix bảo mật org scope.
     *
     * Khi lotId = null và có status,
     * service phải query theo organization hiện tại
     * kèm status filter,
     * không được lấy toàn bộ yêu cầu kiểm nghiệm
     * của mọi tổ chức.
     */
    @Test
    void getInspectionRequests_lotIdNull_statusNotNull_shouldScopeByOrganizationAndStatus() {

        /*
         * Arrange
         */
        Pageable pageable =
                Pageable.unpaged();

        InspectionRequestStatus status =
                InspectionRequestStatus.PENDING_RESULT;

        when(
                inspectionRequestRepository
                        .findByProductionLot_Organization_OrganizationIdAndStatus(
                                orgId,
                                status,
                                pageable))
                .thenReturn(
                        Page.empty());

        /*
         * Act
         */
        inspectionRequestService
                .getInspectionRequests(
                        null,
                        status,
                        pageable,
                        currentUser);

        /*
         * Assert
         */
        verify(
                inspectionRequestRepository)
                .findByProductionLot_Organization_OrganizationIdAndStatus(
                        orgId,
                        status,
                        pageable);

        verify(
                inspectionRequestRepository,
                never())
                .findAll(any(Pageable.class));
    }

    /**
     * Test lấy chi tiết yêu cầu kiểm nghiệm thành công.
     *
     * Response phải chứa danh sách chỉ tiêu kèm UUID snapshot
     * và kết quả đã ghi (nếu có).
     */
    @Test
    void getDetail_shouldReturnCriteriaAndExistingResult_whenRequestInOrganization() {

        /*
         * Arrange
         */
        UUID requestId =
                UUID.randomUUID();

        InspectionCriterion criterion =
                InspectionCriterion.builder()
                        .id(
                                UUID.randomUUID())
                        .criterionCode(
                                "RESIDUE_PESTICIDE")
                        .criterionName(
                                "Dư lượng thuốc trừ sâu")
                        .standard(
                                standard)
                        .build();

        InspectionRequest request =
                InspectionRequest.builder()
                        .productionLot(lot)
                        .inspectionUnit(
                                "Lab ABC")
                        .sampleSentDate(
                                LocalDate.now())
                        .status(
                                InspectionRequestStatus
                                        .PENDING_RESULT)
                        .criteria(
                                List.of(criterion))
                        .build();

        request.setId(requestId);

        InspectionCriterionResult result =
                InspectionCriterionResult.builder()
                        .inspectionCriterion(criterion)
                        .resultDate(
                                LocalDate.now().minusDays(1))
                        .expiryDate(
                                LocalDate.now().plusMonths(6))
                        .passed(true)
                        .createdBy(user)
                        .build();

        when(
                inspectionRequestRepository
                        .findDetailById(requestId))
                .thenReturn(
                        Optional.of(request));

        when(
                inspectionCriterionResultRepository
                        .findByInspectionCriterion_InspectionRequest_Id(
                                requestId))
                .thenReturn(
                        List.of(result));

        /*
         * Act
         */
        InspectionRequestDetailResponse response =
                inspectionRequestService
                        .getDetail(
                                requestId,
                                currentUser);

        /*
         * Assert
         */
        assertThat(response)
                .isNotNull();

        assertThat(response.getTestRequestId())
                .isEqualTo(requestId);

        assertThat(response.getStatus())
                .isEqualTo("PENDING");

        assertThat(response.getCriteria())
                .hasSize(1);

        assertThat(
                response.getCriteria()
                        .get(0)
                        .getCriterionId())
                .isEqualTo(
                        criterion.getId());

        assertThat(
                response.getCriteria()
                        .get(0)
                        .getStandardName())
                .isEqualTo(
                        "VietGAP");

        assertThat(
                response.getCriteria()
                        .get(0)
                        .getResult())
                .isNotNull();

        assertThat(
                response.getCriteria()
                        .get(0)
                        .getResult()
                        .getPassed())
                .isTrue();
    }

    /**
     * Test org boundary: yêu cầu kiểm nghiệm của tổ chức khác
     * phải bị từ chối với BusinessException.
     */
    @Test
    void getDetail_shouldReject_whenRequestBelongsToAnotherOrganization() {

        /*
         * Arrange
         */
        UUID requestId =
                UUID.randomUUID();

        UUID otherOrgId =
                UUID.randomUUID();

        ProductionLot otherLot =
                new ProductionLot();

        otherLot.setId(
                UUID.randomUUID());

        otherLot.setName(
                "Lô khác tổ chức");

        otherLot.setOrganization(
                new vn.nguongocso.organization.entity.Organization());

        otherLot.getOrganization()
                .setOrganizationId(otherOrgId);

        InspectionRequest request =
                InspectionRequest.builder()
                        .productionLot(otherLot)
                        .inspectionUnit(
                                "Lab ABC")
                        .sampleSentDate(
                                LocalDate.now())
                        .status(
                                InspectionRequestStatus
                                        .PENDING_RESULT)
                        .criteria(
                                List.of())
                        .build();

        request.setId(requestId);

        when(
                inspectionRequestRepository
                        .findDetailById(requestId))
                .thenReturn(
                        Optional.of(request));

        /*
         * Act & Assert
         */
        assertThatThrownBy(() ->
                inspectionRequestService
                        .getDetail(
                                requestId,
                                currentUser))
                .isInstanceOf(
                        BusinessException.class)
                .hasMessageContaining(
                        "không tồn tại");
    }
}