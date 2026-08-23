package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.FarmLogServiceImpl;
import vn.nguongocso.organization.entity.Organization;

/**
 * TASK-21: kiểm tra thời gian tạo nhật ký canh tác (createdAt) được ghi theo
 * múi giờ nghiệp vụ (Asia/Ho_Chi_Minh), không phụ thuộc timezone của JVM.
 */
@ExtendWith(MockitoExtension.class)
class FarmLogServiceImplTest {

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private FarmLogAttachmentRepository attachmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    private FarmLogServiceImpl farmLogService;

    private UUID organizationId;
    private ProductionLot productionLot;
    private CustomUserDetails currentUser;

    @BeforeEach
    void setUp() {
        farmLogService = new FarmLogServiceImpl(
                farmLogRepository,
                productionLotRepository,
                attachmentRepository,
                eventPublisher,
                clock);

        organizationId = UUID.randomUUID();

        Organization organization = new Organization();
        organization.setOrganizationId(organizationId);

        FarmArea farmArea = new FarmArea();
        farmArea.setOrganization(organization);

        productionLot = new ProductionLot();
        productionLot.setId(UUID.randomUUID());
        productionLot.setName("Lô chè 01");
        productionLot.setStatus(ProductionLotStatus.APPROVED);
        productionLot.setFarmArea(farmArea);

        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setFullName("Người ghi nhật ký");

        currentUser = mock(CustomUserDetails.class);
        lenient().when(currentUser.getRoleCode()).thenReturn("VT-03");
        lenient().when(currentUser.getOrganizationId()).thenReturn(organizationId);
        lenient().when(currentUser.getUser()).thenReturn(user);
        lenient().when(currentUser.getUserId()).thenReturn(user.getUserId());
        lenient().when(currentUser.getUsername()).thenReturn("recorder");
        lenient().when(currentUser.getFullName()).thenReturn("Người ghi nhật ký");

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(currentUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Clock nghiệp vụ cố định: 2026-08-23T03:00:00Z == 10:00 giờ Việt Nam.
        lenient().when(clock.instant())
                .thenReturn(Instant.parse("2026-08-23T03:00:00Z"));
        lenient().when(clock.getZone())
                .thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));

        lenient().when(productionLotRepository.findById(productionLot.getId()))
                .thenReturn(Optional.of(productionLot));
        // Mô phỏng save: gán ID như @PrePersist của JPA.
        lenient().when(farmLogRepository.save(any(FarmLog.class)))
                .thenAnswer(invocation -> {
                    FarmLog log = invocation.getArgument(0);
                    if (log.getId() == null) {
                        log.setId(UUID.randomUUID());
                    }
                    return log;
                });
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldCaptureCreatedAtInBusinessTimezone() {
        CreateFarmLogRequest request = new CreateFarmLogRequest();
        request.setProductionLotId(productionLot.getId());
        request.setActivityType(FarmActivityType.PLANTING);
        request.setMaterial("Phân hữu cơ");
        request.setQuantity(10.0);
        request.setUnit("kg");
        request.setExecutedDate(LocalDate.of(2026, 8, 23));
        request.setNotes("Ghi chú thử nghiệm");

        FarmLogResponse response = farmLogService.create(request);

        ArgumentCaptor<FarmLog> captor = ArgumentCaptor.forClass(FarmLog.class);
        Mockito.verify(farmLogRepository).save(captor.capture());
        FarmLog saved = captor.getValue();

        // createdAt phải là giờ nghiệp vụ Asia/Ho_Chi_Minh (10:00),
        // không phải giờ UTC của JVM (03:00).
        assertThat(saved.getCreatedAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 23, 10, 0, 0));
        assertThat(response.getCreatedAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 23, 10, 0, 0));

        // executedDate (ngày xảy ra sự kiện) giữ nguyên giá trị người dùng nhập,
        // không bị gộp với createdAt.
        assertThat(saved.getExecutedDate())
                .isEqualTo(LocalDate.of(2026, 8, 23));
    }
}