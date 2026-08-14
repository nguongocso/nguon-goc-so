package vn.nguongocso.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.organization.entity.Invitation;
import vn.nguongocso.organization.enums.InvitationStatus;

/**
 * Repository cho thực thể Invitation.
 */
@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    /**
     * Tìm lời mời theo token.
     *
     * @param token token của lời mời
     * @return Optional chứa lời mời nếu tìm thấy, hoặc rỗng nếu không tìm thấy
     */
    Optional<Invitation> findByToken(String token);

    /**
     * Tìm tất cả lời mời theo email, ID tổ chức và trạng thái.
     *
     * @param email          email của người nhận lời mời
     * @param organizationId ID của tổ chức
     * @param status         trạng thái của lời mời
     * @return danh sách lời mời phù hợp
     */
    List<Invitation> findByEmailAndOrganizationOrganizationIdAndStatus(
            String email,
            UUID organizationId,
            InvitationStatus status);
}
