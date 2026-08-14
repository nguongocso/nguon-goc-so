package vn.nguongocso.alert.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.nguongocso.alert.entity.ActivityLog;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Specification cho thực thể ActivityLog, hỗ trợ tìm kiếm động dựa trên các
 * tiêu chí khác nhau.
 */
public class ActivityLogSpecification {
    /**
     * Tạo Specification để lọc ActivityLog theo organizationId.
     */
    public static Specification<ActivityLog> hasOrganizationId(UUID organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    /**
     * Tạo Specification để lọc ActivityLog theo action.
     */
    public static Specification<ActivityLog> hasAction(String action) {
        return (root, query, cb) -> (action == null || action.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("action"), action);
    }

    /**
     * Tạo Specification để lọc ActivityLog theo tên người thực hiện (username hoặc
     * fullName).
     */
    public static Specification<ActivityLog> hasActorName(String actorName) {
        return (root, query, cb) -> {
            if (actorName == null || actorName.isBlank()) {
                return cb.conjunction();
            }
            String searchPattern = "%" + actorName.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), searchPattern),
                    cb.like(cb.lower(root.get("fullName")), searchPattern));
        };
    }

    /**
     * Tạo Specification để lọc ActivityLog theo khoảng thời gian tạo.
     */
    public static Specification<ActivityLog> createdBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            }
            Instant startInstant = startDate != null
                    ? startDate.atStartOfDay().toInstant(ZoneOffset.UTC)
                    : Instant.EPOCH;
            Instant endInstant = endDate != null
                    ? endDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC)
                    : Instant.now();
            return cb.between(root.get("createdAt"), startInstant, endInstant);
        };
    }
}
