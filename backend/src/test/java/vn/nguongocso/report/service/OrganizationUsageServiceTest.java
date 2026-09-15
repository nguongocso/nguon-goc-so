package vn.nguongocso.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse;
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse.MetricComparison;
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse.OrganizationUsageItem;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.report.service.impl.OrganizationUsageServiceImpl;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử service bảng điều khiển mức độ sử dụng theo tổ chức (NCL-07-CN-008).
 */
@ExtendWith(MockitoExtension.class)
class OrganizationUsageServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant NOW = Instant.parse("2026-09-14T03:00:00Z");

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private ProductionLotRepository productionLotRepository;
    @Mock
    private FarmLogRepository farmLogRepository;
    @Mock
    private ChainEventRepository chainEventRepository;
    @Mock
    private TraceCodeRepository traceCodeRepository;
    @Mock
    private TraceCodeScanLogRepository traceCodeScanLogRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;

    private OrganizationUsageService service;

    private Organization orgA;
    private Organization orgB;
    private Organization orgC;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZONE);
        service = new OrganizationUsageServiceImpl(
                organizationRepository, productionLotRepository, farmLogRepository,
                chainEventRepository, traceCodeRepository, traceCodeScanLogRepository,
                activityLogRepository, fixedClock);

        orgA = buildOrg("HTX A", "HTXA", LocalDateTime.of(2026, 1, 10, 8, 0));
        orgB = buildOrg("HTX B", "HTXB", LocalDateTime.of(2026, 2, 15, 8, 0));
        orgC = buildOrg("HTX C Moi", "HTXC", LocalDateTime.of(2026, 9, 14, 8, 0));
    }

    private Organization buildOrg(String name, String code, LocalDateTime createdAt) {
        return Organization.builder()
                .organizationId(UUID.randomUUID())
                .name(name)
                .code(code)
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private void mockEmptyMetrics() {
        when(productionLotRepository.countLotsGroupedByOrg(any(), any())).thenReturn(List.of());
        when(farmLogRepository.countFarmLogsGroupedByOrg(any(), any())).thenReturn(List.of());
        when(chainEventRepository.countEventsGroupedByShipmentOrg(any(), any())).thenReturn(List.of());
        when(chainEventRepository.countUnassignedEventsGroupedByRecordedOrg(any(), any())).thenReturn(List.of());
        when(traceCodeRepository.countActivatedGroupedByOrg(any(), any())).thenReturn(List.of());
        when(traceCodeScanLogRepository.countScansGroupedByOrg(any(), any())).thenReturn(List.of());
        when(activityLogRepository.countDistinctUsersGroupedByOrg(any(), any())).thenReturn(List.of());
        when(productionLotRepository.maxLotCreatedAtGroupedByOrg()).thenReturn(List.of());
        when(farmLogRepository.maxFarmLogCreatedAtGroupedByOrg()).thenReturn(List.of());
        when(chainEventRepository.maxEventCreatedAtGroupedByShipmentOrg()).thenReturn(List.of());
        when(chainEventRepository.maxUnassignedEventCreatedAtGroupedByRecordedOrg()).thenReturn(List.of());
        when(traceCodeRepository.maxActivatedAtGroupedByOrg()).thenReturn(List.of());
        when(traceCodeScanLogRepository.maxScannedAtGroupedByOrg()).thenReturn(List.of());
        when(activityLogRepository.maxActivityAtGroupedByOrg()).thenReturn(List.of());
    }
    private OrganizationUsageItem findItem(OrganizationUsageDashboardResponse response, UUID orgId) {
        return response.getItems().stream()
                .filter(item -> item.getOrganizationId().equals(orgId))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Dựng danh sách dòng [organizationId, giá trị] cho stub repository,
     * tránh lỗi suy luận generics của List.of với mảng Object[].
     */
    private static List<Object[]> rows(Object[]... rows) {
        return Arrays.asList(rows);
    }

    @Test
    @DisplayName("AC-01: 3 tổ chức có mức hoạt động khác nhau, đủ 6 chỉ số current/previous/change")
    void getDashboard_ThreeOrgsWithDifferentActivity() {
        when(organizationRepository.findAll()).thenReturn(List.of(orgA, orgB, orgC));
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        // Kỳ hiện tại: A hoạt động mạnh, B yếu, C không có gì
        when(productionLotRepository.countLotsGroupedByOrg(
                LocalDate.of(2026, 9, 1).atStartOfDay(), LocalDate.of(2026, 9, 30).atTime(23, 59, 59)))
                .thenReturn(rows(                        new Object[]{orgA.getOrganizationId(), 10L},
                        new Object[]{orgB.getOrganizationId(), 1L}));
        when(productionLotRepository.countLotsGroupedByOrg(
                LocalDate.of(2026, 8, 2).atStartOfDay(), LocalDate.of(2026, 8, 31).atTime(23, 59, 59)))
                .thenReturn(rows(new Object[]{orgA.getOrganizationId(), 8L}));
        when(farmLogRepository.countFarmLogsGroupedByOrg(any(), any()))
                .thenReturn(rows(new Object[]{orgA.getOrganizationId(), 35L}));
        when(chainEventRepository.countEventsGroupedByShipmentOrg(any(), any()))
                .thenReturn(rows(new Object[]{orgA.getOrganizationId(), 20L}));
        when(chainEventRepository.countUnassignedEventsGroupedByRecordedOrg(any(), any()))
                .thenReturn(List.of());
        when(traceCodeRepository.countActivatedGroupedByOrg(any(), any()))
                .thenReturn(rows(new Object[]{orgA.getOrganizationId(), 500L}));
        when(traceCodeScanLogRepository.countScansGroupedByOrg(any(), any()))
                .thenReturn(rows(new Object[]{orgA.getOrganizationId(), 120L}));
        when(activityLogRepository.countDistinctUsersGroupedByOrg(any(), any()))
                .thenReturn(rows(new Object[]{orgA.getOrganizationId(), 5L}));
        when(productionLotRepository.maxLotCreatedAtGroupedByOrg()).thenReturn(List.of());
        when(farmLogRepository.maxFarmLogCreatedAtGroupedByOrg()).thenReturn(List.of());
        when(chainEventRepository.maxEventCreatedAtGroupedByShipmentOrg()).thenReturn(List.of());
        when(chainEventRepository.maxUnassignedEventCreatedAtGroupedByRecordedOrg()).thenReturn(List.of());
        when(traceCodeRepository.maxActivatedAtGroupedByOrg()).thenReturn(List.of());
        when(traceCodeScanLogRepository.maxScannedAtGroupedByOrg()).thenReturn(List.of());
        when(activityLogRepository.maxActivityAtGroupedByOrg()).thenReturn(rows(                new Object[]{orgA.getOrganizationId(), LocalDateTime.of(2026, 9, 13, 10, 0)},
                new Object[]{orgB.getOrganizationId(), LocalDateTime.of(2026, 9, 10, 10, 0)}));

        OrganizationUsageDashboardResponse response = service.getDashboard(start, end, null);

        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getStartDate()).isEqualTo(start);
        assertThat(response.getEndDate()).isEqualTo(end);
        // Kỳ trước dài tương đương kỳ hiện tại (30 ngày: 02/08 -> 31/08)
        assertThat(response.getPreviousStartDate()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(response.getPreviousEndDate()).isEqualTo(LocalDate.of(2026, 8, 31));

        OrganizationUsageItem itemA = findItem(response, orgA.getOrganizationId());
        assertThat(itemA.getProductionLots().getCurrent()).isEqualTo(10);
        assertThat(itemA.getProductionLots().getPrevious()).isEqualTo(8);
        assertThat(itemA.getProductionLots().getChange()).isEqualTo(2);
        assertThat(itemA.getProductionLots().getChangePercent()).isEqualTo(25.0);
        assertThat(itemA.getFarmLogs().getCurrent()).isEqualTo(35);
        assertThat(itemA.getChainEvents().getCurrent()).isEqualTo(20);
        assertThat(itemA.getActivatedLabels().getCurrent()).isEqualTo(500);
        assertThat(itemA.getPublicLookups().getCurrent()).isEqualTo(120);
        assertThat(itemA.getActiveUsers().getCurrent()).isEqualTo(5);
        assertThat(itemA.isHasData()).isTrue();
        assertThat(itemA.isNeedsSupport()).isFalse();

        OrganizationUsageItem itemB = findItem(response, orgB.getOrganizationId());
        assertThat(itemB.getProductionLots().getCurrent()).isEqualTo(1);
        assertThat(itemB.getProductionLots().getPrevious()).isEqualTo(0);
        assertThat(itemB.isHasData()).isTrue();

        // AC-04: tổ chức mới không có hoạt động trong kỳ
        OrganizationUsageItem itemC = findItem(response, orgC.getOrganizationId());
        assertThat(itemC.isHasData()).isFalse();
        assertThat(itemC.getProductionLots().getCurrent()).isZero();
        assertThat(itemC.getProductionLots().getPrevious()).isZero();
    }

    @Test
    @DisplayName("previous = 0 thì changePercent = null, không Infinity/NaN")
    void getDashboard_PreviousZero_ChangePercentNull() {
        when(organizationRepository.findAll()).thenReturn(List.of(orgA));
        mockEmptyMetrics();
        when(productionLotRepository.countLotsGroupedByOrg(any(), any()))
                .thenReturn(rows(new Object[]{orgA.getOrganizationId(), 7L}))
                .thenReturn(List.of());

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);

        MetricComparison lots = findItem(response, orgA.getOrganizationId()).getProductionLots();
        assertThat(lots.getCurrent()).isEqualTo(7);
        assertThat(lots.getPrevious()).isZero();
        assertThat(lots.getChange()).isEqualTo(7);
        assertThat(lots.getChangePercent()).isNull();
    }

    @Test
    @DisplayName("current = 0 và previous = 0 thì change = 0, changePercent = null")
    void getDashboard_BothZero_ChangeZeroAndPercentNull() {
        when(organizationRepository.findAll()).thenReturn(List.of(orgA));
        mockEmptyMetrics();

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);

        OrganizationUsageItem item = findItem(response, orgA.getOrganizationId());
        assertThat(item.getProductionLots().getChange()).isZero();
        assertThat(item.getProductionLots().getChangePercent()).isNull();
        assertThat(item.isHasData()).isFalse();
    }

    @Test
    @DisplayName("AC-02: không có hoạt động trong 30 ngày thì needsSupport = true")
    void getDashboard_Inactive30Days_NeedsSupport() {
        when(organizationRepository.findAll()).thenReturn(List.of(orgA, orgB));
        mockEmptyMetrics();
        when(activityLogRepository.maxActivityAtGroupedByOrg()).thenReturn(rows(                new Object[]{orgA.getOrganizationId(), LocalDateTime.of(2026, 9, 13, 10, 0)},
                new Object[]{orgB.getOrganizationId(), LocalDateTime.of(2026, 7, 1, 10, 0)}));

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);

        assertThat(findItem(response, orgA.getOrganizationId()).isNeedsSupport()).isFalse();
        OrganizationUsageItem itemB = findItem(response, orgB.getOrganizationId());
        assertThat(itemB.isNeedsSupport()).isTrue();
        assertThat(itemB.getLastActivityAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
    }

    @ParameterizedTest(name = "AC-02 biên: hoạt động cách đây {0} ngày")
    @ValueSource(ints = {29, 30, 31})
    void getDashboard_InactiveBoundary(int daysAgo) {
        when(organizationRepository.findAll()).thenReturn(List.of(orgA));
        mockEmptyMetrics();
        LocalDateTime activity = LocalDateTime.of(2026, 9, 14, 10, 0).minusDays(daysAgo);
        when(activityLogRepository.maxActivityAtGroupedByOrg())
                .thenReturn(rows(new Object[]{orgA.getOrganizationId(), activity}));

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);

        // Ngưỡng: hoạt động trước thời điểm now - 30 ngày thì cần hỗ trợ
        assertThat(findItem(response, orgA.getOrganizationId()).isNeedsSupport()).isEqualTo(daysAgo >= 30);
    }

    @Test
    @DisplayName("Tổ chức chưa từng có hoạt động thì needsSupport = true, lastActivityAt = null")
    void getDashboard_NeverActive_NeedsSupportTrue() {
        when(organizationRepository.findAll()).thenReturn(List.of(orgC));
        mockEmptyMetrics();

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);

        OrganizationUsageItem item = findItem(response, orgC.getOrganizationId());
        assertThat(item.getLastActivityAt()).isNull();
        assertThat(item.isNeedsSupport()).isTrue();
        assertThat(item.isHasData()).isFalse();
    }

    @Test
    @DisplayName("AC-04: tổ chức tạo sau kỳ được chọn thì hasData = false (chưa tồn tại trong kỳ)")
    void getDashboard_OrgCreatedAfterPeriod_HasDataFalse() {
        Organization futureOrg = buildOrg("HTX Tuong Lai", "HTXF", LocalDateTime.of(2026, 10, 5, 8, 0));
        when(organizationRepository.findAll()).thenReturn(List.of(futureOrg));
        mockEmptyMetrics();

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);

        assertThat(findItem(response, futureOrg.getOrganizationId()).isHasData()).isFalse();
    }

    @Test
    @DisplayName("Kỳ tùy chỉnh 10 ngày thì kỳ trước dài đúng 10 ngày liền kề")
    void getDashboard_CustomRange_PreviousPeriodSameLength() {
        when(organizationRepository.findAll()).thenReturn(List.of(orgA));
        mockEmptyMetrics();

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 19), null);

        assertThat(response.getPreviousStartDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(response.getPreviousEndDate()).isEqualTo(LocalDate.of(2026, 8, 9));
    }

    @Test
    @DisplayName("Lọc theo organizationId chỉ trả một tổ chức")
    void getDashboard_FilterByOrganizationId() {
        when(organizationRepository.findById(orgA.getOrganizationId())).thenReturn(Optional.of(orgA));
        mockEmptyMetrics();

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), orgA.getOrganizationId());

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalOrganizations()).isEqualTo(1);
    }

    @Test
    @DisplayName("organizationId không tồn tại thì trả danh sách rỗng")
    void getDashboard_UnknownOrganizationId_Empty() {
        when(organizationRepository.findById(any())).thenReturn(Optional.empty());
        mockEmptyMetrics();

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), UUID.randomUUID());

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalOrganizations()).isZero();
    }

    @Test
    @DisplayName("startDate sau endDate thì báo lỗi khoảng thời gian")
    void getDashboard_InvalidRange_ThrowsBusinessException() {
        assertThatThrownBy(() -> service.getDashboard(
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Khoảng thời gian không hợp lệ");
    }

    @Test
    @DisplayName("Không truyền kỳ thì mặc định 30 ngày gần nhất")
    void getDashboard_DefaultPeriod_Last30Days() {
        when(organizationRepository.findAll()).thenReturn(List.of(orgA));
        mockEmptyMetrics();

        OrganizationUsageDashboardResponse response = service.getDashboard(null, null, null);

        // today = 2026-09-14 theo Clock cố định
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 16));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(response.getPreviousStartDate()).isEqualTo(LocalDate.of(2026, 7, 17));
        assertThat(response.getPreviousEndDate()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    @DisplayName("lastActivityAt lấy max từ mọi nguồn hoạt động thực tế")
    void getDashboard_LastActivity_MaxOfAllSources() {
        when(organizationRepository.findAll()).thenReturn(List.of(orgA));
        mockEmptyMetrics();
        when(productionLotRepository.maxLotCreatedAtGroupedByOrg()).thenReturn(rows(                new Object[]{orgA.getOrganizationId(), LocalDateTime.of(2026, 8, 1, 8, 0)}));
        when(traceCodeScanLogRepository.maxScannedAtGroupedByOrg()).thenReturn(rows(                new Object[]{orgA.getOrganizationId(), LocalDateTime.of(2026, 9, 12, 20, 30)}));
        when(activityLogRepository.maxActivityAtGroupedByOrg()).thenReturn(rows(                new Object[]{orgA.getOrganizationId(), LocalDateTime.of(2026, 9, 5, 8, 0)}));

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);

        OrganizationUsageItem item = findItem(response, orgA.getOrganizationId());
        assertThat(item.getLastActivityAt()).isEqualTo(LocalDateTime.of(2026, 9, 12, 20, 30));
        assertThat(item.isNeedsSupport()).isFalse();
    }

    @Test
    @DisplayName("Sự kiện chưa gắn lô hàng được cộng vào chỉ số sự kiện chuỗi")
    void getDashboard_UnassignedEvents_MergedIntoChainEvents() {
        when(organizationRepository.findAll()).thenReturn(List.of(orgA));
        mockEmptyMetrics();
        when(chainEventRepository.countEventsGroupedByShipmentOrg(any(), any()))
                .thenReturn(rows(new Object[]{orgA.getOrganizationId(), 4L}));
        when(chainEventRepository.countUnassignedEventsGroupedByRecordedOrg(any(), any()))
                .thenReturn(rows(new Object[]{orgA.getOrganizationId(), 3L}))
                .thenReturn(List.of());

        OrganizationUsageDashboardResponse response =
                service.getDashboard(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);

        assertThat(findItem(response, orgA.getOrganizationId()).getChainEvents().getCurrent()).isEqualTo(7);
    }
}
