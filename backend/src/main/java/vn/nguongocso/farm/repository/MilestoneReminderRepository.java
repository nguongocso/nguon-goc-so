package vn.nguongocso.farm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.nguongocso.farm.entity.MilestoneReminder;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository thao tác dữ liệu nhắc việc theo mốc canh tác bắt buộc (NCL-03-CN-007).
 */
@Repository
public interface MilestoneReminderRepository extends JpaRepository<MilestoneReminder, UUID> {

    /**
     * Kiểm tra xem nhắc việc cho mốc và lô đó đã được tạo trong ngày hay chưa (TC-04).
     */
    boolean existsByProductionLot_IdAndMilestone_IdAndReminderDate(
            UUID lotId,
            Long milestoneId,
            LocalDate reminderDate);

    /**
     * Kiểm tra xem nhắc việc cho mốc, lô và người nhận cụ thể đã được tạo trong ngày hay chưa (TC-04).
     */
    boolean existsByProductionLot_IdAndMilestone_IdAndUser_UserIdAndReminderDate(
            UUID lotId,
            Long milestoneId,
            UUID userId,
            LocalDate reminderDate);

    /**
     * Tìm nhắc việc mở cho người dùng hoặc các lô thuộc tổ chức của người dùng.
     */
    @Query("SELECT DISTINCT r FROM MilestoneReminder r " +
           "WHERE r.status = :status " +
           "AND (r.user.userId = :userId OR (r.productionLot.organization.organizationId = :orgId AND :orgId IS NOT NULL)) " +
           "ORDER BY r.overdueDays DESC")
    List<MilestoneReminder> findActiveRemindersForUserOrOrganization(
            @Param("userId") UUID userId,
            @Param("orgId") UUID orgId,
            @Param("status") MilestoneReminderStatus status);

    /**
     * Tìm các nhắc việc theo trạng thái cho người dùng hoặc tổ chức có phân trang.
     */
    @Query("SELECT DISTINCT r FROM MilestoneReminder r " +
           "WHERE r.status = :status " +
           "AND (r.user.userId = :userId OR (r.productionLot.organization.organizationId = :orgId AND :orgId IS NOT NULL))")
    Page<MilestoneReminder> findRemindersForUserOrOrganizationAndStatus(
            @Param("userId") UUID userId,
            @Param("orgId") UUID orgId,
            @Param("status") MilestoneReminderStatus status,
            Pageable pageable);

    /**
     * Tìm tất cả các nhắc việc cho người dùng hoặc tổ chức có phân trang.
     */
    @Query("SELECT DISTINCT r FROM MilestoneReminder r " +
           "WHERE (r.user.userId = :userId OR (r.productionLot.organization.organizationId = :orgId AND :orgId IS NOT NULL))")
    Page<MilestoneReminder> findRemindersForUserOrOrganization(
            @Param("userId") UUID userId,
            @Param("orgId") UUID orgId,
            Pageable pageable);

    /**
     * Tìm tất cả các nhắc việc theo lô và trạng thái.
     */
    List<MilestoneReminder> findByProductionLot_IdAndStatus(
            UUID lotId,
            MilestoneReminderStatus status);

    /**
     * Tìm các nhắc việc theo lô, mốc và trạng thái.
     */
    List<MilestoneReminder> findByProductionLot_IdAndMilestone_IdAndStatus(
            UUID lotId,
            Long milestoneId,
            MilestoneReminderStatus status);

    /**
     * Tìm các nhắc việc của người dùng theo trạng thái, sắp xếp theo số ngày quá hạn giảm dần.
     */
    List<MilestoneReminder> findByUser_UserIdAndStatusOrderByOverdueDaysDesc(
            UUID userId,
            MilestoneReminderStatus status);

    /**
     * Tìm danh sách nhắc việc của tổ chức theo trạng thái, sắp xếp theo số ngày quá hạn giảm dần.
     */
    List<MilestoneReminder> findByProductionLot_Organization_OrganizationIdAndStatusOrderByOverdueDaysDesc(
            UUID orgId,
            MilestoneReminderStatus status);

    /**
     * Phân trang nhắc việc theo người dùng.
     */
    Page<MilestoneReminder> findByUser_UserId(
            UUID userId,
            Pageable pageable);

    /**
     * Phân trang nhắc việc theo người dùng và trạng thái.
     */
    Page<MilestoneReminder> findByUser_UserIdAndStatus(
            UUID userId,
            MilestoneReminderStatus status,
            Pageable pageable);

    /**
     * Phân trang nhắc việc theo tổ chức.
     */
    Page<MilestoneReminder> findByProductionLot_Organization_OrganizationId(
            UUID orgId,
            Pageable pageable);

    /**
     * Phân trang nhắc việc theo tổ chức và trạng thái.
     */
    Page<MilestoneReminder> findByProductionLot_Organization_OrganizationIdAndStatus(
            UUID orgId,
            MilestoneReminderStatus status,
            Pageable pageable);

    /**
     * Phân trang nhắc việc theo trạng thái.
     */
    Page<MilestoneReminder> findByStatus(
            MilestoneReminderStatus status,
            Pageable pageable);

    /**
     * Phân trang nhắc việc theo lô sản xuất.
     */
    Page<MilestoneReminder> findByProductionLot_Id(
            UUID lotId,
            Pageable pageable);

    /**
     * Phân trang nhắc việc theo lô sản xuất và trạng thái.
     */
    Page<MilestoneReminder> findByProductionLot_IdAndStatus(
            UUID lotId,
            MilestoneReminderStatus status,
            Pageable pageable);
}
