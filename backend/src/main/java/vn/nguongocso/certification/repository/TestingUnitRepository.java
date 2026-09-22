package vn.nguongocso.certification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.nguongocso.certification.entity.TestingUnit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho danh mục đơn vị kiểm nghiệm (TestingUnit).
 */
public interface TestingUnitRepository
                extends JpaRepository<TestingUnit, UUID> {
        /**
         * Tìm đơn vị kiểm nghiệm theo tên (không phân biệt hoa/thường).
         */
        Optional<TestingUnit> findByNameIgnoreCase(String name);

        /**
         * Tìm đơn vị kiểm nghiệm trùng tên (loại trừ một ID, dùng khi cập nhật).
         */
        Optional<TestingUnit> findByNameIgnoreCaseAndIdNot(String name, UUID id);

        /**
         * Lấy trang danh sách đơn vị kiểm nghiệm lọc theo trạng thái hiệu lực có phân trang.
         */
        Page<TestingUnit> findByIsActive(Boolean isActive, Pageable pageable);

        /**
         * Lấy danh sách đơn vị kiểm nghiệm còn hiệu lực, sắp xếp theo tên.
         * Dùng cho danh sách lựa chọn khi tạo yêu cầu kiểm nghiệm.
         */
        List<TestingUnit> findByIsActiveTrueOrderByNameAsc();
}