package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateInspectionRequest;
import vn.nguongocso.certification.dto.response.InspectionRequestResponse;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionDefinition;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.InspectionCriterionDefinitionRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.certification.service.impl.DuplicateInspectionRequestException;
import vn.nguongocso.certification.service.impl.InspectionRequestServiceImpl;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;

@ExtendWith(MockitoExtension.class)
class InspectionRequestServiceImplTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private InspectionRequestRepository inspectionRequestRepository;

    @Mock
    private InspectionCriterionDefinitionRepository inspectionCriterionDefinitionRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private ProductionLotCertificationRepository productionLotCertificationRepository;

    @InjectMocks
    private InspectionRequestServiceImpl inspectionRequestService;

    private UUID lotId;
    private UUID orgId;

    private CustomUserDetails currentUser;

    private User user;

    private ProductionLot lot;

    private Standard standard;

    private InspectionCriterionDefinition criterionDefinition;

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

        when(currentUser.getRoleCode())
                .thenReturn("VT-02");

        when(currentUser.getOrganizationId())
                .thenReturn(orgId);

        when(currentUser.getUser())
                .thenReturn(user);

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
         * Standard test.
         */
        standard = new Standard();

        standard.setId(
                UUID.randomUUID());

        standard.setName("VietGAP");

        /*
         * Inspection criterion definition test.
         */
        criterionDefinition =
                new InspectionCriterionDefinition();

        criterionDefinition.setId(101);

        criterionDefinition.setCode(
                "RESIDUE_PESTICIDE");

        criterionDefinition.setName(
                "Dư lượng thuốc trừ sâu");

        criterionDefinition.setStandard(
                standard);
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
                List.of(101));

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
                        .existsByShipment_ProductionLot_IdAndEventType(
                                lotId,
                                ChainEventType.HARVEST))
                .thenReturn(true);

        /*
         * Criterion definition tồn tại.
         */
        when(
                inspectionCriterionDefinitionRepository
                        .findById(101))
                .thenReturn(
                        Optional.of(
                                criterionDefinition));

        /*
         * Standard VietGAP đã được gắn với lô.
         */
        when(
                productionLotCertificationRepository
                        .existsByProductionLotIdAndStandardId(
                                lotId,
                                standard.getId()))
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
         * Kiểm tra code.
         */
        assertThat(
                response.getCriteria()
                        .get(0)
                        .getCode())
                .isEqualTo(
                        "RESIDUE_PESTICIDE");

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
         * Kiểm tra standard.
         */
        assertThat(
                response.getCriteria()
                        .get(0)
                        .getStandardId())
                .isEqualTo(
                        standard.getId());

        assertThat(
                response.getCriteria()
                        .get(0)
                        .getStandardName())
                .isEqualTo(
                        "VietGAP");

        /*
         * Repository phải được gọi save().
         */
        verify(
                inspectionRequestRepository)
                .save(any(InspectionRequest.class));
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
                List.of(101));

        /*
         * ID của request cũ.
         *
         * Exception DuplicateInspectionRequestException
         * cần ID này.
         */
        UUID existingRequestId =
                UUID.randomUUID();

        /*
         * Criterion của request cũ.
         *
         * Quan trọng:
         *
         * service hiện tại dùng:
         *
         * resolveCriterionKey()
         *
         * để lấy criterionCode.
         */
        InspectionCriterion existingCriterion =
                InspectionCriterion.builder()
                        .criterionCode(
                                "RESIDUE_PESTICIDE")
                        .criterionName(
                                "Dư lượng thuốc trừ sâu")
                        .standard(
                                standard)
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
                        .existsByShipment_ProductionLot_IdAndEventType(
                                lotId,
                                ChainEventType.HARVEST))
                .thenReturn(true);

        /*
         * Criterion definition tồn tại.
         */
        when(
                inspectionCriterionDefinitionRepository
                        .findById(101))
                .thenReturn(
                        Optional.of(
                                criterionDefinition));

        /*
         * Standard đã được gắn với lô.
         */
        when(
                productionLotCertificationRepository
                        .existsByProductionLotIdAndStandardId(
                                lotId,
                                standard.getId()))
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
}