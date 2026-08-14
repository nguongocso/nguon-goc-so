package vn.nguongocso.certification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.certification.entity.Standard;

/**
 * Repository thao tác Standard.
 */
public interface StandardRepository extends JpaRepository<Standard, UUID> {
    /**
     * Tìm tiêu chuẩn theo tên (không phân biệt hoa thường).
     * Dùng khi tạo mới để kiểm tra trùng tên.
     */
    Optional<Standard> findByNameIgnoreCase(String name);

    /**
     * Tìm tiêu chuẩn theo tên, loại trừ bản ghi hiện tại.
     * Dùng khi cập nhật để kiểm tra trùng tên.
     */
    Optional<Standard> findByNameIgnoreCaseAndIdNot(String name, UUID id);

    /**
     * Lấy danh sách tiêu chuẩn theo trạng thái hoạt động.
     */
    Page<Standard> findByIsActive(Boolean isActive, Pageable pageable);

}