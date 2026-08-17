package vn.nguongocso.help.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.help.entity.HelpContent;

/**
 * Repository quản lý nội dung hướng dẫn sử dụng (NCL-01-CN-006).
 */
@Repository
public interface HelpContentRepository extends JpaRepository<HelpContent, UUID> {

    /** Lấy nội dung hướng dẫn khớp màn hình + vai trò cụ thể. */
    List<HelpContent> findByScreenKeyAndRoleCodeOrderBySortOrderAsc(String screenKey, String roleCode);

    /** Lấy tất cả nội dung hướng dẫn của một màn hình (dự phòng tổng quát). */
    List<HelpContent> findByScreenKeyOrderBySortOrderAsc(String screenKey);
}