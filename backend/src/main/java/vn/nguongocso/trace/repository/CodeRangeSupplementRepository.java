package vn.nguongocso.trace.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.trace.entity.CodeRangeSupplementRequest;
import vn.nguongocso.trace.enums.CodeRangeSupplementStatus;

/** Repository quản lý yêu cầu cấp bổ sung dải mã truy xuất. */
@Repository
public interface CodeRangeSupplementRepository extends JpaRepository<CodeRangeSupplementRequest, UUID> {

    /** Lấy danh sách yêu cầu theo trạng thái có phân trang. */
    Page<CodeRangeSupplementRequest> findByStatus(CodeRangeSupplementStatus status, Pageable pageable);

    /** Lấy danh sách yêu cầu của một tổ chức có phân trang. */
    Page<CodeRangeSupplementRequest> findByOrganization_OrganizationId(UUID organizationId, Pageable pageable);

    /** Lấy danh sách yêu cầu của một tổ chức theo trạng thái có phân trang. */
    Page<CodeRangeSupplementRequest> findByOrganization_OrganizationIdAndStatus(
            UUID organizationId, CodeRangeSupplementStatus status, Pageable pageable);

    /** Kiểm tra một tổ chức đã có yêu cầu đang chờ duyệt hay chưa. */
    boolean existsByOrganization_OrganizationIdAndStatus(UUID organizationId, CodeRangeSupplementStatus status);
}
