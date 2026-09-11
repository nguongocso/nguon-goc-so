package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.certification.entity.SystemConfiguration;

/**
 * Repository thao tác với bảng cấu hình hệ thống (NCL-11-CN-004).
 */
@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {
}
