package vn.nguongocso.organization.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationType;

/**
 * Repository cho thực thể Organization.
 */
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Tìm các tổ chức được map vào một trong các đơn vị hành chính (khớp cột
     * {@code province_id} hoặc {@code commune_id}) — phục vụ lọc báo cáo theo
     * địa bàn của VT-05 / bộ lọc unitIds.
     *
     * @param provinceIds danh sách ID đơn vị cấp tỉnh
     * @param communeIds  danh sách ID đơn vị cấp xã
     * @return danh sách tổ chức thuộc các địa bàn trên
     */
    List<Organization> findByProvince_IdInOrCommune_IdIn(Collection<UUID> provinceIds, Collection<UUID> communeIds);

    /**
     * Kiểm tra xem tổ chức có tồn tại theo mã hay không.
     *
     * @param code mã của tổ chức
     * @return true nếu tổ chức tồn tại, false nếu không
     */
    boolean existsByCode(String code);

    /**
     * Kiểm tra xem tổ chức có tồn tại theo tên hay không.
     *
     * @param name tên của tổ chức
     * @return true nếu tổ chức tồn tại, false nếu không
     */
    boolean existsByName(String name);

    /**
     * Tìm tổ chức theo mã.
     *
     * @param code mã của tổ chức
     * @return Optional chứa tổ chức nếu tìm thấy, hoặc rỗng nếu không tìm thấy
     */
    Optional<Organization> findByCode(String code);

    /**
     * Kiểm tra xem tổ chức có tồn tại theo email hay không.
     *
     * @param email email của tổ chức
     * @return true nếu tổ chức tồn tại, false nếu không
     */
    boolean existsByEmail(@Email(message = "Email tổ chức không đúng định dạng") String email);

    /**
     * Tìm các tổ chức theo địa bàn.
     */
    List<Organization> findByAddressContainingIgnoreCase(String region);

    /**
     * Tìm tổ chức theo email.
     *
     * @param email email của tổ chức
     * @return Optional chứa tổ chức nếu tìm thấy, hoặc rỗng nếu không tìm thấy
     */
    Optional<Organization> findByEmail(String email);

    /**
     * Tìm tổ chức theo số điện thoại.
     *
     * @param phone số điện thoại của tổ chức
     * @return Optional chứa tổ chức nếu tìm thấy, hoặc rỗng nếu không tìm thấy
     */
    Optional<Organization> findByPhone(String phone);

    /**
     * Tìm tất cả các tổ chức theo loại, ngoại trừ tổ chức có ID được chỉ định.
     *
     * @param type           loại của tổ chức
     * @param organizationId ID của tổ chức cần loại trừ
     * @return danh sách các tổ chức phù hợp
     */
    List<Organization> findByTypeAndOrganizationIdNot(OrganizationType type, UUID organizationId);
}
