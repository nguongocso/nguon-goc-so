package vn.nguongocso.alert_reclaim_history.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.alert.specification.ActivityLogSpecification;

@DataJpaTest
@ActiveProfiles("test")
class ActivityLogExportSpecificationTest {
    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Test
    void specification_shouldCombineAllFiltersAndExcludeOtherOrganizations() {
        UUID organizationId = UUID.randomUUID();
        activityLogRepository.saveAll(List.of(
                createLog(organizationId, "manager-a", "Nguyễn Văn A", "UPDATE_LOT", "PRODUCTION_LOT",
                        LocalDateTime.of(2026, 9, 10, 8, 0)),
                createLog(organizationId, "manager-a", "Nguyễn Văn A", "CREATE_LOT", "PRODUCTION_LOT",
                        LocalDateTime.of(2026, 9, 10, 9, 0)),
                createLog(UUID.randomUUID(), "manager-a", "Nguyễn Văn A", "UPDATE_LOT", "PRODUCTION_LOT",
                        LocalDateTime.of(2026, 9, 10, 10, 0))));

        Specification<ActivityLog> specification = ActivityLogSpecification.hasOrganizationId(organizationId)
                .and(ActivityLogSpecification.createdBetween(
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 10)))
                .and(ActivityLogSpecification.hasAction("UPDATE_LOT"))
                .and(ActivityLogSpecification.hasActorName("nguyễn"))
                .and(ActivityLogSpecification.hasEntityType("PRODUCTION_LOT"));

        List<ActivityLog> result = activityLogRepository.findAll(specification);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getOrganizationId()).isEqualTo(organizationId);
        assertThat(result.getFirst().getAction()).isEqualTo("UPDATE_LOT");
    }

    @Test
    void createdBetween_shouldSupportOneSidedRangesWithoutSyntheticDatabaseDates() {
        UUID organizationId = UUID.randomUUID();
        activityLogRepository.saveAll(List.of(
                createLog(organizationId, "manager", "Quản lý", "ACTION", "LOT",
                        LocalDateTime.of(2026, 9, 9, 23, 59)),
                createLog(organizationId, "manager", "Quản lý", "ACTION", "LOT",
                        LocalDateTime.of(2026, 9, 10, 0, 0))));

        long fromCount = activityLogRepository.count(
                ActivityLogSpecification.hasOrganizationId(organizationId)
                        .and(ActivityLogSpecification.createdBetween(LocalDate.of(2026, 9, 10), null)));
        long toCount = activityLogRepository.count(
                ActivityLogSpecification.hasOrganizationId(organizationId)
                        .and(ActivityLogSpecification.createdBetween(null, LocalDate.of(2026, 9, 9))));

        assertThat(fromCount).isEqualTo(1);
        assertThat(toCount).isEqualTo(1);
    }

    @Test
    void hasOrganizationId_shouldReturnDisjunction_whenOrganizationIdIsNull() {
        activityLogRepository.save(createLog(UUID.randomUUID(), "manager", "Quản lý", "ACTION", "LOT", LocalDateTime.now()));
        List<ActivityLog> logs = activityLogRepository.findAll(ActivityLogSpecification.hasOrganizationId(null));
        assertThat(logs).isEmpty();
    }

    @Test
    void hasAction_shouldMatchActionFamily_whenGenericActionIsSelected() {
        UUID organizationId = UUID.randomUUID();
        activityLogRepository.saveAll(List.of(
                createLog(organizationId, "manager", "Quản lý", "UPDATE_FARM_AREA", "FARM_AREA",
                        LocalDateTime.now()),
                createLog(organizationId, "manager", "Quản lý", "UPDATE_FARM_AREA_BOUNDARY", "FARM_AREA",
                        LocalDateTime.now()),
                createLog(organizationId, "manager", "Quản lý", "CREATE_FARM_AREA", "FARM_AREA",
                        LocalDateTime.now())));

        List<ActivityLog> logs = activityLogRepository.findAll(
                ActivityLogSpecification.hasOrganizationId(organizationId)
                        .and(ActivityLogSpecification.hasAction("UPDATE"))
                        .and(ActivityLogSpecification.hasEntityType("FARM_AREA")));

        assertThat(logs).extracting(ActivityLog::getAction)
                .containsExactlyInAnyOrder("UPDATE_FARM_AREA", "UPDATE_FARM_AREA_BOUNDARY");
    }

    @Test
    void hasAction_shouldKeepExactMatching_whenSpecificActionIsSelected() {
        UUID organizationId = UUID.randomUUID();
        activityLogRepository.saveAll(List.of(
                createLog(organizationId, "manager", "Quản lý", "UPDATE_FARM_AREA", "FARM_AREA",
                        LocalDateTime.now()),
                createLog(organizationId, "manager", "Quản lý", "UPDATE_FARM_AREA_BOUNDARY", "FARM_AREA",
                        LocalDateTime.now())));

        List<ActivityLog> logs = activityLogRepository.findAll(
                ActivityLogSpecification.hasOrganizationId(organizationId)
                        .and(ActivityLogSpecification.hasAction("UPDATE_FARM_AREA")));

        assertThat(logs).extracting(ActivityLog::getAction)
                .containsExactly("UPDATE_FARM_AREA");
    }

    private ActivityLog createLog(
            UUID organizationId,
            String username,
            String fullName,
            String action,
            String entityType,
            LocalDateTime createdAt) {
        return ActivityLog.builder()
                .organizationId(organizationId)
                .userId(UUID.randomUUID())
                .username(username)
                .fullName(fullName)
                .action(action)
                .description("Dữ liệu kiểm thử")
                .entityType(entityType)
                .createdAt(createdAt)
                .build();
    }
}
