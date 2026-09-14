package vn.nguongocso.alert.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.nguongocso.alert.entity.ActivityLog;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Specification cho thực thể ActivityLog, hỗ trợ tìm kiếm động dựa trên các
 * tiêu chí khác nhau.
 */
public class ActivityLogSpecification {
    /**
     * Tạo Specification để lọc ActivityLog theo organizationId.
     * Nếu organizationId là null, trả về disjunction (luôn sai) để đảm bảo cách ly dữ liệu.
     */
    public static Specification<ActivityLog> hasOrganizationId(UUID organizationId) {
        return (root, query, cb) -> organizationId == null
                ? cb.disjunction()
                : cb.equal(root.get("organizationId"), organizationId);
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
     * Tạo Specification để lọc ActivityLog theo loại đối tượng dữ liệu.
     */
    public static Specification<ActivityLog> hasEntityType(String entityType) {
        return (root, query, cb) -> (entityType == null || entityType.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("entityType"), entityType);
    }

    /**
     * Tạo Specification để lọc ActivityLog theo khoảng thời gian tạo.
     *
     * <p>
     * Cột createdAt là LocalDateTime (DATETIME, lưu giờ nghiệp vụ
     * Asia/Ho_Chi_Minh), nên khoảng lọc cũng dùng LocalDateTime thay vì
     * Instant quy đổi theo UTC — tránh lệch múi giờ khi so sánh.
     */
    public static Specification<ActivityLog> createdBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            }
            if (startDate == null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59, 999_999_999));
            }
            if (endDate == null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay());
            }
            return cb.between(
                    root.get("createdAt"),
                    startDate.atStartOfDay(),
                    endDate.atTime(23, 59, 59, 999_999_999));
        };
    }
}
