package vn.nguongocso.alert.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.alert.entity.ActivityLogExportJob;

/** Truy cập yêu cầu xuất nhật ký hoạt động theo phạm vi tổ chức. */
public interface ActivityLogExportJobRepository extends JpaRepository<ActivityLogExportJob, UUID> {
    Optional<ActivityLogExportJob> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
