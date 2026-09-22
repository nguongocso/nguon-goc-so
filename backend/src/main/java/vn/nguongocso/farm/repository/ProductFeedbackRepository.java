package vn.nguongocso.farm.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;

/**
 * Repository thao tác dữ liệu phản hồi sản phẩm.
*/
public interface ProductFeedbackRepository extends JpaRepository<ProductFeedback, UUID>, JpaSpecificationExecutor<ProductFeedback> {
    /** Lấy tất cả phản hồi sản phẩm mới nhất trước. */
    Page<ProductFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Lấy phản hồi sản phẩm theo tổ chức mới nhất trước. */
    Page<ProductFeedback> findByProductionLot_Organization_OrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    /** Tìm phản hồi sản phẩm theo ID và tổ chức. */
    Optional<ProductFeedback> findByIdAndProductionLot_Organization_OrganizationId(UUID id, UUID organizationId);

    /** Tìm phản hồi sản phẩm theo mã tra cứu băm. */
    Optional<ProductFeedback> findByLookupCodeHash(String lookupCodeHash);

    /** Kiểm tra tồn tại phản hồi theo mã tra cứu băm. */
    boolean existsByLookupCodeHash(String lookupCodeHash);

    /** Kiểm tra tồn tại phản hồi theo mã truy vết và mức độ. */
    boolean existsByTraceCode_IdAndSeverity(UUID traceCodeId, ProductFeedbackSeverity severity);

    /** Tìm các phản ánh nghiêm trọng chưa đóng theo danh sách lô sản xuất. */
    @Query("SELECT pf FROM ProductFeedback pf WHERE pf.productionLot.id IN :lotIds AND pf.severity IN :severities AND pf.status <> :closedStatus")
    List<ProductFeedback> findSeriousOpenFeedbacksByLotIds(
            @Param("lotIds") Collection<UUID> lotIds,
            @Param("severities") Collection<ProductFeedbackSeverity> severities,
            @Param("closedStatus") ProductFeedbackStatus closedStatus);

    /** Tìm danh sách phản ánh theo tổ chức và danh sách trạng thái. */
    List<ProductFeedback> findByProductionLot_Organization_OrganizationIdAndStatusIn(
            UUID organizationId,
            Collection<ProductFeedbackStatus> statuses);

    /** Tìm danh sách tất cả phản ánh theo danh sách trạng thái. */
    List<ProductFeedback> findByStatusIn(
            Collection<ProductFeedbackStatus> statuses);
}

