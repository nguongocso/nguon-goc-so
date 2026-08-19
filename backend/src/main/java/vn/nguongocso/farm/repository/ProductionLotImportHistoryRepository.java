package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.farm.entity.ProductionLotImportHistory;

/**
 * Repository thao tác với lịch sử nhập dữ liệu lô sản xuất.
 */
public interface ProductionLotImportHistoryRepository
                extends JpaRepository<ProductionLotImportHistory, UUID> {
        /**
         * Lấy lịch sử nhập dữ liệu của một tổ chức, sắp xếp mới nhất trước.
         *
         * @param organizationId ID tổ chức
         * @return Danh sách lịch sử nhập
         */
        List<ProductionLotImportHistory> findByOrganization_OrganizationIdOrderByImportedAtDesc(
                        UUID organizationId);

}