package vn.nguongocso.certification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.certification.entity.TestingUnit;

/**
 * Repository cho danh mục đơn vị kiểm nghiệm.
 */
public interface TestingUnitRepository extends JpaRepository<TestingUnit, UUID> {

    /**
     * Tìm đơn vị kiểm nghiệm theo tên (không phân biệt hoa/thường).
     */
    Optional<TestingUnit> findByNameIgnoreCase(String name);

    /**
     * Tìm đơn vị kiểm nghiệm trùng tên (loại trừ một ID, dùng khi cập nhật).
     */
    Optional<TestingUnit> findByNameIgnoreCaseAndIdNot(String name, UUID id);

    /**
     * Lấy trang danh sách đơn vị kiểm nghiệm lọc theo trạng thái hiệu lực.
     */
    Page<TestingUnit> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * Lấy danh sách đơn vị kiểm nghiệm còn hiệu lực, sắp xếp theo tên.
     * Dùng cho dropdown chọn đơn vị khi tạo yêu cầu kiểm nghiệm.
     */
    java.util.List<TestingUnit> findByIsActiveTrueOrderByNameAsc();
}