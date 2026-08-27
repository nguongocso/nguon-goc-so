package vn.nguongocso.farm.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.entity.LotAssignment;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.LotAssignmentRepository;

/**
 * Service quản lý phân công thành viên vào lô sản xuất.
 *
 * <p>
 * Hiện tại module chỉ phục vụ nghiệp vụ vô hiệu hóa thành viên
 * (NCL-01-CN-009 / QTN-32): xác định các lô chưa hoàn thành đang
 * phân công cho thành viên và chuyển giao phân công cho người thay thế.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LotAssignmentService {

    /**
     * Trạng thái lô được coi là "chưa hoàn thành" khi kiểm soát việc
     * vô hiệu hóa thành viên. Lô đã chốt vòng đời (CLOSED) hoặc đã bị
     * loại (REJECTED, RECALLED) không chặn việc vô hiệu hóa (quyết định D-4).
     */
    public static final List<ProductionLotStatus> UNFINISHED_LOT_STATUSES = List.of(
            ProductionLotStatus.DRAFT,
            ProductionLotStatus.PENDING,
            ProductionLotStatus.APPROVED,
            ProductionLotStatus.HARVESTED,
            ProductionLotStatus.PREPROCESSED,
            ProductionLotStatus.PACKAGED);

    private final LotAssignmentRepository lotAssignmentRepository;

    /**
     * Lấy các phân công còn hiệu lực của thành viên vào các lô chưa hoàn
     * thành trong tổ chức.
     *
     * @param organizationId ID tổ chức (scope đa tổ chức)
     * @param userId         ID thành viên cần kiểm tra
     * @return danh sách phân công còn hiệu lực vào lô chưa hoàn thành
     */
    public List<LotAssignment> findUnfinishedAssignments(UUID organizationId, UUID userId) {
        return lotAssignmentRepository.findActiveByUserOrganizationAndLotStatusIn(
                userId,
                organizationId,
                UNFINISHED_LOT_STATUSES);
    }

    /**
     * Chuyển toàn bộ phân công còn hiệu lực của {@code fromUserId} sang
     * {@code replacementUser} trong cùng tổ chức.
     *
     * <p>
     * Bản ghi phân công cũ chỉ bị vô hiệu hóa (active = FALSE, ghi nhận
     * thời điểm và người release) chứ không xóa, sau đó tạo bản ghi phân
     * công mới cho người thay thế với cùng lô. Phương thức phải được gọi
     * trong transaction của nghiệp vụ gọi để "chuyển giao + vô hiệu hóa
     * thành viên" là nguyên tố (atomic).
     * </p>
     *
     * @param organizationId ID tổ chức
     * @param fromUserId     ID thành viên đang bị vô hiệu hóa
     * @param replacementUser người thay thế (đã validate trước khi gọi)
     * @param transferredBy  người thao tác chuyển giao
     * @return số lô được chuyển giao
     */
    @Transactional
    public int transferActiveAssignments(
            UUID organizationId,
            UUID fromUserId,
            User replacementUser,
            CustomUserDetails transferredBy) {

        List<LotAssignment> activeAssignments = lotAssignmentRepository
                .findByUser_UserIdAndOrganization_OrganizationIdAndActiveTrue(fromUserId, organizationId);

        if (activeAssignments.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        User operator = transferredBy.getUser();

        for (LotAssignment assignment : activeAssignments) {
            releaseAssignment(assignment, operator, now);
            createReplacementAssignment(assignment, replacementUser, operator, now);
        }

        log.info("Chuyển giao phân công lô: orgId={}, fromUserId={}, toUserId={}, lots={}",
                organizationId, fromUserId, replacementUser.getUserId(), activeAssignments.size());

        return activeAssignments.size();
    }

    /** Vô hiệu hóa bản ghi phân công cũ, giữ lại làm lịch sử. */
    private void releaseAssignment(LotAssignment assignment, User releasedBy, LocalDateTime releasedAt) {
        assignment.setActive(Boolean.FALSE);
        assignment.setReleasedAt(releasedAt);
        assignment.setReleasedBy(releasedBy);
        lotAssignmentRepository.save(assignment);
    }

    /**
     * Tạo bản ghi phân công mới cho người thay thế trên cùng lô.
     * Organization lấy từ bản ghi cũ để đảm bảo phân công mới không lệch
     * tổ chức.
     */
    private void createReplacementAssignment(
            LotAssignment source,
            User replacementUser,
            User assignedBy,
            LocalDateTime assignedAt) {

        LotAssignment newAssignment = LotAssignment.builder()
                .productionLot(source.getProductionLot())
                .user(replacementUser)
                .organization(source.getOrganization())
                .active(Boolean.TRUE)
                .assignedBy(assignedBy)
                .assignedAt(assignedAt)
                .build();

        lotAssignmentRepository.save(newAssignment);
    }
}
