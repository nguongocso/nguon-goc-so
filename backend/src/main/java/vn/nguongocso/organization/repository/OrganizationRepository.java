package vn.nguongocso.organization.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.validation.constraints.Email;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;

/** Repository cho thực thể Organization. */
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    /** Tìm các tổ chức theo địa bàn cấp tỉnh hoặc cấp xã. */
    List<Organization> findByProvince_IdInOrCommune_IdIn(Collection<UUID> provinceIds, Collection<UUID> communeIds);

    /** Kiểm tra xem tổ chức có tồn tại theo mã hay không. */
    boolean existsByCode(String code);

    /** Kiểm tra xem tổ chức có tồn tại theo tên hay không. */
    boolean existsByName(String name);

    /** Tìm tổ chức theo mã. */
    Optional<Organization> findByCode(String code);

    /** Kiểm tra xem tổ chức có tồn tại theo email hay không. */
    boolean existsByEmail(@Email(message = "Email tổ chức không đúng định dạng") String email);

    /** Tìm các tổ chức theo địa chỉ địa bàn. */
    List<Organization> findByAddressContainingIgnoreCase(String region);

    /** Tìm tổ chức theo email. */
    Optional<Organization> findByEmail(String email);

    /** Tìm tổ chức theo số điện thoại. */
    Optional<Organization> findByPhone(String phone);

    /** Tìm tất cả các tổ chức theo loại, ngoại trừ tổ chức chỉ định. */
    List<Organization> findByTypeAndOrganizationIdNot(OrganizationType type, UUID organizationId);

    @Query("SELECT o FROM Organization o WHERE o.type = :type AND o.status = :status "
            + "AND o.organizationId <> :organizationId AND (LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(o.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Organization> searchActiveEnterprisePartners(
            @Param("type") OrganizationType type,
            @Param("status") OrganizationStatus status,
            @Param("organizationId") UUID organizationId,
            @Param("keyword") String keyword,
            Pageable pageable);

    /** Tìm các tổ chức theo trạng thái, loại trừ một tổ chức cụ thể. */
    List<Organization> findByStatusAndOrganizationIdNot(OrganizationStatus status, UUID organizationId);
}
